package com.wolf.securityweb.model;

import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "scan_reports")
public class ScanReport {

    @Transient//@Transient 代表這個欄位不會在資料庫建立實體欄位，只是在記憶體中用來打包傳給前端
    private SystemContactInfo contactInfo;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_url", nullable = false)
    private String siteUrl;

    @Column(name = "generated_on")
    private LocalDateTime generatedOn;

    @Column(name = "zap_version")
    private String zapVersion;

    @Column(name = "summary_high")
    private Integer summaryHigh;

    @Column(name = "summary_medium")
    private Integer summaryMedium;

    @Column(name = "summary_low")
    private Integer summaryLow;

    @Column(name = "summary_info")
    private Integer summaryInfo;

    // 這行讓它自動產生建立時間
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // 關聯設定：一份報告有多個 Alerts
    @OneToMany(mappedBy = "scanReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScanAlert> scanAlerts = new ArrayList<>();
}