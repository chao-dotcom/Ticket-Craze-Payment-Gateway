package com.yourname.paymentgateway.controller;

import com.yourname.paymentgateway.dto.response.DailySummaryResponse;
import com.yourname.paymentgateway.dto.response.RevenueReportResponse;
import com.yourname.paymentgateway.security.MerchantDetails;
import com.yourname.paymentgateway.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting and analytics APIs")
@SecurityRequirement(name = "API-Key")
public class ReportController {
    
    private final ReportService reportService;
    
    @GetMapping("/daily-summary")
    @Operation(summary = "Get daily transaction summary")
    public ResponseEntity<List<DailySummaryResponse>> getDailySummary(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        List<DailySummaryResponse> summary = reportService.getDailySummary(
            merchantDetails.getMerchant().getId(),
            startDate,
            endDate
        );
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/revenue")
    @Operation(summary = "Get revenue report by date range")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        RevenueReportResponse report = reportService.getRevenueReport(
            merchantDetails.getMerchant().getId(),
            startDate,
            endDate
        );
        
        return ResponseEntity.ok(report);
    }
}

