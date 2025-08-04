package tech.challenge.producer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.challenge.consumer.service.BankAccountService;
import tech.challenge.domain.Transaction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Component responsible for producing credit and debit transactions at regular intervals.
 * Uses scheduled executors to generate transactions and process them via the BankAccountService.
 */
@Slf4j
@Component
public class TransactionProducer {

    private final BankAccountService bankAccountService;
    private final ScheduledExecutorService creditExecutor;
    private final ScheduledExecutorService debitExecutor;
    private final Supplier<Double> randomSupplier;

    private static final double MIN = 200; // Minimum transaction amount
    private static final double MAX = 500_000; // Maximum transaction amount

    /**
     * Constructs a TransactionProducer with the required dependencies.
     *
     * @param bankAccountService the service responsible for processing transactions
     * @param randomSupplier a supplier that generates random double values in the range [0.0, 1.0)
     */
    public TransactionProducer(BankAccountService bankAccountService, Supplier<Double> randomSupplier) {
        this.bankAccountService = bankAccountService;
        this.randomSupplier = randomSupplier;
        this.creditExecutor = Executors.newSingleThreadScheduledExecutor();
        this.debitExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Initializes the TransactionProducer by starting scheduled tasks for producing credit and debit transactions.
     * This method is called automatically after the bean is constructed.
     */
    @PostConstruct
    public void start() {
        creditExecutor.scheduleAtFixedRate(this::produceCredit, 0, 40, TimeUnit.MILLISECONDS);
        debitExecutor.scheduleAtFixedRate(this::produceDebit, 0, 40, TimeUnit.MILLISECONDS);
        log.info("TransactionProducer started with dedicated threads for credits and debits.");
    }


    private double getRandomAmount() {
        return MIN + (MAX - MIN) * randomSupplier.get(); // randomSupplier.get() should return [0.0, 1.0)
    }

    /**
     * Produces a credit transaction and processes it using the BankAccountService.
     * Logs the transaction details or any errors encountered during processing.
     */
    private void produceCredit() {
        try {
            double amount = getRandomAmount();
            Transaction tx = Transaction.credit(amount);
            bankAccountService.processTransaction(tx);
            log.debug("Produced credit: {}", tx);
        } catch (Exception e) {
            log.error("Error generating credit transaction", e);
        }
    }

    /**
     * Produces a debit transaction and processes it using the BankAccountService.
     * Logs the transaction details or any errors encountered during processing.
     */
    private void produceDebit() {
        try {
            double amount = getRandomAmount();
            Transaction tx = Transaction.debit(amount);
            bankAccountService.processTransaction(tx);
            log.debug("Produced debit: {}", tx);
        } catch (Exception e) {
            log.error("Error generating debit transaction", e);
        }
    }


    @PreDestroy
    public void stop() {
        creditExecutor.shutdownNow();
        debitExecutor.shutdownNow();
        log.info("TransactionProducer shutdown.");
    }
}