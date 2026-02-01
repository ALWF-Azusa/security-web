package com.wolf.securityweb.controller;

import com.wolf.securityweb.model.ScanReport;
import com.wolf.securityweb.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController // 告訴 Spring 這是一個 API 入口
@RequestMapping("/api/reports") // 設定網址開頭為 /api/reports
@CrossOrigin(origins = "*") // 允許所有來源連線 (開發方便，解決跨域問題)
public class ReportController {

    @Autowired
    private ReportService reportService;

    // 定義上傳接口：POST /api/reports/upload
    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("請選擇一個檔案！");
        }

        try {
            // 呼叫 Service 處理解析與存檔
            ScanReport savedReport = reportService.parseAndSaveUpload(file);

            // 回傳成功訊息跟 ID
            return ResponseEntity.ok("上傳成功！報告已存入資料庫，ID: " + savedReport.getId());

        } catch (Exception e) {
            e.printStackTrace(); // 在後端印出錯誤日誌
            return ResponseEntity.internalServerError().body("處理失敗: " + e.getMessage());
        }
    }
}