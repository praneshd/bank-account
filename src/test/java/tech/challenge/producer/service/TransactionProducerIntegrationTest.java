package tech.challenge.producer.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.challenge.consumer.service.BankAccountService;
import tech.challenge.domain.Transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProducerIntegrationTest {

    @Mock
    private BankAccountService bankAccountService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private TransactionProducer transactionProducer;

    private final Supplier<Double> fixedRandomSupplier = () -> 0.5; // deterministic value

    @AfterEach
    void tearDown() {
        if (transactionProducer != null) {
            transactionProducer.stop();
        }
    }

    @Test
    void test_givenFixedRandomSupplier_thenCreditAndDebitHaveExpectedAmounts() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(2);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(bankAccountService).processTransaction(any(Transaction.class));

        transactionProducer = new TransactionProducer(bankAccountService, fixedRandomSupplier);

        // Act
        transactionProducer.start();
        boolean completed = latch.await(1, TimeUnit.SECONDS);

        // Assert
        assertThat(completed).isTrue();

        verify(bankAccountService, atLeast(2)).processTransaction(transactionCaptor.capture());

        double expectedAmount = 200 + (500_000 - 200) * 0.5; // based on supplier
        double expectedCredit = expectedAmount;
        double expectedDebit = -expectedAmount;

        boolean hasExpectedCredit = transactionCaptor.getAllValues().stream()
                .anyMatch(tx -> tx.getAmount() == expectedCredit);

        boolean hasExpectedDebit = transactionCaptor.getAllValues().stream()
                .anyMatch(tx -> tx.getAmount() == expectedDebit);

        assertThat(hasExpectedCredit).isTrue();
        assertThat(hasExpectedDebit).isTrue();
    }
}