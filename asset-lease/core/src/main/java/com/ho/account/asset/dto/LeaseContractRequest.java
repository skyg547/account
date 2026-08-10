package com.ho.account.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class LeaseContractRequest {

    @NotBlank
    private String contractNo;

    @NotBlank
    private String contractName;

    private String lessorBusinessPartnerCode;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private BigDecimal monthlyPayment;

    @Min(1)
    @Max(31)
    private int paymentDay;

    @NotNull
    private BigDecimal discountRate;

    private BigDecimal initialRightOfUseAssetValue;
    private BigDecimal initialLeaseLiabilityValue;

    private String status;
    private String departmentCode;
    private String expenseAccountCode;

    private boolean ifrs16Applicable = true;
    private boolean shortTermLease = false;
    private boolean lowValueLease = false;
}
