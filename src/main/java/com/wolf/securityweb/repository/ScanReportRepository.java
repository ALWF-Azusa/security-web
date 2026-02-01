package com.wolf.securityweb.repository;

import com.wolf.securityweb.model.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScanReportRepository extends JpaRepository<ScanReport, Long> {
    // 繼承 JpaRepository 後，這裡雖然是空的，但已經擁有所有資料庫操作功能了
}