package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.SalesInvoice;

import java.util.List;

public interface ReceivableAccountMappingPort {

    SalesRecognitionAccounts resolveSalesRecognitionAccounts(SalesInvoice invoice);

    CollectionRecognitionAccounts resolveCollectionRecognitionAccounts(Collection collection);

    CollectionMatchAccounts resolveCollectionMatchAccounts(Collection collection, Receivable receivable);

    record SalesRecognitionAccounts(
            String accountsReceivableAccountCode,
            String revenueAccountCode,
            String outputVatAccountCode) {

        public SalesRecognitionAccounts {
            requireNonBlank(accountsReceivableAccountCode, "accountsReceivableAccountCode");
            requireNonBlank(revenueAccountCode, "revenueAccountCode");
            requireNonBlank(outputVatAccountCode, "outputVatAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(accountsReceivableAccountCode, revenueAccountCode, outputVatAccountCode);
        }
    }

    record CollectionRecognitionAccounts(
            String cashAccountCode,
            String arClearingAccountCode) {

        public CollectionRecognitionAccounts {
            requireNonBlank(cashAccountCode, "cashAccountCode");
            requireNonBlank(arClearingAccountCode, "arClearingAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(cashAccountCode, arClearingAccountCode);
        }
    }

    record CollectionMatchAccounts(
            String arClearingAccountCode,
            String accountsReceivableAccountCode) {

        public CollectionMatchAccounts {
            requireNonBlank(arClearingAccountCode, "arClearingAccountCode");
            requireNonBlank(accountsReceivableAccountCode, "accountsReceivableAccountCode");
        }

        public List<String> requiredAccountCodes() {
            return List.of(arClearingAccountCode, accountsReceivableAccountCode);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
