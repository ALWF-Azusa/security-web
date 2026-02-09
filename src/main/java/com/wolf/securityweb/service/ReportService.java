package com.wolf.securityweb.service;

import com.wolf.securityweb.dto.RiskGroup; // 👈 記得加這個
import java.util.ArrayList;
import java.util.List;
import com.wolf.securityweb.model.*;
import com.wolf.securityweb.repository.ScanReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ScanReportRepository reportRepository;

    @Transactional
    public ScanReport parseAndSaveUpload(MultipartFile file) throws IOException {

        // 1. 解析 HTML
        ZapReportParser.Report rawReport = ZapReportParser.parse(file.getInputStream());

        // 2. 清洗網址 (解決 Unknown 和重複問題)
        String rawSiteName = rawReport.meta.site;
        String siteName = cleanSiteUrl(rawSiteName, file.getOriginalFilename());

        // 3. 解析時間 (🔥 修改重點：解決 Invalid Date)
        // 嘗試從報告中抓時間，抓不到就用現在時間
        LocalDateTime genTime = parseZapDate(rawReport.meta.generatedOn);

        // 4. 檢查重複並覆蓋 (用清洗過的網址 + 解析出的時間)
        reportRepository.findBySiteUrlAndGeneratedOn(siteName, genTime)
                .ifPresent(existing -> {
                    reportRepository.delete(existing);
                    reportRepository.flush();
                });

        // 5. 建立 Entity
        ScanReport entity = new ScanReport();
        entity.setSiteUrl(siteName);
        entity.setZapVersion(rawReport.meta.zapVersion);
        entity.setGeneratedOn(genTime); // 設定正確的時間
        entity.setSummaryHigh(rawReport.summary.high);
        entity.setSummaryMedium(rawReport.summary.medium);
        entity.setSummaryLow(rawReport.summary.low);
        entity.setSummaryInfo(rawReport.summary.informational);

        // 6. 轉換 Alerts
        if (rawReport.alerts != null) {
            for (ZapReportParser.AlertItem rawAlert : rawReport.alerts) {
                ScanAlert alertEntity = convertToScanAlert(rawAlert, entity);
                entity.getScanAlerts().add(alertEntity);
            }
        }

        return reportRepository.save(entity);
    }

    // === 輔助方法：轉換 Alert ===
    private ScanAlert convertToScanAlert(ZapReportParser.AlertItem rawAlert, ScanReport report) {
        ScanAlert alertEntity = new ScanAlert();
        alertEntity.setScanReport(report);
        alertEntity.setPluginId(rawAlert.pluginId);
        alertEntity.setAlertName(rawAlert.name);
        alertEntity.setRiskLevel(rawAlert.risk);
        alertEntity.setRiskCount(rawAlert.count != null ? rawAlert.count : 0);
        alertEntity.setDescription(rawAlert.description);
        alertEntity.setSolution(rawAlert.solution);
        alertEntity.setCweId(rawAlert.cweId);
        alertEntity.setWascId(rawAlert.wascId);

        if (rawAlert.instances != null) {
            for (ZapReportParser.Instance rawInst : rawAlert.instances) {
                AlertInstance instEntity = new AlertInstance();
                instEntity.setScanAlert(alertEntity);
                instEntity.setUrl(rawInst.url);
                instEntity.setMethod(rawInst.method);
                instEntity.setParameter(rawInst.parameter);
                instEntity.setAttack(rawInst.attack);
                instEntity.setEvidence(rawInst.evidence);

                if (alertEntity.getAlertInstances() == null) {
                    // Entity 初始化保護
                }
                alertEntity.getAlertInstances().add(instEntity);
            }
        }
        return alertEntity;
    }

    // === 🔥 核心修改 1：強力日期解析器 ===
    private LocalDateTime parseZapDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now(); // 沒寫日期就回傳現在
        }

        try {
            // ZAP 格式範例: "週四, 26 6月 2025 10:45:27"
            // 這種格式很難用標準 Formatter 解，我們手動拆解比較穩
            // 1. 先把 "Generated on" 去掉
            String cleanDate = dateStr.replace("Generated on", "").trim();

            // 2. 如果是中文格式，嘗試手動提取數字
            // 正則表達式抓取： (文字), (日) (月) (年) (時):(分):(秒)
            // 例如: 週四, 26 6月 2025 10:45:27
            // 群組:       1   2    3        4  5  6
            Pattern pattern = Pattern.compile(".*?,\\s*(\\d+)\\s*(\\d+)月\\s*(\\d+)\\s*(\\d+):(\\d+):(\\d+)");
            Matcher matcher = pattern.matcher(cleanDate);

            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                int minute = Integer.parseInt(matcher.group(5));
                int second = Integer.parseInt(matcher.group(6));
                return LocalDateTime.of(year, month, day, hour, minute, second);
            }

            // 3. 如果不是上面的格式，嘗試標準英文格式 (Thu, 26 Jun 2025 10:45:27)
            // 這裡可以視情況擴充，但目前先以中文為主

        } catch (Exception e) {
            System.err.println("解析日期失敗: " + dateStr + "，改用當前時間。");
        }

        return LocalDateTime.now(); // 所有解析失敗都回傳現在時間
    }

    // === 🔥 核心修改 2：網址清洗器 ===
    private String cleanSiteUrl(String rawUrl, String filename) {
        String url = rawUrl;
        String protocolTag = ""; // 用來存 (HTTP) 或 (HTTPS) 的標籤

        // 1. 【判斷協定】先檢查原始資料是 HTTP 還是 HTTPS
        if (url != null && !url.trim().isEmpty() && !url.equalsIgnoreCase("null")) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.startsWith("https://")) {
                protocolTag = " (HTTPS)";
            } else if (lowerUrl.startsWith("http://")) {
                protocolTag = " (HTTP)";
            }
        }

        // 2. 【救援行動】如果 ZAP 報告裡抓不到 Site 欄位 (Unknown)
        if (url == null || url.trim().isEmpty() || url.equalsIgnoreCase("null") || url.equalsIgnoreCase("Unknown Site")) {
            if (filename != null) {
                String cleanName = filename;
                // 去除 report-, zap- 等前綴
                cleanName = cleanName.replaceAll("(?i)^report[-_]?", "").replaceAll("(?i)^zap[-_]?", "");
                // 去除副檔名
                cleanName = cleanName.replaceAll("(?i)\\.(html|xml|json)$", "");
                url = cleanName;

                // 如果是從檔名救回來的，我們可能不知道協定，
                // 除非檔名裡有寫 (例如 report-http-site.html)，不然就保持空白或標記 (Unknown)
                if (protocolTag.isEmpty()) {
                    protocolTag = ""; // 或者可以設為 " (?)" 提醒使用者
                }
            } else {
                return "Unknown Site";
            }
        }

        url = url.trim();

        // 3. 【多網址處理】只取第一個
        if (url.contains(" ")) {
            url = url.split("\\s+")[0];
        }

        // 4. 【標準化】移除前面的 http:// 或 https:// (只保留網域)
        url = url.replaceAll("(?i)^https?://", "");

        // 5. 去除結尾斜線
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // 6. 去除標準 Port 號 (:80, :443)
        url = url.replaceAll(":80$", "").replaceAll(":443$", "");

        // 7. 防呆
        if (url.isEmpty()) {
            return "Unknown (" + filename + ")";
        }

        // 8. 🔥 回傳：網域 + 協定標籤
        // 結果範例: "rac3.ncut.edu.tw (HTTPS)"
        return url + protocolTag;
    }
    public List<RiskGroup> getSitesByRiskLevel(String level) {
        // 1. 抓出所有網站最新報告
        List<ScanReport> latestReports = reportRepository.findLatestReportsForEachSite();
        List<RiskGroup> result = new ArrayList<>();

        for (ScanReport report : latestReports) {
            List<String> matchingAlerts = new ArrayList<>();

            // 2. 遍歷這份報告的所有弱點，找符合等級的
            if (report.getScanAlerts() != null) {
                for (ScanAlert alert : report.getScanAlerts()) {
                    // 比對風險等級 (忽略大小寫，例如 "High" == "high")
                    if (alert.getRiskLevel() != null && alert.getRiskLevel().equalsIgnoreCase(level)) {
                        matchingAlerts.add(alert.getAlertName());
                    }
                }
            }

            // 3. 如果這個網站有這個等級的風險，就加入結果清單
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
}