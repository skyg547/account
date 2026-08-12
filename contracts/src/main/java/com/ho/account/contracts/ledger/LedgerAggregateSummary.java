package com.ho.account.contracts.ledger;

import java.math.BigDecimal;

/**
 * [Push-Down Aggregation DTO]
 * DB 레벨에서 SUM(amount) 및 COUNT(*) 집계를 직접 수행한 결과를 담는 요약 DTO.
 *
 * <p>교육적 가치 및 아키텍처 관점:</p>
 * <ul>
 *   <li><b>JVM Heap OOM 방지:</b> 원장 데이터가 대량으로 존재할 경우 모든 객체를 애플리케이션 메모리로 로딩하면
 *       Memory Footprint가 급증하여 Out-Of-Memory(OOM) 오류가 발생할 위험이 있습니다.</li>
 *   <li><b>Push-Down Aggregation:</b> 연산(합계/개수)을 데이터가 위치한 DB 쿼리 레벨로 내려보냄으로써(Push-down),
 *       네트워크 I/O 및 메모리 사용량을 최소화하고 DB 쿼리 엔진의 인덱스 및 집계 최적화를 최대한 활용합니다.</li>
 * </ul>
 */
public class LedgerAggregateSummary {

    private long count;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public LedgerAggregateSummary() {
    }

    public LedgerAggregateSummary(long count, BigDecimal totalAmount) {
        this.count = count;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }
}
