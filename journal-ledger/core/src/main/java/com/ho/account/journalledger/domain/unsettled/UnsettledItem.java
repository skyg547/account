package com.ho.account.journalledger.domain.unsettled;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 미결 항목(Unsettled Item) 엔티티
 * 반제(Settlement) 처리가 필요한 항목을 관리한다.
 */
@Entity
@Table(name = "unsettled_items")
@Getter @Setter
@NoArgsConstructor
public class UnsettledItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String managementNo; // 미결 관리 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id", nullable = false)
    private JournalDetail journalDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bp_id")
    private BusinessPartner businessPartner;

    @Column(nullable = false)
    private LocalDate occurrenceDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(length = 20)
    private String status; // OPEN, PARTIAL, CLEARED

    @Column(nullable = false)
    private boolean resolved = false;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "OPEN";
        if (remainingAmount == null) remainingAmount = originalAmount;
        if (managementNo == null) managementNo = "UNS-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        updateResolvedStatus();
    }

    private void updateResolvedStatus() {
        this.resolved = "CLEARED".equals(this.status);
    }

    public void settle(BigDecimal amount) {
        if (remainingAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException("반제 금액이 잔액보다 클 수 없습니다.");
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
