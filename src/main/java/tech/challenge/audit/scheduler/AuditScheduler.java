package tech.challenge.audit.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.challenge.audit.service.ScoringBasedAuditService;

@Component
public class AuditScheduler {

    private final ScoringBasedAuditService auditService;

    public AuditScheduler(ScoringBasedAuditService auditService) {
        this.auditService = auditService;
    }

    @Scheduled(fixedRate = 5000) // every 5 seconds
    public void submitAuditBatches() {
        auditService.processSubmissions();
    }
}
