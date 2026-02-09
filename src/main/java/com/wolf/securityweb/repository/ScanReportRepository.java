package com.wolf.securityweb.repository;

import com.wolf.securityweb.model.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // 1. 記得加這個 Import

@Repository
public interface ScanReportRepository extends JpaRepository<ScanReport, Long> {

    @Query("SELECT r FROM ScanReport r WHERE r.id IN (SELECT MAX(r2.id) FROM ScanReport r2 GROUP BY r2.siteUrl)")
    List<ScanReport> findLatestReportsForEachSite();

    // === 2. 新增這行：用來找舊檔案 ===
    // 透過「網址」和「掃描時間」來找出是否已經有這份報告
    Optional<ScanReport> findBySiteUrlAndGeneratedOn(String siteUrl, LocalDateTime generatedOn);
}