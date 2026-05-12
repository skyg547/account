package com.ho.account.loan.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account.loan.accounting")
public class LoanAccountingProperties {

    private String cashAccountCode = "101000";
    private String loanReceivableAccountCode = "131000";
    private String deferredAssetAccountCode = "171000";
    private String recognizedIncomeAccountCode = "401000";
    private String accruedInterestReceivableAccountCode = "11501";
    private String interestIncomeAccountCode = "41101";

    public String getCashAccountCode() {
        return cashAccountCode;
    }

    public void setCashAccountCode(String cashAccountCode) {
        this.cashAccountCode = cashAccountCode;
    }

    public String getLoanReceivableAccountCode() {
        return loanReceivableAccountCode;
    }

    public void setLoanReceivableAccountCode(String loanReceivableAccountCode) {
        this.loanReceivableAccountCode = loanReceivableAccountCode;
    }

    public String getDeferredAssetAccountCode() {
        return deferredAssetAccountCode;
    }

    public void setDeferredAssetAccountCode(String deferredAssetAccountCode) {
        this.deferredAssetAccountCode = deferredAssetAccountCode;
    }

    public String getRecognizedIncomeAccountCode() {
        return recognizedIncomeAccountCode;
    }

    public void setRecognizedIncomeAccountCode(String recognizedIncomeAccountCode) {
        this.recognizedIncomeAccountCode = recognizedIncomeAccountCode;
    }

    public String getAccruedInterestReceivableAccountCode() {
        return accruedInterestReceivableAccountCode;
    }

    public void setAccruedInterestReceivableAccountCode(String accruedInterestReceivableAccountCode) {
        this.accruedInterestReceivableAccountCode = accruedInterestReceivableAccountCode;
    }

    public String getInterestIncomeAccountCode() {
        return interestIncomeAccountCode;
    }

    public void setInterestIncomeAccountCode(String interestIncomeAccountCode) {
        this.interestIncomeAccountCode = interestIncomeAccountCode;
    }
}
