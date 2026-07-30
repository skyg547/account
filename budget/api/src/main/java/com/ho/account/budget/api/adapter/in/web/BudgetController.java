package com.ho.account.budget.api.adapter.in.web;

import com.ho.account.budget.api.dto.BudgetExecutionResponse;
import com.ho.account.budget.api.dto.BudgetPlanResponse;
import com.ho.account.budget.api.dto.BudgetTransferResponse;
import com.ho.account.budget.api.dto.CreateBudgetPlanRequest;
import com.ho.account.budget.api.dto.ExecuteBudgetRequest;
import com.ho.account.budget.api.dto.RequestBudgetTransferRequest;
import com.ho.account.budget.application.exception.BudgetResourceNotFoundException;
import com.ho.account.budget.application.port.in.BudgetManagementUseCase;
import com.ho.account.budget.application.port.in.CreateBudgetPlanCommand;
import com.ho.account.budget.application.port.in.ExecuteBudgetCommand;
import com.ho.account.budget.application.port.in.RequestBudgetTransferCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP inbound adapter for budget management.
 *
 * <p>This class validates transport input, maps it to technology-neutral
 * commands, and converts domain results to response DTOs. Approval, transfer,
 * execution, and cancellation rules deliberately remain in the use case.</p>
 */
@Validated
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetManagementUseCase budgetManagementUseCase;

    public BudgetController(BudgetManagementUseCase budgetManagementUseCase) {
        this.budgetManagementUseCase = budgetManagementUseCase;
    }

    @PostMapping("/plans")
    public ResponseEntity<BudgetPlanResponse> createPlan(
            @Valid @RequestBody CreateBudgetPlanRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CreateBudgetPlanCommand command = new CreateBudgetPlanCommand(
                request.planCode(),
                request.yearMonth(),
                request.departmentCode(),
                request.accountCode(),
                request.allocationAmount(),
                jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BudgetPlanResponse.from(budgetManagementUseCase.createPlan(command)));
    }

    @PostMapping("/plans/{planId}/approval")
    public BudgetPlanResponse approvePlan(
            @PathVariable @Positive Long planId,
            @AuthenticationPrincipal Jwt jwt) {
        return BudgetPlanResponse.from(
                budgetManagementUseCase.approvePlan(planId, jwt.getSubject()));
    }

    @GetMapping("/plans/available")
    public ResponseEntity<BudgetPlanResponse> findAvailablePlan(
            @RequestParam
            @NotBlank
            @Pattern(
                    regexp = "\\d{4}(0[1-9]|1[0-2])",
                    message = "yearMonth must use YYYYMM with a valid month")
            String yearMonth,
            @RequestParam @NotBlank String departmentCode,
            @RequestParam @NotBlank String accountCode) {
        return ResponseEntity.ok(BudgetPlanResponse.from(
                budgetManagementUseCase.findPlan(yearMonth, departmentCode, accountCode)
                        .orElseThrow(() -> new BudgetResourceNotFoundException(
                                "예산을 찾을 수 없습니다: "
                                        + yearMonth
                                        + "/"
                                        + departmentCode
                                        + "/"
                                        + accountCode))));
    }

    @PostMapping("/transfers")
    public ResponseEntity<BudgetTransferResponse> requestTransfer(
            @Valid @RequestBody RequestBudgetTransferRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        RequestBudgetTransferCommand command = new RequestBudgetTransferCommand(
                request.requestKey(),
                request.fromPlanId(),
                request.toPlanId(),
                request.amount(),
                jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BudgetTransferResponse.from(
                        budgetManagementUseCase.requestTransfer(command)));
    }

    @PostMapping("/transfers/{transferId}/approval")
    public BudgetTransferResponse approveTransfer(
            @PathVariable @Positive Long transferId,
            @AuthenticationPrincipal Jwt jwt) {
        return BudgetTransferResponse.from(
                budgetManagementUseCase.approveTransfer(transferId, jwt.getSubject()));
    }

    @PostMapping("/executions")
    public ResponseEntity<BudgetExecutionResponse> execute(
            @Valid @RequestBody ExecuteBudgetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ExecuteBudgetCommand command = new ExecuteBudgetCommand(
                request.planId(),
                request.sourceType(),
                request.sourceId(),
                request.sourceLineId(),
                request.executionDate(),
                request.amount(),
                jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BudgetExecutionResponse.from(budgetManagementUseCase.execute(command)));
    }

    @PostMapping("/executions/{executionId}/cancellation")
    public BudgetExecutionResponse cancelExecution(
            @PathVariable @Positive Long executionId,
            @AuthenticationPrincipal Jwt jwt) {
        return BudgetExecutionResponse.from(
                budgetManagementUseCase.cancelExecution(executionId, jwt.getSubject()));
    }
}
