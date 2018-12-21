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
    public void should_get_correct_statistics() {
        when(transactionRepository.save(any())).thenReturn(Transaction.builder().build());

        createTransactionWithAmountAndTimestamp(1l,100.0, new Date().getTime());
        createTransactionWithAmountAndTimestamp(2l, 200.00, new Date().getTime());

        Statistics stat = Statistics.builder()
            .avg(150.0)
            .min(100.0)
            .max(200.0)
            .sum(300.0)
            .count(2l).build();

        Statistics response = transactionService.getStatistics();
        assertEquals(response.getAvg(), stat.getAvg());
        assertEquals(response.getCount(), stat.getCount());
        assertEquals(response.getMax(), stat.getMax());
        assertEquals(response.getMin(), stat.getMin());



    }

    private Transaction createTransactionWithAmountAndTimestamp(Long id, Double amount, Long timestamp) {
        Transaction transaction = Transaction.builder()
            .id(id)
            .amount(amount)
            .timestamp(timestamp).build();
        return transactionService.create(transaction);
    }

}
