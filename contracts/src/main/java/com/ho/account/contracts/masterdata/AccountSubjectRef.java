package com.ho.account.contracts.masterdata;

public record AccountSubjectRef(
        String code,
        String name,
        boolean unsettled,
        boolean fixedAsset) {
}
