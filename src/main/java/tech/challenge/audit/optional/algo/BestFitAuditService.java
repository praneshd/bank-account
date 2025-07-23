package tech.challenge.audit.optional.algo;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.challenge.audit.submission.Batch;
import tech.challenge.audit.submission.Submission;
import tech.challenge.domain.Transaction;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BestFitAuditService {
    private static final int MAX_TRANSACTIONS_PER_SUBMISSION = 100000;
    private static final double MAX_BATCH_TOTAL_VALUE = 1_000_000.0;

    private final BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();

    public void acceptTransaction(Transaction transaction) {
        try {
            transactionQueue.put(transaction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to accept transaction", e);
        }
    }

    public void processSubmissions() {

        List<Transaction> drainedTransactions = new ArrayList<>();
        transactionQueue.drainTo(drainedTransactions, MAX_TRANSACTIONS_PER_SUBMISSION);
        if (drainedTransactions.isEmpty()) {
            return;
        }
        log.info("Pulled {} transactions for efficeient batching", drainedTransactions.size());
        List<Transaction> submissionTransactions = drainedTransactions.stream()
                .sorted(Comparator.comparingDouble(t -> -Math.abs(t.getAmount())))
                .limit(MAX_TRANSACTIONS_PER_SUBMISSION)
                .collect(Collectors.toList());

        List<Batch> batches = bestFitPack(submissionTransactions);
        Submission submission = Submission.builder().batches(batches).build();
        logSubmission(submission);

    }

    private List<Batch> bestFitPack(List<Transaction> transactions) {
        List<Batch> batches = new ArrayList<>();

        for (Transaction tx : transactions) {
            double value = Math.abs(tx.getAmount());
            Batch bestFit = null;
            for (Batch batch : batches) {
                if (batch.getTotalValue() + value <= MAX_BATCH_TOTAL_VALUE) {
                    if (bestFit == null || batch.getTotalValue() > bestFit.getTotalValue()) {
                        bestFit = batch;
                    }
                }
            }
            if (bestFit != null) {
                bestFit.addTransaction(value);
            } else {
                Batch newBatch = Batch.builder()
                        .transactionCount(1)
                        .totalValue(value)
                        .build();
                batches.add(newBatch);
            }
        }
        return batches;
    }

    private void logSubmission(Submission submission) {
        //log.info(submission.toString());
        log.info("total batches submitted {}", submission.getBatches().size());
    }
}
