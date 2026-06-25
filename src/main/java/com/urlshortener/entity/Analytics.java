package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics", indexes = {
    @Index(name = "idx_url_id", columnList = "url_id"),
    @Index(name = "idx_clicked_at", columnList = "clicked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "CLOB")
    private String userAgent;

    @Column(name = "referrer", columnDefinition = "CLOB")
    private String referrer;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @PrePersist
    protected void onCreate() {
        clickedAt = LocalDateTime.now();
    }
}
