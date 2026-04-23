package com.ho.account.loan.dto;

import com.ho.account.loan.domain.Loan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 (Loan) 응답 DTO
 */
@Data
@Builder
public class LoanDto {
    private Long id;
    private String loanNumber;
    private Long businessPartnerId;
    private String businessPartnerName;
    private String currencyCode;
    private Loan.LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private LocalDate disbursalDate;
    private LocalDate maturityDate;
    private Loan.PaymentFrequency paymentFrequency;
    private BigDecimal initialEIR;
    private BigDecimal currentEIR;
    private Loan.LoanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static LoanDto fromEntity(Loan entity) {
        return LoanDto.builder()
                .id(entity.getId())
                .loanNumber(entity.getLoanNumber())
                .businessPartnerId(entity.getBusinessPartner() != null ? entity.getBusinessPartner().getId() : null)
                .businessPartnerName(entity.getBusinessPartner() != null ? entity.getBusinessPartner().getBusinessPartnerName() : null)
                .currencyCode(entity.getCurrency() != null ? entity.getCurrency().getCurrencyCode() : null)
                .loanType(entity.getLoanType())
                .principalAmount(entity.getPrincipalAmount())
                .interestRate(entity.getInterestRate())
                .disbursalDate(entity.getDisbursalDate())
                .maturityDate(entity.getMaturityDate())
                .paymentFrequency(entity.getPaymentFrequency())
                .initialEIR(entity.getInitialEIR())
                .currentEIR(entity.getCurrentEIR())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
