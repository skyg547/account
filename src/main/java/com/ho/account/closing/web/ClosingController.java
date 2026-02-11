package com.ho.account.closing.web;

import com.ho.account.closing.domain.ClosingPeriod;
import com.ho.account.closing.service.AnnualClosingService;
import com.ho.account.closing.service.ClosingService;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 결산(일/월/년) 관련 API 엔드포인트를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/closing")
public class ClosingController {

    private final ClosingService closingService;
    private final JournalService journalService;
    private final AnnualClosingService annualClosingService;

    @Autowired
    public ClosingController(ClosingService closingService, JournalService journalService, AnnualClosingService annualClosingService) {
        this.closingService = closingService;
        this.journalService = journalService;
        this.annualClosingService = annualClosingService;
    }

    // --- 일 마감 ---
    @PostMapping("/day/{date}")
    public ResponseEntity<Void> closeDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            closingService.closeDay(date, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/day/{date}/cancel")
    public ResponseEntity<Void> cancelDayClosing(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            closingService.cancelDayClosing(date);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/day/{date}/status")
    public ResponseEntity<Boolean> isDayClosed(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(closingService.isDayClosed(date));
    }

    // --- 월 마감 ---
    @PostMapping("/month/{yearMonthId}")
    public ResponseEntity<ClosingPeriod> closeMonth(@PathVariable Long yearMonthId, @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            ClosingPeriod closingPeriod = closingService.closeMonth(yearMonthId, userId);
            return ResponseEntity.ok(closingPeriod);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/month/{yearMonthId}/reopen/request")
    public ResponseEntity<ClosingPeriod> requestMonthReopen(@PathVariable Long yearMonthId, @RequestBody Map<String, String> body) {
        try {
            String requestorId = body.get("requestorId");
            String reason = body.get("reason");
            ClosingPeriod closingPeriod = closingService.requestMonthReopen(yearMonthId, requestorId, reason);
            return ResponseEntity.ok(closingPeriod);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/month/{yearMonthId}/reopen/approve")
    public ResponseEntity<ClosingPeriod> approveMonthReopen(@PathVariable Long yearMonthId, @RequestBody Map<String, String> body) {
        try {
            String approverId = body.get("approverId");
            ClosingPeriod closingPeriod = closingService.approveMonthReopen(yearMonthId, approverId);
            return ResponseEntity.ok(closingPeriod);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/month/{yearMonthId}/reopen/reject")
    public ResponseEntity<ClosingPeriod> rejectMonthReopen(@PathVariable Long yearMonthId, @RequestBody Map<String, String> body) {
        try {
            String approverId = body.get("approverId");
            ClosingPeriod closingPeriod = closingService.rejectMonthReopen(yearMonthId, approverId);
            return ResponseEntity.ok(closingPeriod);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/month/{yearMonthId}/status")
    public ResponseEntity<Boolean> isMonthClosed(@PathVariable Long yearMonthId) {
        return ResponseEntity.ok(closingService.isMonthClosed(yearMonthId));
    }

    // --- 결산 보정 및 연차 결산 ---

    /**
     * 결산 보정 분개를 생성합니다.
     * @param journalEntry 결산 보정 전표 데이터
     * @return 생성된 전표
     */
    @PostMapping("/adjustment")
    public ResponseEntity<JournalEntry> createAdjustmentEntry(@RequestBody JournalEntry journalEntry) {
        try {
            // 전표 유형을 '결산보정'으로 설정
            journalEntry.setEntryType("ADJUSTMENT");
            JournalEntry createdEntry = journalService.createJournalEntry(journalEntry);
            return ResponseEntity.ok(createdEntry);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 연차 결산(손익 대체)을 수행합니다.
     * @param year 결산할 연도
     * @param body 이익잉여금 계정 코드 포함
     * @return 성공 응답
     */
    @PostMapping("/annual/{year}")
    public ResponseEntity<Void> performAnnualClosing(@PathVariable int year, @RequestBody Map<String, String> body) {
        try {
            String retainedEarningsAccountCode = body.get("retainedEarningsAccountCode");
            annualClosingService.performIncomeStatementClosing(year, retainedEarningsAccountCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
