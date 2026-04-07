package com.ho.account.asset.web;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.dto.LeaseContractRequest;
import com.ho.account.asset.dto.LeaseRemeasurementRequest;
import com.ho.account.asset.repository.LeaseContractRepository; // For fetching LeaseContract
import com.ho.account.asset.service.LeaseAccountingService;
import com.ho.account.asset.service.LeaseService; // For basic contract registration/update
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ifrs16/leases")
public class LeaseAccountingController {

    private final LeaseService leaseService;
    private final LeaseAccountingService leaseAccountingService;
    private final LeaseContractRepository leaseContractRepository; // To get contract details after creation
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;


    public LeaseAccountingController(LeaseService leaseService,
                                     LeaseAccountingService leaseAccountingService,
                                     LeaseContractRepository leaseContractRepository,
                                     AccountSubjectRepository accountSubjectRepository,
                                     DepartmentRepository departmentRepository,
                                     BusinessPartnerRepository businessPartnerRepository) {
        this.leaseService = leaseService;
        this.leaseAccountingService = leaseAccountingService;
        this.leaseContractRepository = leaseContractRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * 새로운 리스 계약을 등록하고 IFRS 16 초기 인식을 수행합니다.
     *
     * @param request 리스 계약 생성 요청 DTO
     * @return 생성된 리스 계약 정보
     */
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

        // 관련 엔티티 매핑
        if (request.getLessorBusinessPartnerCode() != null) {
            BusinessPartner lessor = businessPartnerRepository.findByBusinessPartnerCode(request.getLessorBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException("Lessor Business Partner not found"));
            contract.setLessor(lessor);
        }
        if (request.getDepartmentCode() != null) {
            Department department = departmentRepository.findByCode(request.getDepartmentCode())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            contract.setDepartment(department);
        }
        if (request.getExpenseAccountCode() != null) {
            AccountSubject expenseAccount = accountSubjectRepository.findById(request.getExpenseAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Expense Account Subject not found"));
            contract.setExpenseAccount(expenseAccount);
        }

        // IFRS 16 필드 설정
        contract.setIfrs16Applicable(request.isIfrs16Applicable());
        contract.setShortTermLease(request.isShortTermLease());
        contract.setLowValueLease(request.isLowValueLease());
        contract.setDiscountRate(request.getDiscountRate());
        contract.setInitialRightOfUseAssetValue(request.getInitialRightOfUseAssetValue());
        contract.setInitialLeaseLiabilityValue(request.getInitialLeaseLiabilityValue());

        LeaseContract createdContract = leaseService.registerContract(contract);
        return new ResponseEntity<>(createdContract, HttpStatus.CREATED);
    }

    /**
     * 모든 리스 계약을 조회합니다.
     *
     * @return 모든 리스 계약 리스트
     */
    @GetMapping
    public ResponseEntity<List<LeaseContract>> getAllLeaseContracts() {
        return ResponseEntity.ok(leaseContractRepository.findAll());
    }

    /**
     * 특정 ID의 리스 계약을 조회합니다.
     *
     * @param id 리스 계약 ID
     * @return 리스 계약 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<LeaseContract> getLeaseContractById(@PathVariable Long id) {
        return leaseContractRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 월별 리스 회계 처리를 수동으로 트리거합니다. (배치 작업 또는 스케줄링 대신 테스트용)
     *
     * @param processDate 처리 기준일 (YYYY-MM-DD)
     * @return 처리 결과 메시지
     */
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

    /**
     * 리스 계약의 조건을 변경하고 재측정을 수행합니다.
     *
     * @param request 리스 재측정 요청 DTO
     * @return 재측정된 리스 계약 정보
     */
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
            return ResponseEntity.badRequest().body(null); // Or return specific error DTO
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Or return specific error DTO
        }
    }

    // TODO: 필요하다면 리스부채/사용권자산 조회 API 추가
}
