package tech.challenge.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.challenge.audit.service.AuditService;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.InvalidTransactionException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service class responsible for tracking the balance of a bank account and processing transactions.
 * It ensures thread-safe updates to the balance and delegates transaction auditing to the AuditService.
 */
@Slf4j
@Service
class BalanceTracker implements BankAccountService {

    // Storing balance in pence to avoid floating-point errors
    private final AuditService auditService;
    private final AtomicLong balanceInPence = new AtomicLong(0);

    /**
     * Constructor for BalanceTracker.
     *
     * @param auditService the AuditService used for auditing transactions
     */
    public BalanceTracker(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Processes a given transaction by updating the account balance and auditing the transaction.
     *
     * @param transaction the transaction to process
     * @throws InvalidTransactionException if the transaction is null or has zero amount
     */
    @Override
    public void processTransaction(Transaction transaction) {
        Optional.ofNullable(transaction)
                .ifPresentOrElse(
                        tx -> {
                            // Convert transaction amount to pence and update the balance
                            long amountInPence = Math.round(tx.getAmount() * 100);
                            long updated = balanceInPence.addAndGet(amountInPence);

                            // Audit the transaction
                            auditService.processTransaction(transaction);

                            // Log the processed transaction and updated balance
                            log.info("Processed transaction {}. New balance: {} pence", tx.getId(), updated);
                        },
                        () -> {
                            // Log and throw an exception for invalid or null transactions
                            log.warn("Invalid or null transaction received");
                            throw new InvalidTransactionException("Transaction is null or has zero amount");
                        }
                );
    }

    /**
     * Retrieves the current balance of the account.
     *
     * @return the current balance in pounds as a double
     */
    @Override
    public double retrieveBalance() {
        // Convert balance from pence to pounds
        double balance = balanceInPence.get() / 100.0;

        // Log the retrieved balance
        log.trace("Balance retrieved: {}", balance);

        return balance;
    }
}