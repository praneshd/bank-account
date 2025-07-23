package tech.challenge.exception;

public class AuditTransactionProcessingException extends RuntimeException {
    public AuditTransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}