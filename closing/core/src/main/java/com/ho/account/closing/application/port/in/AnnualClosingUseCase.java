package com.ho.account.closing.application.port.in;

/**
 * 연차 결산(Annual Closing) 관련 유스케이스 인터페이스.
 */
public interface AnnualClosingUseCase {
    /**
     * 손익 대체 분개를 생성합니다.
     */
    void performIncomeStatementClosing(int year, String retainedEarningsAccountCode);
}
