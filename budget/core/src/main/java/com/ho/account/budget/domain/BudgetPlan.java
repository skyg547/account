package com.ho.account.budget.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 한 달의 부서/계정 예산을 소유하는 Aggregate Root입니다.
 *
 * <p>{@code yearMonth + departmentCode + accountCode}가 업무상 유일키이며,
 * 가용액은 배정액 + 전입 - 전출 - 집행액으로 항상 다시 계산합니다. 파생값을 별도
 * 필드로 저장하지 않으므로 일부 값만 갱신되어 잔액이 어긋나는 문제를 막습니다.</p>
 */
public final class BudgetPlan {

    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{6}");

    private final Long id;
    private final String planCode;
    private final String yearMonth;
    private final String departmentCode;
    private final String accountCode;
    private final BigDecimal allocatedAmount;
    private BigDecimal transferInAmount;
    private BigDecimal transferOutAmount;
    private BigDecimal executedAmount;
    private BudgetPlanStatus status;
    private final String createdBy;
    private String approvedBy;
    private String closedBy;

    private BudgetPlan(
            Long id,
            String planCode,
            String yearMonth,
            String departmentCode,
            String accountCode,
            BigDecimal allocatedAmount,
            BigDecimal transferInAmount,
            BigDecimal transferOutAmount,
            BigDecimal executedAmount,
            BudgetPlanStatus status,
            String createdBy,
            String approvedBy,
            String closedBy) {
        this.id = validateId(id);
        this.planCode = requireText(planCode, "planCode");
        this.yearMonth = requireYearMonth(yearMonth);
        this.departmentCode = requireText(departmentCode, "departmentCode");
        this.accountCode = requireText(accountCode, "accountCode");
        this.allocatedAmount = BudgetPrecision.positive(allocatedAmount, "allocatedAmount");
        this.transferInAmount = BudgetPrecision.nonNegative(transferInAmount, "transferInAmount");
        this.transferOutAmount = BudgetPrecision.nonNegative(transferOutAmount, "transferOutAmount");
        this.executedAmount = BudgetPrecision.nonNegative(executedAmount, "executedAmount");
        this.status = Objects.requireNonNull(status, "status은(는) 필수입니다.");
        this.createdBy = requireText(createdBy, "createdBy");
        this.approvedBy = optionalText(approvedBy, "approvedBy");
        this.closedBy = optionalText(closedBy, "closedBy");
        validateRestoredState();
    }

    public static BudgetPlan create(
            String planCode,
            String yearMonth,
            String departmentCode,
            String accountCode,
            BigDecimal allocatedAmount,
            String createdBy) {
        return new BudgetPlan(
                null,
                planCode,
                yearMonth,
                departmentCode,
                accountCode,
                allocatedAmount,
                BudgetPrecision.zero(),
                BudgetPrecision.zero(),
                BudgetPrecision.zero(),
                BudgetPlanStatus.DRAFT,
                createdBy,
                null,
                null);
    }

    /**
     * 영속성 Adapter가 저장된 snapshot을 도메인으로 복원하는 유일한 진입점입니다.
     * 일반 호출자가 상태를 임의 변경하지 못하도록 setter 대신 완전한 상태 검증을 거칩니다.
     */
    public static BudgetPlan restore(
            Long id,
            String planCode,
            String yearMonth,
            String departmentCode,
            String accountCode,
            BigDecimal allocatedAmount,
            BigDecimal transferInAmount,
            BigDecimal transferOutAmount,
            BigDecimal executedAmount,
            BudgetPlanStatus status,
            String createdBy,
            String approvedBy,
            String closedBy) {
        if (id == null) {
            throw new IllegalArgumentException("복원하는 BudgetPlan의 id는 필수입니다.");
        }
        return new BudgetPlan(
                id,
                planCode,
                yearMonth,
                departmentCode,
                accountCode,
                allocatedAmount,
                transferInAmount,
                transferOutAmount,
                executedAmount,
                status,
                createdBy,
                approvedBy,
                closedBy);
    }

    public void approve(String actor) {
        requireStatus(BudgetPlanStatus.DRAFT, "승인");
        this.status = BudgetPlanStatus.APPROVED;
        this.approvedBy = requireText(actor, "actor");
    }

    public void close(String actor) {
        requireStatus(BudgetPlanStatus.APPROVED, "마감");
        this.status = BudgetPlanStatus.CLOSED;
        this.closedBy = requireText(actor, "actor");
    }

    public void transferOut(BigDecimal amount) {
        ensureApproved("예산 전출");
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        ensureAvailable(normalized);
        transferOutAmount = BudgetPrecision.amount(transferOutAmount.add(normalized), "transferOutAmount");
    }

    public void transferIn(BigDecimal amount) {
        ensureApproved("예산 전입");
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        transferInAmount = BudgetPrecision.amount(
                transferInAmount.add(normalized), "transferInAmount");
    }

    /**
     * 두 계획의 월/승인/잔액 규칙을 모두 확인한 뒤 양쪽 금액을 함께 변경합니다.
     *
     * <p>application service가 전출과 전입을 따로 호출하면 YYYYMM 검증을 잊기 쉽습니다.
     * 이 도메인 협력 메서드가 전용의 완전한 규칙을 한 진입점에 모읍니다.</p>
     */
    public void transferTo(BudgetPlan target, BigDecimal amount) {
        ensureTransferAllowedTo(target, amount);
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        transferOut(normalized);
        target.transferIn(normalized);
    }

    /**
     * 전용 요청 단계처럼 아직 금액을 바꾸면 안 되는 시점에 전체 전용 규칙만 검증합니다.
     */
    public void ensureTransferAllowedTo(BudgetPlan target, BigDecimal amount) {
        Objects.requireNonNull(target, "target은(는) 필수입니다.");
        ensureSameYearMonth(target, "예산 전용");
        ensureApproved("예산 전용");
        target.ensureApproved("예산 전용");
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        ensureAvailable(normalized);
        target.ensureTransferInCapacity(normalized);
    }

    public void execute(LocalDate executionDate, BigDecimal amount) {
        ensureExecutionDateMatches(executionDate);
        ensureApproved("예산 집행");
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        ensureAvailable(normalized);
        executedAmount = BudgetPrecision.amount(executedAmount.add(normalized), "executedAmount");
    }

    public void cancelExecution(BigDecimal amount) {
        ensureApproved("예산 집행 취소");
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        if (executedAmount.compareTo(normalized) < 0) {
            throw new IllegalStateException("취소 금액이 현재 집행액을 초과합니다.");
        }
        executedAmount =
                BudgetPrecision.amount(executedAmount.subtract(normalized), "executedAmount");
    }

    public void ensureApproved(String operation) {
        if (status != BudgetPlanStatus.APPROVED) {
            throw new IllegalStateException(operation + "은(는) 승인된 예산에서만 가능합니다.");
        }
    }

    public void ensureAvailable(BigDecimal amount) {
        BigDecimal normalized = BudgetPrecision.positive(amount, "amount");
        if (availableAmount().compareTo(normalized) < 0) {
            throw new IllegalStateException(
                    "가용 예산이 부족합니다. available=" + availableAmount() + ", requested=" + normalized);
        }
    }

    public void ensureSameYearMonth(BudgetPlan other, String operation) {
        Objects.requireNonNull(other, "other BudgetPlan은(는) 필수입니다.");
        if (!yearMonth.equals(other.yearMonth)) {
            throw new IllegalStateException(
                    operation
                            + "은(는) 같은 YYYYMM 예산 사이에서만 가능합니다. source="
                            + yearMonth
                            + ", target="
                            + other.yearMonth);
        }
    }

    public void ensureExecutionDateMatches(LocalDate executionDate) {
        Objects.requireNonNull(executionDate, "executionDate은(는) 필수입니다.");
        String executionYearMonth = "%04d%02d".formatted(
                executionDate.getYear(), executionDate.getMonthValue());
        if (!yearMonth.equals(executionYearMonth)) {
            throw new IllegalStateException(
                    "집행일은 예산의 YYYYMM과 일치해야 합니다. plan="
                            + yearMonth
                            + ", execution="
                            + executionYearMonth);
        }
    }

    public BigDecimal availableAmount() {
        /*
         * 가용액은 DB 컬럼이 아니라 여러 DECIMAL(19,2) 컬럼의 파생 합계입니다.
         * 각 저장 컬럼의 정밀도는 엄격히 제한하되, 합계까지 단일 컬럼의 17자리
         * 정수부로 자르면 저장 가능한 두 큰 금액의 정상적인 합산을 거부하게 됩니다.
         */
        return allocatedAmount
                .add(transferInAmount)
                .subtract(transferOutAmount)
                .subtract(executedAmount);
    }

    public String fiscalYear() {
        return yearMonth.substring(0, 4);
    }

    private void validateRestoredState() {
        BigDecimal available = availableAmount();
        if (available.signum() < 0) {
            throw new IllegalArgumentException("복원된 예산의 가용액은 음수일 수 없습니다.");
        }
        if (status == BudgetPlanStatus.DRAFT && (approvedBy != null || closedBy != null)) {
            throw new IllegalArgumentException("DRAFT 예산에는 승인자나 마감자가 있을 수 없습니다.");
        }
        if (status == BudgetPlanStatus.DRAFT
                && (transferInAmount.signum() != 0
                        || transferOutAmount.signum() != 0
                        || executedAmount.signum() != 0)) {
            throw new IllegalArgumentException("DRAFT 예산에는 전용이나 집행 실적이 있을 수 없습니다.");
        }
        if (status == BudgetPlanStatus.APPROVED && approvedBy == null) {
            throw new IllegalArgumentException("APPROVED 예산에는 승인자가 필요합니다.");
        }
        if (status == BudgetPlanStatus.APPROVED && closedBy != null) {
            throw new IllegalArgumentException("APPROVED 예산에는 마감자가 있을 수 없습니다.");
        }
        if (status == BudgetPlanStatus.CLOSED && (approvedBy == null || closedBy == null)) {
            throw new IllegalArgumentException("CLOSED 예산에는 승인자와 마감자가 필요합니다.");
        }
    }

    private void ensureTransferInCapacity(BigDecimal amount) {
        BudgetPrecision.amount(transferInAmount.add(amount), "transferInAmount");
    }

    private void requireStatus(BudgetPlanStatus expected, String operation) {
        if (status != expected) {
            throw new IllegalStateException(
                    operation + "할 수 없는 예산 상태입니다. expected=" + expected + ", actual=" + status);
        }
    }

    private static Long validateId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }
        return id;
    }

    private static String requireYearMonth(String value) {
        String normalized = requireText(value, "yearMonth");
        if (!YEAR_MONTH.matcher(normalized).matches()) {
            throw new IllegalArgumentException("yearMonth는 YYYYMM 형식이어야 합니다.");
        }
        int month = Integer.parseInt(normalized.substring(4));
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("yearMonth의 월은 01부터 12 사이여야 합니다.");
        }
        return normalized;
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    private static String optionalText(String value, String fieldName) {
        return value == null ? null : requireText(value, fieldName);
    }

    public Long id() {
        return id;
    }

    public String planCode() {
        return planCode;
    }

    public String yearMonth() {
        return yearMonth;
    }

    public String departmentCode() {
        return departmentCode;
    }

    public String accountCode() {
        return accountCode;
    }

    public BigDecimal allocatedAmount() {
        return allocatedAmount;
    }

    public BigDecimal transferInAmount() {
        return transferInAmount;
    }

    public BigDecimal transferOutAmount() {
        return transferOutAmount;
    }

    public BigDecimal executedAmount() {
        return executedAmount;
    }

    public BudgetPlanStatus status() {
        return status;
    }

    public String createdBy() {
        return createdBy;
    }

    public String approvedBy() {
        return approvedBy;
    }

    public String closedBy() {
        return closedBy;
    }

    /*
     * JavaBean getter들은 Adapter의 명시적 매핑을 편하게 하기 위한 읽기 전용 표면입니다.
     * 대응 setter는 의도적으로 제공하지 않으며 모든 변경은 위의 업무 메서드를 거칩니다.
     */
    public Long getId() {
        return id();
    }

    public String getPlanCode() {
        return planCode();
    }

    public String getYearMonth() {
        return yearMonth();
    }

    public String getDepartmentCode() {
        return departmentCode();
    }

    public String getAccountCode() {
        return accountCode();
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount();
    }

    public BigDecimal getTransferInAmount() {
        return transferInAmount();
    }

    public BigDecimal getTransferOutAmount() {
        return transferOutAmount();
    }

    public BigDecimal getExecutedAmount() {
        return executedAmount();
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount();
    }

    public BudgetPlanStatus getStatus() {
        return status();
    }

    public String getCreatedBy() {
        return createdBy();
    }

    public String getApprovedBy() {
        return approvedBy();
    }

    public String getClosedBy() {
        return closedBy();
    }
}
