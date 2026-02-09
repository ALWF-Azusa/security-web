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

    // ✅ 改好了：URL 容易過長，設為 TEXT
    @Column(columnDefinition = "TEXT")
    private String url;

    // ⚠️ 剛剛你原本的程式碼這裡少掉了 parameter，記得加回來！
    private String parameter;

    // ✅ 改好了：攻擊字串非常長，設為 LONGTEXT
    @Column(columnDefinition = "LONGTEXT")
    private String attack;

    // ✅ 改好了：證據字串非常長，設為 LONGTEXT
    @Column(columnDefinition = "LONGTEXT")
    private String evidence;
}