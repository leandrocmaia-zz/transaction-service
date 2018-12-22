package com.leomaya.transaction.repository;

import com.leomaya.transaction.model.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@DataJpaTest
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void should_save_transaction() {
        Transaction transaction = Transaction.builder().amount(100.0).timestamp(Instant.now().toEpochMilli()).build();
        Transaction response = transactionRepository.save(transaction);
        assertNotNull(response);
        assertEquals(transaction.getTimestamp(), response.getTimestamp());
    }

    @Test
    public void should_find_transactions_from_timestamp_range() {
        long currentTimestamp = Instant.now().toEpochMilli();
        long minuteAgoTimestamp = Instant.now().minusSeconds(60).toEpochMilli();

        createTransactionWithTimestamp(Instant.now().toEpochMilli()); // now
        createTransactionWithTimestamp(minuteAgoTimestamp); // 1 minute ago
        createTransactionWithTimestamp(Instant.now().minusSeconds(120).toEpochMilli()); // 2 minutes ago

        assertEquals(transactionRepository.findAllByTimestampBetween(minuteAgoTimestamp, currentTimestamp).size(), 2);

        createTransactionWithTimestamp(Instant.now().minusSeconds(61).toEpochMilli()); // older
        createTransactionWithTimestamp(Instant.now().minusSeconds(61).toEpochMilli()); // older
        createTransactionWithTimestamp(Instant.now().minusSeconds(60).toEpochMilli()); // new

        assertEquals(transactionRepository.findAllByTimestampBetween(minuteAgoTimestamp, currentTimestamp).size(), 3);
    }

    private void createTransactionWithTimestamp(Long timestamp) {
        Transaction transaction = Transaction.builder().amount(100.0).timestamp(timestamp).build();
        transactionRepository.save(transaction);
    }
}
