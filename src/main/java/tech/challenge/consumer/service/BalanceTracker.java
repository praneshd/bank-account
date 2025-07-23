package tech.challenge.consumer.service;



import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.challenge.audit.service.ScoringBasedAuditService;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.InvalidTransactionException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class BalanceTracker implements BankAccountService {
    //Storing long to avoid floating-point errors
    private final ScoringBasedAuditService auditService;
    private final AtomicLong balanceInPence = new AtomicLong(0);

    public BalanceTracker(ScoringBasedAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void processTransaction(Transaction transaction) {
        Optional.ofNullable(transaction)
                .filter(t -> t.getAmount() != 0.0)
                .ifPresentOrElse(
                        tx -> {
                            long amountInPence = Math.round(tx.getAmount() * 100);
                            long updated = balanceInPence.addAndGet(amountInPence);
                            auditService.acceptTransaction(transaction);
                            log.info("Processed transaction {}. New balance: {} pence", tx.getId(), updated);
                        },
                        () -> {
                            log.warn("Invalid or null transaction received");
                            throw new InvalidTransactionException("Transaction is null or has zero amount");
                        }
                );
    }

    @Override
    public double retrieveBalance() {
        double balance = balanceInPence.get() / 100.0;
        log.trace("Balance retrieved: {}", balance);
        return balance;
    }
}
