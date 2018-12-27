package com.leomaya.transaction.controller;

import com.leomaya.transaction.model.Statistics;
import com.leomaya.transaction.model.Transaction;
import com.leomaya.transaction.service.TransactionService;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transaction")
    public Transaction create(@RequestBody TransactionRequest request) {

        return transactionService.create(
                Transaction.builder()
                        .amount(request.getAmount())
                        .timestamp(request.getTimestamp())
                .build());
    }

    @GetMapping("/statistics")
    public Statistics getStatistics() {
        return transactionService.getStatistics();
    }


    @Data
    static class TransactionRequest {
        Double amount;
        Long timestamp;
    }
}
