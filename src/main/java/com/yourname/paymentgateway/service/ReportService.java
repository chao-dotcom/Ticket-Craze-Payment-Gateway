package com.yourname.paymentgateway.service;

import com.yourname.paymentgateway.dto.response.DailySummaryResponse;
import com.yourname.paymentgateway.dto.response.RevenueReportResponse;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.enums.TransactionStatus;
import com.yourname.paymentgateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final TransactionRepository transactionRepository;
    
    @Transactional(readOnly = true)
    public List<DailySummaryResponse> getDailySummary(Long merchantId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        // Get all transactions for the merchant in the date range
        List<Transaction> transactions = transactionRepository.findAll()
            .stream()
            .filter(t -> t.getMerchant().getId().equals(merchantId))
            .filter(t -> t.getCreatedAt().isAfter(start) && t.getCreatedAt().isBefore(end))
            .collect(Collectors.toList());
        
        // Group by date
        Map<LocalDate, List<Transaction>> transactionsByDate = transactions.stream()
            .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));
        
        List<DailySummaryResponse> summary = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Transaction> dayTransactions = transactionsByDate.getOrDefault(currentDate, List.of());
            
            long totalCount = dayTransactions.size();
            long completedCount = dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .count();
            long failedCount = dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();
            
            BigDecimal totalAmount = dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            summary.add(DailySummaryResponse.builder()
                .date(currentDate)
                .totalTransactions(totalCount)
                .completedTransactions(completedCount)
                .failedTransactions(failedCount)
                .totalAmount(totalAmount)
                .build());
            
            currentDate = currentDate.plusDays(1);
        }
        
        return summary;
    }
    
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(Long merchantId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        // Get all completed transactions for the merchant in the date range
        List<Transaction> transactions = transactionRepository.findAll()
            .stream()
            .filter(t -> t.getMerchant().getId().equals(merchantId))
            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
            .filter(t -> t.getCreatedAt().isAfter(start) && t.getCreatedAt().isBefore(end))
            .collect(Collectors.toList());
        
        BigDecimal totalRevenue = transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalTransactions = transactions.size();
        
        BigDecimal averageTransactionAmount = totalTransactions > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalTransactions), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;
        
        // Group by currency
        Map<String, BigDecimal> revenueByCurrency = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCurrency,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        
        return RevenueReportResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalRevenue(totalRevenue)
            .totalTransactions(totalTransactions)
            .averageTransactionAmount(averageTransactionAmount)
            .revenueByCurrency(revenueByCurrency)
            .build();
    }
}

