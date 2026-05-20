package com.ho.account.reporting.application.service;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.application.port.out.LoadReportLineMappingPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreReportSnapshotPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import com.ho.account.reporting.domain.model.ReportLineMapping;
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
    private final LoadReportHistoryPort loadReportHistoryPort;
    private final LoadReportLineMappingPort loadReportLineMappingPort;
    private final StoreReportSnapshotPort storeReportSnapshotPort;

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

        // 4. SCD2 보고 라인 매핑을 기준으로 비교식 항목 계산 및 추가
        for (ReportLineMapping mapping : loadReportLineMappingPort.loadMappings(command.type(), command.baseDate())) {
            BigDecimal currentAmount = mapping.accountCodes().stream()
                    .map(accountCode -> currentBalances.getOrDefault(accountCode, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal previousAmount = previousStatement
                    .map(s -> s.getLines().stream()
                            .filter(line -> mapping.lineCode().equals(line.getLineCode()))
                            .map(ReportLine::getCurrentAmount)
                            .findFirst()
                            .orElse(BigDecimal.ZERO))
                    .orElse(BigDecimal.ZERO);

            statement.addLine(new ReportLine(
                    mapping.lineCode(),
                    mapping.label(),
                    currentAmount,
                    previousAmount,
                    mapping.noteNumber(),
                    mapping.level()));
        }

        statement.finalizeStatement();
        storeReportSnapshotPort.saveFinalized(statement);
        return statement;
    }
}
