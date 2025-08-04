package tech.challenge.audit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.challenge.audit.submission.Batch;
import tech.challenge.audit.submission.Submission;
import tech.challenge.audit.submission.SubmissionHandler;
import tech.challenge.domain.Transaction;
import tech.challenge.exception.AuditTransactionProcessingException;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringBasedAuditServiceTest {

    @Mock
    private SubmissionHandler submissionHandler;


    // Helper method to create a Transaction
    private Transaction createTransaction(double amount) {
        return Transaction.builder()
                .id("test-id")
                .amount(amount)
                .build();
    }


    @Test
    @DisplayName("Given 10 transactions with max value 100 and 1 worker, should create 4 batches")
    void shouldCreate4BatchesFor10TransactionsWithMaxValue100And1Worker() throws InterruptedException {
        // Given
        double[] transactionAmounts = {30, -40, 45, 25, -45, 65, -11, 5, 75, 25, -62, 24};
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 10);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);

        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // When
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Then
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        Submission submission = submissionCaptor.getValue();
        assertNotNull(submission, "Submission should not be null");

        List<Batch> batches = submission.getBatches();
        assertEquals(4, batches.size());

        assertBatch(batches.get(0), 4, 100.0); // 30, 40, 25, 5
        assertBatch(batches.get(1), 2, 90.0);  // 45, 45
        assertBatch(batches.get(2), 2, 76.0);  // 65, 11
        assertBatch(batches.get(3), 2, 100.0); // 75, 25
    }

    @Test
    @DisplayName("Given 4 transactions per batch with max value 100 and 4 workers, should create 4 batches")
    void shouldCreate4BatchesFor4TransactionsPerBatchWithMaxValue100And4Workers() throws InterruptedException {
        // Given
        double[] transactionAmounts = {12, 34, 1, 45, -4, -30, -40, 45, 25, -45, 65, -11, 5, 75, 25, -62};
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 4);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 4);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        CountDownLatch latch = new CountDownLatch(4);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // When
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);

        // Then
        assertTrue(completed, "Submission handler was not invoked in time");

        List<Submission> submissions = submissionCaptor.getAllValues();
        assertNotNull(submissions, "Submissions should not be null");
        assertEquals(4, submissions.size(), "Should have captured 4 submissions");

        List<Batch> allBatches = submissions.stream()
                .flatMap(s -> s.getBatches().stream())
                .toList();

        assertEquals(4, submissions.size(), "Should have 4 submission total");
        assertEquals(7, allBatches.size(), "Should have 7 batches total");

        for (Batch batch : allBatches) {
            double total = batch.getTotalValue();
            assertTrue(batch.getTransactionCount() <= 4, "Batch exceeds max transactions per submission");
            assertTrue(Math.abs(total) <= 100.0, "Batch exceeds max batch total value");
        }

        for (int i = 0; i < allBatches.size(); i++) {
            Batch b = allBatches.get(i);
            double total = b.getTotalValue();
            System.out.printf("Batch %d: size=%d, total=%.2f%n", i + 1, b.getTransactionCount(), total);
        }
    }

    @Test
    @DisplayName("Edge case: Given 10 transactions with max value 5, should create 1 batch")
    void shouldCreate1BatchFor10TransactionsWithMaxValue5() throws InterruptedException {
        // Given
        double[] transactionAmounts = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 10);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 5);
        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // When
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        boolean completed = latch.await(2, TimeUnit.SECONDS);

        // Then
        assertTrue(completed, "Submission handler was not invoked in time");
        Submission submission = submissionCaptor.getValue();
        assertNotNull(submission, "Submission should not be null");

        List<Batch> batches = submission.getBatches();
        assertEquals(1, batches.size());
        assertBatch(batches.get(0), 10, 5);
    }

    @Test
    @DisplayName("Edge case: Each transaction equals max value, should create a batch for each transaction")
    void shouldCreateBatchForEachTransactionWhenEachTransactionEqualsMaxValue() throws InterruptedException {
        // Given
        double[] transactionAmounts = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100};
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 20);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100);

        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // When
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        boolean completed = latch.await(2, TimeUnit.SECONDS);

        // Then
        assertTrue(completed, "Submission handler was not invoked in time");

        Submission submission = submissionCaptor.getValue();
        assertNotNull(submission, "Submission should not be null");

        List<Batch> batches = submission.getBatches();
        assertEquals(20, batches.size());
        batches.forEach(batch -> assertBatch(batch, 1, 100));
    }

    @Test
    @DisplayName("Given a single transaction exceeding max value, no batches should be submitted")
    void testGivenSingleTransactionExceedValue_then0BatchesSubmitted() throws InterruptedException {
        // Given
        double[] transactionAmounts = {101};
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        CountDownLatch latch = new CountDownLatch(1);

        // When
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Wait for a short time to ensure no invocation happens
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        // Then
        assertFalse(completed, "Submission handler should not have been invoked");
        verify(submissionHandler, times(0)).handle(any(Submission.class));
    }

    @Test
    @DisplayName("Given more transactions than allowed in queue, should submit only max transactions per submission")
    void testGivenMoreThanTransactionsInQueue_shouldSubmitOnlyMaxTransactionsPerSubmission() throws InterruptedException {
        // Given
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1000);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100_000.0);
        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // When
        for (int i = 0; i < 1001; i++) {
            scoringBasedAuditService.processTransaction(createTransaction(1.0));
        }
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        // Then
        assertTrue(completed, "Submission handler was not invoked in time");
        Submission submission = captureSubmission();
        assertNotNull(submission);
        assertEquals(1, submission.getBatches().size());
        assertEquals(1000, submission.getBatches().get(0).getTransactionCount());
        assertEquals(1000.0, submission.getBatches().get(0).getTotalValue());
    }

    @Test
    @DisplayName("Given batch size 1 and max value equal to transaction amount, should submit 1 batch")
    void testGivenBatchSize1MaxValueEqualToTransactionAmount_then1BatchSubmitted() throws InterruptedException {
        // Given
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // When
        scoringBasedAuditService.processTransaction(createTransaction(100.0));
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        // Then
        assertTrue(completed, "Submission handler was not invoked in time");
        Submission submission = captureSubmission();
        assertNotNull(submission);
        assertEquals(1, submission.getBatches().size());
        assertEquals(1, submission.getBatches().get(0).getTransactionCount());
        assertEquals(100.0, submission.getBatches().get(0).getTotalValue());
    }

    @Test
    @DisplayName("Given queue throws InterruptedException, should rethrow custom runtime exception")
    void testGivenQueueThrowsInterruptedException_thenCustomRuntimeExceptionRethrown() throws InterruptedException {
        // Given
        BlockingQueue<Transaction> mockQueue = mock(BlockingQueue.class);
        doThrow(new InterruptedException()).when(mockQueue).put(any());

        ScoringBasedAuditService service = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(service, "transactionQueue", mockQueue);

        // When & Then
        assertThrows(AuditTransactionProcessingException.class, () -> service.processTransaction(createTransaction(100.0)));
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