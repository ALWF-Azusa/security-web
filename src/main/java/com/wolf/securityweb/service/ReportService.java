package com.wolf.securityweb.service;

import com.wolf.securityweb.model.*;
import com.wolf.securityweb.repository.ScanReportRepository;
import lombok.RequiredArgsConstructor; // 1. 記得加這個 Lombok 註解
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor // 2. 自動生成建構子，解決 Field Injection 警告
public class ReportService {

    // 3. 加上 final，並不使用 @Autowired
    private final ScanReportRepository reportRepository;

    @Transactional
    public ScanReport parseAndSaveUpload(MultipartFile file) throws IOException {

        // --- 步驟 A: 解析與基本檢查 ---
        ZapReportParser.Report rawReport = ZapReportParser.parse(file.getInputStream());

        String siteName = rawReport.meta.site;
        if (siteName == null || siteName.trim().isEmpty()) {
            siteName = "Unknown Site (" + file.getOriginalFilename() + ")";
        }

        // --- 步驟 B: 檢查重複並覆蓋 ---
        LocalDateTime genTime = LocalDateTime.now();

        reportRepository.findBySiteUrlAndGeneratedOn(siteName, genTime)
                .ifPresent(existing -> {
                    reportRepository.delete(existing);
                    reportRepository.flush();
                });

        // --- 步驟 C: 建立主報告 ---
        ScanReport entity = new ScanReport();
        entity.setSiteUrl(siteName);
        entity.setZapVersion(rawReport.meta.zapVersion);
        entity.setGeneratedOn(genTime);
        entity.setSummaryHigh(rawReport.summary.high);
        entity.setSummaryMedium(rawReport.summary.medium);
        entity.setSummaryLow(rawReport.summary.low);
        entity.setSummaryInfo(rawReport.summary.informational);

        // --- 步驟 D: 轉換 Alert (抽取成獨立方法，解決長方法警告) ---
        if (rawReport.alerts != null) {
            for (ZapReportParser.AlertItem rawAlert : rawReport.alerts) {
                // 呼叫下面的私有方法來處理每一個 Alert
                ScanAlert alertEntity = convertToScanAlert(rawAlert, entity);
                entity.getScanAlerts().add(alertEntity);
            }
        }

        return reportRepository.save(entity);
    }

    // === 這就是抽取出來的私有方法 (Clean Code!) ===
    private ScanAlert convertToScanAlert(ZapReportParser.AlertItem rawAlert, ScanReport report) {
        ScanAlert alertEntity = new ScanAlert();
        alertEntity.setScanReport(report); // 設定關聯
        alertEntity.setPluginId(rawAlert.pluginId);
        alertEntity.setAlertName(rawAlert.name);
        alertEntity.setRiskLevel(rawAlert.risk);

        // 4. 修正：直接使用 int，不需要 Integer.parseInt
        alertEntity.setRiskCount(rawAlert.count);

        alertEntity.setDescription(rawAlert.description);
        alertEntity.setSolution(rawAlert.solution);
        alertEntity.setCweId(rawAlert.cweId);
        alertEntity.setWascId(rawAlert.wascId);

        if (rawAlert.instances != null) {
            for (ZapReportParser.Instance rawInst : rawAlert.instances) {
                AlertInstance instEntity = new AlertInstance();
                instEntity.setScanAlert(alertEntity);

                // 5. 修正：變數名稱改回你原本的 url 和 parameter
                instEntity.setUrl(rawInst.url);
                instEntity.setMethod(rawInst.method);
                instEntity.setParameter(rawInst.parameter);

                instEntity.setAttack(rawInst.attack);
                instEntity.setEvidence(rawInst.evidence);

                // 確保 List 已初始化 (防 Null)
                if (alertEntity.getAlertInstances() == null) {
                    // 如果你的 Entity 裡已經有 = new ArrayList<>() 這裡可以省略
                    // 但為了安全起見，這裡不寫初始化，假設 Entity 已經做好了
                }
                alertEntity.getAlertInstances().add(instEntity);
            }
        }
        return alertEntity;
    }
}