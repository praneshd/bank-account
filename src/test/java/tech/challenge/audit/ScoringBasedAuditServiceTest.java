package tech.challenge.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringBasedAuditServiceTest {

    @Mock
    private SubmissionHandler submissionHandler;

    private ScoringBasedAuditService scoringBasedAuditService;


    // Helper method to create a Transaction
    private Transaction createTransaction(double amount) {
        return Transaction.builder()
                .id("test-id")
                .amount(amount)
                .build();
    }


    @Test
    void test_given10Transactions100MaxValue1Worker_shouldCreate4Batches() throws InterruptedException {
        // Provide 12 transactions
        double[] transactionAmounts = {30, -40, 45, 25, -45, 65, -11, 5, 75, 25, -62, 24};

        // Set test configuration via reflection
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 10);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);

        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(1);

        // Intercept submission and count down when called
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());


        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Wait for async thread to finish
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        // Now assert captured submission
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
    void test_given4TransactionsBatch100MaxValue4WorkerThread_shouldCreate4Batches() throws InterruptedException {
        double[] transactionAmounts = {12, 34, 1, 45, -4, -30, -40, 45, 25, -45, 65, -11, 5, 75, 25, -62};

        // Set test configuration via reflection
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 4);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 4);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);

        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(4);

        // Intercept submission and count down when called
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // Provide 16 transactions (so 4 batches of 4 transactions should trigger 4 submissions)

        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Wait for async thread to finish
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        // Assert submissions captured
        List<Submission> submissions = submissionCaptor.getAllValues();
        assertNotNull(submissions, "Submissions should not be null");
        assertEquals(4, submissions.size(), "Should have captured 4 submissions");

        // Flatten batches from all submissions
        List<Batch> allBatches = submissions.stream()
                .flatMap(s -> s.getBatches().stream())
                .toList();

        assertEquals(4, submissions.size(), "Should have 4 submission total");
        assertEquals(7, allBatches.size(), "Should have 7 batches total");

        // Assert each batch meets the size and max value constraint
        for (Batch batch : allBatches) {
            double total = batch.getTotalValue();
            assertTrue(batch.getTransactionCount() <= 4, "Batch exceeds max transactions per submission");
            assertTrue(Math.abs(total) <= 100.0, "Batch exceeds max batch total value");
        }

        // Optionally: log or print for visual check
        for (int i = 0; i < allBatches.size(); i++) {
            Batch b = allBatches.get(i);
            double total =  b.getTotalValue();
            System.out.printf("Batch %d: size=%d, total=%.2f%n", i + 1, b.getTransactionCount(), total);
        }
    }

    @Test
    void testEdgeCases_given10Transactions5MaxValue1Worker_shouldCreate1Batches() throws InterruptedException {
        // Set test configuration via reflection
        double[] transactionAmounts = {1,0,1,0,1,0,1,0,1,0};

        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 10);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 5);

        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(1);

        // Intercept submission and count down when called
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // Provide 12 transactions
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Wait for async thread to finish
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        // Now assert captured submission
        Submission submission = submissionCaptor.getValue();
        assertNotNull(submission, "Submission should not be null");

        List<Batch> batches = submission.getBatches();
        assertEquals(1, batches.size());

        assertBatch(batches.get(0), 10, 5); // 30, 40, 25, 5

    }

    @Test
    void testEdgeCases_givenEachTransactionsEqualMaxValue1Worker_shouldCreateBatchForEachTransaction() throws InterruptedException {
        // Set test configuration via reflection
        double[] transactionAmounts = {100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100};

        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 20);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100);

        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(1);

        // Intercept submission and count down when called
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        // Provide 12 transactions
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Wait for async thread to finish
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        // Now assert captured submission
        Submission submission = submissionCaptor.getValue();
        assertNotNull(submission, "Submission should not be null");

        List<Batch> batches = submission.getBatches();
        assertEquals(20, batches.size());
        batches.forEach(batch -> assertBatch(batch, 1, 100));


    }

    @Test
    void test_givenSingleTransactionExceedValue_then0BatchesSubmitted() throws InterruptedException {

        double[] transactionAmounts = {101};

        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);

        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(1);

        // Provide transactions
        for (double amount : transactionAmounts) {
            scoringBasedAuditService.processTransaction(createTransaction(amount));
        }

        // Wait for a short time to ensure no invocation happens
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertFalse(completed, "Submission handler should not have been invoked");

        // Verify that the submission handler was never invoked
        verify(submissionHandler, times(0)).handle(any(Submission.class));
    }

    @Test
    void test_givenMoreThanTransactionsInQueue_shouldSubmitOnlyMaxTransactionsPerSubmission() throws InterruptedException {

        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1000);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100_000.0);

        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(1);

        // Intercept submission and count down when called
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());

        for (int i = 0; i < 1001; i++) {
            scoringBasedAuditService.processTransaction(createTransaction(1.0));
        }

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        Submission submission = captureSubmission();
        assertNotNull(submission);
        assertEquals(1, submission.getBatches().size());
        assertEquals(1000, submission.getBatches().get(0).getTransactionCount());
        assertEquals(1000.0, submission.getBatches().get(0).getTotalValue());
    }

    @Test
    void test_givenBatchSize1MaxValueEqualToTransactionAmount_then1BatchSubmitted() throws InterruptedException {
        ScoringBasedAuditService scoringBasedAuditService = new ScoringBasedAuditService(submissionHandler, 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxTransactionsPerSubmission", 1);
        ReflectionTestUtils.setField(scoringBasedAuditService, "maxBatchTotalValue", 100.0);
        // Prepare latch to wait for async submission
        CountDownLatch latch = new CountDownLatch(1);

        // Intercept submission and count down when called
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(submissionCaptor.capture());


        scoringBasedAuditService.processTransaction(createTransaction(100.0));

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Submission handler was not invoked in time");

        Submission submission = captureSubmission();
        assertNotNull(submission);
        assertEquals(1, submission.getBatches().size());
        assertEquals(1, submission.getBatches().get(0).getTransactionCount());
        assertEquals(100.0, submission.getBatches().get(0).getTotalValue());
    }

    @Test
    void test_givenQueueThrowsInterruptedException_thenCustomRuntimeExceptionRethrown() throws InterruptedException {
        BlockingQueue<Transaction> mockQueue = mock(BlockingQueue.class);
        doThrow(new InterruptedException()).when(mockQueue).put(any());

        ScoringBasedAuditService service = new ScoringBasedAuditService(submissionHandler,1);
        ReflectionTestUtils.setField(service, "transactionQueue", mockQueue);

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