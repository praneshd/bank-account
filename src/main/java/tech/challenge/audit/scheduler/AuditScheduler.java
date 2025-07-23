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

    @Scheduled(fixedRate = 1000*60*60*4) // every 4 hrs in case we have any left over messages just for precaution
    public void submitAuditBatches() {
        //auditService.processSubmissions();
    }
}
