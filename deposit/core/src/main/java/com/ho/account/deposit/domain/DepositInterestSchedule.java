package com.ho.account.deposit.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예금 이자 지급 스케줄 (Deposit Interest Schedule) 엔티티.
 */
@Entity
@Table(name = "deposit_interest_schedules")
@Getter @Setter @NoArgsConstructor
public class DepositInterestSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false)
    private LocalDate accrualDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accruedInterest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterestStatus status;

    private Long journalEntryId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
