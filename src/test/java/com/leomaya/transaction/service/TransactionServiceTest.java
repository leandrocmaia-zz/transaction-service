package com.leomaya.transaction.service;

import com.leomaya.transaction.model.Statistics;
import com.leomaya.transaction.model.Transaction;
import com.leomaya.transaction.repository.TransactionRepository;
import com.leomaya.transaction.service.impl.TransactionServiceImpl;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.*;
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

        Transaction transaction = createTransactionWithAmountAndTimestamp(1l,100.0, Instant.now().toEpochMilli());
        assertNotNull(transaction);
    }


    @Test
    public void should_get_correct_statistics() throws InterruptedException {
        when(transactionRepository.save(any())).thenReturn(Transaction.builder().build());

        // first batch of transactions with timestamp 59s ago
        createTransactionWithAmountAndTimestamp(1l,100.0, Instant.now().minusSeconds(59).toEpochMilli());
        createTransactionWithAmountAndTimestamp(2l, 200.00, Instant.now().minusSeconds(59).toEpochMilli());

        Statistics should0 = Statistics.builder()
            .avg(150.0)
            .min(100.0)
            .max(200.0)
            .sum(300.0)
            .count(2l)
            .build();

        Statistics after0eviction = transactionService.getStatistics();

        assertStats(after0eviction, should0);

        // emulate @Scheduled
        Thread.sleep(1000);
        transactionService.evictCache();

        Statistics should1 = Statistics.builder()
            .avg(0.0)
            .min(0.0)
            .max(0.0)
            .sum(0.0)
            .count(0l)
            .build();

        Statistics after1eviction = transactionService.getStatistics();
        assertStats(after1eviction, should1);

        // second batch with timestamp current (the first batch should be already evicted)

        createTransactionWithAmountAndTimestamp(3l,500.0, Instant.now().toEpochMilli());
        createTransactionWithAmountAndTimestamp(4l, 1000.0, Instant.now().toEpochMilli());

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

        // third batch with timestamp of 59s ago (the first batch should be out but not the second)

        createTransactionWithAmountAndTimestamp(5l,1.99, Instant.now().minusSeconds(59).toEpochMilli());
        createTransactionWithAmountAndTimestamp(6l, 2000.0, Instant.now().minusSeconds(59).toEpochMilli());

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

        // fourth batch without any transaction, the third batch should have been evicted out, but not the second.

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

    /**
     * Test case that creates multiple threads, do overlaping and check if the cache is indeed thread-safe.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void should_result_be_thread_safe() throws ExecutionException, InterruptedException {
        when(transactionRepository.save(any())).thenReturn(Transaction.builder().build());

        int threads = Runtime.getRuntime().availableProcessors() - 1;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        Collection<Future<Pair<Transaction, Statistics>>> futures = new ArrayList<>(threads);

        AtomicBoolean running = new AtomicBoolean();
        AtomicInteger overlaps = new AtomicInteger();

        // submiting the callables in parallel
        for (int t = 0; t < threads; ++t) {
            futures.add(service.submit(() -> {
                if (running.get()) {
                    overlaps.incrementAndGet();
                }
                running.set(true);
                Transaction transaction = createTransactionWithAmountAndTimestamp(new Random().nextLong(), 100.0, Instant.now().minusSeconds(59).toEpochMilli());
                Statistics statistics = transactionService.getStatistics();
                running.set(false);
                return new Pair(transaction, statistics);
            }));
        }

        // awaiting all futures to finish
        Set<Pair<Transaction, Statistics>> completedFutures = new HashSet<>();
        for (Future<Pair<Transaction, Statistics>> f : futures) {
            completedFutures.add(f.get());
        }

        // the whole idea is to test if there was overlap and the statistics are the same across all threads.
        assertThat(overlaps.get(), greaterThan(0));
        assertEquals(completedFutures.size(), threads);

        Statistics should = Statistics.builder()
            .avg(100.00)
            .min(100.0)
            .max(100.0)
            .sum(threads * 100.00)
            .count(threads * 1l)
            .build();

        List<Pair<Transaction, Statistics>> actual = completedFutures.stream()
            .filter(a -> !a.getValue().getAvg().equals(should.getAvg()))
            .filter(a -> !a.getValue().getMax().equals(should.getMax()))
            .filter(a -> !a.getValue().getMin().equals(should.getMin()))
            .filter(a -> !a.getValue().getSum().equals(should.getSum()))
            .filter(a -> !a.getValue().getCount().equals(should.getCount()))
            .collect(Collectors.toList());

        assertEquals(actual.size(), 0);


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
