package com.ho.account.contracts.tax;

public record TaxInvoiceRef(
        Long id,
        String issueId,
        String type,
        String status) {

    /**
     * 외부 모듈이 tax 도메인 enum을 직접 참조하지 않고도 증빙 상태를 판단하도록 제공하는 편의 메서드입니다.
     */
    public boolean active() {
        return "ACTIVE".equals(status);
    }

    /**
     * 지출결의/AP 지급은 매입 세금계산서만 연결할 수 있습니다.
     */
    public boolean purchase() {
        return "PURCHASE".equals(type);
    }

    /**
     * 외부 모듈이 가장 자주 쓰는 업무 정책입니다.
     * 초보자 관점에서는 "매입 영수증이고 취소되지 않은 상태"만 새 지출결의나 지급에 붙일 수 있다는 뜻입니다.
     */
    public boolean usableForPurchaseSettlement() {
        return purchase() && active();
    }
}