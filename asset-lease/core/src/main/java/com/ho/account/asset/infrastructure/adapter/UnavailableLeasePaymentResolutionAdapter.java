package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;

public class UnavailableLeasePaymentResolutionAdapter implements LeasePaymentResolutionPort {

    @Override
    public void createLeasePaymentResolution(LeasePaymentResolutionCommand command) {
        throw new IllegalStateException(
                "LeasePaymentResolutionPort is not configured for asset-lease runtime.");
    }
}
