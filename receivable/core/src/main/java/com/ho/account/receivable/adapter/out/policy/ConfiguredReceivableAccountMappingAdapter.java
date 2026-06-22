package com.ho.account.receivable.adapter.out.policy;

import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.SalesInvoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredReceivableAccountMappingAdapter implements ReceivableAccountMappingPort {

    private final String accountsReceivableAccountCode;
    private final String revenueAccountCode;
    private final String outputVatAccountCode;
    private final String cashAccountCode;
    private final String arClearingAccountCode;

    public ConfiguredReceivableAccountMappingAdapter(
            @Value("${account.receivable.account-mapping.accounts-receivable-account-code:11100}")
            String accountsReceivableAccountCode,
            @Value("${account.receivable.account-mapping.revenue-account-code:40100}")
            String revenueAccountCode,
            @Value("${account.receivable.account-mapping.output-vat-account-code:22100}")
            String outputVatAccountCode,
            @Value("${account.receivable.account-mapping.cash-account-code:10100}")
            String cashAccountCode,
            @Value("${account.receivable.account-mapping.ar-clearing-account-code:21100}")
            String arClearingAccountCode) {
        this.accountsReceivableAccountCode = accountsReceivableAccountCode;
        this.revenueAccountCode = revenueAccountCode;
        this.outputVatAccountCode = outputVatAccountCode;
        this.cashAccountCode = cashAccountCode;
        this.arClearingAccountCode = arClearingAccountCode;
    }

    @Override
    public SalesRecognitionAccounts resolveSalesRecognitionAccounts(SalesInvoice invoice) {
        return new SalesRecognitionAccounts(
                accountsReceivableAccountCode,
                revenueAccountCode,
                outputVatAccountCode);
    }

    @Override
    public CollectionRecognitionAccounts resolveCollectionRecognitionAccounts(Collection collection) {
        return new CollectionRecognitionAccounts(cashAccountCode, arClearingAccountCode);
    }

    @Override
    public CollectionMatchAccounts resolveCollectionMatchAccounts(Collection collection, Receivable receivable) {
        return new CollectionMatchAccounts(arClearingAccountCode, accountsReceivableAccountCode);
    }
}
