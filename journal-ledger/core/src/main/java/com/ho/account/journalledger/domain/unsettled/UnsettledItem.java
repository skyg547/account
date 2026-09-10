package com.ho.account.journalledger.domain.unsettled;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.ledger.domain.AccountingPrecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Column(length = 50)
    private String lastSettledBy;

    @Column(length = 100)
    private String lastSettlementReference;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "unsettled_item_settlement_references", joinColumns = @JoinColumn(name = "unsettled_item_id"))
    @Column(name = "settlement_reference", length = 100, nullable = false)
    private Set<String> settlementReferences = new LinkedHashSet<>();

    private LocalDateTime lastSettledAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "OPEN";
        if (remainingAmount == null) remainingAmount = originalAmount;
        if (managementNo == null) {
            managementNo = "UNS-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (settlementReferences == null) {
            settlementReferences = new LinkedHashSet<>();
        }
        updateResolvedStatus();
    }

    private void updateResolvedStatus() {
        this.resolved = "CLEARED".equals(this.status);
    }

    public void settle(BigDecimal amount, String actor, String settlementReference) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("반제 처리자는 필수입니다.");
        }
        if (settlementReference == null || settlementReference.isBlank()) {
            throw new IllegalArgumentException("반제 참조번호는 필수입니다.");
        }
        String trimmedReference = settlementReference.trim();
        // 처리한 참조번호는 재시도 금액의 유효성과 무관하게 다시 반제하지 않습니다.
        if ((this.settlementReferences != null && this.settlementReferences.contains(trimmedReference))
                || trimmedReference.equals(this.lastSettlementReference)) {
            return;
        }
        BigDecimal normalizedAmount = AccountingPrecision.positiveLedgerAmount(amount);
        if (remainingAmount.compareTo(normalizedAmount) < 0) {
            throw new IllegalArgumentException("반제 금액이 잔액보다 클 수 없습니다. 잔액: " + remainingAmount + ", 반제요청: " + normalizedAmount);
        }
        // DB의 NUMERIC(19,2) 반올림으로 잔액과 상태가 어긋나지 않도록 두 다음 금액을 대입 전에 검증합니다.
        BigDecimal nextSettledAmount = AccountingPrecision.nonNegativeLedgerAmount(this.settledAmount.add(normalizedAmount));
        BigDecimal nextRemainingAmount = AccountingPrecision.nonNegativeLedgerAmount(this.remainingAmount.subtract(normalizedAmount));
        this.settledAmount = nextSettledAmount;
        this.remainingAmount = nextRemainingAmount;

        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = "CLEARED";
        } else {
            this.status = "PARTIAL";
        }
        if (this.settlementReferences == null) {
            this.settlementReferences = new LinkedHashSet<>();
        }
        this.settlementReferences.add(trimmedReference);
        this.lastSettledBy = actor.trim();
        this.lastSettlementReference = trimmedReference;
        this.lastSettledAt = LocalDateTime.now();
        updateResolvedStatus();
    }
}
