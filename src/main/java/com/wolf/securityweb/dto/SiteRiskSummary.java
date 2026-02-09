package com.wolf.securityweb.dto;

import lombok.Data;

@Data
public class SiteRiskSummary {
    private String url;
    private DashboardSummary riskCounts; //每個網站自己也有一組統計
}