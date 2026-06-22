package com.ho.account.contracts.masterdata;

public record AccountSubjectRef(
        String code,
        String name,
        boolean unsettled,
        boolean fixedAsset,
        String normalBalanceSide) {

    public AccountSubjectRef {
        normalBalanceSide = normalBalanceSide == null || normalBalanceSide.isBlank()
                ? "DEBIT"
                : normalBalanceSide.trim().toUpperCase();
    }

    public AccountSubjectRef(String code, String name, boolean unsettled, boolean fixedAsset) {
        this(code, name, unsettled, fixedAsset, "DEBIT");
    }

    public boolean debitNormalBalance() {
        return "DEBIT".equalsIgnoreCase(normalBalanceSide);
    }

    public boolean creditNormalBalance() {
        return "CREDIT".equalsIgnoreCase(normalBalanceSide);
    }
}
