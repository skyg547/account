package com.ho.account.budget.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 한 회계연도의 모든 예산 변경을 여닫는 durable Aggregate Root입니다.
 *
 * <p>예산안 자체의 상태만 확인하면, 연말 마감이 예산 행들을 잠그는 사이 아직 잠기지
 * 않은 행을 다른 트랜잭션이 변경할 수 있습니다. 모든 변경이 먼저 이 제어 행을 잠그면
 * 마감과 변경이 같은 직렬화 지점을 공유하므로 그 틈이 사라집니다.</p>
 */
public final class BudgetFiscalYearControl {

    private static final Pattern FISCAL_YEAR = Pattern.compile("\\d{4}");

    private final String fiscalYear;
    private BudgetFiscalYearStatus status;
    private String closedBy;

    private BudgetFiscalYearControl(
            String fiscalYear, BudgetFiscalYearStatus status, String closedBy) {
        this.fiscalYear = requireFiscalYear(fiscalYear);
        this.status = Objects.requireNonNull(status, "status은(는) 필수입니다.");
        this.closedBy = closedBy == null ? null : BudgetPlan.requireText(closedBy, "closedBy");
        if (status == BudgetFiscalYearStatus.OPEN && this.closedBy != null) {
            throw new IllegalArgumentException("OPEN 회계연도에는 마감자가 있을 수 없습니다.");
        }
        if (status == BudgetFiscalYearStatus.CLOSED && this.closedBy == null) {
            throw new IllegalArgumentException("CLOSED 회계연도에는 마감자가 필요합니다.");
        }
    }

    /**
     * migration이나 관리 기능이 사전 생성할 OPEN 제어 행을 만듭니다.
     */
    public static BudgetFiscalYearControl open(String fiscalYear) {
        return new BudgetFiscalYearControl(fiscalYear, BudgetFiscalYearStatus.OPEN, null);
    }

    /**
     * 영속성 Adapter가 저장된 회계연도 제어 상태를 복원하는 유일한 진입점입니다.
     */
    public static BudgetFiscalYearControl restore(
            String fiscalYear, BudgetFiscalYearStatus status, String closedBy) {
        return new BudgetFiscalYearControl(fiscalYear, status, closedBy);
    }

    public void ensureOpen(String operation) {
        if (status != BudgetFiscalYearStatus.OPEN) {
            throw new IllegalStateException(
                    fiscalYear + " 회계연도는 마감되어 " + operation + "할 수 없습니다.");
        }
    }

    public void close(String actor) {
        ensureOpen("마감");
        status = BudgetFiscalYearStatus.CLOSED;
        closedBy = BudgetPlan.requireText(actor, "actor");
    }

    public String fiscalYear() {
        return fiscalYear;
    }

    public BudgetFiscalYearStatus status() {
        return status;
    }

    public String closedBy() {
        return closedBy;
    }

    public String getFiscalYear() {
        return fiscalYear();
    }

    public BudgetFiscalYearStatus getStatus() {
        return status();
    }

    public String getClosedBy() {
        return closedBy();
    }

    private static String requireFiscalYear(String fiscalYear) {
        if (fiscalYear == null || !FISCAL_YEAR.matcher(fiscalYear.trim()).matches()) {
            throw new IllegalArgumentException("fiscalYear는 YYYY 형식이어야 합니다.");
        }
        return fiscalYear.trim();
    }
}
