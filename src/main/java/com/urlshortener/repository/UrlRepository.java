package com.urlshortener.repository;

import com.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByCustomAlias(String customAlias);

    Optional<Url> findByShortCodeAndIsActiveTrue(String shortCode);

    @Query("SELECT u FROM Url u WHERE u.apiKey = :apiKey ORDER BY u.createdAt DESC")
    List<Url> findByApiKey(@Param("apiKey") String apiKey);

    @Query("SELECT u FROM Url u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    List<Url> findExpiredUrls(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM Url u WHERE u.apiKey = :apiKey AND u.isActive = true")
    Long countActiveUrlsByApiKey(@Param("apiKey") String apiKey);

    @Query("SELECT u FROM Url u WHERE u.isActive = true ORDER BY u.clickCount DESC LIMIT :limit")
    List<Url> findTopUrlsByClicks(@Param("limit") int limit);

    void deleteByIdAndApiKey(Long id, String apiKey);
}
