package com.ho.account.contracts.ledger;

import java.time.LocalDate;
import java.util.List;

public interface LedgerQueryPort {

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
