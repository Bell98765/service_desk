package com.servicedesk.sla;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlaBreachJob {
    private final SlaService sla;

    @Scheduled(fixedDelayString = "${app.sla.check-interval-ms:60000}")
    public void run() {
        sla.escalateOverdue();
    }
}
