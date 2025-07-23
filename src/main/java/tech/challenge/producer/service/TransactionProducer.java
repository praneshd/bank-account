package tech.challenge.producer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.challenge.consumer.service.BankAccountService;
import tech.challenge.domain.Transaction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
@Component
public class TransactionProducer {

    private final BankAccountService bankAccountService;
    private final ScheduledExecutorService creditExecutor;
    private final ScheduledExecutorService debitExecutor;
    private final Supplier<Double> randomSupplier;

    private static final double MIN = 200;
    private static final double MAX = 500_000;

    public TransactionProducer(BankAccountService bankAccountService, Supplier<Double> randomSupplier) {
        this.bankAccountService = bankAccountService;
        this.randomSupplier = randomSupplier;
        this.creditExecutor = Executors.newSingleThreadScheduledExecutor();
        this.debitExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    public void start() {
        creditExecutor.scheduleAtFixedRate(this::produceCredit, 0, 40, TimeUnit.MILLISECONDS);
        debitExecutor.scheduleAtFixedRate(this::produceDebit, 0, 40, TimeUnit.MILLISECONDS);
        log.info("TransactionProducer started with dedicated threads for credits and debits.");
    }

    private double getRandomAmount() {
        return MIN + (MAX - MIN) * randomSupplier.get(); // randomSupplier.get() should return [0.0, 1.0)
    }

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