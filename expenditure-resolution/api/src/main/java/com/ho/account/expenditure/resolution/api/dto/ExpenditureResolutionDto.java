package com.ho.account.expenditure.resolution.api.dto;

import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExpenditureResolutionDto {

    private Long id;
    private String resolutionNo;
    private String title;
    private LocalDate resolutionDate;
    private LocalDate paymentDate;
    private String departmentCode;
    private String departmentName;
    private String paymentAccountCode;
    private String paymentAccountName;
    private BigDecimal totalAmount;
    private String status;
    private String rejectionReason;
    private Long taxInvoiceId;
    private List<ExpenditureDetailDto> details;

    public ExpenditureResolutionDto() {
    }

    public ExpenditureResolutionDto(
            Long id,
            String resolutionNo,
            String title,
            LocalDate resolutionDate,
            LocalDate paymentDate,
            String departmentCode,
            String departmentName,
            String paymentAccountCode,
            String paymentAccountName,
            BigDecimal totalAmount,
            String status,
            String rejectionReason,
            Long taxInvoiceId,
            List<ExpenditureDetailDto> details) {
        this.id = id;
        this.resolutionNo = resolutionNo;
        this.title = title;
        this.resolutionDate = resolutionDate;
        this.paymentDate = paymentDate;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.paymentAccountCode = paymentAccountCode;
        this.paymentAccountName = paymentAccountName;
        this.totalAmount = totalAmount;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.taxInvoiceId = taxInvoiceId;
        this.details = details;
    }

    public static ExpenditureResolutionDto fromEntity(ExpenditureResolution resolution) {
        List<ExpenditureDetailDto> detailDtos = resolution.getDetails() == null
                ? List.of()
                : resolution.getDetails().stream()
                        .map(ExpenditureDetailDto::fromEntity)
                        .toList();

        return new ExpenditureResolutionDto(
                resolution.getId(),
                resolution.getResolutionNo(),
                resolution.getTitle(),
                resolution.getResolutionDate(),
                resolution.getPaymentDate(),
                resolution.getDeptCode(),
                null, 
                resolution.getPaymentAccountCode(),
                null, 
                resolution.getTotalAmount(),
                resolution.getStatus() != null ? resolution.getStatus().name() : null,
                resolution.getRejectionReason(),
                resolution.getTaxInvoiceId(),
                detailDtos);
    }

    public Long getId() {
        return id;
    }

    public String getResolutionNo() {
        return resolutionNo;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getPaymentAccountCode() {
        return paymentAccountCode;
    }

    public String getPaymentAccountName() {
        return paymentAccountName;
    }

    public void setPaymentAccountName(String paymentAccountName) {
        this.paymentAccountName = paymentAccountName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Long getTaxInvoiceId() {
        return taxInvoiceId;
    }

    public List<ExpenditureDetailDto> getDetails() {
        return details;
    }

    public static class ExpenditureDetailDto {

        private Long id;
        private String accountSubjectCode;
        private String accountSubjectName;
        private BigDecimal amount;
        private String businessPartnerCode;
        private String businessPartnerName;
        private String description;

        public ExpenditureDetailDto() {
        }

        public ExpenditureDetailDto(
                Long id,
                String accountSubjectCode,
                String accountSubjectName,
                BigDecimal amount,
                String businessPartnerCode,
                String businessPartnerName,
                String description) {
            this.id = id;
            this.accountSubjectCode = accountSubjectCode;
            this.accountSubjectName = accountSubjectName;
            this.amount = amount;
            this.businessPartnerCode = businessPartnerCode;
            this.businessPartnerName = businessPartnerName;
            this.description = description;
        }

        public static ExpenditureDetailDto fromEntity(ExpenditureDetail detail) {
            return new ExpenditureDetailDto(
                    detail.getId(),
                    detail.getAccountCode(),
                    null, 
                    detail.getAmount(),
                    detail.getBusinessPartnerCode(),
                    null, 
                    detail.getDescription());
        }

        public Long getId() {
            return id;
        }

        public String getAccountSubjectCode() {
            return accountSubjectCode;
        }

        public String getAccountSubjectName() {
            return accountSubjectName;
        }

        public void setAccountSubjectName(String accountSubjectName) {
            this.accountSubjectName = accountSubjectName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getBusinessPartnerCode() {
            return businessPartnerCode;
        }

        public String getBusinessPartnerName() {
            return businessPartnerName;
        }

        public void setBusinessPartnerName(String businessPartnerName) {
            this.businessPartnerName = businessPartnerName;
        }

        public String getDescription() {
            return description;
        }
    }
}
