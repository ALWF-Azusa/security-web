package com.wolf.securityweb.controller;

import com.wolf.securityweb.dto.DashboardSummary;
import com.wolf.securityweb.dto.SiteRiskSummary;
import com.wolf.securityweb.service.ScanReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // 告訴 Spring 這是一個 API 入口
@RequestMapping("/api/dashboard") // 所有的網址都以 /api/dashboard 開頭
public class DashboardController {

    @Autowired
    private ScanReportService scanReportService; // 呼叫剛剛寫好的主廚

    // 1. 給儀表板上方圓圈的資料
    // 網址：http://localhost:8080/api/dashboard/global
    @GetMapping("/global")
    public DashboardSummary getGlobalSummary() {
        return scanReportService.getGlobalSummary();
    }

    // 2. 給儀表板下方列表的資料
    // 網址：http://localhost:8080/api/dashboard/sites
    @GetMapping("/sites")
    public List<SiteRiskSummary> getSiteSummaries() {
        return scanReportService.getSiteSummaries();
    }
}