package com.ho.account.reconciliation.service;

import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.reconciliation.domain.BankStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutomatedMatchingEngineTest {

    private final AutomatedMatchingEngine matchingEngine = new AutomatedMatchingEngine();

    @Test
    @DisplayName("금액과 날짜가 정확히 일치하는 경우 매칭 성공해야 함")
    void testExactMatch() {
        BankStatement stmt = new BankStatement();
        stmt.setTransactionDate(LocalDate.of(2026, 2, 13));
        stmt.setDepositAmount(new BigDecimal("100.00"));
        stmt.setWithdrawalAmount(BigDecimal.ZERO);
        stmt.setDescription("입금 테스트");

        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(LocalDate.of(2026, 2, 13));

        JournalDetail detail = new JournalDetail();
        detail.setJournalEntry(entry);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setDetailDescription("장부 입금");

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(stmt), List.of(detail));

        assertEquals(1, results.size());
        assertTrue(results.get(0).isMatch());
        assertEquals("EXACT_DATE_AMOUNT_MATCH", results.get(0).getMatchReason());
    }

    @Test
    @DisplayName("금액이 다른 경우 매칭 실패해야 함")
    void testAmountMismatch() {
        BankStatement stmt = new BankStatement();
        stmt.setTransactionDate(LocalDate.of(2026, 2, 13));
        stmt.setDepositAmount(new BigDecimal("100.00"));
        stmt.setWithdrawalAmount(BigDecimal.ZERO);

        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(LocalDate.of(2026, 2, 13));

        JournalDetail detail = new JournalDetail();
        detail.setJournalEntry(entry);
        detail.setAmount(new BigDecimal("100.01"));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(stmt), List.of(detail));

        assertFalse(results.get(0).isMatch());
    }
}
