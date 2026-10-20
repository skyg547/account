package com.ho.account.masterdata.core.infrastructure.adapter;

/** 현재 벌크 거래처 조회에서 한 업무 코드에 활성 버전이 둘 이상 발견된 무결성 오류입니다. */
public final class DuplicateBusinessPartnerReferenceException extends IllegalStateException {

    private final String businessPartnerCode;

    public DuplicateBusinessPartnerReferenceException(String businessPartnerCode) {
        super("Multiple active business-partner versions for code: " + businessPartnerCode);
        this.businessPartnerCode = businessPartnerCode;
    }

    public String businessPartnerCode() {
        return businessPartnerCode;
    }
}
