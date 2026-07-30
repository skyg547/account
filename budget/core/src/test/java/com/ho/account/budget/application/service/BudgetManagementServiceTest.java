package com.ho.account.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.budget.application.exception.BudgetConflictException;
import com.ho.account.budget.application.exception.BudgetResourceNotFoundException;
import com.ho.account.budget.application.exception.BudgetRuleViolationException;
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
import com.ho.account.budget.domain.BudgetPlanStatus;
import com.ho.account.budget.domain.BudgetTransfer;
import com.ho.account.budget.domain.BudgetTransferStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class BudgetManagementServiceTest {

    private BudgetPlanPersistencePort planPort;
    private BudgetTransferPersistencePort transferPort;
    private BudgetExecutionPersistencePort executionPort;
    private BudgetFiscalYearControlPersistencePort fiscalPort;
    private BudgetIdempotencyLockPort idempotencyPort;
    private BudgetManagementService service;

    @BeforeEach
    void setUp() {
        planPort = mock(BudgetPlanPersistencePort.class);
        transferPort = mock(BudgetTransferPersistencePort.class);
        executionPort = mock(BudgetExecutionPersistencePort.class);
        fiscalPort = mock(BudgetFiscalYearControlPersistencePort.class);
        idempotencyPort = mock(BudgetIdempotencyLockPort.class);
        service = new BudgetManagementService(
                planPort, transferPort, executionPort, fiscalPort, idempotencyPort);
    }

    @Test
    void malformedCreateYearMonthIsRejectedBeforeTheServiceCanTakeFiscalLocks() {
        assertThatThrownBy(() -> new CreateBudgetPlanCommand(
                        "PLAN-1", "1", "D001", "A100", new BigDecimal("100.00"), "maker"))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("YYYYMM");

        assertThatThrownBy(() -> new CreateBudgetPlanCommand(
                        "PLAN-1", "202613", "D001", "A100", new BigDecimal("100.00"), "maker"))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("01부터 12");

        verifyNoInteractions(fiscalPort);
    }

    @Test
    void transferRequestUsesShardFiscalAndAscendingPlanLockOrder() {
        BudgetPlan target = approvedPlan(10L, "TARGET", "202601", "100.00");
        BudgetPlan source = approvedPlan(20L, "SOURCE", "202601", "100.00");
        RequestBudgetTransferCommand command =
                new RequestBudgetTransferCommand("REQ-1", 20L, 10L, new BigDecimal("25.00"), "maker");
        when(transferPort.findByRequestKey("REQ-1")).thenReturn(Optional.empty());
        when(planPort.findById(20L)).thenReturn(Optional.of(source));
        when(planPort.findById(10L)).thenReturn(Optional.of(target));
        stubOpenFiscalYear("2026");
        when(planPort.findByIdForUpdate(10L)).thenReturn(Optional.of(target));
        when(planPort.findByIdForUpdate(20L)).thenReturn(Optional.of(source));
        when(transferPort.save(any(BudgetTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetTransfer result = service.requestTransfer(command);

        InOrder order = inOrder(idempotencyPort, transferPort, fiscalPort, planPort);
        order.verify(idempotencyPort).lockTransferRequestKey("REQ-1");
        order.verify(transferPort).findByRequestKey("REQ-1");
        order.verify(fiscalPort).findByFiscalYearForUpdate("2026");
        order.verify(planPort).findByIdForUpdate(10L);
        order.verify(planPort).findByIdForUpdate(20L);
        assertThat(result.sourcePlanId()).isEqualTo(20L);
        assertThat(result.targetPlanId()).isEqualTo(10L);
    }

    @Test
    void crossMonthAndCrossYearTransfersAreRejectedWithoutMutationOrDurableWrites() {
        for (String targetMonth : List.of("202602", "202701")) {
            BudgetPlan source = approvedPlan(10L, "SOURCE", "202601", "100.00");
            BudgetPlan target = approvedPlan(20L, "TARGET-" + targetMonth, targetMonth, "100.00");
            String key = "REQ-" + targetMonth;
            when(transferPort.findByRequestKey(key)).thenReturn(Optional.empty());
            when(planPort.findById(10L)).thenReturn(Optional.of(source));
            when(planPort.findById(20L)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.requestTransfer(new RequestBudgetTransferCommand(
                            key, 10L, 20L, new BigDecimal("25.00"), "maker")))
                    .isInstanceOf(BudgetRuleViolationException.class)
                    .hasMessageContaining("같은 YYYYMM");
            assertThat(source.transferOutAmount()).isEqualByComparingTo("0.00");
            assertThat(target.transferInAmount()).isEqualByComparingTo("0.00");
        }
        verifyNoInteractions(fiscalPort);
        verify(planPort, never()).findByIdForUpdate(any());
        verify(planPort, never()).save(any());
        verify(transferPort, never()).save(any());
    }

    @Test
    void executionOutsidePlanMonthIsRejectedBeforeFiscalOrPlanLocks() {
        BudgetPlan plan = approvedPlan(10L, "PLAN", "202601", "100.00");
        when(executionPort.findBySource("AP", "INV-1", "LINE-1")).thenReturn(Optional.empty());
        when(planPort.findById(10L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.execute(new ExecuteBudgetCommand(
                        10L,
                        "AP",
                        "INV-1",
                        "LINE-1",
                        LocalDate.of(2026, 2, 1),
                        new BigDecimal("10.00"),
                        "executor")))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("YYYYMM과 일치");
        assertThat(plan.executedAmount()).isEqualByComparingTo("0.00");
        verifyNoInteractions(fiscalPort);
        verify(planPort, never()).findByIdForUpdate(any());
        verify(planPort, never()).save(any());
        verify(executionPort, never()).save(any());
    }

    @Test
    void closedFiscalYearBlocksCreateApproveTransferApprovalAndExecution() {
        BudgetFiscalYearControl closed =
                BudgetFiscalYearControl.restore("2026", BudgetFiscalYearStatus.CLOSED, "closer");
        when(fiscalPort.findByFiscalYearForUpdate("2026")).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.createPlan(new CreateBudgetPlanCommand(
                        "NEW", "202601", "D001", "A100", new BigDecimal("100.00"), "maker")))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("마감");

        BudgetPlan draft = draftPlan(10L, "DRAFT", "202601");
        when(planPort.findById(10L)).thenReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.approvePlan(10L, "checker"))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("마감");

        BudgetPlan source = approvedPlan(20L, "SOURCE", "202601", "100.00");
        BudgetPlan target = approvedPlan(30L, "TARGET", "202601", "100.00");
        BudgetTransfer transfer = BudgetTransfer.restore(
                101L,
                "REQ-1",
                20L,
                30L,
                new BigDecimal("10.00"),
                BudgetTransferStatus.REQUESTED,
                "maker",
                null);
        when(transferPort.findById(101L)).thenReturn(Optional.of(transfer));
        when(planPort.findById(20L)).thenReturn(Optional.of(source));
        when(planPort.findById(30L)).thenReturn(Optional.of(target));
        when(transferPort.findByIdForUpdate(101L)).thenReturn(Optional.of(transfer));
        assertThatThrownBy(() -> service.approveTransfer(101L, "checker"))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("마감");

        when(executionPort.findBySource("AP", "INV-1", "LINE-1")).thenReturn(Optional.empty());
        when(planPort.findById(20L)).thenReturn(Optional.of(source));
        assertThatThrownBy(() -> service.execute(new ExecuteBudgetCommand(
                        20L,
                        "AP",
                        "INV-1",
                        "LINE-1",
                        LocalDate.of(2026, 1, 31),
                        new BigDecimal("5.00"),
                        "executor")))
                .isInstanceOf(BudgetRuleViolationException.class)
                .hasMessageContaining("마감");

        assertThat(draft.status()).isEqualTo(BudgetPlanStatus.DRAFT);
        assertThat(source.transferOutAmount()).isEqualByComparingTo("0.00");
        assertThat(target.transferInAmount()).isEqualByComparingTo("0.00");
        assertThat(source.executedAmount()).isEqualByComparingTo("0.00");
        verify(planPort, never()).save(any());
        verify(transferPort, never()).save(any());
        verify(executionPort, never()).save(any());
    }

    @Test
    void approvalAndCloseBothAcquireFiscalControlBeforePlanWriteLocks() {
        BudgetPlan draft = draftPlan(10L, "PLAN", "202601");
        when(planPort.findById(10L)).thenReturn(Optional.of(draft));
        when(planPort.findByIdForUpdate(10L)).thenReturn(Optional.of(draft));
        stubOpenFiscalYear("2026");
        when(planPort.save(draft)).thenReturn(draft);

        service.approvePlan(10L, "checker");

        InOrder approvalOrder = inOrder(fiscalPort, planPort);
        approvalOrder.verify(planPort).findById(10L);
        approvalOrder.verify(fiscalPort).findByFiscalYearForUpdate("2026");
        approvalOrder.verify(planPort).findByIdForUpdate(10L);

        BudgetPlan approved = approvedPlan(20L, "APPROVED", "202612", "100.00");
        when(fiscalPort.findByFiscalYearForUpdate("2027"))
                .thenReturn(Optional.of(BudgetFiscalYearControl.open("2027")));
        when(planPort.findApprovedByFiscalYearForUpdateOrderById("2027"))
                .thenReturn(List.of());
        service.closeFiscalYear("2027", "closer");
        InOrder closeOrder = inOrder(fiscalPort, planPort);
        closeOrder.verify(fiscalPort).findByFiscalYearForUpdate("2027");
        closeOrder.verify(planPort).findApprovedByFiscalYearForUpdateOrderById("2027");
        assertThat(approved.status()).isEqualTo(BudgetPlanStatus.APPROVED);
    }

    @Test
    void identicalRetriesReturnPersistedResultsAfterTakingIdempotencyShard() {
        BudgetTransfer transfer = BudgetTransfer.restore(
                101L,
                "REQ-1",
                10L,
                20L,
                new BigDecimal("25.00"),
                BudgetTransferStatus.REQUESTED,
                "maker",
                null);
        when(transferPort.findByRequestKey("REQ-1")).thenReturn(Optional.of(transfer));
        assertThat(service.requestTransfer(new RequestBudgetTransferCommand(
                        "REQ-1", 10L, 20L, new BigDecimal("25.0"), "maker")))
                .isSameAs(transfer);
        verify(idempotencyPort).lockTransferRequestKey("REQ-1");

        LocalDate date = LocalDate.of(2026, 1, 31);
        BudgetExecution execution = BudgetExecution.restore(
                301L,
                10L,
                "AP",
                "INV-1",
                "LINE-1",
                date,
                new BigDecimal("15.00"),
                BudgetExecutionStatus.EXECUTED,
                "executor",
                null);
        when(executionPort.findBySource("AP", "INV-1", "LINE-1"))
                .thenReturn(Optional.of(execution));
        assertThat(service.execute(new ExecuteBudgetCommand(
                        10L, "AP", "INV-1", "LINE-1", date, new BigDecimal("15.0"), "executor")))
                .isSameAs(execution);
        verify(idempotencyPort).lockExecutionSourceKey("AP", "INV-1", "LINE-1");
        verifyNoInteractions(fiscalPort);
        verify(planPort, never()).save(any());
    }

    @Test
    void conflictingIdempotencyPayloadsUseTypedConflictAndDoNotMutatePlans() {
        BudgetTransfer transfer = BudgetTransfer.restore(
                101L,
                "REQ-1",
                10L,
                20L,
                new BigDecimal("25.00"),
                BudgetTransferStatus.REQUESTED,
                "maker",
                null);
        when(transferPort.findByRequestKey("REQ-1")).thenReturn(Optional.of(transfer));
        assertThatThrownBy(() -> service.requestTransfer(new RequestBudgetTransferCommand(
                        "REQ-1", 10L, 20L, new BigDecimal("25.01"), "maker")))
                .isInstanceOf(BudgetConflictException.class);

        LocalDate date = LocalDate.of(2026, 1, 31);
        BudgetExecution execution = BudgetExecution.restore(
                301L,
                10L,
                "AP",
                "INV-1",
                "LINE-1",
                date,
                new BigDecimal("15.00"),
                BudgetExecutionStatus.EXECUTED,
                "executor",
                null);
        when(executionPort.findBySource("AP", "INV-1", "LINE-1"))
                .thenReturn(Optional.of(execution));
        assertThatThrownBy(() -> service.execute(new ExecuteBudgetCommand(
                        20L, "AP", "INV-1", "LINE-1", date, new BigDecimal("15.00"), "executor")))
                .isInstanceOf(BudgetConflictException.class);
        verifyNoInteractions(fiscalPort);
        verify(planPort, never()).save(any());
    }

    @Test
    void missingResourcesUseTypedNotFoundException() {
        when(planPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approvePlan(99L, "checker"))
                .isInstanceOf(BudgetResourceNotFoundException.class)
                .hasMessageContaining("예산을 찾을 수 없습니다");
        verifyNoInteractions(fiscalPort);
    }

    @Test
    void yearEndClosesControlAndPlansInIdOrderAndReturnsCount() {
        BudgetPlan id30 = approvedPlan(30L, "P30", "202612", "100.00");
        BudgetPlan id10 = approvedPlan(10L, "P10", "202601", "100.00");
        BudgetPlan id20 = approvedPlan(20L, "P20", "202606", "100.00");
        BudgetFiscalYearControl control = stubOpenFiscalYear("2026");
        when(planPort.findApprovedByFiscalYearForUpdateOrderById("2026"))
                .thenReturn(List.of(id30, id10, id20));
        when(fiscalPort.save(control)).thenReturn(control);

        assertThat(service.closeFiscalYear("2026", "closer")).isEqualTo(3);

        ArgumentCaptor<BudgetPlan> saved = ArgumentCaptor.forClass(BudgetPlan.class);
        verify(planPort, org.mockito.Mockito.times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(BudgetPlan::id).containsExactly(10L, 20L, 30L);
        assertThat(control.status()).isEqualTo(BudgetFiscalYearStatus.CLOSED);
        assertThat(control.closedBy()).isEqualTo("closer");
    }

    private BudgetFiscalYearControl stubOpenFiscalYear(String year) {
        BudgetFiscalYearControl control = BudgetFiscalYearControl.open(year);
        when(fiscalPort.findByFiscalYearForUpdate(year)).thenReturn(Optional.of(control));
        return control;
    }

    private static BudgetPlan draftPlan(Long id, String planCode, String yearMonth) {
        return BudgetPlan.restore(
                id,
                planCode,
                yearMonth,
                "D001",
                "A100",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.DRAFT,
                "maker",
                null,
                null);
    }

    private static BudgetPlan approvedPlan(Long id, String planCode, String yearMonth, String allocated) {
        return BudgetPlan.restore(
                id,
                planCode,
                yearMonth,
                "D001",
                "A100",
                new BigDecimal(allocated),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.APPROVED,
                "maker",
                "checker",
                null);
    }
}
