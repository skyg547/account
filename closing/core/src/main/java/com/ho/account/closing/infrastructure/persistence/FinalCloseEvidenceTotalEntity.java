package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "final_close_evidence_totals")
class FinalCloseEvidenceTotalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "control_id", nullable = false, updatable = false)
    private Long controlId;

    @Column(name = "account_code", nullable = false, length = 100, updatable = false)
    private String accountCode;

    @Column(name = "currency_code", nullable = false, length = 3, updatable = false)
    private String currencyCode;

    @Column(name = "source_total", nullable = false, precision = 38, scale = 18, updatable = false)
    private BigDecimal sourceTotal;

    @Column(name = "posted_total", nullable = false, precision = 38, scale = 18, updatable = false)
    private BigDecimal postedTotal;

    protected FinalCloseEvidenceTotalEntity() {
    }

    FinalCloseEvidenceTotalEntity(Long controlId, FinalCloseEvidenceTotal total) {
        this.controlId = controlId;
        this.accountCode = total.accountCode();
        this.currencyCode = total.currencyCode();
        this.sourceTotal = total.sourceTotal();
        this.postedTotal = total.postedTotal();
    }

    Long getControlId() {
        return controlId;
    }

    FinalCloseEvidenceTotal toDomain() {
        return new FinalCloseEvidenceTotal(accountCode, currencyCode, sourceTotal, postedTotal);
    }
}
