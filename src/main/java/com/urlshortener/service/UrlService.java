package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.CreateUrlResponse;
import com.urlshortener.entity.Analytics;
import com.urlshortener.entity.Url;
import com.urlshortener.exception.*;
import com.urlshortener.repository.AnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final AnalyticsRepository analyticsRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.url-shortener.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.url-shortener.max-urls-per-user:1000}")
    private Long maxUrlsPerUser;

    private static final String CACHE_PREFIX = "url:";
    private static final String CLICK_COUNT_PREFIX = "clicks:";
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    /**
     * Create a shortened URL with analytics tracking
     */
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        // Validate API key
        validateApiKey(request.getApiKey());

        // Validate URL
        String originalUrl = request.getOriginalUrl();
        if (!shortCodeGenerator.isValidUrl(originalUrl)) {
            throw new InvalidUrlException("Invalid URL format: " + originalUrl);
        }
        originalUrl = shortCodeGenerator.normalizeUrl(originalUrl);

        // Check user's URL limit
        Long activeUrlCount = urlRepository.countActiveUrlsByApiKey(request.getApiKey());
        if (activeUrlCount >= maxUrlsPerUser) {
            throw new RuntimeException("Maximum URL limit (" + maxUrlsPerUser + ") reached");
        }

        // Generate short code or use custom alias
        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            if (urlRepository.findByCustomAlias(request.getCustomAlias()).isPresent()) {
                throw new DuplicateUrlException("Custom alias already in use: " + request.getCustomAlias());
            }
            shortCode = request.getCustomAlias();
        } else {
            shortCode = generateUniqueShortCode();
        }

        // Create URL entity
        Url url = Url.builder()
            .shortCode(shortCode)
            .originalUrl(originalUrl)
            .customAlias(request.getCustomAlias())
            .description(request.getDescription())
            .expiresAt(request.getExpiresAt())
            .apiKey(request.getApiKey())
            .clickCount(0L)
            .isActive(true)
            .build();

        Url savedUrl = urlRepository.save(url);
        log.info("Created short URL: {} -> {}", shortCode, originalUrl);

        // Cache the mapping in Redis
        cacheUrlMapping(savedUrl);

        return CreateUrlResponse.builder()
            .id(savedUrl.getId())
            .shortCode(savedUrl.getShortCode())
            .shortUrl(baseUrl + "/redirect/" + shortCode)
            .originalUrl(savedUrl.getOriginalUrl())
            .customAlias(savedUrl.getCustomAlias())
            .createdAt(savedUrl.getCreatedAt())
            .expiresAt(savedUrl.getExpiresAt())
            .description(savedUrl.getDescription())
            .build();
    }

    /**
     * Retrieve original URL by short code with caching
     */
    @Cacheable(value = "urls", key = "#shortCode")
    public Url getUrlByShortCode(String shortCode) {
        log.debug("Fetching URL for short code: {}", shortCode);

        // Try Redis first (super fast)
        Url cachedUrl = getFromRedisCache(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache hit for short code: {}", shortCode);
            return cachedUrl;
        }

        // Fall back to database
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));

        // Check expiry
        if (url.isExpired()) {
            deactivateUrl(url);
            throw new UrlExpiredException("URL has expired: " + shortCode);
        }

        // Cache for future requests
        cacheUrlMapping(url);
        return url;
    }

    /**
     * Record a click with analytics data (async for performance)
     */
    @Async
    public void recordClick(Url url, String ipAddress, String userAgent, String referrer) {
        try {
            // Increment counter in Redis (atomic, sub-millisecond)
            incrementClickCount(url.getId());

            // Create analytics record (async batch processing)
            Analytics analytics = Analytics.builder()
                .urlId(url.getId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referrer(referrer)
                .clickedAt(LocalDateTime.now())
                .build();

            analyticsRepository.save(analytics);
            log.debug("Recorded click for URL ID: {}", url.getId());
        } catch (Exception e) {
            log.error("Error recording click for URL ID: " + url.getId(), e);
        }
    }

    /**
     * Get analytics for a URL
     */
    public AnalyticsResponse getAnalytics(Long urlId, String apiKey) {
        Url url = urlRepository.findById(urlId)
            .orElseThrow(() -> new ResourceNotFoundException("URL not found"));

        // Authorize: only owner can view analytics
        if (!url.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("Unauthorized access to analytics");
        }

        // Get click count from Redis or database
        Long totalClicks = getClickCount(url.getId());

        // Get recent clicks
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<Analytics> recentClicks = analyticsRepository.findRecentClicksForUrl(urlId, sevenDaysAgo);

        // Build response
        return AnalyticsResponse.builder()
            .urlId(url.getId())
            .shortCode(url.getShortCode())
            .originalUrl(url.getOriginalUrl())
            .totalClicks(totalClicks)
            .createdAt(url.getCreatedAt())
            .recentClicks(convertAnalyticsToDto(recentClicks))
            .build();
    }

    /**
     * Delete a URL (soft delete)
     */
    @CacheEvict(value = "urls", key = "#shortCode")
    public void deleteUrl(String shortCode, String apiKey) {
        Url url = urlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));

        if (!url.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("Unauthorized to delete this URL");
        }

        deactivateUrl(url);
        clearRedisCache(shortCode);
        log.info("Deleted URL: {}", shortCode);
    }

    /**
     * Get URL details
     */
    public CreateUrlResponse getUrlDetails(String shortCode, String apiKey) {
        Url url = getUrlByShortCode(shortCode);

        if (!url.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("Unauthorized access");
        }

        return CreateUrlResponse.builder()
            .id(url.getId())
            .shortCode(url.getShortCode())
            .shortUrl(baseUrl + "/redirect/" + shortCode)
            .originalUrl(url.getOriginalUrl())
            .customAlias(url.getCustomAlias())
            .createdAt(url.getCreatedAt())
            .expiresAt(url.getExpiresAt())
            .description(url.getDescription())
            .build();
    }

    // ==================== Private Helper Methods ====================

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            shortCode = shortCodeGenerator.generateShortCode();
            attempts++;
        } while (urlRepository.findByShortCode(shortCode).isPresent() && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new RuntimeException("Failed to generate unique short code");
        }

        return shortCode;
    }

    private void cacheUrlMapping(Url url) {
        redisTemplate.opsForValue().set(
            CACHE_PREFIX + url.getShortCode(),
            url,
            1,
            TimeUnit.HOURS
        );
    }

    private Url getFromRedisCache(String shortCode) {
        Object cached = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
        if (cached instanceof Url) {
            return (Url) cached;
        }
        return null;
    }

    private void clearRedisCache(String shortCode) {
        redisTemplate.delete(CACHE_PREFIX + shortCode);
        redisTemplate.delete(CLICK_COUNT_PREFIX + shortCode);
    }

    private void incrementClickCount(Long urlId) {
        String key = CLICK_COUNT_PREFIX + urlId;
        redisTemplate.opsForValue().increment(key);
    }

    private Long getClickCount(Long urlId) {
        String key = CLICK_COUNT_PREFIX + urlId;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }

    private void deactivateUrl(Url url) {
        url.setIsActive(false);
        urlRepository.save(url);
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new UnauthorizedException("API key is required");
        }
        // In production, validate against a user table or external auth service
    }

    private List<AnalyticsResponse.ClickDetail> convertAnalyticsToDto(List<Analytics> analytics) {
        return analytics.stream()
            .map(a -> AnalyticsResponse.ClickDetail.builder()
                .ipAddress(a.getIpAddress())
                .userAgent(a.getUserAgent())
                .referrer(a.getReferrer())
                .clickedAt(a.getClickedAt())
                .country(a.getCountry())
                .deviceType(a.getDeviceType())
                .build())
            .toList();
    }
}
