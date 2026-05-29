package com.ho.account.expenditure.adapter.out.policy;

import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredPayableAccountMappingAdapter implements PayableAccountMappingPort {

    private final String purchaseExpenseAccountCode;
    private final String inputVatAccountCode;
    private final String accountsPayableAccountCode;
    private final String cashAccountCode;
    private final String advanceAccountCode;

    public ConfiguredPayableAccountMappingAdapter(
            @Value("${account.payable.account-mapping.purchase-expense-account-code:50100}")
            String purchaseExpenseAccountCode,
            @Value("${account.payable.account-mapping.input-vat-account-code:13500}")
            String inputVatAccountCode,
            @Value("${account.payable.account-mapping.accounts-payable-account-code:21100}")
            String accountsPayableAccountCode,
            @Value("${account.payable.account-mapping.cash-account-code:10100}")
            String cashAccountCode,
            @Value("${account.payable.account-mapping.advance-account-code:13100}")
            String advanceAccountCode) {
        this.purchaseExpenseAccountCode = purchaseExpenseAccountCode;
        this.inputVatAccountCode = inputVatAccountCode;
        this.accountsPayableAccountCode = accountsPayableAccountCode;
        this.cashAccountCode = cashAccountCode;
        this.advanceAccountCode = advanceAccountCode;
    }

    @Override
    public PurchaseRecognitionAccounts resolvePurchaseRecognitionAccounts(PurchaseInvoice invoice) {
        return new PurchaseRecognitionAccounts(
                purchaseExpenseAccountCode,
                inputVatAccountCode,
                accountsPayableAccountCode);
    }

    @Override
    public PaymentExecutionAccounts resolvePaymentExecutionAccounts(Payment payment) {
        return new PaymentExecutionAccounts(accountsPayableAccountCode, cashAccountCode);
    }

    @Override
    public AdvancePaymentAccounts resolveAdvancePaymentAccounts(AdvancePayment advancePayment) {
        return new AdvancePaymentAccounts(advanceAccountCode, cashAccountCode);
    }

    @Override
    public AdvanceOffsetAccounts resolveAdvanceOffsetAccounts(Payable payable) {
        return new AdvanceOffsetAccounts(accountsPayableAccountCode, advanceAccountCode);
    }
}
