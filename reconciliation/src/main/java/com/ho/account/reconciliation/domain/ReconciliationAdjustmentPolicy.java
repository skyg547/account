package com.ho.account.reconciliation.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 대사 조정 정책(Reconciliation Adjustment Policy).
 *
 * 조정분개 계정 산정 규칙을 서비스에서 분리해 검증한다. 숨은 기본 계정을 사용하면
 * 회계 처리가 잘못된 계정으로 전기될 수 있으므로, 조정 가능한 대사 단위는 명시적인
 * 차변/대변 계정 설정을 반드시 제공해야 한다.
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

        // @todo Configuration hardening: move adjustment account codes from free-form criteriaJson into a typed, validated reconciliation policy.
        String criteriaJson = unit.getCriteriaJson();
        if (criteriaJson == null || criteriaJson.isBlank()) {
            throw missingAccountCodes(unit);
        }

        try {
            JsonNode root = objectMapper.readTree(criteriaJson);
            String debitAccountCode = readText(root, "adjustmentDebitAccountCode");
            String creditAccountCode = readText(root, "adjustmentCreditAccountCode");

            if (debitAccountCode == null || creditAccountCode == null) {
                throw missingAccountCodes(unit);
            }

            return new AdjustmentAccountCodes(debitAccountCode, creditAccountCode);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Reconciliation unit " + unit.getId()
                    + " has invalid criteriaJson for adjustment account policy.", e);
        }
    }

    private IllegalArgumentException missingAccountCodes(ReconciliationUnit unit) {
        return new IllegalArgumentException("Adjustable reconciliation unit " + unit.getId()
                + " requires adjustmentDebitAccountCode and adjustmentCreditAccountCode in criteriaJson.");
    }

    private String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    /**
     * 조정 계정 코드 쌍을 담는 레코드
     */
    public record AdjustmentAccountCodes(String debitAccountCode, String creditAccountCode) {
    }
}
