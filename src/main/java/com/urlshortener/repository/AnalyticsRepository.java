package com.urlshortener.repository;

import com.urlshortener.entity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    @Query("SELECT a FROM Analytics a WHERE a.urlId = :urlId ORDER BY a.clickedAt DESC")
    List<Analytics> findByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT a FROM Analytics a WHERE a.urlId = :urlId AND a.clickedAt >= :startDate ORDER BY a.clickedAt DESC")
    List<Analytics> findRecentClicksForUrl(@Param("urlId") Long urlId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(a) FROM Analytics a WHERE a.urlId = :urlId")
    Long countClicksByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT a.country, COUNT(a) as count FROM Analytics a WHERE a.urlId = :urlId GROUP BY a.country ORDER BY count DESC")
    List<Object[]> getClicksByCountry(@Param("urlId") Long urlId);

    @Query("SELECT a.deviceType, COUNT(a) as count FROM Analytics a WHERE a.urlId = :urlId GROUP BY a.deviceType ORDER BY count DESC")
    List<Object[]> getClicksByDeviceType(@Param("urlId") Long urlId);

    @Query("DELETE FROM Analytics a WHERE a.urlId = :urlId")
    void deleteByUrlId(@Param("urlId") Long urlId);
}
