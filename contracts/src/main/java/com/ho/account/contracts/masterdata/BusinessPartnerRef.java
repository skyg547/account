package com.ho.account.contracts.masterdata;

public record BusinessPartnerRef(
        String code,
        String name,
        String partnerType,
        Boolean active) {
}
