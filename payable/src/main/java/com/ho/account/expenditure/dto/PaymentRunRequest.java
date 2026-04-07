package com.ho.account.expenditure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

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
