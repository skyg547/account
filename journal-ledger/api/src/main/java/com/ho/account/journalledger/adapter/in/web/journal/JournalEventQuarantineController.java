package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import com.ho.account.journalledger.application.service.journal.JournalEventQuarantineNotFoundException;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin-only inbound adapter for completeness visibility and controlled event replay. */
@RestController
@RequestMapping("/api/journals/event-quarantine")
public class JournalEventQuarantineController {

    private final KafkaJournalEventUseCase useCase;

    public JournalEventQuarantineController(KafkaJournalEventUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/summary")
    public SummaryView summary(
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles) {
        JournalCommandAuthorization.requireCompletenessOperator(actor, roles);
        var summary = useCase.completenessSummary();
        return new SummaryView(
                summary.quarantinedCount(),
                summary.replayedCount(),
                summary.oldestUnresolvedAt().orElse(null));
    }

    @GetMapping
    public List<EventView> find(
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles,
            @RequestParam(defaultValue = "QUARANTINED") JournalEventQuarantineStatus status,
            @RequestParam(defaultValue = "50") int limit) {
        JournalCommandAuthorization.requireCompletenessOperator(actor, roles);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Quarantine query limit must be between 1 and 100");
        }
        return useCase.find(status, limit).stream().map(EventView::from).toList();
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<ReplayView> replay(
            @PathVariable Long id,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles) {
        String operator = JournalCommandAuthorization.requireCompletenessOperator(actor, roles);
        var result = useCase.replay(id, operator);
        ReplayView body = new ReplayView(
                result.replayed(),
                result.newlyCreated(),
                result.journalEntry() == null ? null : result.journalEntry().getId(),
                EventView.from(result.quarantine()));
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(JournalEventQuarantineNotFoundException.class)
    ResponseEntity<Void> quarantineNotFound(JournalEventQuarantineNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    public record SummaryView(long quarantinedCount, long replayedCount, Instant oldestUnresolvedAt) {
    }

    public record ReplayView(boolean replayed, boolean newlyCreated, Long journalEntryId, EventView quarantine) {
    }

    /** Payload JSON is intentionally excluded from operational list responses. */
    public record EventView(
            Long id,
            String sourceTopic,
            int sourcePartition,
            long sourceOffset,
            JournalEventQuarantineStatus status,
            String reasonCode,
            Long journalEntryId,
            Instant firstSeenAt,
            Instant lastReplayAt,
            String lastReplayActor,
            int replayAttempts) {
        static EventView from(JournalEventQuarantine event) {
            return new EventView(
                    event.getId(),
                    event.getSourceTopic(),
                    event.getSourcePartition(),
                    event.getSourceOffset(),
                    event.getStatus(),
                    event.getReasonCode(),
                    event.getJournalEntryId(),
                    event.getFirstSeenAt(),
                    event.getLastReplayAt(),
                    event.getLastReplayActor(),
                    event.getReplayAttempts());
        }
    }
}
