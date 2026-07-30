package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * 한 회계연도의 모든 예산 변경이 공유하는 영구 잠금 행입니다.
 *
 * <p>네 자리 연도 행은 migration에서 모두 생성되므로 application은 없는 행을
 * 잠그려 시도하지 않습니다. {@code @Version}은 비관적 잠금 외의 우발적 갱신도 감지합니다.</p>
 */
@Entity
@Table(name = "budget_fiscal_year_controls")
public class BudgetFiscalYearControlJpaEntity {

    @Id
    @Column(name = "fiscal_year", nullable = false, length = 4, updatable = false)
    private String fiscalYear;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected BudgetFiscalYearControlJpaEntity() {
    }

    String getFiscalYear() {
        return fiscalYear;
    }

    Long getVersion() {
        return version;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getClosedBy() {
        return closedBy;
    }

    void setClosedBy(String closedBy) {
        if (this.closedBy == null && closedBy != null && closedAt == null) {
            closedAt = LocalDateTime.now();
        }
        this.closedBy = closedBy;
    }

    LocalDateTime getClosedAt() {
        return closedAt;
    }
}
