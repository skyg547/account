package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.asset.application.port.out.LeaseAccountMappingPort;
import com.ho.account.asset.domain.LeaseContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredLeaseAccountMappingAdapter implements LeaseAccountMappingPort {

    private final String leaseLiabilityAccountCode;
    private final String leaseInterestExpenseAccountCode;
    private final String accountsPayableAccountCode;

    public ConfiguredLeaseAccountMappingAdapter(
            @Value("${account.asset-lease.lease-accounting.lease-liability-account-code:25100}") String leaseLiabilityAccountCode,
            @Value("${account.asset-lease.lease-accounting.lease-interest-expense-account-code:93100}") String leaseInterestExpenseAccountCode,
            @Value("${account.asset-lease.lease-accounting.accounts-payable-account-code:21100}") String accountsPayableAccountCode) {
        this.leaseLiabilityAccountCode = leaseLiabilityAccountCode;
        this.leaseInterestExpenseAccountCode = leaseInterestExpenseAccountCode;
        this.accountsPayableAccountCode = accountsPayableAccountCode;
    }

    @Override
    public LeasePaymentAccounts resolvePaymentAccounts(LeaseContract contract) {
        return new LeasePaymentAccounts(
                leaseLiabilityAccountCode,
                leaseInterestExpenseAccountCode,
                accountsPayableAccountCode);
    }
}
