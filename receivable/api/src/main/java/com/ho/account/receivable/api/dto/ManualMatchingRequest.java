package com.ho.account.receivable.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ho.account.receivable.application.port.in.ManualMatchingCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 수동 수납 매칭 HTTP 요청 DTO입니다. */
public class ManualMatchingRequest {

    @NotNull(message = "수금 ID는 필수입니다.")
    private Long collectionId;

    @NotNull(message = "매칭할 매출채권 ID는 필수입니다.")
    private Long receivableId;

    @NotNull(message = "매칭 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "매칭 금액은 0보다 커야 합니다.")
    @JsonAlias("amount")
    private BigDecimal matchingAmount;

    @NotBlank(message = "매칭 작업 ID는 필수입니다.")
    @Size(max = 90, message = "매칭 작업 ID는 90자를 넘을 수 없습니다.")
    private String operationId;

    public ManualMatchingCommand toCommand() {
        return new ManualMatchingCommand(collectionId, receivableId, matchingAmount, operationId);
    }

    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }

    public Long getReceivableId() { return receivableId; }
    public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }

    public BigDecimal getMatchingAmount() { return matchingAmount; }
    public void setMatchingAmount(BigDecimal matchingAmount) { this.matchingAmount = matchingAmount; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
}
