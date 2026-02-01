package com.wolf.securityweb.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "scan_alerts")
public class ScanAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 關聯回主報告 (Foreign Key)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_report_id", nullable = false)
    @ToString.Exclude // 避免 Lombok 造成無限迴圈
    private ScanReport scanReport;

    @Column(name = "plugin_id")
    private String pluginId;

    @Column(name = "alert_name")
    private String alertName;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "risk_count")
    private Integer riskCount;

    @Column(columnDefinition = "TEXT") // 支援長文字
    private String description;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(name = "cwe_id")
    private String cweId;

    @Column(name = "wasc_id")
    private String wascId;

    // 關聯設定：一個 Alert 有多個 Instances
    @OneToMany(mappedBy = "scanAlert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlertInstance> alertInstances = new ArrayList<>();
}