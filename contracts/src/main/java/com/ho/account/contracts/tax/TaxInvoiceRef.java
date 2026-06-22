package com.ho.account.contracts.tax;

public record TaxInvoiceRef(
        Long id,
        String issueId,
        String type,
        String status) {

    /**
     * 외부 모듈이 tax 도메인 enum을 직접 참조하지 않고도 증빙 유효성을 판단하도록 제공하는 편의 메서드입니다.
     */
    public boolean active() {
        return "ACTIVE".equals(status);
    }
}
