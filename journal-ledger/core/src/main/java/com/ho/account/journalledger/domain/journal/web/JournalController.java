package com.ho.account.journal.web;

import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/journals")
public class JournalController {

    private final JournalService journalService;

    @Autowired
    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    // 전표 생성
    @PostMapping
    public ResponseEntity<JournalEntry> createJournalEntry(@RequestBody JournalEntry journalEntry) {
        try {
            JournalEntry createdEntry = journalService.createJournalEntry(journalEntry);
            return ResponseEntity.ok(createdEntry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 전표 자동 생성 (From Event)
    @PostMapping("/from-event")
    public ResponseEntity<JournalEntry> createJournalEntryFromEvent(@RequestBody Map<String, String> eventData,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate accountingDate) {
        try {
            Optional<JournalEntry> createdEntry = journalService.createJournalEntryFromEvent(eventData, accountingDate);
            return createdEntry.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build()); // Or BadRequest if no rule matches
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 전표 수정
    @PutMapping("/{id}")
    public ResponseEntity<JournalEntry> updateJournalEntry(@PathVariable Long id,
            @RequestBody JournalEntry journalEntry) {
        try {
            JournalEntry updatedEntry = journalService.updateJournalEntry(id, journalEntry);
            return ResponseEntity.ok(updatedEntry);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 전표 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournalEntry(@PathVariable Long id) {
        try {
            journalService.deleteJournalEntry(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 전표 조회 (기간별)
    @GetMapping
    public List<JournalEntry> getJournalEntries(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return journalService.getJournalEntriesByDate(startDate, endDate);
    }

    // 전표 상세 조회 (전표번호)
    @GetMapping("/{slipNo}")
    public ResponseEntity<JournalEntry> getJournalEntry(@PathVariable String slipNo) {
        return journalService.getJournalEntryBySlipNo(slipNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 승인 요청
    @PostMapping("/{id}/request")
    public ResponseEntity<Void> requestApproval(@PathVariable Long id) {
        try {
            journalService.requestApproval(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 전표 승인
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveJournalEntry(@PathVariable Long id) {
        try {
            journalService.approveJournalEntry(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 전표 반려
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectJournalEntry(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            journalService.rejectJournalEntry(id, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 전표 전기 (Post)
    @PostMapping("/{id}/post")
    public ResponseEntity<Void> postJournalEntry(@PathVariable Long id) {
        try {
            journalService.postJournalEntry(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 전표 역분개 (Reverse)
    @PostMapping("/{id}/reverse")
    public ResponseEntity<JournalEntry> reverseJournalEntry(@PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate reversalDate) {
        try {
            // If no reversalDate is provided, use today's date
            LocalDate actualReversalDate = (reversalDate != null) ? reversalDate : LocalDate.now();
            JournalEntry reversedEntry = journalService.reverseJournalEntry(id, actualReversalDate);
            return ResponseEntity.ok(reversedEntry);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
