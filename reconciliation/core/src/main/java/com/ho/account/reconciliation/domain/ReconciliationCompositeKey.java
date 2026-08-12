package com.ho.account.reconciliation.domain;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * [DDD(도메인 주도 설계) - Value Object (복합 키 값 객체)]
 * 대사 항목의 자동 매칭 그룹화에 사용되는 복합 키(Composite Key).
 * 
 * 🐣 [초보자를 위한 개념 설명: 복합 키(Composite Key) 기반 대사 그룹화]
 * 1. 개념:
 *    금융 대사에서는 단일 데이터(예: 금액만 비교, 날짜만 비교)로 매칭을 시도할 경우 동등 금액의 서류가 서로 엉키는
 *    오매칭(False Positive Match)이 발생할 수 있습니다.
 *    따라서 `거래일자(transactionDate)`, `참조ID(referenceId)`, `거래처코드(partnerCode)`, `계정과목(accountCode)`을
 *    결합한 복합 키를 구성하여 대사 대상 데이터셋을 식별 가능한 소그룹(Bucket)으로 안전하게 파티셔닝합니다.
 * 
 * 2. 키 정규화(Normalization) 이유:
 *    시스템 간 이종 데이터 텍스트 차이(예: 소문자/대문자 "ref-001" vs "REF-001", 좌우 공백 등)로 인해
 *    매칭이 불일치 처리되는 현상을 방지하기 위해, 복합 키 생성 시 자동 공백 제거 및 대문자 변환(Uppercase Normalization)을 적용합니다.
 */
public final class ReconciliationCompositeKey {

    private final LocalDate transactionDate;
    private final String referenceId;
    private final String partnerCode;
    private final String accountCode;

    public ReconciliationCompositeKey(LocalDate transactionDate, String referenceId, String partnerCode, String accountCode) {
        this.transactionDate = transactionDate;
        this.referenceId = normalize(referenceId);
        this.partnerCode = normalize(partnerCode);
        this.accountCode = normalize(accountCode);
    }

    public static ReconciliationCompositeKey fromItem(ReconciliationItem item) {
        if (item == null) {
            return new ReconciliationCompositeKey(null, "", "", "");
        }
        return new ReconciliationCompositeKey(
                item.getTransactionDate(),
                item.getReferenceId(),
                item.getPartnerCode(),
                item.getAccountCode()
        );
    }

    public static ReconciliationCompositeKey relaxedKey(ReconciliationItem item) {
        if (item == null) {
            return new ReconciliationCompositeKey(null, "", "", "");
        }
        // 참조ID나 거래처코드가 누락/상이한 경우를 대비한 완화된 복합 키 (거래일자 + 계정과목)
        return new ReconciliationCompositeKey(
                item.getTransactionDate(),
                "",
                "",
                item.getAccountCode()
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public String getAccountCode() {
        return accountCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReconciliationCompositeKey key = (ReconciliationCompositeKey) o;
        return Objects.equals(transactionDate, key.transactionDate) &&
               Objects.equals(referenceId, key.referenceId) &&
               Objects.equals(partnerCode, key.partnerCode) &&
               Objects.equals(accountCode, key.accountCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionDate, referenceId, partnerCode, accountCode);
    }

    @Override
    public String toString() {
        return "CompositeKey[" +
                "date=" + transactionDate +
                ", ref='" + referenceId + '\'' +
                ", partner='" + partnerCode + '\'' +
                ", account='" + accountCode + '\'' +
                ']';
    }
}
