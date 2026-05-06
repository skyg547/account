package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 전기 서비스 (Posting Service) — 전표를 원장에 반영합니다.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전기(Posting)는 회계에서 "전표 작성"과 "장부 반영"을 구분하는 핵심 단계입니다.
 *
 * 전기 전: 전표는 작성되었지만 원장에 반영되지 않아 재무제표에 영향이 없습니다.
 * 전기 후: 원장(GL/SL)에 잔액이 반영되어 재무제표 작성의 기초 데이터가 됩니다.
 *
 * 전기 처리 시 발생하는 일 (전표 상세 라인 1개당):
 *   1. GL Entry(총계정원장 분개항목) 생성 — 계정과목 단위 기록
 *   2. SL Entry(보조원장 분개항목) 생성  — 계정과목+거래처+부서 단위 기록
 *   3. GL Balance(총계정원장 잔액) 갱신  — 차변/대변 누적 및 기말잔액 재계산
 *   4. SL Balance(보조원장 잔액) 갱신    — 거래처별/부서별 잔액 갱신
 *
 * 예시:
 *   전표: 차변 매출채권(11000) 100,000 / 대변 매출(41000) 100,000
 *   전기 후:
 *     GlEntry: 매출채권 차변 100,000 생성
 *     GlEntry: 매출     대변 100,000 생성
 *     SlEntry: 매출채권+거래처A 차변 100,000 생성
 *     GlBalance: 매출채권 잔액 +100,000
 *     SlBalance: 매출채권+거래처A 잔액 +100,000
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - JournalEntryService.postJournalEntry()가 전표 상태를 POSTED로 변경한 후,
 *   이 서비스가 원장 항목(Entry)과 잔액(Balance)을 생성/갱신합니다.
 * - 전표 상세 라인(JournalDetail) 하나당 GlEntry 1개 + SlEntry 1개가 생성됩니다.
 * - 잔액 갱신(Carry-forward 포함)은 LedgerService에 위임합니다.
 * - POSTED 또는 REVERSED 상태의 전표에 재전기를 시도하면 예외가 발생합니다.
 *
 * 트랜잭션:
 *   전체 전기 과정이 하나의 트랜잭션으로 묶입니다.
 *   전기 도중 오류 발생 시 전표 상태 변경, Entry 생성, Balance 갱신이 모두 롤백됩니다.
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class PostingService {

    /**
     * 전표 저장소.
     * 전기할 전표를 조회하고, 상태(POSTED)를 저장합니다.
     */
    private final JournalEntryRepository journalEntryRepository;

    /**
     * GL Entry 저장소.
     * 총계정원장 분개항목(계정과목 단위)을 저장합니다.
     */
    private final GlEntryRepository glEntryRepository;

    /**
     * SL Entry 저장소.
     * 보조원장 분개항목(계정과목+거래처+부서 단위)을 저장합니다.
     */
    private final SlEntryRepository slEntryRepository;

    /**
     * 원장 잔액 서비스.
     * GL Balance / SL Balance 잔액 갱신 및 Carry-forward(기초잔액 이월)를 담당합니다.
     */
    private final LedgerService ledgerService;

    /**
     * 전표를 원장에 전기합니다.
     *
     * [업무 설명]
     * APPROVED 상태의 전표를 원장(GL/SL)에 공식 반영합니다.
     * 이 메서드 호출 후 재무제표에 해당 거래가 반영됩니다.
     *
     * 처리 순서:
     *   1. 전표 조회 및 상태 검증 (이미 전기/역전기된 전표 재전기 방지)
     *   2. 전표 상태 → POSTED 변경 및 저장
     *   3. 전표 상세 라인(JournalDetail) 반복 처리:
     *      a. GL Entry 생성 (총계정원장 기록)
     *      b. SL Entry 생성 (보조원장 기록)
     *      c. GL Balance / SL Balance 잔액 갱신 (Carry-forward 포함)
     *
     * [개발 설명]
     * - 차변/대변 구분: JournalSide.DEBIT이면 drAmount, 아니면 crAmount에 값 설정
     * - amount vs baseAmount:
     *     detail.getAmount()     → 거래통화 금액 (외화일 경우 USD 그대로)
     *     detail.getBaseAmount() → 기본통화(KRW) 환산 금액 (잔액 계산 기준)
     * - lineageSourceType/lineageSourceId: JournalEntry 헤더에서 복사하여
     *   "GlEntry → 원천 문서" 역추적(drill-down)에 사용
     *
     * @param journalEntryId 전기할 전표의 내부 PK
     * @throws IllegalArgumentException 존재하지 않는 전표 ID
     * @throws IllegalStateException    이미 POSTED 또는 REVERSED 상태인 전표
     */
    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        // ─── 1단계: 전표 조회 및 중복 전기 방지 ───────────────────────────
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        // 이미 전기(POSTED)되었거나 역전기(REVERSED)된 전표는 재전기 불가
        if (journalEntry.getStatus() == JournalEntryStatus.POSTED || journalEntry.getStatus() == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("JournalEntry is already posted or reversed.");
        }

        // ─── 2단계: 전표 상태 POSTED로 변경 ────────────────────────────────
        journalEntry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(journalEntry);

        // ─── 3단계: 회계연도/회계기간 계산 ─────────────────────────────────
        LocalDate accountingDate = journalEntry.getAccountingDate();
        // 회계연도: "2026" (4자리 연도 문자열)
        String fiscalYear = String.valueOf(accountingDate.getYear());
        // 회계기간: "01" ~ "12" (2자리 월 문자열, 앞자리 0 패딩)
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        // ─── 4단계: 전표 상세 라인별 원장 항목 준비 (벌크 처리를 위해 리스트에 수집) ────────
        List<GlEntry> glEntries = new ArrayList<>();
        List<SlEntry> slEntries = new ArrayList<>();
        List<JournalDetail> details = journalEntry.getDetails();

        for (JournalDetail detail : details) {
            boolean isDebit = JournalSide.DEBIT.equals(detail.getSide());

            // ── 4-1. GL Entry 준비 ──────────────────────
            GlEntry glEntry = new GlEntry();
            glEntry.setJournalDetail(detail);
            glEntry.setAccount(detail.getAccountSubject());
            glEntry.setFiscalYear(fiscalYear);
            glEntry.setFiscalPeriod(fiscalPeriod);
            glEntry.setPostingDate(accountingDate);
            glEntry.setCurrency(journalEntry.getCurrency());
            glEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            glEntry.setLineageSourceId(journalEntry.getLineageSourceId());

            if (isDebit) {
                glEntry.setDrAmount(detail.getAmount());
                glEntry.setCrAmount(BigDecimal.ZERO);
                glEntry.setBaseDrAmount(detail.getBaseAmount());
                glEntry.setBaseCrAmount(BigDecimal.ZERO);
            } else {
                glEntry.setDrAmount(BigDecimal.ZERO);
                glEntry.setCrAmount(detail.getAmount());
                glEntry.setBaseDrAmount(BigDecimal.ZERO);
                glEntry.setBaseCrAmount(detail.getBaseAmount());
            }
            glEntries.add(glEntry);

            // ── 4-2. SL Entry 준비 ───────────────────────
            SlEntry slEntry = new SlEntry();
            slEntry.setJournalDetail(detail);
            slEntry.setAccount(detail.getAccountSubject());
            slEntry.setBusinessPartner(detail.getBusinessPartner());
            slEntry.setDepartment(detail.getDepartment());
            slEntry.setFiscalYear(fiscalYear);
            slEntry.setFiscalPeriod(fiscalPeriod);
            slEntry.setPostingDate(accountingDate);
            slEntry.setCurrency(journalEntry.getCurrency());
            slEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            slEntry.setLineageSourceId(journalEntry.getLineageSourceId());

            if (isDebit) {
                slEntry.setDrAmount(detail.getAmount());
                slEntry.setCrAmount(BigDecimal.ZERO);
                slEntry.setBaseDrAmount(detail.getBaseAmount());
                slEntry.setBaseCrAmount(BigDecimal.ZERO);
            } else {
                slEntry.setDrAmount(BigDecimal.ZERO);
                slEntry.setCrAmount(detail.getAmount());
                slEntry.setBaseDrAmount(BigDecimal.ZERO);
                slEntry.setBaseCrAmount(detail.getBaseAmount());
            }
            slEntries.add(slEntry);
        }

        // ─── 5단계: 벌크 저장 및 잔액 갱신 ────────────────────────────────
        glEntryRepository.saveAll(glEntries);
        slEntryRepository.saveAll(slEntries);
        
        // LedgerService의 벌크 갱신 메서드 호출
        ledgerService.updateLedgerBalancesBulk(details);
    }
}
