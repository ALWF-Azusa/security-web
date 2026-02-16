package com.wolf.securityweb.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_contact_info")
@Data
public class SystemContactInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ipAddress;      // 網路IP位址
    private String systemName;     // 系統名稱
    private String department;     // 業務單位

    @Column(unique = true)
    private String rawUrl;         // 網頁位址 (如: https://account.ncut.edu.tw/)

    // 🔥 這個很重要，用來跟 ZAP 報告的網域做比對 (如: account.ncut.edu.tw)
    @Column(unique = true)
    private String domainName;

    private String notes;          // 備註與用途
    private String managerName;    // 管理人
    private String managerEmail;   // 管理人信箱
    private String vendor;         // 委外廠商
}