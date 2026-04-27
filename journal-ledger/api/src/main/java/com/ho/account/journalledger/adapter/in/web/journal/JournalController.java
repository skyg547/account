package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전표 관리 API 컨트롤러 (Inbound Adapter)
 * 헥사고날 아키텍처의 Inbound Port(JournalUseCase)를 통해 비즈니스 로직을 호출합니다.
 */
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalUseCase journalUseCase;

    /**
     * 수동 전표 생성
     */
    @PostMapping
    public ResponseEntity<JournalEntry> createJournalEntry(@RequestBody JournalEntry journalEntry) {
        try {
            JournalEntry createdEntry = journalUseCase.createJournalEntry(journalEntry);
            return ResponseEntity.ok(createdEntry);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 이벤트에 의한 전표 자동 생성 (룰 엔진 사용)
     */
    @PostMapping("/from-event")
    public ResponseEntity<JournalEntry> createJournalEntryFromEvent(@RequestBody Map<String, Object> eventData,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) {
        try {
            Optional<JournalEntry> createdEntry = journalUseCase.createJournalEntryFromEvent(eventData, accountingDate);
            return createdEntry.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 기간별 전표 목록 조회
     */
    @GetMapping
    public List<JournalEntry> getJournalEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return journalUseCase.getJournalEntriesByDate(startDate, endDate);
    }

    /**
     * 전표 번호로 상세 조회
     */
    @GetMapping("/{slipNo}")
    public ResponseEntity<JournalEntry> getJournalEntry(@PathVariable String slipNo) {
        return journalUseCase.getJournalEntryBySlipNo(slipNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 전표 승인
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveJournalEntry(@PathVariable Long id, @RequestParam(defaultValue = "ADMIN") String user) {
        try {
            journalUseCase.approveJournalEntry(id, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 전표 전기 (원장 반영)
     */
    @PostMapping("/{id}/post")
    public ResponseEntity<Void> postJournalEntry(@PathVariable Long id, @RequestParam(defaultValue = "ADMIN") String user) {
        try {
            journalUseCase.postJournalEntry(id, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
