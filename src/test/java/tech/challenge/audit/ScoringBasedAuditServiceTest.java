package tech.challenge.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.challenge.audit.service.ScoringBasedAuditService;
import tech.challenge.audit.submission.Batch;
import tech.challenge.audit.submission.Submission;
import tech.challenge.audit.submission.SubmissionHandler;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.AuditTransactionProcessingException;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringBasedAuditServiceTest {

    @Mock
    private SubmissionHandler submissionHandler;

    @InjectMocks
    private ScoringBasedAuditService scoringBasedAuditService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scoringBasedAuditService, "transactionQueue", new LinkedBlockingQueue<>());
    }

    // Helper method to create a Transaction
    private Transaction createTransaction(double amount) {
        return Transaction.builder()
                .id("test-id")
                .amount(amount)
                .build();
    }


    @Test
    void test_given10Transactions100MaxValue_shouldCreate4Batches() {
        // Set up transactions
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 10);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        double[] transactionAmounts = {30, 40, 45, 25, 45, 65, 11, 5, 75, 25, 62, 24};
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.acceptTransaction(createTransaction(amount));
        }

        // Process submissions
        scoringBasedAuditService.processSubmissions();

        // Capture the submission
        Submission submission = captureSubmission();
        assertNotNull(submission);

        // Verify batches
        List<Batch> batches = submission.getBatches();
        assertEquals(4, batches.size()); // Expecting 6 batches

        // Verify each batch's constraints
        assertBatch(batches.get(0), 4, 100.0); // Transactions: 30, 40, 25, 5
        assertBatch(batches.get(1), 2, 90.0);  // Transactions: 45, 45
        assertBatch(batches.get(2), 2, 76.0);  // Transactions: 65, 11
        assertBatch(batches.get(3), 2, 100.0); // Transactions: 75, 25

    }

    @Test
    void test_givenSingleTransactionExceedValue_then0BatchesSubmitted() {
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 10);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        double[] transactionAmounts = {101};
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.acceptTransaction(createTransaction(amount));
        }
        scoringBasedAuditService.processSubmissions();
        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionHandler,times(0)).handle(captor.capture());

    }

    @Test
    void test_givenMoreThanTransactionsInQueue_shouldSubmitOnlyMaxTransactionsPerSubmission() {
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1000);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100_000.0);

        for (int i = 0; i < 1001; i++) {
            scoringBasedAuditService.acceptTransaction(createTransaction(1.0));
        }
        scoringBasedAuditService.processSubmissions();

        Submission submission = captureSubmission();
        assertNotNull(submission);
        assertEquals(1, submission.getBatches().size());
        assertEquals(1000, submission.getBatches().get(0).getTransactionCount());
        assertEquals(1000.0, submission.getBatches().get(0).getTotalValue());
    }

    @Test
    void test_givenBatchSize1MaxValueEqualToTransactionAmount_then1BatchSubmitted() {
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        scoringBasedAuditService.acceptTransaction(createTransaction(100.0));
        scoringBasedAuditService.processSubmissions();

        Submission submission = captureSubmission();
        assertNotNull(submission);
        assertEquals(1, submission.getBatches().size());
        assertEquals(1, submission.getBatches().get(0).getTransactionCount());
        assertEquals(100.0, submission.getBatches().get(0).getTotalValue());
    }
    @Test
    void test_givenEmptyQueue_thenNoSubmissionDone() {
        scoringBasedAuditService.processSubmissions();
        verify(submissionHandler, never()).handle(any());
    }

    @Test
    void test_givenQueueThrowsInterruptedException_thenCustomRuntimeExceptionRethrown() throws InterruptedException {
        BlockingQueue<Transaction> mockQueue = mock(BlockingQueue.class);
        doThrow(new InterruptedException()).when(mockQueue).put(any());

        ScoringBasedAuditService service = new ScoringBasedAuditService(submissionHandler);
        ReflectionTestUtils.setField(service, "transactionQueue", mockQueue);

        assertThrows(AuditTransactionProcessingException.class, () -> service.acceptTransaction(createTransaction(100.0)));
    }


    // Helper method to capture the Submission
    private Submission captureSubmission() {
        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionHandler).handle(captor.capture());
        return captor.getValue();
    }

    // Helper method to assert batch properties
    private void assertBatch(Batch batch, int expectedTransactionCount, double expectedTotalValue) {
        assertEquals(expectedTransactionCount, batch.getTransactionCount());
        assertEquals(expectedTotalValue, batch.getTotalValue());
    }

}