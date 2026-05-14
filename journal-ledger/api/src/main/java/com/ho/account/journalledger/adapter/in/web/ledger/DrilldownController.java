package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전표 역추적 (Drill-down) 컨트롤러.
 * 원천 문서 ID를 기반으로 생성된 전표 목록을 조회합니다.
 */
@RestController
@RequestMapping("/api/v1/drilldown")
@RequiredArgsConstructor
public class DrilldownController {

    private final JournalEntryService journalEntryService;

    /**
     * 원천 문서 역추적 조회
     * 예: /api/v1/drilldown/PURCHASE_INVOICE/INV-2026-001
     * 
     * @param sourceType 원천 문서 유형 (예: PURCHASE_INVOICE)
     * @param sourceId 원천 문서 식별자 (예: INV-2026-001)
     * @return 원천 문서에 의해 생성된 전표 목록 및 상세 내역
     */
    @GetMapping("/{sourceType}/{sourceId}")
    public ResponseEntity<List<Map<String, Object>>> getJournalEntriesBySource(
            @PathVariable String sourceType,
            @PathVariable String sourceId) {

        List<JournalEntry> entries = journalEntryService.getJournalEntriesBySource(sourceType, sourceId);

        List<Map<String, Object>> response = entries.stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("journalEntryId", entry.getId());
            map.put("slipNo", entry.getSlipNo());
            map.put("accountingDate", entry.getAccountingDate());
            map.put("status", entry.getStatus());
            map.put("description", entry.getDescription());
            map.put("entryType", entry.getEntryType());
            map.put("currencyCode", entry.getCurrencyCode());

            List<Map<String, Object>> details = entry.getDetails().stream().map(detail -> {
                Map<String, Object> detailResponse = new HashMap<>();
                detailResponse.put("journalDetailId", detail.getId());
                detailResponse.put("side", detail.getSide());
                detailResponse.put("accountCode", detail.getAccountCode());
                detailResponse.put("departmentCode", detail.getDepartmentCode());
                detailResponse.put("businessPartnerCode", detail.getBusinessPartnerCode());
                detailResponse.put("amount", detail.getAmount());
                detailResponse.put("baseAmount", detail.getBaseAmount());
                detailResponse.put("detailDescription", detail.getDetailDescription());
                return detailResponse;
            }).collect(Collectors.toList());

            map.put("details", details);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}