package com.wolf.securityweb.service;

import com.wolf.securityweb.dto.DashboardSummary;
import com.wolf.securityweb.dto.SiteRiskSummary;
import com.wolf.securityweb.model.ScanReport;
import com.wolf.securityweb.repository.ScanReportRepository;
import lombok.RequiredArgsConstructor; // 1. 匯入這個
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor // 2. 加這個標籤，自動產生建構子注入 (解決 Field injection 警告)
public class ScanReportService {

    // 3. 把 @Autowired 拿掉，並加上 final
    private final ScanReportRepository repository;

    // === 1. 計算全域統計 ===
    public DashboardSummary getGlobalSummary() {
        List<ScanReport> latestReports = repository.findLatestReportsForEachSite();

        DashboardSummary summary = new DashboardSummary();

        for (ScanReport report : latestReports) {
            // 利用抽取出來的方法來計算，這裡看起來就清爽多了！
            DashboardSummary reportSummary = convertToRiskCounts(report);

            summary.setHigh(summary.getHigh() + reportSummary.getHigh());
            summary.setMedium(summary.getMedium() + reportSummary.getMedium());
            summary.setLow(summary.getLow() + reportSummary.getLow());
            summary.setInformational(summary.getInformational() + reportSummary.getInformational());
            summary.setFalsePositives(0);
        }
        return summary;
    }

    // === 2. 計算網站列表 ===
    public List<SiteRiskSummary> getSiteSummaries() {
        List<ScanReport> latestReports = repository.findLatestReportsForEachSite();
        List<SiteRiskSummary> siteSummaries = new ArrayList<>();

        for (ScanReport report : latestReports) {
            SiteRiskSummary siteSummary = new SiteRiskSummary();
            siteSummary.setUrl(report.getSiteUrl());

            // 4. 使用抽取出來的私有方法 (解決 Extract method 警告)
            siteSummary.setRiskCounts(convertToRiskCounts(report));

            siteSummaries.add(siteSummary);
        }

        return siteSummaries;
    }

    // === 3. 私有工具方法 (這就是抽取出來的邏輯) ===
    private DashboardSummary convertToRiskCounts(ScanReport report) {
        DashboardSummary riskCounts = new DashboardSummary();
        // 使用三元運算子防止 Null，並統一在這裡處理
        riskCounts.setHigh(report.getSummaryHigh() != null ? report.getSummaryHigh() : 0);
        riskCounts.setMedium(report.getSummaryMedium() != null ? report.getSummaryMedium() : 0);
        riskCounts.setLow(report.getSummaryLow() != null ? report.getSummaryLow() : 0);
        riskCounts.setInformational(report.getSummaryInfo() != null ? report.getSummaryInfo() : 0);
        riskCounts.setFalsePositives(0);
        return riskCounts;
    }
}