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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class ScoringBasedAuditService {

    @Value("${audit.max.transactions.per.submission:1000}")
    private int maxTransactionsPerSubmission;

    @Value("${audit.max.batch.total.value:1000000.0}")
    private double maxBatchTotalValue;

    private final BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();
    private final SubmissionHandler submissionHandler;

    public ScoringBasedAuditService(SubmissionHandler submissionHandler) {
        this.submissionHandler = submissionHandler;
    }

    public void acceptTransaction(Transaction transaction) {
        try {
            transactionQueue.put(transaction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuditTransactionProcessingException("Failed to accept transaction in audit service", e);
        }
    }

    public void processSubmissions() {
        List<Transaction> drainedTransactions = new ArrayList<>();
        transactionQueue.drainTo(drainedTransactions, maxTransactionsPerSubmission);

        if (drainedTransactions.isEmpty()) {
            return;
        }
        log.info("Pulled {} transactions for efficient batching", drainedTransactions.size());

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


    /*private Optional<Batch> getBatch1(List<Batch> batches, double value) {
        Batch bestBatch = null;
        double smallestRemainingCapacity = Double.MAX_VALUE;

        for (Batch batch : batches) {
            double remainingCapacity = maxBatchTotalValue - batch.getTotalValue();
            if (value <= remainingCapacity) {
                double adjustedCapacity = remainingCapacity - value;
                if (adjustedCapacity < smallestRemainingCapacity) {
                    smallestRemainingCapacity = adjustedCapacity;
                    bestBatch = batch;
                }
            }
        }

        return Optional.ofNullable(bestBatch);
    }*/
}