package com.ho.account.ledger.web;

import com.ho.account.common.service.SourceDocumentService;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 보고서에서 원천 문서로의 드릴다운 기능을 제공하는 REST 컨트롤러.
 * GL/SL 잔액 -> 전표 -> 원천 문서 순서로 정보를 조회할 수 있도록 합니다.
 */
@RestController
@RequestMapping("/api/drilldown")
public class DrilldownController {

    private final JournalService journalService;
    private final SourceDocumentService sourceDocumentService;

    public DrilldownController(JournalService journalService, SourceDocumentService sourceDocumentService) {
        this.journalService = journalService;
        this.sourceDocumentService = sourceDocumentService;
    }

    /**
     * 전표 ID를 통해 해당 전표의 상세 정보를 조회합니다.
     * @param journalEntryId 조회할 전표의 ID
     * @return 전표 상세 정보
     */
    @GetMapping("/journal-entry/{journalEntryId}")
    public ResponseEntity<JournalEntry> getJournalEntryDetails(@PathVariable Long journalEntryId) {
        JournalEntry journalEntry = journalService.getJournalEntryWithDetails(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal Entry not found with ID: " + journalEntryId));
        return ResponseEntity.ok(journalEntry);
    }

    /**
     * 전표 ID를 통해 해당 전표와 연결된 원천 문서를 조회합니다.
     * @param journalEntryId 조회할 전표의 ID
     * @return 원천 문서 상세 정보
     */
    @GetMapping("/journal-entry/{journalEntryId}/source-document")
    public ResponseEntity<Map<String, Object>> getSourceDocumentForJournalEntry(@PathVariable Long journalEntryId) {
        JournalEntry journalEntry = journalService.getJournalEntryWithDetails(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal Entry not found with ID: " + journalEntryId));

        if (journalEntry.getLineageSourceType() == null || journalEntry.getLineageSourceId() == null) {
            throw new IllegalArgumentException("Source document information not available for Journal Entry ID: " + journalEntryId);
        }

        Map<String, Object> sourceDocument = sourceDocumentService.getSourceDocument(
                journalEntry.getLineageSourceType(),
                journalEntry.getLineageSourceId())
                .orElseThrow(() -> new IllegalArgumentException("Source document not found for type " + journalEntry.getLineageSourceType() + " and ID " + journalEntry.getLineageSourceId()));

        return ResponseEntity.ok(sourceDocument);
    }
}
