package com.leomaya.transaction.repository;

import com.leomaya.transaction.model.Transaction;
import com.leomaya.transaction.repository.TransactionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@DataJpaTest
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void should_save_transaction() {
        Transaction transaction = Transaction.builder().amount(100.0).timestamp(new Date().getTime()).build();
        Transaction response = transactionRepository.save(transaction);
        assertNotNull(response);
        assertEquals(transaction.getTimestamp(), response.getTimestamp());
    }

    @Test
    public void should_find_transactions_from_timestamp_range() {
        long currentTimestamp = new Date().getTime();
        long minuteAgoTimestamp = currentTimestamp - (1 * 60 * 1000);

        createTransactionWithTimestamp(new Date().getTime()); // now
        createTransactionWithTimestamp(minuteAgoTimestamp); // 1 minute ago
        createTransactionWithTimestamp(new Date().getTime() - (2 * 60 * 1000)); // 2 minutes ago

        assertEquals(transactionRepository.findAllByTimestampBetween(minuteAgoTimestamp, currentTimestamp).size(), 2);

        createTransactionWithTimestamp(new Date().getTime() - (61 * 1000)); // 1 minutes ago
        createTransactionWithTimestamp(new Date().getTime() - (61 * 1000)); // 1 minutes ago
        createTransactionWithTimestamp(new Date().getTime() - (60 * 1000)); // 2 minutes ago

        assertEquals(transactionRepository.findAllByTimestampBetween(minuteAgoTimestamp, currentTimestamp).size(), 3);
    }

    private void createTransactionWithTimestamp(Long timestamp) {
        Transaction transaction = Transaction.builder().amount(100.0).timestamp(timestamp).build();
        transactionRepository.save(transaction);
    }
}
