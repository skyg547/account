package com.ho.account.reporting.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * [도메인 모델] ReportLine (보고서 항목)
 * 초보자 가이드: 재무제표의 한 줄 한 줄을 의미합니다.
 * 예를 들어 "현금 및 현금성 자산: 1,000,000,000원" 이 한 줄이 하나의 ReportLine 객체가 됩니다.
 */
@Getter
@NoArgsConstructor
public class ReportLine {
    
    // 항목 코드 (예: ASSET_CASH)
    private String lineCode;

    // 항목 이름 (예: 현금 및 현금성자산)
    private String label;

    // 당기 금액 (정밀한 계산을 위해 반드시 BigDecimal을 사용합니다)
    private BigDecimal currentAmount;

    // 전기 금액 (작년 동기 데이터와 비교하기 위해 사용합니다)
    // 초보자 가이드: 대외 보고서는 작년과 비교해서 얼마나 늘고 줄었는지를 보여주는 것이 매우 중요합니다.
    private BigDecimal previousAmount;

    // 주석 번호 (공시 보고서에서 상세 설명을 찾기 위한 인덱스입니다)
    // 초보자 가이드: "상세한 내용은 주석 3번을 보세요"라고 할 때 그 3번을 여기에 기록합니다.
    private String noteNumber;

    // 항목의 레벨 (1: 대분류, 2: 중분류, 3: 소분류)
    private int level;

    /**
     * 항목 생성자
     * @param lineCode 코드
     * @param label 이름
     * @param currentAmount 당기 금액
     * @param previousAmount 전기 금액
     * @param noteNumber 주석 번호
     * @param level 계층 레벨
     */
    public ReportLine(String lineCode, String label, BigDecimal currentAmount, BigDecimal previousAmount, String noteNumber, int level) {
        this.lineCode = lineCode;
        this.label = label;
        this.currentAmount = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        this.previousAmount = previousAmount != null ? previousAmount : BigDecimal.ZERO;
        this.noteNumber = noteNumber;
        this.level = level;
    }

    /**
     * 당기 금액을 가산합니다.
     * @param addAmount 더할 금액
     */
    public void addCurrentAmount(BigDecimal addAmount) {
        if (addAmount != null) {
            this.currentAmount = this.currentAmount.add(addAmount);
        }
    }
}
