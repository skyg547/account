package com.ho.account.expenditure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 지급 런 생성 요청 DTO입니다.
 *
 * <p>초보자용 설명: 지급 런은 "오늘 지급 대상 채무를 한 번에 모으는 작업"입니다.
 * 컨트롤러는 이 DTO로 실행일과 생성자를 먼저 확인한 뒤 PaymentService에 전달합니다.</p>
 */
public class PaymentRunRequest {

    @NotNull(message = "지급 실행일은 필수입니다.")
    private LocalDate runDate;

    private String description;

    @NotBlank(message = "생성자 정보는 필수입니다.")
    private String createdBy;

    // Getter 및 Setter
    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
