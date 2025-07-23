package tech.challenge.consumer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.challenge.audit.service.ScoringBasedAuditService;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.InvalidTransactionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceTrackerTest {

    @Mock
    private ScoringBasedAuditService auditService;

    @InjectMocks
    private BalanceTracker balanceTracker;

    @Test
    void test_givenPositiveTransaction_thenBalanceUpdatedAndAuditServiceCalled() {
        Transaction tx = Transaction.builder()
                .id("tx1")
                .amount(25.50)
                .build();

        balanceTracker.processTransaction(tx);

        assertThat(balanceTracker.retrieveBalance()).isEqualTo(25.50);
        verify(auditService).acceptTransaction(tx);
    }

    @Test
    void test_givenNegativeTransaction_thenBalanceDecreasedAndAuditServiceCalled() {
        Transaction tx1 = Transaction.builder().id("tx2").amount(50.00).build();
        Transaction tx2 = Transaction.builder().id("tx3").amount(-20.00).build();

        balanceTracker.processTransaction(tx1);
        balanceTracker.processTransaction(tx2);

        assertThat(balanceTracker.retrieveBalance()).isEqualTo(30.00);
        verify(auditService).acceptTransaction(tx1);
        verify(auditService).acceptTransaction(tx2);
    }

    @Test
    void test_givenZeroAmountTransaction_thenThrowInvalidTransactionException() {
        Transaction tx = Transaction.builder().id("tx4").amount(0.0).build();

        assertThatThrownBy(() -> balanceTracker.processTransaction(tx))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("zero amount");

        verifyNoInteractions(auditService);
    }

    @Test
    void test_givenNullTransaction_thenThrowInvalidTransactionException() {
        assertThatThrownBy(() -> balanceTracker.processTransaction(null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("null");

        verifyNoInteractions(auditService);
    }

    @Test
    void test_givenNoTransactions_thenInitialBalanceIsZero() {
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(0.0);
    }

    @Test
    void test_givenMultipleTransactions_thenBalanceAccumulatesCorrectly() {
        balanceTracker.processTransaction(Transaction.builder().id("tx5").amount(10.00).build());
        balanceTracker.processTransaction(Transaction.builder().id("tx6").amount(20.00).build());
        balanceTracker.processTransaction(Transaction.builder().id("tx7").amount(-5.00).build());

        assertThat(balanceTracker.retrieveBalance()).isEqualTo(25.00);
        verify(auditService, times(3)).acceptTransaction(any());
    }

    @Test
    void test_givenValidTransaction_thenAuditServiceCalledOnce() {
        Transaction tx = Transaction.builder().id("tx8").amount(99.99).build();

        balanceTracker.processTransaction(tx);

        verify(auditService, times(1)).acceptTransaction(tx);
    }

    @Test
    void test_givenInvalidTransactions_thenAuditServiceNotCalled() {
        Transaction tx1 = Transaction.builder().id("tx9").amount(0.0).build();
        Transaction tx2 = null;

        assertThatThrownBy(() -> balanceTracker.processTransaction(tx1))
                .isInstanceOf(InvalidTransactionException.class);

        assertThatThrownBy(() -> balanceTracker.processTransaction(tx2))
                .isInstanceOf(InvalidTransactionException.class);

        verifyNoInteractions(auditService);
    }
}
