package com.leomaya.transaction.service;

import com.leomaya.transaction.model.Statistics;
import com.leomaya.transaction.model.Transaction;
import com.leomaya.transaction.repository.TransactionRepository;
import com.leomaya.transaction.service.impl.TransactionServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {

    TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;


    @Before
    public void setUp() throws Exception {
        when(transactionRepository.findAllByTimestampBetween(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        transactionService = new TransactionServiceImpl(transactionRepository);
    }

    @Test
    public void should_create_transaction() {
        when(transactionRepository.save(any())).thenReturn(any());

        Transaction transaction = createTransactionWithAmountAndTimestamp(1l,100.0, new Date().getTime());
        assertNotNull(transaction);
    }


    @Test
    public void should_get_correct_statistics() throws InterruptedException {
        when(transactionRepository.save(any())).thenReturn(Transaction.builder().build());

        createTransactionWithAmountAndTimestamp(1l,100.0, new Date().getTime() - (59 * 1000));
        createTransactionWithAmountAndTimestamp(2l, 200.00, new Date().getTime() - (59 * 1000));

        Statistics should = Statistics.builder()
            .avg(150.0)
            .min(100.0)
            .max(200.0)
            .sum(300.0)
            .count(2l)
            .build();

        Statistics response = transactionService.getStatistics();

        assertStats(response, should);

        Thread.sleep(1000);
        transactionService.evictCache();

        Statistics after1eviction = transactionService.getStatistics();
        assertEquals(after1eviction.getCount(), new Long(0l));

        createTransactionWithAmountAndTimestamp(3l,500.0, new Date().getTime());
        createTransactionWithAmountAndTimestamp(4l, 1000.0, new Date().getTime());

        Statistics after2eviction = transactionService.getStatistics();

        Statistics should2 = Statistics.builder()
            .avg(750.0)
            .min(500.0)
            .max(1000.0)
            .sum(1500.0)
            .count(2l)
            .build();

        assertStats(after2eviction, should2);

        Thread.sleep(1000);
        transactionService.evictCache();

        createTransactionWithAmountAndTimestamp(5l,1.99, new Date().getTime() - (59 * 1000));
        createTransactionWithAmountAndTimestamp(6l, 2000.0, new Date().getTime() - (59 * 1000));

        Statistics after3eviction = transactionService.getStatistics();

        Statistics should3 = Statistics.builder()
            .avg(875.4975)
            .min(1.99)
            .max(2000.0)
            .sum(3501.99)
            .count(4l)
            .build();

        assertStats(after3eviction, should3);

        Thread.sleep(1000);
        transactionService.evictCache();

        Statistics after4eviction = transactionService.getStatistics();

        Statistics should4 = Statistics.builder()
            .avg(750.0)
            .min(500.0)
            .max(1000.0)
            .sum(1500.0)
            .count(2l)
            .build();

        assertStats(after4eviction, should4);


    }

    private Transaction createTransactionWithAmountAndTimestamp(Long id, Double amount, Long timestamp) {
        Transaction transaction = Transaction.builder()
            .id(id)
            .amount(amount)
            .timestamp(timestamp).build();
        return transactionService.create(transaction);
    }

    private void assertStats(Statistics actual, Statistics should) {

        assertEquals(should.getAvg(), actual.getAvg());
        assertEquals(should.getCount(), actual.getCount());
        assertEquals(should.getMax(), actual.getMax());
        assertEquals(should.getMin(), actual.getMin());

    }

}
