package com.ho.account.contracts.ledger;

import java.time.LocalDate;
import java.util.List;

/**
 * [LedgerQueryPort] 원장(Ledger) 잔액 및 집계 조회를 위한 공통 계약 포트.
 *
 * 🐣 [초보자를 위한 개념 설명]
 * - GL (General Ledger, 총계정원장): 전사 계정과목별 총 잔액과 발생액을 관리하는 장부입니다.
 * - SL (Sub-Ledger, 보조원장): 거래처(Business Partner), 부서(Department) 등 관리 차원(Dimension)별 세부 잔액을 관리하는 보조 장부입니다.
 * - 푸시다운 집계(Push-Down Aggregation): 수백만 건의 원장 라인을 애플리케이션 메모리에 모두 띄우지 않고,
 *   SQL 데이터베이스 엔진의 `SUM/COUNT/GROUP BY` 집계 능력을 활용하여 초고속으로 잔액을 조회하는 최적화 패턴입니다.
 */
public interface LedgerQueryPort {

    /**
     * 지정된 기간, 계정코드, 통화 기준의 총계정원장(GL) 잔액 요약 목록을 조회합니다.
     */
    List<LedgerBalanceSummary> getGlBalanceSummaries(LocalDate startDate,
                                                     LocalDate endDate,
                                                     String accountCode,
                                                     String currencyCode);

    List<LedgerBalanceSummary> getSlBalanceSummaries(LocalDate startDate,
                                                     LocalDate endDate,
                                                     String accountCode,
                                                     String businessPartnerCode,
                                                     String departmentCode,
                                                     String currencyCode);

    /**
     * 특정 계정 과목 및 날짜 기준의 원장 요약 집계 정보를 DB 레벨에서 직접 푸시다운(Push-Down Aggregation)하여 조회합니다.
     *
     * @param accountSubjectCode 계정과목 코드
     * @param date 대상 일자
     * @return DB 집계 결과 (COUNT, SUM)
     */
    default LedgerAggregateSummary calculateLedgerSummary(String accountSubjectCode, LocalDate date) {
        return calculateLedgerSummary(date, date, accountSubjectCode, null, "DEBIT");
    }

    /**
     * 지정된 조건과 집계 기준(DEBIT, CREDIT, ENDING_BALANCE 등)에 따른 GL 원장 잔액의 DB 레벨 푸시다운 요약 집계를 수행합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param accountCode 계정코드
     * @param currencyCode 통화코드
     * @param amountBasis 집계 금액 기준 (DEBIT, CREDIT, ENDING_BALANCE, ABS_ENDING_BALANCE)
     * @return DB 집계 결과 (COUNT, SUM)
     */
    LedgerAggregateSummary calculateLedgerSummary(LocalDate startDate,
                                                  LocalDate endDate,
                                                  String accountCode,
                                                  String currencyCode,
                                                  String amountBasis);
}
