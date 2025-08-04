package tech.challenge.audit.service;

import tech.challenge.domain.Transaction;

/**
 * Auditing services that process transactions.
 * Implementations of this interface are responsible for handling
 * the auditing of financial transactions.
 *
 */
public interface AuditService {

    /**
     * Processes a given transaction for auditing purposes.
     *
     * @param tx the transaction to be audited
     */
    void processTransaction(Transaction tx);
}