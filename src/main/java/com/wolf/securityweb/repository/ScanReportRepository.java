package com.wolf.securityweb.repository;

import com.wolf.securityweb.model.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 1. 記得匯入這個 Query
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScanReportRepository extends JpaRepository<ScanReport, Long> {

    // 既有的方法 (檢查重複用)
    Optional<ScanReport> findBySiteUrlAndGeneratedOn(String siteUrl, LocalDateTime generatedOn);

    // 🔥 補上這一段！這就是電腦找不到的那個符號
    // 原理：先分組找出每個網站最大的 ID (最新)，再把整筆資料抓出來
    @Query("SELECT r FROM ScanReport r WHERE r.id IN (SELECT MAX(r2.id) FROM ScanReport r2 GROUP BY r2.siteUrl)")
    List<ScanReport> findLatestReportsForEachSite();
}