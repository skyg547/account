package com.ho.account.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class FixedAssetRequest {

    @NotBlank(message = "자산 코드는 필수입니다.")
    private String assetCode;

    @NotBlank(message = "자산명은 필수입니다.")
    private String assetName;

    @NotBlank(message = "자산 계정 코드는 필수입니다.")
    private String accountSubjectCode;

    @NotBlank(message = "감가상각누계액 계정 코드는 필수입니다.")
    private String accumulatedAccountCode;

    @NotBlank(message = "감가상각비 계정 코드는 필수입니다.")
    private String expenseAccountCode;

    @NotNull(message = "취득일은 필수입니다.")
    private LocalDate acquisitionDate;

    @NotNull(message = "취득원가는 필수입니다.")
    @DecimalMin(value = "0.01", message = "취득원가는 0보다 커야 합니다.")
    private BigDecimal acquisitionCost;

    @NotNull(message = "내용연수는 필수입니다.")
    @Min(value = 1, message = "내용연수는 1년 이상이어야 합니다.")
    private Integer usefulLife;

    @NotBlank(message = "감가상각방법은 필수입니다.")
    private String depreciationMethod; // STRAIGHT_LINE, DECLINING

    @NotNull(message = "잔존가치는 필수입니다.")
    @DecimalMin(value = "0.00", message = "잔존가치는 0 이상이어야 합니다.")
    private BigDecimal residualValue;

    @NotBlank(message = "관리 부서 코드는 필수입니다.")
    private String departmentCode;

    // Getter 및 Setter
    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAccountSubjectCode() {
        return accountSubjectCode;
    }

    public void setAccountSubjectCode(String accountSubjectCode) {
        this.accountSubjectCode = accountSubjectCode;
    }

    public String getAccumulatedAccountCode() {
        return accumulatedAccountCode;
    }

    public void setAccumulatedAccountCode(String accumulatedAccountCode) {
        this.accumulatedAccountCode = accumulatedAccountCode;
    }

    public String getExpenseAccountCode() {
        return expenseAccountCode;
    }

    public void setExpenseAccountCode(String expenseAccountCode) {
        this.expenseAccountCode = expenseAccountCode;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public BigDecimal getAcquisitionCost() {
        return acquisitionCost;
    }

    public void setAcquisitionCost(BigDecimal acquisitionCost) {
        this.acquisitionCost = acquisitionCost;
    }

    public Integer getUsefulLife() {
        return usefulLife;
    }

    public void setUsefulLife(Integer usefulLife) {
        this.usefulLife = usefulLife;
    }

    public String getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(String depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public BigDecimal getResidualValue() {
        return residualValue;
    }

    public void setResidualValue(BigDecimal residualValue) {
        this.residualValue = residualValue;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }
}
