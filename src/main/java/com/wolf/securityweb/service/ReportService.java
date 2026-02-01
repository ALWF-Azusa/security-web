package com.wolf.securityweb.service;

import com.wolf.securityweb.model.*;
import com.wolf.securityweb.repository.ScanReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class ReportService {

    @Autowired
    private ScanReportRepository reportRepository;

    @Transactional
    public ScanReport parseAndSaveUpload(MultipartFile file) throws IOException {
        // 1. 解析 HTML
        ZapReportParser.Report rawReport = ZapReportParser.parse(file.getInputStream());

        // 2. 轉成主表 Entity
        ScanReport entity = new ScanReport();
        entity.setSiteUrl(rawReport.meta.site);
        entity.setZapVersion(rawReport.meta.zapVersion);
        entity.setGeneratedOn(LocalDateTime.now()); // 暫時用現在時間
        entity.setSummaryHigh(rawReport.summary.high);
        entity.setSummaryMedium(rawReport.summary.medium);
        entity.setSummaryLow(rawReport.summary.low);
        entity.setSummaryInfo(rawReport.summary.informational);

        // 3. 轉成 Alert Entity
        for (ZapReportParser.AlertItem rawAlert : rawReport.alerts) {
            ScanAlert alertEntity = new ScanAlert();
            alertEntity.setScanReport(entity);
            alertEntity.setPluginId(rawAlert.pluginId);
            alertEntity.setAlertName(rawAlert.name);
            alertEntity.setRiskLevel(rawAlert.risk);
            alertEntity.setRiskCount(rawAlert.count);
            alertEntity.setDescription(rawAlert.description);
            alertEntity.setSolution(rawAlert.solution);
            alertEntity.setCweId(rawAlert.cweId);
            alertEntity.setWascId(rawAlert.wascId);

            // 4. 轉成 Instance Entity
            for (ZapReportParser.Instance rawInst : rawAlert.instances) {
                AlertInstance instEntity = new AlertInstance();
                instEntity.setScanAlert(alertEntity);
                instEntity.setUrl(rawInst.url);
                instEntity.setMethod(rawInst.method);
                instEntity.setParameter(rawInst.parameter);
                instEntity.setAttack(rawInst.attack);
                instEntity.setEvidence(rawInst.evidence);

                alertEntity.getAlertInstances().add(instEntity);
            }
            entity.getScanAlerts().add(alertEntity);
        }

        // 5. 存檔
        return reportRepository.save(entity);
    }
}