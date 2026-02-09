package com.wolf.securityweb.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "alert_instances")
@Data
public class AlertInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "scan_alert_id")
    @JsonBackReference
    private ScanAlert scanAlert;

    private String method;

    // 🔥 修改：全部加上 @Lob，這在處理大資料時最安全
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String url;

    private String parameter;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String attack;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String evidence;
}