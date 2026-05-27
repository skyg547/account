package com.risk.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 행동 모델링용 이력 엔티티
 * 통계적으로 고객의 출금(NMD), 조기상환(CPR) 예측을 위한 시계열 데이터 저장
 */
@Entity
@Table(name = "ods_behavioral_history", indexes = {
        @Index(name = "idx_bh_acc_date", columnList = "acc_no, base_dt")
})
public class OdsBehavioralHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_dt", nullable = false)
    @Comment("기준일자")
    private LocalDate baseDate;

    @Column(name = "acc_no", length = 50, nullable = false)
    @Comment("계좌번호")
    private String accountNo;

    @Column(name = "event_type", length = 20, nullable = false)
    @Comment("이벤트 유형 (SNAPSHOT, PREPAYMENT, WITHDRAWAL)")
    private String eventType;

    @Column(name = "balance_amt", precision = 19, scale = 4)
    @Comment("잔액")
    private BigDecimal balanceAmount;

    @Column(name = "event_amt", precision = 19, scale = 4)
    @Comment("발생 금액")
    private BigDecimal eventAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public OdsBehavioralHistoryEntity() {}

    public Long getId() { return id; }
    public LocalDate getBaseDate() { return baseDate; }
    public void setBaseDate(LocalDate baseDate) { this.baseDate = baseDate; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }
    public BigDecimal getEventAmount() { return eventAmount; }
    public void setEventAmount(BigDecimal eventAmount) { this.eventAmount = eventAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class Builder {
        private final OdsBehavioralHistoryEntity entity = new OdsBehavioralHistoryEntity();

        public Builder baseDate(LocalDate baseDate) {
            entity.setBaseDate(baseDate);
            return this;
        }

        public Builder accountNo(String accountNo) {
            entity.setAccountNo(accountNo);
            return this;
        }

        public Builder eventType(String eventType) {
            entity.setEventType(eventType);
            return this;
        }

        public Builder balanceAmount(BigDecimal balanceAmount) {
            entity.setBalanceAmount(balanceAmount);
            return this;
        }

        public Builder eventAmount(BigDecimal eventAmount) {
            entity.setEventAmount(eventAmount);
            return this;
        }

        public OdsBehavioralHistoryEntity build() {
            return entity;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}