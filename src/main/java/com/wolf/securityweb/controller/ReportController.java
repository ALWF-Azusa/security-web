package com.wolf.securityweb.controller;

import com.wolf.securityweb.dto.RiskGroup;
import com.wolf.securityweb.model.ScanReport;
import com.wolf.securityweb.model.SystemContactInfo;
import com.wolf.securityweb.repository.ScanReportRepository;
import com.wolf.securityweb.repository.SystemContactInfoRepository;
import com.wolf.securityweb.service.ReportService;
import com.wolf.securityweb.service.ScanReportService;

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

    @Autowired
    private ScanReportRepository reportRepository;

    @Autowired
    private ScanReportService scanReportService;

    // 🔥 新增：注入負責人的資料庫
    @Autowired
    private SystemContactInfoRepository contactInfoRepository;

    // === 1. 上傳 ZAP HTML 報告 ===
    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("請選擇一個檔案！");
        try {
            ScanReport savedReport = reportService.parseAndSaveUpload(file);
            return ResponseEntity.ok("上傳成功！報告已存入資料庫，ID: " + savedReport.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("處理失敗: " + e.getMessage());
        }
    }

    // === 2. 🔥 新增：上傳負責人名單 (CSV) ===
    @PostMapping("/upload-contacts")
    public ResponseEntity<?> uploadContactsExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("請選擇一個 Excel 檔案！");
        try {
            // 🔥 方法名稱改成這個：
            reportService.parseAndSaveContactsExcel(file);
            return ResponseEntity.ok("負責人名單 Excel 匯入成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("匯入失敗: " + e.getMessage());
        }
    }

    // === 3. 取得單一報告詳情 (🔥 修改：加入聯絡人資料) ===
    @GetMapping("/{id}")
    public ResponseEntity<ScanReport> getReport(@PathVariable Long id) {
        Optional<ScanReport> reportOpt = reportRepository.findById(id);

        if (reportOpt.isPresent()) {
            ScanReport report = reportOpt.get();

            // 1. 去除 (HTTPS) 標籤，例如拿到 "ohr.ncut.edu.tw:8888"
            String domainToMatch = report.getSiteUrl().split(" ")[0].trim();

            // 🔥 2. 新增這行：把 Port 號拔掉 (例如把 :8888 刪除)，變成純網域 "ohr.ncut.edu.tw"
            domainToMatch = domainToMatch.replaceAll(":\\d+$", "");

            // 3. 去資料庫找負責人，這樣兩邊的字串就完美一致了！
            contactInfoRepository.findByDomainName(domainToMatch)
                    .ifPresent(report::setContactInfo);

            return ResponseEntity.ok(report);
        }
        return ResponseEntity.notFound().build();
    }

    // === 4. 取得特定風險等級列表 ===
    @GetMapping("/risk/{level}")
    public ResponseEntity<List<RiskGroup>> getRiskDetails(@PathVariable String level) {
        return ResponseEntity.ok(scanReportService.getSitesByRiskLevel(level));
    }
}