package com.wolf.securityweb.service;

import com.wolf.securityweb.dto.RiskGroup;
import com.wolf.securityweb.model.*;
import com.wolf.securityweb.repository.ScanReportRepository;
import com.wolf.securityweb.repository.SystemContactInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 🔥 換成 Apache POI 的 Excel 套件
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ScanReportRepository reportRepository;
    private final SystemContactInfoRepository contactInfoRepository;

    @Transactional
    public ScanReport parseAndSaveUpload(MultipartFile file) throws IOException {
        ZapReportParser.Report rawReport = ZapReportParser.parse(file.getInputStream());

        String rawSiteName = rawReport.meta.site;
        String siteName = cleanSiteUrl(rawSiteName, file.getOriginalFilename());
        LocalDateTime genTime = parseZapDate(rawReport.meta.generatedOn);

        reportRepository.findBySiteUrlAndGeneratedOn(siteName, genTime)
                .ifPresent(existing -> {
                    reportRepository.delete(existing);
                    reportRepository.flush();
                });

        ScanReport entity = new ScanReport();
        entity.setSiteUrl(siteName);
        entity.setZapVersion(rawReport.meta.zapVersion);
        entity.setGeneratedOn(genTime);
        entity.setSummaryHigh(rawReport.summary.high);
        entity.setSummaryMedium(rawReport.summary.medium);
        entity.setSummaryLow(rawReport.summary.low);
        entity.setSummaryInfo(rawReport.summary.informational);

        if (rawReport.alerts != null) {
            for (ZapReportParser.AlertItem rawAlert : rawReport.alerts) {
                ScanAlert alertEntity = convertToScanAlert(rawAlert, entity);
                entity.getScanAlerts().add(alertEntity);
            }
        }

        return reportRepository.save(entity);
    }

    // === 🔥 核心修改：解析並儲存 Excel (.xlsx) 的方法 ===
    @Transactional
    public void parseAndSaveContactsExcel(MultipartFile file) throws Exception {
        // DataFormatter 可以安全地將任何 Excel 儲存格轉為字串
        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) { // 讀取 .xlsx 檔案

            // 取得第一個工作表
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) return; // 若為空檔案則跳過

            // 1. 讀取第一行(標題列)，動態記錄每個標題在哪一欄
            Row headerRow = rowIterator.next();
            Map<String, Integer> colMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerName = formatter.formatCellValue(cell).trim();
                colMap.put(headerName, cell.getColumnIndex());
            }

            // 2. 開始讀取資料列
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                try {
                    // 取得網址欄位的索引
                    Integer urlColIdx = colMap.get("*網頁位址(URL/服務埠）");
                    if (urlColIdx == null) continue;

                    String rawUrl = formatter.formatCellValue(row.getCell(urlColIdx));
                    if (rawUrl == null || rawUrl.trim().isEmpty()) continue;

                    // 萃取出純網域
                    String domain = rawUrl.replaceAll("(?i)^https?://", "")
                            .replaceAll("/.*$", "")
                            .replaceAll(":\\d+$", "")
                            .trim();

                    // 查詢或新增
                    SystemContactInfo info = contactInfoRepository.findByDomainName(domain).orElse(new SystemContactInfo());

                    info.setRawUrl(rawUrl);
                    info.setDomainName(domain);

                    // 寫入其他欄位 (動態透過標題名稱抓取對應欄位的資料)
                    if (colMap.containsKey("*對外網路IP位址")) info.setIpAddress(formatter.formatCellValue(row.getCell(colMap.get("*對外網路IP位址"))));
                    if (colMap.containsKey("*名稱")) info.setSystemName(formatter.formatCellValue(row.getCell(colMap.get("*名稱"))));
                    if (colMap.containsKey("*業務單位")) info.setDepartment(formatter.formatCellValue(row.getCell(colMap.get("*業務單位"))));
                    if (colMap.containsKey("備註與用途")) info.setNotes(formatter.formatCellValue(row.getCell(colMap.get("備註與用途"))));
                    if (colMap.containsKey("管理人")) info.setManagerName(formatter.formatCellValue(row.getCell(colMap.get("管理人"))));
                    if (colMap.containsKey("管理人信箱")) info.setManagerEmail(formatter.formatCellValue(row.getCell(colMap.get("管理人信箱"))));
                    if (colMap.containsKey("*委外廠商")) info.setVendor(formatter.formatCellValue(row.getCell(colMap.get("*委外廠商"))));

                    contactInfoRepository.save(info);

                } catch (Exception e) {
                    System.err.println("解析 Excel 資料列時發生錯誤，跳過該列: " + e.getMessage());
                }
            }
        }
    }

    // === 下方的輔助方法保持不變 ===
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

                if (alertEntity.getAlertInstances() == null) {}
                alertEntity.getAlertInstances().add(instEntity);
            }
        }
        return alertEntity;
    }

    private LocalDateTime parseZapDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            String cleanDate = dateStr.replace("Generated on", "").trim();
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
        } catch (Exception e) {
            System.err.println("解析日期失敗: " + dateStr + "，改用當前時間。");
        }
        return LocalDateTime.now();
    }

    private String cleanSiteUrl(String rawUrl, String filename) {
        String url = rawUrl;
        String protocolTag = "";
        if (url != null && !url.trim().isEmpty() && !url.equalsIgnoreCase("null")) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.startsWith("https://")) {
                protocolTag = " (HTTPS)";
            } else if (lowerUrl.startsWith("http://")) {
                protocolTag = " (HTTP)";
            }
        }
        if (url == null || url.trim().isEmpty() || url.equalsIgnoreCase("null") || url.equalsIgnoreCase("Unknown Site")) {
            if (filename != null) {
                String cleanName = filename;
                cleanName = cleanName.replaceAll("(?i)^report[-_]?", "").replaceAll("(?i)^zap[-_]?", "");
                cleanName = cleanName.replaceAll("(?i)\\.(html|xml|json)$", "");
                url = cleanName;
                if (protocolTag.isEmpty()) {
                    protocolTag = "";
                }
            } else {
                return "Unknown Site";
            }
        }
        url = url.trim();
        if (url.contains(" ")) {
            url = url.split("\\s+")[0];
        }
        url = url.replaceAll("(?i)^https?://", "");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url.replaceAll(":80$", "").replaceAll(":443$", "");
        if (url.isEmpty()) {
            return "Unknown (" + filename + ")";
        }
        return url + protocolTag;
    }
}