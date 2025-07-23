package tech.challenge.producer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.challenge.consumer.service.BankAccountService;
import tech.challenge.domain.Transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProducerIntegrationTest {

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private TransactionProducer transactionProducer;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Test
    void test_givenProducerStarted_thenCreditAndDebitAreProducedUsingLatch() throws InterruptedException {
        // Expecting 2 transactions (1 credit + 1 debit)
        CountDownLatch latch = new CountDownLatch(2);

        // Wrap mock to count down latch on each call
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(bankAccountService).processTransaction(any(Transaction.class));

        transactionProducer.start();

        // Wait up to 1 second for both credit and debit to be processed
        boolean completed = latch.await(1, TimeUnit.SECONDS);

        // Ensure both transactions processed
        assertThat(completed).isTrue();

        // Capture and assert both types of transactions (credit: amount > 0, debit: amount < 0)
        verify(bankAccountService, atLeast(2)).processTransaction(transactionCaptor.capture());

        boolean hasCredit = transactionCaptor.getAllValues().stream().anyMatch(tx -> tx.getAmount() > 200
                && tx.getAmount() < 500_000  );
        boolean hasDebit = transactionCaptor.getAllValues().stream().anyMatch(tx -> tx.getAmount() < -200
                && tx.getAmount() > -500_000);

        assertThat(hasCredit).isTrue();
        assertThat(hasDebit).isTrue();

        transactionProducer.stop();
    }
}