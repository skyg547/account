package com.ho.account.reconciliation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * [DDD(도메인 주도 설계) - Value Object (값 객체)]
 * 대사 항목(Reconciliation Item) 모델.
 * 
 * 🐣 [초보자를 위한 개념 설명: 대사 항목(Reconciliation Item)과 헥사고날 아키텍처]
 * 1. 개념과 의의:
 *    금융 대사(Financial Reconciliation) 시스템에서 원천(Source, 예: 원천 결제 시스템 레코드/은행 입출금 내역)과
 *    대상(Target, 예: 분개전표 상세/총계정원장)의 개별 거래 건을 추상화한 도메인 객체입니다.
 * 
 * 2. 왜 필요한가? (단순 총액 비교의 한계 극복):
 *    과거 단순 총액/건수 비교 방식은 "원천 100만 원(50만 원 2건)"과 "대상 100만 원(60만 원 + 40만 원 2건)"처럼
 *    총액은 동일하지만 개별 거래 건이 불일치하는 **상쇄 오류(Offsetting Errors)**를 적발하지 못하는 치명적인 상용 금융 위험이 있었습니다.
 *    `ReconciliationItem`을 도입함으로써 거래일자, 참조ID, 거래처코드, 계정과목 등 복합 키(Composite Key) 기반의
 *    건별 추적성(Item-Level Traceability)을 확보할 수 있습니다.
 * 
 * 3. 불변성(Immutability):
 *    대사 분석 수행 중 항목의 값이 임의로 변경되면 대사 결과 및 감사 추적(Audit Trail)의 신뢰성이 훼손되므로,
 *    모든 필드는 불변(Immutable) 상태로 관리됩니다.
 */
public class ReconciliationItem {

    public enum ItemSide {
        SOURCE,
        TARGET
    }

    private final String id;
    private final LocalDate transactionDate;
    private final String referenceId;
    private final String partnerCode;
    private final String accountCode;
    private final BigDecimal amount;
    private final ItemSide side;
    private final String description;

    public ReconciliationItem(String id,
                              LocalDate transactionDate,
                              String referenceId,
                              String partnerCode,
                              String accountCode,
                              BigDecimal amount,
                              ItemSide side,
                              String description) {
        this.id = id != null ? id.trim() : "";
        this.transactionDate = transactionDate;
        this.referenceId = referenceId != null ? referenceId.trim() : "";
        this.partnerCode = partnerCode != null ? partnerCode.trim() : "";
        this.accountCode = accountCode != null ? accountCode.trim() : "";
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.side = side != null ? side : ItemSide.SOURCE;
        this.description = description != null ? description.trim() : "";
    }

    public static ReconciliationItem ofSource(String id,
                                             LocalDate date,
                                             String refId,
                                             String partnerCode,
                                             String accountCode,
                                             BigDecimal amount,
                                             String description) {
        return new ReconciliationItem(id, date, refId, partnerCode, accountCode, amount, ItemSide.SOURCE, description);
    }

    public static ReconciliationItem ofTarget(String id,
                                             LocalDate date,
                                             String refId,
                                             String partnerCode,
                                             String accountCode,
                                             BigDecimal amount,
                                             String description) {
        return new ReconciliationItem(id, date, refId, partnerCode, accountCode, amount, ItemSide.TARGET, description);
    }

    public String getId() {
        return id;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public ItemSide getSide() {
        return side;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReconciliationItem item = (ReconciliationItem) o;
        return Objects.equals(id, item.id) &&
               side == item.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, side);
    }

    @Override
    public String toString() {
        return "ReconciliationItem{" +
                "id='" + id + '\'' +
                ", transactionDate=" + transactionDate +
                ", referenceId='" + referenceId + '\'' +
                ", partnerCode='" + partnerCode + '\'' +
                ", accountCode='" + accountCode + '\'' +
                ", amount=" + amount +
                ", side=" + side +
                '}';
    }
}
