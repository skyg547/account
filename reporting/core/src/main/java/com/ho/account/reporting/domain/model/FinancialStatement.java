package com.ho.account.reporting.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 재무제표(Financial Statement) 도메인 모델 — 특정 시점의 회사 재무 상태나 성과를 나타내는 최상위 보고 객체입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 재무제표는 '회사의 성적표'입니다. 
 * "지금 우리 회사에 현금이 얼마 있고 빚이 얼마인지(재무상태표)", 
 * "이번 달에 얼마나 벌고 얼마나 썼는지(손익계산서)"를 한눈에 보여줍니다. 
 * 이 클래스는 수많은 상세 거래(전표)들을 의미 있는 항목별로 묶어서 최종 합계를 보여주는 역할을 합니다.
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
