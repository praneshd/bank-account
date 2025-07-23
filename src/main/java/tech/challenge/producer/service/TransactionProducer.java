package tech.challenge.producer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.challenge.consumer.service.BankAccountService;
import tech.challenge.domain.Transaction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Random;
import java.util.concurrent.*;

@Slf4j
@Component
public class TransactionProducer {

    private static final double MIN_AMOUNT = 200;
    private static final double MAX_AMOUNT = 500_000;

    private final BankAccountService bankAccountService;
    private final ScheduledExecutorService creditExecutor;
    private final ScheduledExecutorService debitExecutor;
    private final Random random;

    public TransactionProducer(BankAccountService bankAccountService, Random random) {
        this.bankAccountService = bankAccountService;
        this.random = random;
        this.creditExecutor = Executors.newSingleThreadScheduledExecutor();
        this.debitExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    public void start() {
        creditExecutor.scheduleAtFixedRate(this::produceCredit, 0, 40, TimeUnit.MILLISECONDS);
        debitExecutor.scheduleAtFixedRate(this::produceDebit, 0, 40, TimeUnit.MILLISECONDS);
        log.info("TransactionProducer started with dedicated threads for credits and debits.");
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

    double getRandomAmount() {
        return MIN_AMOUNT + (MAX_AMOUNT - MIN_AMOUNT) * random.nextDouble();
    }

    @PreDestroy
    public void stop() {
        creditExecutor.shutdownNow();
        debitExecutor.shutdownNow();
        log.info("TransactionProducer shutdown.");
    }
}