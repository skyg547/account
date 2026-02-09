package com.ho.account.basic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_subjects")
public class AccountSubject {

    @Id
    @Column(length = 10)
    private String accountCode;

    @Column(nullable = false, length = 100)
    private String accountName;

    @Column(length = 20)
    private String accountType; // 자산, 부채, 자본, 수익, 비용

    @Column(nullable = false)
    private Boolean isFixedAsset = false; // 고정자산 계정 여부

    @Column(nullable = false)
    private Boolean manageUnsettled = false; // 미결 관리 여부 (채권/채무 등)

    private Boolean useYn = true;

    // Getters and Setters
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public Boolean getIsFixedAsset() { return isFixedAsset; }
    public void setIsFixedAsset(Boolean fixedAsset) { isFixedAsset = fixedAsset; }

    public Boolean getManageUnsettled() { return manageUnsettled; }
    public void setManageUnsettled(Boolean manageUnsettled) { this.manageUnsettled = manageUnsettled; }

    public Boolean getUseYn() { return useYn; }
    public void setUseYn(Boolean useYn) { this.useYn = useYn; }
}
