package com.wolf.securityweb.controller;

import com.wolf.securityweb.dto.RiskGroup;
import com.wolf.securityweb.model.ScanReport;
import com.wolf.securityweb.repository.ScanReportRepository;
import com.wolf.securityweb.service.ReportService;
import com.wolf.securityweb.service.ScanReportService; // 🔥 記得匯入這個！

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // 注入 Repository (用來抓取單一報告詳情)
    @Autowired
    private ScanReportRepository reportRepository;

    // 🔥 注入 ScanReportService (用來處理風險過濾)
    @Autowired
    private ScanReportService scanReportService;

    // === 1. 上傳報告 (POST) ===
    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("請選擇一個檔案！");
        }

        try {
            ScanReport savedReport = reportService.parseAndSaveUpload(file);
            return ResponseEntity.ok("上傳成功！報告已存入資料庫，ID: " + savedReport.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("處理失敗: " + e.getMessage());
        }
    }

    // === 2. 取得單一報告詳情 (GET /api/reports/{id}) ===
    @GetMapping("/{id}")
    public ResponseEntity<ScanReport> getReport(@PathVariable Long id) {
        Optional<ScanReport> report = reportRepository.findById(id);
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === 3. 取得特定風險等級列表 (GET /api/reports/risk/{level}) ===
    @GetMapping("/risk/{level}")
    public ResponseEntity<List<RiskGroup>> getRiskDetails(@PathVariable String level) {
        // 呼叫 ScanReportService 裡的過濾方法
        return ResponseEntity.ok(scanReportService.getSitesByRiskLevel(level));
    }
}