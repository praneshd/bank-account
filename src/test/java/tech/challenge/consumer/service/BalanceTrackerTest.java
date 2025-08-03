package tech.challenge.consumer.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.challenge.audit.service.AuditService;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.InvalidTransactionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceTrackerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BalanceTracker balanceTracker;

    @Test
    @DisplayName("Given a positive transaction, when processed, then balance is updated and audit service is called")
    void testGivenPositiveTransactionThenBalanceUpdatedAndAuditServiceCalled() {
        // Given
        Transaction tx = Transaction.builder()
                .id("tx1")
                .amount(25.50)
                .build();

        // When
        balanceTracker.processTransaction(tx);

        // Then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(25.50);
        verify(auditService).processTransaction(tx);
    }

    @Test
    @DisplayName("Given a negative transaction, when processed, then balance is decreased and audit service is called")
    void testGivenNegativeTransactionThenBalanceDecreasedAndAuditServiceCalled() {
        // Given
        Transaction tx1 = Transaction.builder().id("tx2").amount(50.00).build();
        Transaction tx2 = Transaction.builder().id("tx3").amount(-20.00).build();

        // When
        balanceTracker.processTransaction(tx1);
        balanceTracker.processTransaction(tx2);

        // Then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(30.00);
        verify(auditService).processTransaction(tx1);
        verify(auditService).processTransaction(tx2);
    }

    @Test
    @DisplayName("Given a null transaction, when processed, then an InvalidTransactionException is thrown")
    void testGivenNullTransactionThenThrowInvalidTransactionException() {
        // Given
        Transaction tx = null;

        // When & Then
        assertThatThrownBy(() -> balanceTracker.processTransaction(tx))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("null");

        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Given no transactions, when balance is retrieved, then initial balance is zero")
    void testGivenNoTransactionsThenInitialBalanceIsZero() {
        // Given
        // No transactions

        // When
        double balance = balanceTracker.retrieveBalance();

        // Then
        assertThat(balance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Given multiple transactions, when processed, then balance accumulates correctly")
    void testGivenMultipleTransactionsThenBalanceAccumulatesCorrectly() {
        // Given
        Transaction tx1 = Transaction.builder().id("tx5").amount(10.00).build();
        Transaction tx2 = Transaction.builder().id("tx6").amount(20.00).build();
        Transaction tx3 = Transaction.builder().id("tx7").amount(-5.00).build();

        // When
        balanceTracker.processTransaction(tx1);
        balanceTracker.processTransaction(tx2);
        balanceTracker.processTransaction(tx3);

        // Then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(25.00);
        verify(auditService, times(3)).processTransaction(any());
    }

    @Test
    @DisplayName("Given a valid transaction, when processed, then audit service is called once")
    void testGivenValidTransactionThenAuditServiceCalledOnce() {
        // Given
        Transaction tx = Transaction.builder().id("tx8").amount(99.99).build();

        // When
        balanceTracker.processTransaction(tx);

        // Then
        verify(auditService, times(1)).processTransaction(tx);
    }

    @Test
    @DisplayName("Given invalid transactions, when processed, then audit service is not called")
    void testGivenInvalidTransactionsThenAuditServiceNotCalled() {
        // Given
        Transaction tx = null;

        // When & Then
        assertThatThrownBy(() -> balanceTracker.processTransaction(tx))
                .isInstanceOf(InvalidTransactionException.class);

        verifyNoInteractions(auditService);
    }
}