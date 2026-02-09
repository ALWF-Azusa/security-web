package com.wolf.securityweb.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SiteRiskSummary {

    // 🔥 新增這個欄位！
    // 這樣 Service 才能呼叫 setReportId(...)
    private Long reportId;

    private String url;
    private LocalDateTime generatedOn;
    private DashboardSummary riskCounts;
}