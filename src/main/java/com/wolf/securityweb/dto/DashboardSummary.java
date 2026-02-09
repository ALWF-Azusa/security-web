package com.wolf.securityweb.dto;
import lombok.Data;

@Data
public class DashboardSummary {
    private int high;
    private int medium;
    private int low;
    private int informational;
    private int falsePositives; // 目前你的資料庫還沒存這個，暫時會是 0
}