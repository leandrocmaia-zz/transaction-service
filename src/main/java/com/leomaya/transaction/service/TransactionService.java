package com.leomaya.transaction.service;

import com.leomaya.transaction.model.Statistics;
import com.leomaya.transaction.model.Transaction;

public interface TransactionService {
    Transaction create(Transaction transaction);
    Statistics getStatistics();
}
