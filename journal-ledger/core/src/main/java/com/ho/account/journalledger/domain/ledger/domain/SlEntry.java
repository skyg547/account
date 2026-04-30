package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 보조원장 분개 항목 (Subsidiary Ledger Entry, SL Entry).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 총계정원장(GL)이 계정과목 단위로 잔액을 관리한다면,
 * 보조원장(SL: Subsidiary Ledger)은 계정과목 + 거래처 + 부서 단위로 상세 잔액을 관리합니다.
 *
 * 예시:
 *   GL: 매출채권(11000) 총잔액 = 100,000,000원
 *   SL: 매출채권(11000) + 거래처A = 60,000,000원
 *       매출채권(11000) + 거래처B = 40,000,000원
 *
 * SL Entry는 전표 전기 시 GL Entry와 함께 생성되며, 거래처별/부서별 채권·채무 관리에 사용됩니다.
 *
 * 주요 필드:
 *   - account         : 계정과목
 *   - businessPartner : 거래처 (매입처/매출처 구분 핵심 키)
 *   - department      : 귀속 부서 (부서별 원가 분석)
 *   - drAmount/crAmount : 거래통화 차변/대변 금액
 *   - baseDrAmount/baseCrAmount : 기본통화(KRW) 차변/대변 금액
 *   - lineageSourceType/lineageSourceId : 원천 문서 drill-down 추적용
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - sl_entries 테이블에 매핑됩니다.
 * - PostingService.postJournalEntry()에서 JournalDetail 하나당 SlEntry 하나가 생성됩니다.
 * - GlEntry와 구조가 유사하나 businessPartner, department 필드가 추가됩니다.
 * - SlBalance는 이 SlEntry를 기준으로 계정과목+거래처+부서+통화 단위 잔액을 집계합니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "sl_entries")
@Getter @Setter
@NoArgsConstructor
public class SlEntry {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원천 전표 상세 라인.
     * drill-down: SL Entry → JournalDetail → JournalEntry → 원천 문서
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id")
    private JournalDetail journalDetail;

    /** 계정과목 (SL 집계의 1차 키) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountSubject account;

    /**
     * 거래처 (SL 집계의 2차 키).
     * 채권·채무 관리에서 거래처별 잔액을 분리하는 핵심 구분자입니다.
     * null 가능: 거래처가 없는 내부 계정.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bp_id")
    private BusinessPartner businessPartner;

    /**
     * 귀속 부서 (SL 집계의 3차 키).
     * 부서별 원가/비용 분석에 사용됩니다.
     * null 가능: 부서 귀속 불필요한 계정.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    /** 거래 통화 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code")
    private Currency currency;

    /** 회계연도 (예: "2026") */
    private String fiscalYear;

    /** 회계기간/월 (예: "01" ~ "12") */
    private String fiscalPeriod;

    /** 전기일 (회계 반영일) */
    private LocalDate postingDate;

    /** 거래통화 기준 차변 금액 */
    private BigDecimal drAmount = BigDecimal.ZERO;

    /** 거래통화 기준 대변 금액 */
    private BigDecimal crAmount = BigDecimal.ZERO;

    /** 기본통화(KRW) 기준 차변 금액 — SlBalance 잔액 계산 기준 */
    private BigDecimal baseDrAmount = BigDecimal.ZERO;

    /** 기본통화(KRW) 기준 대변 금액 — SlBalance 잔액 계산 기준 */
    private BigDecimal baseCrAmount = BigDecimal.ZERO;

    /** 적요 */
    private String summary;

    /** 원천 문서 유형 (drill-down 역추적용) */
    private String lineageSourceType;

    /** 원천 문서 식별자 (drill-down 역추적용) */
    private String lineageSourceId;
}
