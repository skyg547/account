package com.ho.account.ecl.core.application.port.out;

import java.time.LocalDate;

/**
 * [Port] [Outbound] 대량 데이터 처리 및 집계 전용 포트.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 서비스(비즈니스 로직)가 데이터베이스에 SQL을 직접 날리지 않고, 
 * "이 일을 대신 해줘"라고 부탁할 때 사용하는 인터페이스입니다.
 * 구체적으로 어떤 쿼리를 쓰는지는 알 필요가 없게 설계합니다.
 */
public interface CrBulkOperationPort {

    /**
     * 특정 기준일자의 기존 산출 결과를 물리적으로 삭제합니다.
     */
    void clearBatchResults(LocalDate baseDate);

    /**
     * 특정 기준일자의 산출 결과를 집계하여 리스크 요약 마트(Result Mart)를 생성합니다.
     */
    int aggregateMonthlyRiskMart(LocalDate baseDate);

    /**
     * 특정 계좌에 배분된 총 담보 가액을 합산합니다.
     */
    java.math.BigDecimal sumAllocationByAccountId(Long accountId);

    /**
     * 특정 담보가 여러 계좌에 배분한 총 가액을 합산합니다.
     */
    java.math.BigDecimal sumAllocationByCollateralId(Long collateralId);

    /**
     * 특정 계좌에 대한 모든 담보 배분 정보를 삭제합니다.
     */
    void deleteAllocationByAccountId(Long accountId);

    /**
     * 모든 산출 결과 테이블을 비웁니다. (테스트 및 초기화 전용)
     */
    void deleteAllResult();

    /**
     * 모든 월간 요약 테이블을 비웁니다. (테스트 및 초기화 전용)
     */
    void deleteAllMonthlySummary();

    /**
     * 월간 요약 엔터티 리스트를 저장합니다.
     */
    void saveMonthlySummary(java.util.List<com.ho.account.ecl.core.domain.result.CrMonthlySummary> summaries);
}
