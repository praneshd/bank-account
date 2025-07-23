package tech.challenge.audit.service;

import tech.challenge.domain.Transaction;

public interface AuditService {

    void processTransaction(Transaction tx);
}
