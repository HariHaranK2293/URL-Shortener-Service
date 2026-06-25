package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.entity.Url;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUrlRequest testRequest;
    private Url testUrl;

    @BeforeEach
    void setUp() {
        testRequest = CreateUrlRequest.builder()
            .originalUrl("https://www.example.com")
            .apiKey("test-key")
            .description("Test URL")
            .build();

        testUrl = Url.builder()
            .id(1L)
            .shortCode("abc123")
            .originalUrl("https://www.example.com")
            .apiKey("test-key")
            .clickCount(0L)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testCreateShortUrl() throws Exception {
        var response = com.urlshortener.dto.CreateUrlResponse.builder()
            .id(1L)
            .shortCode("abc123")
            .shortUrl("http://localhost:8080/redirect/abc123")
            .originalUrl("https://www.example.com")
            .description("Test URL")
            .build();

        when(urlService.createShortUrl(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").value("abc123"));

        verify(urlService, times(1)).createShortUrl(any());
    }

    @Test
    void testRedirectUrl() throws Exception {
        when(urlService.getUrlByShortCode("abc123")).thenReturn(testUrl);
        doNothing().when(urlService).recordClick(any(), anyString(), anyString(), anyString());

        mockMvc.perform(get("/api/v1/redirect/abc123"))
            .andExpect(status().isMovedPermanently())
            .andExpect(header().exists("Location"));

        verify(urlService, times(1)).getUrlByShortCode("abc123");
    }

    @Test
    void testGetUrlDetails() throws Exception {
        var response = com.urlshortener.dto.CreateUrlResponse.builder()
            .id(1L)
            .shortCode("abc123")
            .originalUrl("https://www.example.com")
            .build();

        when(urlService.getUrlDetails("abc123", "test-key")).thenReturn(response);

        mockMvc.perform(get("/api/v1/urls/abc123")
            .header("X-API-Key", "test-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value("abc123"));
    }

    @Test
    void testDeleteUrl() throws Exception {
        doNothing().when(urlService).deleteUrl("abc123", "test-key");

        mockMvc.perform(delete("/api/v1/urls/abc123")
            .header("X-API-Key", "test-key"))
            .andExpect(status().isNoContent());

        verify(urlService, times(1)).deleteUrl("abc123", "test-key");
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Service is running"));
    }
}
