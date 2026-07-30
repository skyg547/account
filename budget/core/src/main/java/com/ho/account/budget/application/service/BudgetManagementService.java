package com.ho.account.budget.application.service;

import com.ho.account.budget.application.exception.BudgetConflictException;
import com.ho.account.budget.application.exception.BudgetResourceNotFoundException;
import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import com.ho.account.budget.application.port.in.BudgetManagementUseCase;
import com.ho.account.budget.application.port.in.BudgetYearEndUseCase;
import com.ho.account.budget.application.port.in.CreateBudgetPlanCommand;
import com.ho.account.budget.application.port.in.ExecuteBudgetCommand;
import com.ho.account.budget.application.port.in.RequestBudgetTransferCommand;
import com.ho.account.budget.application.port.out.BudgetExecutionPersistencePort;
import com.ho.account.budget.application.port.out.BudgetFiscalYearControlPersistencePort;
import com.ho.account.budget.application.port.out.BudgetIdempotencyLockPort;
import com.ho.account.budget.application.port.out.BudgetPlanPersistencePort;
import com.ho.account.budget.application.port.out.BudgetTransferPersistencePort;
import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetExecutionStatus;
import com.ho.account.budget.domain.BudgetFiscalYearControl;
import com.ho.account.budget.domain.BudgetFiscalYearStatus;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetTransfer;
import com.ho.account.budget.domain.BudgetTransferStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예산 Aggregate들의 트랜잭션과 잠금 순서를 소유하는 application service입니다.
 *
 * <p>변경 작업의 전역 순서는 {@code idempotency shard -> fiscal-year control ->
 * plan id ascending}입니다. 연말 마감도 fiscal-year control을 먼저 잠그므로 마감과
 * 승인/전용/집행이 서로 기다리더라도 반대 순서로 잠금을 잡아 교착하지 않습니다.</p>
 */
@Service
@Transactional
public class BudgetManagementService implements BudgetManagementUseCase, BudgetYearEndUseCase {

    private static final Pattern FISCAL_YEAR = Pattern.compile("\\d{4}");
    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{6}");

    private final BudgetPlanPersistencePort planPersistencePort;
    private final BudgetTransferPersistencePort transferPersistencePort;
    private final BudgetExecutionPersistencePort executionPersistencePort;
    private final BudgetFiscalYearControlPersistencePort fiscalYearControlPersistencePort;
    private final BudgetIdempotencyLockPort idempotencyLockPort;

    public BudgetManagementService(
            BudgetPlanPersistencePort planPersistencePort,
            BudgetTransferPersistencePort transferPersistencePort,
            BudgetExecutionPersistencePort executionPersistencePort,
            BudgetFiscalYearControlPersistencePort fiscalYearControlPersistencePort,
            BudgetIdempotencyLockPort idempotencyLockPort) {
        this.planPersistencePort = planPersistencePort;
        this.transferPersistencePort = transferPersistencePort;
        this.executionPersistencePort = executionPersistencePort;
        this.fiscalYearControlPersistencePort = fiscalYearControlPersistencePort;
        this.idempotencyLockPort = idempotencyLockPort;
    }

    @Override
    public BudgetPlan createPlan(CreateBudgetPlanCommand command) {
        requireCommand(command);
        BudgetFiscalYearControl control = lockFiscalYear(command.yearMonth().substring(0, 4));
        applyDomainRule(() -> control.ensureOpen("예산안 생성"));

        planPersistencePort.findByPlanCode(command.planCode()).ifPresent(existing -> {
            throw new BudgetConflictException("이미 존재하는 예산 코드입니다: " + command.planCode());
        });
        planPersistencePort
                .findByBusinessKey(
                        command.yearMonth(), command.departmentCode(), command.accountCode())
                .ifPresent(existing -> {
                    throw new BudgetConflictException(
                            "동일 연월/부서/계정의 예산이 이미 존재합니다: "
                                    + command.yearMonth()
                                    + "/"
                                    + command.departmentCode()
                                    + "/"
                                    + command.accountCode());
                });

        BudgetPlan plan = applyDomainRule(() -> BudgetPlan.create(
                command.planCode(),
                command.yearMonth(),
                command.departmentCode(),
                command.accountCode(),
                command.allocatedAmount(),
                command.actor()));
        return planPersistencePort.save(plan);
    }

    @Override
    public BudgetPlan approvePlan(Long planId, String actor) {
        Long normalizedId = requirePositiveId(planId, "planId");
        String normalizedActor = requireActor(actor);

        // 연도를 알기 위한 snapshot은 잠그지 않습니다. 불변인 yearMonth를 읽은 뒤
        // 반드시 fiscal-year control부터 잠그고 계획 행을 잠급니다.
        BudgetPlan snapshot = findPlan(normalizedId);
        BudgetFiscalYearControl control = lockFiscalYear(snapshot.fiscalYear());
        applyDomainRule(() -> control.ensureOpen("예산안 승인"));
        BudgetPlan plan = lockPlan(normalizedId);
        applyDomainRule(() -> plan.approve(normalizedActor));
        return planPersistencePort.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BudgetPlan> findPlan(
            String yearMonth, String departmentCode, String accountCode) {
        String normalizedYearMonth = requireYearMonth(yearMonth);
        return planPersistencePort.findByBusinessKey(
                normalizedYearMonth,
                requireText(departmentCode, "departmentCode"),
                requireText(accountCode, "accountCode"));
    }

    @Override
    public BudgetTransfer requestTransfer(RequestBudgetTransferCommand command) {
        requireCommand(command);

        // 없는 transfer 결과 행은 잠글 수 없습니다. 먼저 사전 생성 shard를 잠가
        // 같은 requestKey의 첫 동시 호출부터 직렬화한 뒤 존재 여부를 확인합니다.
        idempotencyLockPort.lockTransferRequestKey(command.requestKey());
        Optional<BudgetTransfer> previous =
                transferPersistencePort.findByRequestKey(command.requestKey());
        if (previous.isPresent()) {
            BudgetTransfer existing = previous.get();
            if (!existing.hasSameRequest(
                            command.sourcePlanId(),
                            command.targetPlanId(),
                            command.amount(),
                            command.actor())) {
                throw new BudgetConflictException(
                        "requestKey가 다른 전용 요청에 이미 사용되었습니다.");
            }
            return existing;
        }

        BudgetPlan sourceSnapshot = findPlan(command.sourcePlanId());
        BudgetPlan targetSnapshot = findPlan(command.targetPlanId());
        applyDomainRule(() ->
                sourceSnapshot.ensureTransferAllowedTo(targetSnapshot, command.amount()));

        BudgetFiscalYearControl control = lockFiscalYear(sourceSnapshot.fiscalYear());
        applyDomainRule(() -> control.ensureOpen("예산 전용 요청"));
        LockedPlanPair plans =
                lockPlansAscending(command.sourcePlanId(), command.targetPlanId());
        BudgetPlan source = plans.plan(command.sourcePlanId());
        BudgetPlan target = plans.plan(command.targetPlanId());
        applyDomainRule(() -> source.ensureTransferAllowedTo(target, command.amount()));

        BudgetTransfer transfer = applyDomainRule(() -> BudgetTransfer.request(
                command.requestKey(),
                command.sourcePlanId(),
                command.targetPlanId(),
                command.amount(),
                command.actor()));
        return transferPersistencePort.save(transfer);
    }

    @Override
    public BudgetTransfer approveTransfer(Long transferId, String actor) {
        Long normalizedId = requirePositiveId(transferId, "transferId");
        String normalizedActor = requireActor(actor);
        BudgetTransfer snapshot = findTransfer(normalizedId);
        if (snapshot.status() == BudgetTransferStatus.APPROVED) {
            return snapshot;
        }

        BudgetPlan sourceSnapshot = findPlan(snapshot.sourcePlanId());
        BudgetPlan targetSnapshot = findPlan(snapshot.targetPlanId());
        applyDomainRule(() ->
                sourceSnapshot.ensureSameYearMonth(targetSnapshot, "예산 전용 승인"));

        BudgetFiscalYearControl control = lockFiscalYear(sourceSnapshot.fiscalYear());
        BudgetTransfer transfer = lockTransfer(normalizedId);
        // 제어 행을 기다리는 동안 다른 승인 트랜잭션이 끝났다면 완료 결과를 그대로 읽습니다.
        if (transfer.status() == BudgetTransferStatus.APPROVED) {
            return transfer;
        }
        applyDomainRule(() -> control.ensureOpen("예산 전용 승인"));

        LockedPlanPair plans =
                lockPlansAscending(transfer.sourcePlanId(), transfer.targetPlanId());
        BudgetPlan source = plans.plan(transfer.sourcePlanId());
        BudgetPlan target = plans.plan(transfer.targetPlanId());
        applyDomainRule(() -> source.transferTo(target, transfer.amount()));
        applyDomainRule(() -> transfer.approve(normalizedActor));

        planPersistencePort.save(source);
        planPersistencePort.save(target);
        return transferPersistencePort.save(transfer);
    }

    @Override
    public BudgetExecution execute(ExecuteBudgetCommand command) {
        requireCommand(command);

        idempotencyLockPort.lockExecutionSourceKey(
                command.sourceType(), command.sourceId(), command.sourceLineId());
        Optional<BudgetExecution> previous = executionPersistencePort.findBySource(
                command.sourceType(), command.sourceId(), command.sourceLineId());
        if (previous.isPresent()) {
            BudgetExecution existing = previous.get();
            if (!existing.hasSameExecution(
                            command.budgetPlanId(),
                            command.executionDate(),
                            command.amount(),
                            command.actor())) {
                throw new BudgetConflictException(
                        "동일 원천 라인이 다른 예산 집행에 이미 사용되었습니다.");
            }
            return existing;
        }

        BudgetPlan snapshot = findPlan(command.budgetPlanId());
        applyDomainRule(() -> snapshot.ensureExecutionDateMatches(command.executionDate()));
        BudgetFiscalYearControl control = lockFiscalYear(snapshot.fiscalYear());
        applyDomainRule(() -> control.ensureOpen("예산 집행"));
        BudgetPlan plan = lockPlan(command.budgetPlanId());
        applyDomainRule(() -> plan.execute(command.executionDate(), command.amount()));

        BudgetExecution execution = applyDomainRule(() -> BudgetExecution.execute(
                command.budgetPlanId(),
                command.sourceType(),
                command.sourceId(),
                command.sourceLineId(),
                command.executionDate(),
                command.amount(),
                command.actor()));
        planPersistencePort.save(plan);
        return executionPersistencePort.save(execution);
    }

    @Override
    public BudgetExecution cancelExecution(Long executionId, String actor) {
        Long normalizedId = requirePositiveId(executionId, "executionId");
        String normalizedActor = requireActor(actor);
        BudgetExecution snapshot = findExecution(normalizedId);
        if (snapshot.status() == BudgetExecutionStatus.CANCELLED) {
            return snapshot;
        }

        BudgetPlan planSnapshot = findPlan(snapshot.budgetPlanId());
        BudgetFiscalYearControl control = lockFiscalYear(planSnapshot.fiscalYear());
        BudgetExecution execution = lockExecution(normalizedId);
        if (execution.status() == BudgetExecutionStatus.CANCELLED) {
            return execution;
        }
        applyDomainRule(() -> control.ensureOpen("예산 집행 취소"));
        BudgetPlan plan = lockPlan(execution.budgetPlanId());
        applyDomainRule(() -> plan.cancelExecution(execution.amount()));
        applyDomainRule(() -> execution.cancel(normalizedActor));
        planPersistencePort.save(plan);
        return executionPersistencePort.save(execution);
    }

    @Override
    public int closeFiscalYear(String fiscalYear, String actor) {
        String normalizedYear = requireFiscalYear(fiscalYear);
        String normalizedActor = requireActor(actor);
        BudgetFiscalYearControl control = lockFiscalYear(normalizedYear);
        if (control.status() == BudgetFiscalYearStatus.CLOSED) {
            return 0;
        }

        List<BudgetPlan> plans = new ArrayList<>(
                planPersistencePort.findApprovedByFiscalYearForUpdateOrderById(normalizedYear));
        plans.sort(Comparator.comparing(BudgetPlan::id));

        // Adapter 결과 전체를 먼저 검증해 한 건도 부분 마감하지 않습니다.
        for (BudgetPlan plan : plans) {
            if (!normalizedYear.equals(plan.fiscalYear())) {
                throw new BudgetRuleViolationException(
                        "영속성 Port가 다른 회계연도의 예산을 반환했습니다.");
            }
            applyDomainRule(() -> plan.ensureApproved("회계연도 마감"));
        }

        // 제어 행은 이미 잠겨 있습니다. CLOSED와 개별 계획 마감은 같은 트랜잭션으로
        // commit되므로 뒤따르는 변경자는 CLOSED를 보거나 이 트랜잭션의 rollback을 봅니다.
        applyDomainRule(() -> control.close(normalizedActor));
        fiscalYearControlPersistencePort.save(control);
        for (BudgetPlan plan : plans) {
            applyDomainRule(() -> plan.close(normalizedActor));
            planPersistencePort.save(plan);
        }
        return plans.size();
    }

    private BudgetFiscalYearControl lockFiscalYear(String fiscalYear) {
        BudgetFiscalYearControl control = fiscalYearControlPersistencePort
                .findByFiscalYearForUpdate(fiscalYear)
                .orElseThrow(() -> new BudgetRuleViolationException(
                        "회계연도 제어 행이 구성되지 않았습니다: " + fiscalYear));
        if (!fiscalYear.equals(control.fiscalYear())) {
            throw new BudgetRuleViolationException(
                    "영속성 Port가 다른 회계연도 제어 행을 반환했습니다: requested="
                            + fiscalYear
                            + ", actual="
                            + control.fiscalYear());
        }
        return control;
    }

    private BudgetPlan findPlan(Long planId) {
        return planPersistencePort
                .findById(planId)
                .orElseThrow(() ->
                        new BudgetResourceNotFoundException("예산을 찾을 수 없습니다: " + planId));
    }

    private BudgetPlan lockPlan(Long planId) {
        return planPersistencePort
                .findByIdForUpdate(planId)
                .orElseThrow(() ->
                        new BudgetResourceNotFoundException("예산을 찾을 수 없습니다: " + planId));
    }

    private BudgetTransfer findTransfer(Long transferId) {
        return transferPersistencePort
                .findById(transferId)
                .orElseThrow(() -> new BudgetResourceNotFoundException(
                        "예산 전용 요청을 찾을 수 없습니다: " + transferId));
    }

    private BudgetTransfer lockTransfer(Long transferId) {
        return transferPersistencePort
                .findByIdForUpdate(transferId)
                .orElseThrow(() -> new BudgetResourceNotFoundException(
                        "예산 전용 요청을 찾을 수 없습니다: " + transferId));
    }

    private BudgetExecution findExecution(Long executionId) {
        return executionPersistencePort
                .findById(executionId)
                .orElseThrow(() -> new BudgetResourceNotFoundException(
                        "예산 집행을 찾을 수 없습니다: " + executionId));
    }

    private BudgetExecution lockExecution(Long executionId) {
        return executionPersistencePort
                .findByIdForUpdate(executionId)
                .orElseThrow(() -> new BudgetResourceNotFoundException(
                        "예산 집행을 찾을 수 없습니다: " + executionId));
    }

    private LockedPlanPair lockPlansAscending(Long firstId, Long secondId) {
        Long normalizedFirst = requirePositiveId(firstId, "firstPlanId");
        Long normalizedSecond = requirePositiveId(secondId, "secondPlanId");
        if (normalizedFirst.equals(normalizedSecond)) {
            throw new BudgetRuleViolationException("서로 다른 두 예산이 필요합니다.");
        }

        Long lowerId = Math.min(normalizedFirst, normalizedSecond);
        Long higherId = Math.max(normalizedFirst, normalizedSecond);
        BudgetPlan lower = lockPlan(lowerId);
        BudgetPlan higher = lockPlan(higherId);
        return new LockedPlanPair(lower, higher);
    }

    private static <T> T requireCommand(T command) {
        if (command == null) {
            throw new BudgetRuleViolationException("command는 필수입니다.");
        }
        return command;
    }

    private static Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BudgetRuleViolationException(fieldName + "는 양수여야 합니다.");
        }
        return id;
    }

    private static String requireActor(String actor) {
        return requireText(actor, "actor", 80);
    }

    private static String requireText(String value, String fieldName) {
        return requireText(value, fieldName, Integer.MAX_VALUE);
    }

    private static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new BudgetRuleViolationException(fieldName + "은(는) 필수입니다.");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new BudgetRuleViolationException(
                    fieldName + "은(는) " + maximumLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private static String requireFiscalYear(String fiscalYear) {
        String normalized = requireText(fiscalYear, "fiscalYear");
        if (!FISCAL_YEAR.matcher(normalized).matches()) {
            throw new BudgetRuleViolationException("fiscalYear는 YYYY 형식이어야 합니다.");
        }
        return normalized;
    }

    private static String requireYearMonth(String yearMonth) {
        String normalized = requireText(yearMonth, "yearMonth");
        if (!YEAR_MONTH.matcher(normalized).matches()) {
            throw new BudgetRuleViolationException("yearMonth는 YYYYMM 형식이어야 합니다.");
        }
        int month = Integer.parseInt(normalized.substring(4));
        if (month < 1 || month > 12) {
            throw new BudgetRuleViolationException("yearMonth의 월은 01부터 12 사이여야 합니다.");
        }
        return normalized;
    }

    private static <T> T applyDomainRule(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BudgetRuleViolationException(exception.getMessage(), exception);
        }
    }

    private static void applyDomainRule(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BudgetRuleViolationException(exception.getMessage(), exception);
        }
    }

    private record LockedPlanPair(BudgetPlan lower, BudgetPlan higher) {

        private BudgetPlan plan(Long id) {
            if (lower.id().equals(id)) {
                return lower;
            }
            if (higher.id().equals(id)) {
                return higher;
            }
            throw new BudgetRuleViolationException("잠근 예산 쌍에 id가 없습니다: " + id);
        }
    }
}
