package com.ho.account.deposit.application.port.out;

import com.ho.account.deposit.domain.DepositAccount;

import java.util.List;

public interface DepositAccountMappingPort {

    InitialDepositAccounts resolveInitialDepositAccounts(DepositAccount account);

    record InitialDepositAccounts(
            String cashAccountCode,
            String depositLiabilityAccountCode) {

        public InitialDepositAccounts {
            requireNonBlank(cashAccountCode, "cashAccountCode");
            requireNonBlank(depositLiabilityAccountCode, "depositLiabilityAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(cashAccountCode, depositLiabilityAccountCode);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
