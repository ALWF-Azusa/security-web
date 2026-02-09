package com.wolf.securityweb.dto;

import lombok.Data;
import java.util.List;

@Data
public class RiskGroup {
    private String siteUrl;
    private Long reportId;
    private List<String> alertNames; // 只存該等級的攻擊名稱 (例如: SQL Injection)
}