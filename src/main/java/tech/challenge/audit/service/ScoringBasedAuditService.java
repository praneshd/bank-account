package tech.challenge.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.challenge.audit.submission.Batch;
import tech.challenge.audit.submission.Submission;
import tech.challenge.audit.submission.SubmissionHandler;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.AuditTransactionProcessingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;


/**
 * Service implementation for auditing transactions using a scoring-based approach.
 * Handles transaction batching and submission to a <class>SubmissionHandler</class> for processing.
 */
@Slf4j
@Service
class ScoringBasedAuditService implements AuditService {

    /**
     * Maximum number of transactions allowed per submission.
     * Configurable via the `audit.max.transactions.per.submission` property.
     */
    @Value("${audit.max.transactions.per.submission:1000}")
    private int maxTransactionsPerSubmission;

    /**
     * Maximum total value allowed for a batch of transactions.
     * Configurable via the `audit.max.batch.total.value` property.
     */
    @Value("${audit.max.batch.total.value:1000000.0}")
    private double maxBatchTotalValue;

    private final SubmissionHandler submissionHandler;
    private final BlockingQueue<Transaction> transactionQueue;
    private final ExecutorService executorService;
    private final Semaphore semaphore;

    /**
     * Constructor for `ScoringBasedAuditService`.
     *
     * @param submissionHandler the handler responsible for processing submissions
     * @param threadPoolSize the size of the thread pool for processing transactions
     */
    public ScoringBasedAuditService(SubmissionHandler submissionHandler, @Value("${audit.thread.pool.size:4}") int threadPoolSize) {
        this.submissionHandler = submissionHandler;
        this.transactionQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.semaphore = new Semaphore(threadPoolSize);
    }

    /**
     * Processes a single transaction by adding it to the transaction queue.
     * If the queue size reaches the maximum allowed transactions per submission, triggers processing.
     *
     * @param transaction the transaction to process
     * @throws AuditTransactionProcessingException if the transaction cannot be enqueued
     */
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

    /**
     * Triggers the processing of transactions in the queue if a thread is available.
     */
    private void triggerProcessing() {
        if (semaphore.tryAcquire()) {
            CompletableFuture.supplyAsync(this::buildBatches, executorService)
                    .thenApply(this::buildSubmission)
                    .thenAccept(this::handleSubmission)
                    .whenComplete((result, throwable) -> semaphore.release());
        } else {
            log.debug("All processing threads are currently busy. Waiting for a slot...");
        }
    }

    private List<Batch> buildBatches() {
        List<Transaction> drainedTransactions = new ArrayList<>();
        transactionQueue.drainTo(drainedTransactions, maxTransactionsPerSubmission);

        if (drainedTransactions.isEmpty()) {
            return Collections.emptyList();
        }

        return drainedTransactions.stream()
                .filter(tx -> isTransactionWithinRange(Math.abs(tx.getAmount())))
                .collect(
                        ArrayList::new,
                        this::processTransactionForBatch,
                        ArrayList::addAll
                );
    }


    private boolean isTransactionWithinRange(double value) {
        if (value > maxBatchTotalValue) {
            log.warn("Transaction value {} exceeds max allowed batch total {}", value, maxBatchTotalValue);
            return false; // Skip this transaction
        }
        return true;
    }

    //Create new batch or add it to existing batch
    private void processTransactionForBatch(List<Batch> batches, Transaction tx) {
        double value = Math.abs(tx.getAmount());
        getBatch(batches, value)
                .ifPresentOrElse(
                        batch -> batch.addTransaction(value),
                        () -> batches.add(Batch.builder()
                                .transactionCount(1)
                                .totalValue(value)
                                .build())
                );
    }

    //Goes through each batch and assigns the value to the batch where it fits appropriately.
    private Optional<Batch> getBatch(List<Batch> batches, double value) {
        return batches.stream()
                .filter(batch -> value <= (maxBatchTotalValue - batch.getTotalValue()))
                .min((batch1, batch2) -> {
                    double remainingCapacity1 = maxBatchTotalValue - batch1.getTotalValue() - value;
                    double remainingCapacity2 = maxBatchTotalValue - batch2.getTotalValue() - value;
                    return Double.compare(remainingCapacity1, remainingCapacity2);
                });
    }

    private Submission buildSubmission(List<Batch> batches) {
        return Submission.builder().batches(batches).build();
    }

    private void handleSubmission(Submission submission) {
        if (!submission.getBatches().isEmpty()) {
            submissionHandler.handle(submission);
        }
    }



}