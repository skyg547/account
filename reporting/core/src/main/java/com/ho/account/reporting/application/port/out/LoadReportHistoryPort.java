package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * [아웃바운드 포트] LoadReportHistoryPort
 * 초보자 가이드: 과거에 이미 만들어서 확정했던 보고서 데이터를 찾아오는 통로입니다.
 * 올해 보고서와 작년 보고서를 비교해서 보여주기 위해 필요합니다.
 */
public interface LoadReportHistoryPort {

    /**
     * 특정 날짜와 종류에 맞는 과거 확정 보고서를 찾습니다.
     * @param type 보고서 종류
     * @param date 기준일
     * @return 과거 보고서 (있을 경우)
     */
    Optional<FinancialStatement> findFinalizedStatement(FinancialStatement.StatementType type, LocalDateTime date);
}
