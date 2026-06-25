package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.CreateUrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlService urlService;

    /**
     * Create a shortened URL
     * POST /api/v1/shorten
     */
    @PostMapping("/shorten")
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request) {
        log.info("Creating short URL for: {}", request.getOriginalUrl());
        CreateUrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Redirect to original URL
     * GET /redirect/{shortCode}
     */
    @GetMapping("/redirect/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @PathVariable String shortCode,
            HttpServletRequest request) {
        log.info("Redirect request for: {}", shortCode);

        // Get URL from service
        Url url = urlService.getUrlByShortCode(shortCode);

        // Record click asynchronously
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");
        urlService.recordClick(url, ipAddress, userAgent, referrer);

        // Redirect
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
            .header("Location", url.getOriginalUrl())
            .build();
    }

    /**
     * Get URL details
     * GET /api/v1/urls/{shortCode}
     */
    @GetMapping("/urls/{shortCode}")
    public ResponseEntity<CreateUrlResponse> getUrlDetails(
            @PathVariable String shortCode,
            @RequestHeader("X-API-Key") String apiKey) {
        log.info("Fetching details for short code: {}", shortCode);
        CreateUrlResponse response = urlService.getUrlDetails(shortCode, apiKey);
        return ResponseEntity.ok(response);
    }

    /**
     * Get analytics for a shortened URL
     * GET /api/v1/analytics/{urlId}
     */
    @GetMapping("/analytics/{urlId}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable Long urlId,
            @RequestHeader("X-API-Key") String apiKey) {
        log.info("Fetching analytics for URL ID: {}", urlId);
        AnalyticsResponse response = urlService.getAnalytics(urlId, apiKey);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a shortened URL
     * DELETE /api/v1/urls/{shortCode}
     */
    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            @RequestHeader("X-API-Key") String apiKey) {
        log.info("Deleting URL: {}", shortCode);
        urlService.deleteUrl(shortCode, apiKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }

    // ==================== Helper Methods ====================

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
