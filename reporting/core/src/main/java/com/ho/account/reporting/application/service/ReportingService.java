package com.ho.account.reporting.application.service;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * [애플리케이션 서비스] ReportingService
 * 초보자 가이드: 이 서비스는 요리사와 같습니다. 
 * '재료(원장 데이터)'와 '작년 요리법/결과(과거 데이터)'를 가져와서 '완성품(비교 재무제표)'을 만듭니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportingService implements GenerateStatementUseCase {

    private final LoadLedgerPort loadLedgerPort;
    private final LoadReportHistoryPort loadReportHistoryPort; // 과거 데이터 조회를 위한 포트 추가

    @Override
    public FinancialStatement generate(GenerateCommand command) {
        
        // 1. 당기 원장 데이터를 가져옵니다.
        Map<String, BigDecimal> currentBalances = loadLedgerPort.getAccountBalances(command.baseDate());

        // 2. 전기(작년 동기) 보고서 데이터를 가져옵니다.
        // 초보자 가이드: 작년 이맘때는 어땠는지 확인하기 위해 1년 전 데이터를 찾아봅니다.
        Optional<FinancialStatement> previousStatement = loadReportHistoryPort.findFinalizedStatement(
            command.type(), 
            command.baseDate().minusYears(1)
        );

        // 3. 재무제표 객체 생성
        FinancialStatement statement = new FinancialStatement(
            UUID.randomUUID().toString(),
            command.type(), 
            command.baseDate()
        );

        // 4. 비교식 항목 계산 및 추가
        if (command.type() == FinancialStatement.StatementType.BALANCE_SHEET) {
            
            // 당기 금액 계산
            BigDecimal cashCurrent = currentBalances.getOrDefault("101", BigDecimal.ZERO)
                                     .add(currentBalances.getOrDefault("102", BigDecimal.ZERO));

            // 전기 금액 찾기 (과거 보고서가 있다면 해당 항목의 금액을 가져옵니다)
            BigDecimal cashPrevious = previousStatement
                .map(s -> s.getLines().stream()
                    .filter(l -> "ASSET_CASH".equals(l.getLineCode()))
                    .map(ReportLine::getCurrentAmount)
                    .findFirst()
                    .orElse(BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);

            // 보고서에 '당기 vs 전기' 비교 항목 추가
            // 주석 번호는 예시로 '3'을 부여합니다.
            statement.addLine(new ReportLine("ASSET_CASH", "현금 및 현금성자산", cashCurrent, cashPrevious, "3", 1));
        }

        statement.finalizeStatement();
        return statement;
    }
}
