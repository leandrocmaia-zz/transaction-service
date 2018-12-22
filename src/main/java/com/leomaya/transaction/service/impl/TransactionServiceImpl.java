package com.leomaya.transaction.service.impl;

import com.leomaya.transaction.model.Statistics;
import com.leomaya.transaction.model.Transaction;
import com.leomaya.transaction.repository.TransactionRepository;
import com.leomaya.transaction.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private TransactionRepository transactionRepository;
    private Map<Long, Transaction> cache;
    private  final int TRANSACTION_CACHE_MILLISECONDS = 60 * 1000; // 60s

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;

        // in a real-world production application, in case the database isn't in-memory anymore.
        cache = getLastMinuteTransactions();

    }


    @Override
    public Transaction create(Transaction transaction) {
        transactionRepository.save(transaction);
        Long transactionTimestamp = transaction.getTimestamp();
        long currentTimestamp = new Date().getTime();
        long minuteAgoTimestamp = currentTimestamp - (TRANSACTION_CACHE_MILLISECONDS);
        if (transactionTimestamp <= currentTimestamp && transactionTimestamp >= minuteAgoTimestamp) {
            cache.putIfAbsent(transaction.getId(), transaction);
        }
        return transaction;
    }

    @Override
    public Statistics getStatistics() {
        DoubleSummaryStatistics stat = cache.values().stream().mapToDouble(Transaction::getAmount).summaryStatistics();
        return Statistics
            .builder()
            .sum(stat.getSum())
            .avg(stat.getAverage())
            .max(stat.getMax())
            .min(stat.getMin())
            .count(stat.getCount())
            .build();
    }

    private Map<Long, Transaction> getLastMinuteTransactions() {
        long currentTimestamp = new Date().getTime();
        long minuteAgoTimestamp = currentTimestamp - (TRANSACTION_CACHE_MILLISECONDS);
        List<Transaction> transactions = transactionRepository.findAllByTimestampBetween(currentTimestamp, minuteAgoTimestamp);
        return transactions.stream().collect(Collectors.toConcurrentMap(Transaction::getId, Function.identity()));
    }

    /**
     * evict cache of transactions older than 60s to make getLastMinuteTransactions O(1)
     */
    @Override
    @Scheduled(fixedDelay = 1 * 1000)
    public void evictCache() {
        log.info("Evicting cache...");
        cache.entrySet().removeIf(entry -> entry.getValue().getTimestamp() < new Date().getTime() - (TRANSACTION_CACHE_MILLISECONDS));
    }

}
