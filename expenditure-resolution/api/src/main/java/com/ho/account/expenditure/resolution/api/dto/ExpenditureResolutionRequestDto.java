package com.ho.account.expenditure.resolution.api.dto;

import com.ho.account.expenditure.application.port.in.ExpenditureResolutionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 지출결의 HTTP 요청 DTO입니다.
 *
 * 초보자용 설명:
 * 이 객체는 화면/외부 시스템에서 들어온 JSON을 검증하는 API 전용 객체입니다.
 * 검증이 끝나면 `toCommand()`로 core 업무 명령으로 바뀌고, core는 이 DTO를 직접 알지 않습니다.
 */
public class ExpenditureResolutionRequestDto {

    private Long id; // 수정 시나리오용

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotNull(message = "결의일자는 필수입니다.")
    private LocalDate resolutionDate;

    @NotNull(message = "지급예정일은 필수입니다.")
    private LocalDate paymentDate;

    @NotBlank(message = "부서 코드는 필수입니다.")
    private String departmentCode;

    @NotBlank(message = "지급 계정 코드는 필수입니다.")
    private String paymentAccountCode;

    private Long taxInvoiceId;

    @Valid
    @Size(min = 1, message = "지출 상세 내역은 최소 하나 이상이어야 합니다.")
    private List<ExpenditureDetailRequestDto> details;

    public ExpenditureResolutionCommand toCommand() {
        List<ExpenditureResolutionCommand.DetailCommand> detailCommands = details == null
                ? List.of()
                : details.stream()
                        .map(ExpenditureDetailRequestDto::toCommand)
                        .toList();
        return new ExpenditureResolutionCommand(
                title,
                resolutionDate,
                paymentDate,
                departmentCode,
                paymentAccountCode,
                taxInvoiceId,
                detailCommands);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getResolutionDate() { return resolutionDate; }
    public void setResolutionDate(LocalDate resolutionDate) { this.resolutionDate = resolutionDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getPaymentAccountCode() { return paymentAccountCode; }
    public void setPaymentAccountCode(String paymentAccountCode) { this.paymentAccountCode = paymentAccountCode; }
    public Long getTaxInvoiceId() { return taxInvoiceId; }
    public void setTaxInvoiceId(Long taxInvoiceId) { this.taxInvoiceId = taxInvoiceId; }
    public List<ExpenditureDetailRequestDto> getDetails() { return details; }
    public void setDetails(List<ExpenditureDetailRequestDto> details) { this.details = details; }

    public static class ExpenditureDetailRequestDto {
        @NotBlank(message = "비용 계정 코드는 필수입니다.")
        private String accountSubjectCode;

        @NotNull(message = "금액은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "금액은 0보다 커야 합니다.")
        private BigDecimal amount;

        @NotBlank(message = "거래처 코드는 필수입니다.")
        private String businessPartnerCode;

        private String description;

        ExpenditureResolutionCommand.DetailCommand toCommand() {
            return new ExpenditureResolutionCommand.DetailCommand(
                    accountSubjectCode,
                    amount,
                    businessPartnerCode,
                    description);
        }

        public String getAccountSubjectCode() { return accountSubjectCode; }
        public void setAccountSubjectCode(String accountSubjectCode) { this.accountSubjectCode = accountSubjectCode; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getBusinessPartnerCode() { return businessPartnerCode; }
        public void setBusinessPartnerCode(String businessPartnerCode) { this.businessPartnerCode = businessPartnerCode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}