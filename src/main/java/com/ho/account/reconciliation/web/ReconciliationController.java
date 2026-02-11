package com.ho.account.reconciliation.web;

import com.ho.account.reconciliation.domain.ReconciliationResult;
import com.ho.account.reconciliation.domain.ReconciliationType;
import com.ho.account.reconciliation.domain.ReconciliationVariance;
import com.ho.account.reconciliation.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Autowired
    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // --- Reconciliation Execution Endpoints ---

    @PostMapping("/run/source-standard")
    public ResponseEntity<ReconciliationResult> runSourceStandardReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reconciliationDate,
            @RequestBody Map<String, String> body) {
        try {
            String runBy = body.get("runBy");
            ReconciliationResult result = reconciliationService.performSourceStandardReconciliation(reconciliationDate, runBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/run/account-totals")
    public ResponseEntity<ReconciliationResult> runAccountTotalsReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reconciliationDate,
            @RequestBody Map<String, String> body) {
        try {
            String runBy = body.get("runBy");
            ReconciliationResult result = reconciliationService.performAccountTotalsReconciliation(reconciliationDate, runBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/run/bank-account")
    public ResponseEntity<ReconciliationResult> runBankAccountReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reconciliationDate,
            @RequestBody Map<String, String> body) {
        try {
            String runBy = body.get("runBy");
            ReconciliationResult result = reconciliationService.performBankAccountReconciliation(reconciliationDate, runBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // --- Reconciliation Report/Query Endpoints ---

    @GetMapping("/results")
    public ResponseEntity<List<ReconciliationResult>> getAllReconciliationResults() {
        return ResponseEntity.ok(reconciliationService.getAllReconciliationResults());
    }

    @GetMapping("/results/type/{type}")
    public ResponseEntity<List<ReconciliationResult>> getReconciliationResultsByType(@PathVariable ReconciliationType type) {
        return ResponseEntity.ok(reconciliationService.getReconciliationResultsByType(type));
    }

    @GetMapping("/results/{resultId}/variances")
    public ResponseEntity<List<ReconciliationVariance>> getVariancesByReconciliationResult(@PathVariable Long resultId) {
        return ResponseEntity.ok(reconciliationService.getVariancesByReconciliationResult(resultId));
    }

    // --- Variance Resolution Endpoints ---

    @PostMapping("/variances/{varianceId}/resolve-with-adjustment")
    public ResponseEntity<ReconciliationVariance> resolveVarianceWithAdjustment(
            @PathVariable Long varianceId,
            @RequestBody Map<String, Object> body) { // Using Map for flexibility, JournalEntry might be complex
        try {
            // TODO: In a real scenario, this would involve creating a JournalEntry object
            // from the request body or linking to an existing one.
            // For now, we'll pass a dummy JournalEntry or handle it in service.
            // Assuming 'journalEntryId' is passed and JournalService can fetch it.
            // For this example, we'll just mock the journalEntry.

            // Dummy JournalEntry for compilation, actual implementation would fetch/create it
            // JournalEntry journalEntry = journalService.getJournalEntryById( (Long)body.get("journalEntryId") );
            // For now, creating a new dummy JournalEntry just to satisfy the method signature
            com.ho.account.journal.domain.JournalEntry dummyJournalEntry = new com.ho.account.journal.domain.JournalEntry();
            dummyJournalEntry.setId(1L); // Mock ID

            String resolvedBy = (String) body.get("resolvedBy");
            ReconciliationVariance updatedVariance = reconciliationService.resolveVarianceWithAdjustment(varianceId, dummyJournalEntry, resolvedBy);
            return ResponseEntity.ok(updatedVariance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
