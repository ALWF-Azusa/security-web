package com.wolf.securityweb.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scan_alerts")
@Data
public class ScanAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "scan_report_id")
    @JsonBackReference
    private ScanReport scanReport;

    private String pluginId;
    private String alertName;
    private String riskLevel;
    private int riskCount;

    // 🔥 修改：加上 @Lob，並明確指定 columnDefinition
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String solution;

    private String cweId;
    private String wascId;

    @OneToMany(mappedBy = "scanAlert", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<AlertInstance> alertInstances = new ArrayList<>();
}