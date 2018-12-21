package com.leomaya.transaction.repository;

import com.leomaya.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository  extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByTimestampBetween(long minuteAgoTimestamp, long currentTimestamp);
}
