package tech.challenge.audit.submission;

/**
 * Interface for handling submissions of transaction batches.
 * Implementations of this interface are responsible for processing
 * submissions that contain multiple batches of transactions.
 */
public interface SubmissionHandler {

    /**
     * Handles the processing of a given submission.
     *
     * @param submission the submission containing batches of transactions to process
     */
    void handle(Submission submission);
}