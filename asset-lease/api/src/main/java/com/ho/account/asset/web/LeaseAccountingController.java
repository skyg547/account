package com.ho.account.asset.web;

import com.ho.account.asset.application.port.in.LeaseUseCase;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.dto.LeaseContractRequest;
import com.ho.account.asset.dto.LeaseRemeasurementRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 리스 회계 컨트롤러 (Inbound Adapter)
 */
@RestController
@RequestMapping("/api/ifrs16/leases")
@RequiredArgsConstructor
public class LeaseAccountingController {

    private final LeaseUseCase leaseUseCase;

    @PostMapping
    public ResponseEntity<LeaseContract> createLeaseContract(
            @RequestHeader("X-User-ID") String actor,
            @Valid @RequestBody LeaseContractRequest request) {
        LeaseContract contract = new LeaseContract();
        contract.setContractNo(request.getContractNo());
        contract.setContractName(request.getContractName());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyPayment(request.getMonthlyPayment());
        contract.setPaymentDay(request.getPaymentDay());
        contract.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        contract.setLessorCode(request.getLessorBusinessPartnerCode());
        contract.setDepartmentCode(request.getDepartmentCode());
        contract.setExpenseAccountCode(request.getExpenseAccountCode());

        contract.setIfrs16Applicable(request.isIfrs16Applicable());
        contract.setShortTermLease(request.isShortTermLease());
        contract.setLowValueLease(request.isLowValueLease());
        contract.setDiscountRate(request.getDiscountRate());
        contract.setInitialRightOfUseAssetValue(request.getInitialRightOfUseAssetValue());
        contract.setInitialLeaseLiabilityValue(request.getInitialLeaseLiabilityValue());

        LeaseContract createdContract = leaseUseCase.registerLeaseContract(contract, actor);
        return new ResponseEntity<>(createdContract, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LeaseContract>> getAllLeaseContracts() {
        return ResponseEntity.ok(leaseUseCase.getAllActiveLeaseContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaseContract> getLeaseContractById(@PathVariable Long id) {
        return leaseUseCase.getLeaseContract(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/process-monthly/{processDate}")
    public ResponseEntity<String> triggerMonthlyProcess(@PathVariable String processDate) {
        try {
            LocalDate date = LocalDate.parse(processDate);
            leaseUseCase.processMonthlyLeaseAccounting(date);
            return ResponseEntity.ok("Monthly lease accounting process triggered for " + processDate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing monthly lease accounting: " + e.getMessage());
        }
    }

    @PostMapping("/remeasure")
    public ResponseEntity<LeaseContract> remeasureLease(
            @RequestHeader("X-User-ID") String actor,
            @Valid @RequestBody LeaseRemeasurementRequest request) {
        try {
            LeaseContract updatedContract = leaseUseCase.remeasureLease(
                    request.getContractId(),
                    request.getRemeasurementDate(),
                    request.getNewMonthlyPayment(),
                    request.getNewEndDate(),
                    request.getNewDiscountRate(),
                    actor
            );
            return ResponseEntity.ok(updatedContract);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
