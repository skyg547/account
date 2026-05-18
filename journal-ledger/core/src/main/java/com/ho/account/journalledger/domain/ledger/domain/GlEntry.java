package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 총계정원장 분개 항목 (General Ledger Entry, GL Entry).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(JournalEntry)가 전기(Posting)되면, 각 전표 상세 라인(JournalDetail)이
 * 총계정원장(GL: General Ledger) 항목으로 기록됩니다.
 *
 * GL Entry는 회계상 가장 기본적인 원장 기록 단위입니다.
 *   - 계정과목별로 차변/대변 금액을 기록합니다.
 *   - 재무제표(손익계산서, 재무상태표) 작성의 기초 데이터입니다.
 *   - 회계연도(fiscalYear) + 회계기간(fiscalPeriod)으로 기간별 집계가 가능합니다.
 *
 * 주요 필드:
 *   - journalDetail  : 원천 전표 상세 라인 (drill-down 시 역추적 가능)
 *   - account        : 계정과목 (예: 현금, 매출채권, 복리후생비 등)
 *   - fiscalYear     : 회계연도 (예: "2026")
 *   - fiscalPeriod   : 회계기간/월 (예: "01" ~ "12")
 *   - drAmount       : 거래통화 차변 금액
 *   - crAmount       : 거래통화 대변 금액
 *   - baseDrAmount   : 기본통화(KRW) 차변 금액
 *   - baseCrAmount   : 기본통화(KRW) 대변 금액
 *   - lineageSourceType / lineageSourceId : 원천 문서 추적 (drill-down용)
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - gl_entries 테이블에 매핑됩니다.
 * - PostingService.postJournalEntry()에서 JournalDetail 하나당 GlEntry 하나가 생성됩니다.
 * - drAmount/crAmount: 외화 거래 원본 금액 (조회용)
 * - baseDrAmount/baseCrAmount: KRW 환산 금액 (GlBalance 잔액 계산 기준)
 * - lineageSourceType + lineageSourceId: JournalEntry 헤더에서 복사하여
 *   "GL Entry → 원천 문서" 역추적(drill-down)에 사용합니다.
 * - Lombok @Getter/@Setter/@NoArgsConstructor 사용으로 보일러플레이트 최소화.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "gl_entries")
@Getter @Setter
@NoArgsConstructor
public class GlEntry {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원천 전표 상세 라인.
     * 이 GL Entry를 생성한 JournalDetail을 참조합니다.
     * drill-down: GL Entry → JournalDetail → JournalEntry → 원천 문서
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id")
    private JournalDetail journalDetail;

    /**
     * 계정과목.
     * 이 GL Entry가 귀속되는 계정 (예: 현금 10100, 매출채권 11000).
     * GlBalance는 이 accountCode를 키로 잔액을 집계합니다.
     */
    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    /**
     * 거래 통화.
     * 외화 거래 시 해당 통화 (예: USD, EUR).
     * 원화 거래이면 null 또는 KRW.
     */
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    /**
     * 회계연도 (Fiscal Year).
     * 예: "2026"
     * 연도별 재무제표 집계 시 사용합니다.
     */
    private String fiscalYear;

    /**
     * 회계기간 (Fiscal Period, 월).
     * 예: "01"(1월) ~ "12"(12월), 2자리 숫자 문자열.
     * 월별 손익 집계 및 기간별 잔액 조회에 사용합니다.
     */
    private String fiscalPeriod;

    /**
     * 전기일 (회계 반영일).
     * JournalEntry.accountingDate와 동일합니다.
     * 일별 원장 조회 및 잔액 계산의 기준 날짜입니다.
     */
    private LocalDate postingDate;

    /** 거래통화 기준 차변 금액 (외화 그대로). 0이면 대변 항목. */
    private BigDecimal drAmount = BigDecimal.ZERO;

    /** 거래통화 기준 대변 금액 (외화 그대로). 0이면 차변 항목. */
    private BigDecimal crAmount = BigDecimal.ZERO;

    /**
     * 기본통화(KRW) 기준 차변 금액.
     * GlBalance.addDebit()에서 이 값을 잔액에 반영합니다.
     */
    private BigDecimal baseDrAmount = BigDecimal.ZERO;

    /**
     * 기본통화(KRW) 기준 대변 금액.
     * GlBalance.addCredit()에서 이 값을 잔액에 반영합니다.
     */
    private BigDecimal baseCrAmount = BigDecimal.ZERO;

    /** 적요 (간략한 거래 내용 메모) */
    private String summary;

    /**
     * 원천 문서 유형 (drill-down 역추적용).
     * JournalEntry.lineageSourceType에서 복사됩니다.
     * 예: "EXPENDITURE_RESOLUTION", "PURCHASE_INVOICE"
     */
    private String lineageSourceType;

    /**
     * 원천 문서 식별자 (drill-down 역추적용).
     * JournalEntry.lineageSourceId에서 복사됩니다.
     * 예: "REQ-20260101-001"
     */
    private String lineageSourceId;
}
