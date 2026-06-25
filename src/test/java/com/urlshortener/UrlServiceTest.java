package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.CreateUrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.AnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private UrlService urlService;

    private CreateUrlRequest testRequest;
    private Url testUrl;

    @BeforeEach
    void setUp() {
        testRequest = CreateUrlRequest.builder()
            .originalUrl("https://www.example.com")
            .apiKey("test-api-key-123")
            .description("Test URL")
            .build();

        testUrl = Url.builder()
            .id(1L)
            .shortCode("abc123")
            .originalUrl("https://www.example.com")
            .apiKey("test-api-key-123")
            .clickCount(0L)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testCreateShortUrl_Success() {
        when(shortCodeGenerator.isValidUrl(anyString())).thenReturn(true);
        when(shortCodeGenerator.normalizeUrl(anyString())).thenReturn("https://www.example.com");
        when(urlRepository.countActiveUrlsByApiKey(anyString())).thenReturn(0L);
        when(shortCodeGenerator.generateShortCode()).thenReturn("abc123");
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any())).thenReturn(testUrl);

        CreateUrlResponse response = urlService.createShortUrl(testRequest);

        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals("https://www.example.com", response.getOriginalUrl());
        verify(urlRepository, times(1)).save(any());
    }

    @Test
    void testCreateShortUrl_InvalidUrl() {
        when(shortCodeGenerator.isValidUrl(anyString())).thenReturn(false);

        assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(testRequest));
    }

    @Test
    void testGetUrlByShortCode_Success() {
        when(urlRepository.findByShortCodeAndIsActiveTrue("abc123")).thenReturn(Optional.of(testUrl));
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);

        Url result = urlService.getUrlByShortCode("abc123");

        assertNotNull(result);
        assertEquals("abc123", result.getShortCode());
        assertEquals("https://www.example.com", result.getOriginalUrl());
    }

    @Test
    void testGetUrlByShortCode_NotFound() {
        when(urlRepository.findByShortCodeAndIsActiveTrue("invalid")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> urlService.getUrlByShortCode("invalid"));
    }

    @Test
    void testDeleteUrl_Success() {
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(testUrl));

        urlService.deleteUrl("abc123", "test-api-key-123");

        assertFalse(testUrl.getIsActive());
        verify(urlRepository, times(1)).save(any());
    }

    @Test
    void testDeleteUrl_Unauthorized() {
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(testUrl));

        assertThrows(Exception.class, () -> urlService.deleteUrl("abc123", "wrong-api-key"));
    }
}
