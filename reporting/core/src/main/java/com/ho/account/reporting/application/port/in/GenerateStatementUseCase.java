package com.ho.account.reporting.application.port.in;

import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;

/**
 * [인바운드 포트] GenerateStatementUseCase
 * 초보자 가이드: '포트'는 입구와 같습니다. 
 * 외부(웹 화면이나 배치 작업)에서 "보고서 좀 만들어줘!"라고 요청할 때 이 인터페이스를 통해서 들어옵니다.
 */
public interface GenerateStatementUseCase {

    /**
     * 재무제표 생성 요청을 처리합니다.
     * @param request 생성 요청 정보 (보고서 종류, 기준일 등)
     * @return 생성된 재무제표 객체
     */
    FinancialStatement generate(GenerateCommand request);

    /**
     * 보고서 생성을 위한 데이터 묶음(DTO)
     */
    record GenerateCommand(
        FinancialStatement.StatementType type, // 보고서 종류
        LocalDateTime baseDate,                // 기준일
        String requesterId                     // 요청자 ID
    ) {}
}
