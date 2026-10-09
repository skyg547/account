package com.ho.account.loan.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account.loan.accounting")
public class LoanAccountingProperties {

    private String cashAccountCode;
    private String loanReceivableAccountCode;
    private String deferredAssetAccountCode;
    private String recognizedIncomeAccountCode;
    private String accruedInterestReceivableAccountCode;
    private String interestIncomeAccountCode;
    private String journalApproverActor;

    public String getJournalApproverActor() {
        return required(journalApproverActor, "journal-approver-actor");
    }

    public void setJournalApproverActor(String journalApproverActor) {
        this.journalApproverActor = journalApproverActor;
    }

    public String getCashAccountCode() {
        return required(cashAccountCode, "cash-account-code");
    }

    public void setCashAccountCode(String cashAccountCode) {
        this.cashAccountCode = cashAccountCode;
    }

    public String getLoanReceivableAccountCode() {
        return required(loanReceivableAccountCode, "loan-receivable-account-code");
    }

    public void setLoanReceivableAccountCode(String loanReceivableAccountCode) {
        this.loanReceivableAccountCode = loanReceivableAccountCode;
    }

    public String getDeferredAssetAccountCode() {
        return required(deferredAssetAccountCode, "deferred-asset-account-code");
    }

    public void setDeferredAssetAccountCode(String deferredAssetAccountCode) {
        this.deferredAssetAccountCode = deferredAssetAccountCode;
    }

    public String getRecognizedIncomeAccountCode() {
        return required(recognizedIncomeAccountCode, "recognized-income-account-code");
    }

    public void setRecognizedIncomeAccountCode(String recognizedIncomeAccountCode) {
        this.recognizedIncomeAccountCode = recognizedIncomeAccountCode;
    }

    public String getAccruedInterestReceivableAccountCode() {
        return required(accruedInterestReceivableAccountCode, "accrued-interest-receivable-account-code");
    }

    public void setAccruedInterestReceivableAccountCode(String accruedInterestReceivableAccountCode) {
        this.accruedInterestReceivableAccountCode = accruedInterestReceivableAccountCode;
    }

    public String getInterestIncomeAccountCode() {
        return required(interestIncomeAccountCode, "interest-income-account-code");
    }

    public void setInterestIncomeAccountCode(String interestIncomeAccountCode) {
        this.interestIncomeAccountCode = interestIncomeAccountCode;
    }

    private String required(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("account.loan.accounting." + key + " must be configured");
        }
        return value.trim();
    }
}
