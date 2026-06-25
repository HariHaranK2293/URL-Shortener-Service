package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {

    private Long urlId;
    private String shortCode;
    private String originalUrl;
    private Long totalClicks;
    private LocalDateTime createdAt;
    private Map<String, Long> clicksByDay;
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByDeviceType;
    private List<ClickDetail> recentClicks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClickDetail {
        private String ipAddress;
        private String userAgent;
        private String referrer;
        private LocalDateTime clickedAt;
        private String country;
        private String deviceType;
    }
}
