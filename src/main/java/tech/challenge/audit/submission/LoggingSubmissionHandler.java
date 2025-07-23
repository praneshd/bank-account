package tech.challenge.audit.submission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingSubmissionHandler implements SubmissionHandler {

    @Override
    public void handle(Submission submission) {
        // Log the submission details
        log.info("Handling submission: {}", submission);
    }
}