package com.ho.account.deposit.infrastructure.adapter.out.policy;

import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.domain.DepositAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredDepositAccountMappingAdapter implements DepositAccountMappingPort {

    private final String cashAccountCode;
    private final String depositLiabilityAccountCode;

    public ConfiguredDepositAccountMappingAdapter(
            @Value("${account.deposit.account-mapping.cash-account-code:10100}")
            String cashAccountCode,
            @Value("${account.deposit.account-mapping.deposit-liability-account-code:20200}")
            String depositLiabilityAccountCode) {
        this.cashAccountCode = cashAccountCode;
        this.depositLiabilityAccountCode = depositLiabilityAccountCode;
    }

    @Override
    public InitialDepositAccounts resolveInitialDepositAccounts(DepositAccount account) {
        return new InitialDepositAccounts(cashAccountCode, depositLiabilityAccountCode);
    }
}
