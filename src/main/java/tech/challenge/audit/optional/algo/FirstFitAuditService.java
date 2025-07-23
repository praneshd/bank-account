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
public class FirstFitAuditService {
    private static final int MAX_TRANSACTIONS_PER_SUBMISSION = 100_000;
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
        log.info("Pulled {} transactions for efficeient batching", drainedTransactions.size());
        if (drainedTransactions.isEmpty()) {
            return;
        }

        List<Transaction> submissionTransactions = drainedTransactions.stream()
                .sorted(Comparator.comparingDouble(t -> -Math.abs(t.getAmount())))
                .limit(MAX_TRANSACTIONS_PER_SUBMISSION)
                .collect(Collectors.toList());

        List<Batch> batches = firstFitPack(submissionTransactions);
        Submission submission = Submission.builder().batches(batches).build();
        logSubmission(submission);
    }

    private List<Batch> firstFitPack(List<Transaction> transactions) {
        List<Batch> batches = new ArrayList<>();

        for (Transaction tx : transactions) {
            double value = Math.abs(tx.getAmount());
            boolean placed = false;
            for (Batch batch : batches) {
                if (batch.getTotalValue() + value <= MAX_BATCH_TOTAL_VALUE) {
                    batch.addTransaction(value);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
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
        log.info(submission.toString());
        log.info("total batches submitted {}", submission.getBatches().size());
    }
}
