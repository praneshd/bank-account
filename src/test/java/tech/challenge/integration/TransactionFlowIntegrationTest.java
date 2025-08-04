package tech.challenge.integration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tech.challenge.TestConfig;
import tech.challenge.producer.service.TransactionProducer;
import tech.challenge.audit.submission.Submission;
import tech.challenge.audit.submission.SubmissionHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TestConfig.class)
class TransactionFlowIntegrationTest {

    @Autowired
    private TransactionProducer transactionProducer;

    @MockBean
    private SubmissionHandler submissionHandler;

    @Test
    void testTransactionFlow() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(submissionHandler).handle(any(Submission.class));

        // When
        transactionProducer.start();
        boolean completed = latch.await(10, TimeUnit.SECONDS);

        // Then
       assertThat(completed).isTrue();

        verify(submissionHandler, atLeastOnce()).handle(submissionCaptor.capture());
        Submission capturedSubmission = submissionCaptor.getValue();

        assertThat(capturedSubmission).isNotNull();
        assertThat(capturedSubmission.getBatches()).isNotEmpty();

        // Validate batch properties
        capturedSubmission.getBatches().forEach(batch -> {
            assertThat(batch.getTotalValue()).isGreaterThanOrEqualTo(100000.0);
            assertThat(batch.getTransactionCount()).isGreaterThan(0);
        });
    }
}