package com.ho.account.journalledger.domain.unsettled;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "unsettled_items")
@Getter @Setter
@NoArgsConstructor
public class UnsettledItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String managementNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id", nullable = false)
    private JournalDetail journalDetail;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "bp_code", length = 50)
    private String businessPartnerCode;

    @Column(nullable = false)
    private LocalDate occurrenceDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(length = 20)
    private String status;

    @Column(nullable = false)
    private boolean resolved = false;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "OPEN";
        if (remainingAmount == null) remainingAmount = originalAmount;
        if (managementNo == null) {
            managementNo = "UNS-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        updateResolvedStatus();
    }

    private void updateResolvedStatus() {
        this.resolved = "CLEARED".equals(this.status);
    }

    public void settle(BigDecimal amount) {
        if (remainingAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException("반제 금액이 잔액보다 클 수 없습니다. 잔액: " + remainingAmount + ", 반제요청: " + amount);
        }
        this.settledAmount = this.settledAmount.add(amount);
        this.remainingAmount = this.remainingAmount.subtract(amount);

        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = "CLEARED";
        } else {
            this.status = "PARTIAL";
        }
        updateResolvedStatus();
    }
}