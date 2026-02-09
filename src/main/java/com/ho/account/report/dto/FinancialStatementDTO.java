package com.accounting.system.report.dto;

import java.math.BigDecimal;

/**
 * 재무제표(재무상태표, 손익계산서)의 각 항목을 표현하는 DTO 클래스입니다.
 * DTO(Data Transfer Object)는 계층 간 데이터 교환을 위해 사용되는 객체로, 로직을 가지지 않습니다.
 */
public class FinancialStatementDTO {

    // 계정 과목 코드 (예: 10100)
    private String accountCode;

    // 계정 과목 명 (예: 현금)
    private String accountName;

    // 금액 (잔액)
    // BigDecimal은 부동소수점 오차를 방지하기 위해 금융 계산에서 필수적으로 사용됩니다.
    private BigDecimal amount;

    // 생성자: 객체 생성 시 초기값을 설정합니다.
    public FinancialStatementDTO(String accountCode, String accountName, BigDecimal amount) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.amount = amount;
    }

    // Getters: 외부에서 필드 값을 읽을 수 있게 해주는 메서드들
    public String getAccountCode() { return accountCode; }
    public String getAccountName() { return accountName; }
    public BigDecimal getAmount() { return amount; }
}
