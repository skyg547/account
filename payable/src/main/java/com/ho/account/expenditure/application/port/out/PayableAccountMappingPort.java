package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PurchaseInvoice;

import java.util.List;

public interface PayableAccountMappingPort {

    PurchaseRecognitionAccounts resolvePurchaseRecognitionAccounts(PurchaseInvoice invoice);

    PaymentExecutionAccounts resolvePaymentExecutionAccounts(Payment payment);

    AdvancePaymentAccounts resolveAdvancePaymentAccounts(AdvancePayment advancePayment);

    AdvanceOffsetAccounts resolveAdvanceOffsetAccounts(Payable payable);

    record PurchaseRecognitionAccounts(
            String expenseAccountCode,
            String inputVatAccountCode,
            String accountsPayableAccountCode) {

        public PurchaseRecognitionAccounts {
            requireNonBlank(expenseAccountCode, "expenseAccountCode");
            requireNonBlank(inputVatAccountCode, "inputVatAccountCode");
            requireNonBlank(accountsPayableAccountCode, "accountsPayableAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(expenseAccountCode, inputVatAccountCode, accountsPayableAccountCode);
        }
    }

    record PaymentExecutionAccounts(
            String accountsPayableAccountCode,
            String cashAccountCode) {

        public PaymentExecutionAccounts {
            requireNonBlank(accountsPayableAccountCode, "accountsPayableAccountCode");
            requireNonBlank(cashAccountCode, "cashAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(accountsPayableAccountCode, cashAccountCode);
        }
    }

    record AdvancePaymentAccounts(
            String advanceAccountCode,
            String cashAccountCode) {

        public AdvancePaymentAccounts {
            requireNonBlank(advanceAccountCode, "advanceAccountCode");
            requireNonBlank(cashAccountCode, "cashAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(advanceAccountCode, cashAccountCode);
        }
    }

    record AdvanceOffsetAccounts(
            String accountsPayableAccountCode,
            String advanceAccountCode) {

        public AdvanceOffsetAccounts {
            requireNonBlank(accountsPayableAccountCode, "accountsPayableAccountCode");
            requireNonBlank(advanceAccountCode, "advanceAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(accountsPayableAccountCode, advanceAccountCode);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
