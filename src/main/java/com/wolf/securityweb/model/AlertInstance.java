package com.wolf.securityweb.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "alert_instances")
public class AlertInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 關聯回 Alert
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_alert_id", nullable = false)
    @ToString.Exclude
    private ScanAlert scanAlert;

    @Column(columnDefinition = "TEXT")
    private String url;

    private String method;

    private String parameter;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    private String attack;
}