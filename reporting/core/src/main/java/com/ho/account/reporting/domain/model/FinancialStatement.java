package com.ho.account.reporting.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [핵심 도메인 엔티티] FinancialStatement (재무제표)
 * 초보자 가이드: 이 클래스는 재무제표 그 자체를 의미합니다. 
 * '재무상태표(BS)'나 '손익계산서(PL)' 같은 보고서의 이름과 기간, 그리고 각 항목들을 담고 있습니다.
 */
@Getter
@NoArgsConstructor
public class FinancialStatement {
    
    // 보고서의 고유 ID (예: 2026-BS-001)
    private String statementId;

    // 보고서 종류 (예: BALANCE_SHEET, INCOME_STATEMENT)
    private StatementType type;

    // 결산 기준일 (예: 2026-03-31)
    private LocalDateTime baseDate;

    // 보고서에 들어가는 세부 항목들 (예: 자산, 부채, 자본 등)
    private List<ReportLine> lines = new ArrayList<>();

    // 보고서 생성 상태 (DRAFT: 작성중, FINAL: 확정)
    private StatementStatus status;

    /**
     * 보고서 생성자
     * @param statementId 보고서 아이디
     * @param type 보고서 종류
     * @param baseDate 기준일
     */
    public FinancialStatement(String statementId, StatementType type, LocalDateTime baseDate) {
        this.statementId = statementId;
        this.type = type;
        this.baseDate = baseDate;
        this.status = StatementStatus.DRAFT; // 처음 만들 때는 항상 '작성중' 상태입니다.
    }

    /**
     * 보고서에 새로운 항목(줄)을 추가합니다.
     * @param line 자산, 부채 등의 세부 항목
     */
    public void addLine(ReportLine line) {
        this.lines.add(line);
    }

    /**
     * 보고서의 최종 금액을 확정합니다.
     * 초보자 가이드: 모든 항목이 올바르게 입력되었는지 확인하고 '확정' 상태로 변경하는 로직입니다.
     */
    public void finalizeStatement() {
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("항목이 없는 보고서는 확정할 수 없습니다.");
        }
        this.status = StatementStatus.FINAL;
    }

    // 보고서 종류 정의
    public enum StatementType {
        BALANCE_SHEET,    // 재무상태표 (현재 재산 상태)
        INCOME_STATEMENT  // 손익계산서 (얼마나 벌었나)
    }

    // 보고서 상태 정의
    public enum StatementStatus {
        DRAFT, FINAL
    }
}
