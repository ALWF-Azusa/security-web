package com.wolf.securityweb.service;

import com.wolf.securityweb.dto.RiskGroup;
import com.wolf.securityweb.model.ScanAlert;
import com.wolf.securityweb.dto.DashboardSummary;
import com.wolf.securityweb.dto.SiteRiskSummary;
import com.wolf.securityweb.model.ScanReport;
import com.wolf.securityweb.repository.ScanReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanReportService {

    private final ScanReportRepository repository;

    // === 1. 計算全域統計 (保持不變) ===
    public DashboardSummary getGlobalSummary() {
        List<ScanReport> latestReports = repository.findLatestReportsForEachSite();

        DashboardSummary summary = new DashboardSummary();

        for (ScanReport report : latestReports) {
            DashboardSummary reportSummary = convertToRiskCounts(report);
            summary.setHigh(summary.getHigh() + reportSummary.getHigh());
            summary.setMedium(summary.getMedium() + reportSummary.getMedium());
            summary.setLow(summary.getLow() + reportSummary.getLow());
            summary.setInformational(summary.getInformational() + reportSummary.getInformational());
            summary.setFalsePositives(0);
        }
        return summary;
    }

    // === 2. 計算網站列表 (保持不變) ===
    public List<SiteRiskSummary> getSiteSummaries() {
        List<ScanReport> latestReports = repository.findLatestReportsForEachSite();
        List<SiteRiskSummary> siteSummaries = new ArrayList<>();

        for (ScanReport report : latestReports) {
            SiteRiskSummary siteSummary = new SiteRiskSummary();

            // 設定報告 ID
            siteSummary.setReportId(report.getId());

            siteSummary.setUrl(report.getSiteUrl());
            siteSummary.setGeneratedOn(report.getGeneratedOn());
            siteSummary.setRiskCounts(convertToRiskCounts(report));

            siteSummaries.add(siteSummary);
        }

        // 依照日期排序
        siteSummaries.sort((a, b) -> {
            if (b.getGeneratedOn() == null) return -1;
            if (a.getGeneratedOn() == null) return 1;
            return b.getGeneratedOn().compareTo(a.getGeneratedOn());
        });

        return siteSummaries;
    }

    // === 3. 風險過濾方法 (🔥 已修正變數名稱) ===
    public List<RiskGroup> getSitesByRiskLevel(String level) {
        // 🔥 修正這裡：原本寫 reportRepository，改成 repository
        List<ScanReport> latestReports = repository.findLatestReportsForEachSite();
        List<RiskGroup> result = new ArrayList<>();

        for (ScanReport report : latestReports) {
            List<String> matchingAlerts = new ArrayList<>();

            if (report.getScanAlerts() != null) {
                for (ScanAlert alert : report.getScanAlerts()) {
                    if (alert.getRiskLevel() != null && alert.getRiskLevel().equalsIgnoreCase(level)) {

                        // 去重複邏輯
                        if (!matchingAlerts.contains(alert.getAlertName())) {
                            matchingAlerts.add(alert.getAlertName());
                        }

                    }
                }
            }

            if (!matchingAlerts.isEmpty()) {
                RiskGroup group = new RiskGroup();
                group.setSiteUrl(report.getSiteUrl());
                group.setReportId(report.getId());
                group.setAlertNames(matchingAlerts);
                result.add(group);
            }
        }
        return result;
    }

    // === 4. 私有工具方法 (保持不變) ===
    private DashboardSummary convertToRiskCounts(ScanReport report) {
        DashboardSummary riskCounts = new DashboardSummary();
        riskCounts.setHigh(report.getSummaryHigh() != null ? report.getSummaryHigh() : 0);
        riskCounts.setMedium(report.getSummaryMedium() != null ? report.getSummaryMedium() : 0);
        riskCounts.setLow(report.getSummaryLow() != null ? report.getSummaryLow() : 0);
        riskCounts.setInformational(report.getSummaryInfo() != null ? report.getSummaryInfo() : 0);
        riskCounts.setFalsePositives(0);
        return riskCounts;
    }
}