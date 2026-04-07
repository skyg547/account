package com.ho.account.ledger.web;

import com.ho.account.common.service.SourceDocumentService;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drilldown")
public class DrilldownController {

    private final JournalService journalService;
    private final SourceDocumentService sourceDocumentService;

    public DrilldownController(JournalService journalService, SourceDocumentService sourceDocumentService) {
        this.journalService = journalService;
        this.sourceDocumentService = sourceDocumentService;
    }

    @GetMapping("/journal-entry/{journalEntryId}")
    public ResponseEntity<Map<String, Object>> getJournalEntryDetails(@PathVariable Long journalEntryId) {
        JournalEntry journalEntry = journalService.getJournalEntryWithDetails(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal Entry not found with ID: " + journalEntryId));
        return ResponseEntity.ok(toJournalEntryResponse(journalEntry));
    }

    @GetMapping("/journal-entry/{journalEntryId}/source-document")
    public ResponseEntity<Map<String, Object>> getSourceDocumentForJournalEntry(@PathVariable Long journalEntryId) {
        JournalEntry journalEntry = journalService.getJournalEntryWithDetails(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal Entry not found with ID: " + journalEntryId));

        if (journalEntry.getLineageSourceType() == null || journalEntry.getLineageSourceId() == null) {
            throw new IllegalArgumentException(
                    "Source document information not available for Journal Entry ID: " + journalEntryId);
        }

        Map<String, Object> sourceDocument = sourceDocumentService.getSourceDocument(
                        journalEntry.getLineageSourceType(),
                        journalEntry.getLineageSourceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source document not found for type " + journalEntry.getLineageSourceType()
                                + " and ID " + journalEntry.getLineageSourceId()));

        return ResponseEntity.ok(sourceDocument);
    }

    private Map<String, Object> toJournalEntryResponse(JournalEntry journalEntry) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", journalEntry.getId());
        response.put("slipNo", journalEntry.getSlipNo());
        response.put("slipDate", journalEntry.getSlipDate());
        response.put("accountingDate", journalEntry.getAccountingDate());
        response.put("description", journalEntry.getDescription());
        response.put("status", journalEntry.getStatus());
        response.put("entryType", journalEntry.getEntryType());
        response.put("lineageSourceType", journalEntry.getLineageSourceType());
        response.put("lineageSourceId", journalEntry.getLineageSourceId());
        response.put("details", journalEntry.getDetails().stream().map(detail -> {
            Map<String, Object> detailResponse = new LinkedHashMap<>();
            detailResponse.put("id", detail.getId());
            detailResponse.put("drcrType", detail.getDrcrType());
            detailResponse.put("amount", detail.getAmount());
            detailResponse.put("baseAmount", detail.getBaseAmount());
            detailResponse.put("detailDescription", detail.getDetailDescription());
            detailResponse.put("accountCode",
                    detail.getAccountSubject() != null ? detail.getAccountSubject().getCode() : null);
            detailResponse.put("departmentCode",
                    detail.getDepartment() != null ? detail.getDepartment().getCode() : null);
            detailResponse.put("businessPartnerCode",
                    detail.getBusinessPartner() != null ? detail.getBusinessPartner().getBusinessPartnerCode() : null);
            return detailResponse;
        }).toList());
        return response;
    }
}
