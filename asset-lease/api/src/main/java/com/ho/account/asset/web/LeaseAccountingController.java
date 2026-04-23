package com.ho.account.asset.web;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.dto.LeaseContractRequest;
import com.ho.account.asset.dto.LeaseRemeasurementRequest;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.service.LeaseAccountingService;
import com.ho.account.asset.service.LeaseService;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 리스 회계 컨트롤러
 */
@RestController
@RequestMapping("/api/ifrs16/leases")
public class LeaseAccountingController {

    private final LeaseService leaseService;
    private final LeaseAccountingService leaseAccountingService;
    private final LeaseContractRepository leaseContractRepository;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public LeaseAccountingController(LeaseService leaseService,
                                     LeaseAccountingService leaseAccountingService,
                                     LeaseContractRepository leaseContractRepository,
                                     AccountSubjectPersistencePort accountSubjectPersistencePort,
                                     DepartmentPersistencePort departmentPersistencePort,
                                     BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.leaseService = leaseService;
        this.leaseAccountingService = leaseAccountingService;
        this.leaseContractRepository = leaseContractRepository;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    @PostMapping
    public ResponseEntity<LeaseContract> createLeaseContract(@Valid @RequestBody LeaseContractRequest request) {
        LeaseContract contract = new LeaseContract();
        contract.setContractNo(request.getContractNo());
        contract.setContractName(request.getContractName());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyPayment(request.getMonthlyPayment());
        contract.setPaymentDay(request.getPaymentDay());
        contract.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        if (request.getLessorBusinessPartnerCode() != null) {
            contract.setLessor(businessPartnerPersistencePort.findByBusinessPartnerCode(request.getLessorBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException("Lessor Business Partner not found")));
        }
        if (request.getDepartmentCode() != null) {
            contract.setDepartment(departmentPersistencePort.findByCode(request.getDepartmentCode())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found")));
        }
        if (request.getExpenseAccountCode() != null) {
            contract.setExpenseAccount(accountSubjectPersistencePort.findByCode(request.getExpenseAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Expense Account Subject not found")));
        }

        contract.setIfrs16Applicable(request.isIfrs16Applicable());
        contract.setShortTermLease(request.isShortTermLease());
        contract.setLowValueLease(request.isLowValueLease());
        contract.setDiscountRate(request.getDiscountRate());
        contract.setInitialRightOfUseAssetValue(request.getInitialRightOfUseAssetValue());
        contract.setInitialLeaseLiabilityValue(request.getInitialLeaseLiabilityValue());

        LeaseContract createdContract = leaseService.registerContract(contract);
        return new ResponseEntity<>(createdContract, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LeaseContract>> getAllLeaseContracts() {
        return ResponseEntity.ok(leaseContractRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaseContract> getLeaseContractById(@PathVariable Long id) {
        return leaseContractRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/process-monthly/{processDate}")
    public ResponseEntity<String> triggerMonthlyProcess(@PathVariable String processDate) {
        try {
            LocalDate date = LocalDate.parse(processDate);
            leaseAccountingService.processMonthlyLeaseAccounting(date);
            return ResponseEntity.ok("Monthly lease accounting process triggered for " + processDate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing monthly lease accounting: " + e.getMessage());
        }
    }

    @PostMapping("/remeasure")
    public ResponseEntity<LeaseContract> remeasureLease(@Valid @RequestBody LeaseRemeasurementRequest request) {
        try {
            LeaseContract updatedContract = leaseAccountingService.remeasureLease(
                    request.getContractId(),
                    request.getRemeasurementDate(),
                    request.getNewMonthlyPayment(),
                    request.getNewEndDate(),
                    request.getNewDiscountRate()
            );
            return ResponseEntity.ok(updatedContract);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
