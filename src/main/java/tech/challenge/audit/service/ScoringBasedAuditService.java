package tech.challenge.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.challenge.audit.submission.Batch;
import tech.challenge.audit.submission.Submission;
import tech.challenge.audit.submission.SubmissionHandler;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.AuditTransactionProcessingException;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class ScoringBasedAuditService implements AuditService {

    @Value("${audit.max.transactions.per.submission:1000}")
    private int maxTransactionsPerSubmission;

    @Value("${audit.max.batch.total.value:1000000.0}")
    private double maxBatchTotalValue;

    private final SubmissionHandler submissionHandler;
    private final BlockingQueue<Transaction> transactionQueue;
    private final ExecutorService executorService;
    private final Semaphore semaphore;

    public ScoringBasedAuditService(SubmissionHandler submissionHandler, @Value("${audit.thread.pool.size:4}") int threadPoolSize) {
        this.submissionHandler = submissionHandler;
        this.transactionQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.semaphore = new Semaphore(threadPoolSize);
    }


    @Override
    public void processTransaction(Transaction transaction) {
        try {
            transactionQueue.put(transaction);
            if (transactionQueue.size() >= maxTransactionsPerSubmission) {
                triggerProcessing();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuditTransactionProcessingException("Failed to enqueue transaction", e);
        }
    }

    private void triggerProcessing() {
        if (semaphore.tryAcquire()) {
            executorService.submit(() -> {
                try {
                    processSubmissions();
                } finally {
                    semaphore.release();
                }
            });
        } else {
            log.debug("All processing threads are currently busy. Waiting for a slot...");
        }
    }

    private  void processSubmissions() {
        List<Transaction> drainedTransactions = new ArrayList<>();
        transactionQueue.drainTo(drainedTransactions, maxTransactionsPerSubmission);

        if (drainedTransactions.isEmpty()) {
            return;
        }

        List<Batch> batches = new ArrayList<>();
        drainedTransactions.forEach(tx -> {

            double value = Math.abs(tx.getAmount());
            if (value > maxBatchTotalValue) {
                log.warn("Transaction value {} exceeds max allowed batch total {}", value, maxBatchTotalValue);
                // Option 1: skip this transaction
                return;
                // Option 2: handle separately, e.g., send to overflow queue
            }

            getBatch(batches, value)
                    .filter(batch -> batch.getTransactionCount() < maxTransactionsPerSubmission)
                    .ifPresentOrElse(
                            batch -> batch.addTransaction(value),
                            () -> batches.add(Batch.builder()
                                    .transactionCount(1)
                                    .totalValue(value)
                                    .build())
                    );
        });

        Submission submission = Submission.builder().batches(batches).build();

        if (!submission.getBatches().isEmpty())
            submissionHandler.handle(submission);
    }

    private Optional<Batch> getBatch(List<Batch> batches, double value) {
        return batches.stream()
                .filter(batch -> value <= (maxBatchTotalValue - batch.getTotalValue()))
                .min((batch1, batch2) -> {
                    double remainingCapacity1 = maxBatchTotalValue - batch1.getTotalValue() - value;
                    double remainingCapacity2 = maxBatchTotalValue - batch2.getTotalValue() - value;
                    return Double.compare(remainingCapacity1, remainingCapacity2);
                });
    }


}