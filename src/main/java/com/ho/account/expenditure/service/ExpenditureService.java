package com.ho.account.expenditure.service;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.service.FixedAssetService;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Customer;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.CustomerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ExpenditureService {

    private final ExpenditureResolutionRepository expenditureRepository;
    private final JournalService journalService;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final CustomerRepository customerRepository;
    private final BudgetService budgetService;
    private final FixedAssetService fixedAssetService;
    private final LeaseContractRepository leaseContractRepository;

    @Autowired
    public ExpenditureService(ExpenditureResolutionRepository expenditureRepository,
                              JournalService journalService,
                              AccountSubjectRepository accountSubjectRepository,
                              DepartmentRepository departmentRepository,
                              CustomerRepository customerRepository,
                              BudgetService budgetService,
                              FixedAssetService fixedAssetService,
                              LeaseContractRepository leaseContractRepository) {
        this.expenditureRepository = expenditureRepository;
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.customerRepository = customerRepository;
        this.budgetService = budgetService;
        this.fixedAssetService = fixedAssetService;
        this.leaseContractRepository = leaseContractRepository;
    }

    // 결의서 생성
    public ExpenditureResolution createResolution(ExpenditureResolution resolution) {
        validateResolution(resolution);

        String yearMonth = resolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (ExpenditureDetail detail : resolution.getDetails()) {
            budgetService.useBudget(yearMonth, resolution.getDepartment(), detail.getAccountSubject(), detail.getAmount());
        }

        String resolutionNo = generateResolutionNo(resolution.getResolutionDate());
        resolution.setResolutionNo(resolutionNo);
        resolution.setStatus("DRAFT");
        resolution.calculateTotalAmount();

        for (ExpenditureDetail detail : resolution.getDetails()) {
            detail.setExpenditureResolution(resolution);
        }

        return expenditureRepository.save(resolution);
    }

    // 승인 요청
    public void requestApproval(Long id) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));
        
        if (!"DRAFT".equals(resolution.getStatus()) && !"REJECTED".equals(resolution.getStatus())) {
            throw new IllegalStateException("작성중이거나 반려된 결의서만 승인 요청할 수 있습니다.");
        }
        
        resolution.setStatus("REQUESTED");
        expenditureRepository.save(resolution);
    }

    // 결의서 승인 및 전표/자산/리스 자동 처리
    public void approveResolution(Long id) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        if (!"REQUESTED".equals(resolution.getStatus())) {
            throw new IllegalStateException("승인 요청된 결의서만 승인할 수 있습니다.");
        }

        // 1. 전표 생성 및 승인
        JournalEntry journalEntry = createJournalFromResolution(resolution);
        JournalEntry savedEntry = journalService.createJournalEntry(journalEntry);
        journalService.requestApproval(savedEntry.getId());
        journalService.approveJournalEntry(savedEntry.getId());

        // 2. 고정자산 자동 등록 체크
        for (ExpenditureDetail detail : resolution.getDetails()) {
            if (Boolean.TRUE.equals(detail.getAccountSubject().getIsFixedAsset())) {
                createFixedAssetFromExpenditure(detail, resolution);
            }
        }

        // 3. 리스 계약 연동 처리 (보증금 납부 등으로 간주하여 계약 활성화)
        if (resolution.getLeaseContract() != null) {
            LeaseContract contract = resolution.getLeaseContract();
            // 계약 상태가 아직 활성화되지 않았다면 활성화
            if (!"ACTIVE".equals(contract.getStatus())) {
                contract.setStatus("ACTIVE");
                leaseContractRepository.save(contract);
            }
        }

        resolution.setStatus("APPROVED");
        resolution.setJournalEntry(savedEntry);
        resolution.setRejectionReason(null);
        expenditureRepository.save(resolution);
    }

    // 고정자산 자동 등록 로직
    private void createFixedAssetFromExpenditure(ExpenditureDetail detail, ExpenditureResolution resolution) {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode("FA-" + resolution.getResolutionNo() + "-" + detail.getId()); 
        asset.setAssetName(detail.getDescription() != null ? detail.getDescription() : detail.getAccountSubject().getAccountName());
        asset.setAccountSubject(detail.getAccountSubject());
        asset.setAcquisitionDate(resolution.getPaymentDate());
        asset.setAcquisitionCost(detail.getAmount());
        asset.setDepartment(resolution.getDepartment());
        asset.setUsefulLife(5);
        asset.setDepreciationMethod("STRAIGHT_LINE");
        asset.setStatus("REGISTERED");

        fixedAssetService.registerAsset(asset);
    }

    // 결의서 반려
    public void rejectResolution(Long id, String reason) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        if (!"REQUESTED".equals(resolution.getStatus())) {
            throw new IllegalStateException("승인 요청된 결의서만 반려할 수 있습니다.");
        }

        resolution.setStatus("REJECTED");
        resolution.setRejectionReason(reason);
        expenditureRepository.save(resolution);
    }

    // 전표 변환 로직
    private JournalEntry createJournalFromResolution(ExpenditureResolution resolution) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(resolution.getResolutionDate());
        entry.setAccountingDate(resolution.getPaymentDate());
        entry.setDescription("지출결의: " + resolution.getTitle());
        
        for (ExpenditureDetail detail : resolution.getDetails()) {
            JournalDetail journalDetail = new JournalDetail();
            journalDetail.setDrcrType("DEBIT");
            journalDetail.setAccountSubject(detail.getAccountSubject());
            journalDetail.setAmount(detail.getAmount());
            journalDetail.setDepartment(resolution.getDepartment());
            journalDetail.setCustomer(detail.getCustomer());
            journalDetail.setDetailDescription(detail.getDescription());
            entry.addDetail(journalDetail);
        }

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(resolution.getPaymentAccount());
        creditDetail.setAmount(resolution.getTotalAmount());
        creditDetail.setDepartment(resolution.getDepartment());
        creditDetail.setDetailDescription("지출결의 지급");
        entry.addDetail(creditDetail);

        return entry;
    }

    // 유효성 검증
    private void validateResolution(ExpenditureResolution resolution) {
        if (resolution.getDepartment() != null && resolution.getDepartment().getDeptCode() != null) {
            Department dept = departmentRepository.findByDeptCode(resolution.getDepartment().getDeptCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다."));
            resolution.setDepartment(dept);
        }

        if (resolution.getPaymentAccount() != null && resolution.getPaymentAccount().getAccountCode() != null) {
            AccountSubject account = accountSubjectRepository.findById(resolution.getPaymentAccount().getAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지급 계정입니다."));
            resolution.setPaymentAccount(account);
        }

        // 리스 계약 검증 추가
        if (resolution.getLeaseContract() != null && resolution.getLeaseContract().getId() != null) {
            LeaseContract contract = leaseContractRepository.findById(resolution.getLeaseContract().getId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리스 계약입니다."));
            resolution.setLeaseContract(contract);
        }

        for (ExpenditureDetail detail : resolution.getDetails()) {
            if (detail.getAccountSubject() != null && detail.getAccountSubject().getAccountCode() != null) {
                AccountSubject account = accountSubjectRepository.findById(detail.getAccountSubject().getAccountCode())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 비용 계정입니다."));
                detail.setAccountSubject(account);
            }
            if (detail.getCustomer() != null && detail.getCustomer().getCustomerCode() != null) {
                Customer customer = customerRepository.findByCustomerCode(detail.getCustomer().getCustomerCode())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래처입니다."));
                detail.setCustomer(customer);
            }
        }
    }

    private String generateResolutionNo(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<ExpenditureResolution> list = expenditureRepository.findByResolutionDate(date);
        int seq = list.size() + 1;
        return String.format("REQ-%s-%03d", dateStr, seq);
    }
    
    @Transactional(readOnly = true)
    public List<ExpenditureResolution> getResolutionsByDate(LocalDate startDate, LocalDate endDate) {
        return expenditureRepository.findByResolutionDateBetween(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public ExpenditureResolution getResolution(Long id) {
        return expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다."));
    }
}
