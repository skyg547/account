package com.ho.account.reconciliation.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 대사 조정 정책(Reconciliation Adjustment Policy).
 *
 * 조정분개 계정 산정 규칙을 서비스에서 분리해 검증한다. 숨은 기본 계정을 사용하면
 * 회계 처리가 잘못된 계정으로 전기될 수 있으므로, 조정 가능한 대사 단위는 명시적인
 * 차변/대변 계정 설정을 반드시 제공해야 한다.
 * 또한 동일 논리적 대사 기간(Unit + Date)에 대해 조정 전표가 중복 발행되지 않도록
 * 멱등성 판정 및 결정론적 원천 문서 식별자 생성을 전담한다.
 */
@Component
public class ReconciliationAdjustmentPolicy {

    private final ObjectMapper objectMapper;

    public ReconciliationAdjustmentPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 대사 단위의 설정을 기반으로 조정분개에 사용할 차변/대변 계정코드를 결정합니다.
     * 
     * @param unit 대사 단위
     * @return 조정 계정 코드 쌍
     */
    public AdjustmentAccountCodes resolveAdjustmentAccountCodes(ReconciliationUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Reconciliation unit is required.");
        }

        // T38 fixed: Moved adjustment account codes from free-form criteriaJson into a typed, validated reconciliation policy.
        String criteriaJson = unit.getCriteriaJson();
        if (criteriaJson == null || criteriaJson.isBlank()) {
            throw missingAccountCodes(unit);
        }

        try {
            AdjustmentPolicyConfig config = objectMapper.readValue(criteriaJson, AdjustmentPolicyConfig.class);
            if (config.adjustmentDebitAccountCode() == null || config.adjustmentCreditAccountCode() == null) {
                throw missingAccountCodes(unit);
            }
            return new AdjustmentAccountCodes(config.adjustmentDebitAccountCode(), config.adjustmentCreditAccountCode());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Reconciliation unit " + unit.getId()
                    + " has invalid criteriaJson for adjustment account policy.", e);
        }
    }

    /**
     * 특정 대사 단위와 대사 기준일(논리적 대사 기간)에 대해 조정 전표 생성이 가능한지 정책을 판정합니다.
     * 해당 기간에 이미 조정 전표가 발행된 경우 중복 발행을 불허합니다.
     *
     * @param hasExistingAdjustment 해당 기간에 이미 발행된 조정 전표 존재 여부
     * @return 조정 전표 생성 허용 여부
     */
    public boolean canGenerateAdjustment(boolean hasExistingAdjustment) {
        return !hasExistingAdjustment;
    }

    /**
     * 논리적 대사 기간(Reconciliation Unit ID + Reconciliation Date)에 대한 고유 식별 키를 생성합니다.
     */
    public String buildLogicalPeriodKey(Long unitId, LocalDate reconciliationDate) {
        if (unitId == null || reconciliationDate == null) {
            throw new IllegalArgumentException("Unit ID and reconciliation date are required.");
        }
        return "UNIT-" + unitId + "-DATE-" + reconciliationDate;
    }

    /**
     * 조정 전표의 멱등적 추적을 위한 lineageSourceId를 생성합니다.
     */
    public String buildAdjustmentSourceDocumentId(ReconciliationRun run,
                                                   ReconciliationDifference difference,
                                                   LocalDate accountingDate,
                                                   BigDecimal amount,
                                                   AdjustmentAccountCodes accountCodes) {
        String runKey = (run != null && run.getId() != null)
                ? "RUN-" + run.getId()
                : "UNIT-" + (run != null && run.getReconciliationUnit() != null ? run.getReconciliationUnit().getId() : "NA") + "-DATE-" + accountingDate;
        String differenceKey = (difference != null && difference.getId() != null)
                ? "DIFF-" + difference.getId()
                : "DIFF-" + (difference != null && difference.getDifferenceType() != null ? difference.getDifferenceType() : "MISMATCH") + "-AMOUNT-" + normalizeAmountKey(amount);
        return "RECON_ADJ-" + runKey + "-" + differenceKey
                + "-DR-" + accountCodes.debitAccountCode()
                + "-CR-" + accountCodes.creditAccountCode();
    }

    private String normalizeAmountKey(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString().replace('.', '_');
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdjustmentPolicyConfig(
        String adjustmentDebitAccountCode,
        String adjustmentCreditAccountCode
    ) {}

    private IllegalArgumentException missingAccountCodes(ReconciliationUnit unit) {
        return new IllegalArgumentException("Adjustable reconciliation unit " + unit.getId()
                + " requires adjustmentDebitAccountCode and adjustmentCreditAccountCode in criteriaJson.");
    }

    /**
     * 조정 계정 코드 쌍을 담는 레코드
     */
    public record AdjustmentAccountCodes(String debitAccountCode, String creditAccountCode) {
    }
}
