package com.ho.account.expenditure.service;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.service.FixedAssetService;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final BusinessPartnerRepository businessPartnerRepository;
    private final BudgetService budgetService;
    private final FixedAssetService fixedAssetService;
    private final LeaseContractRepository leaseContractRepository;

    private final TaxInvoiceRepository taxInvoiceRepository;

    @Autowired
    public ExpenditureService(ExpenditureResolutionRepository expenditureRepository,
            JournalService journalService,
            AccountSubjectRepository accountSubjectRepository,
            DepartmentRepository departmentRepository,
            BusinessPartnerRepository businessPartnerRepository,
            BudgetService budgetService,
            FixedAssetService fixedAssetService,
            LeaseContractRepository leaseContractRepository,
            TaxInvoiceRepository taxInvoiceRepository) {
        this.expenditureRepository = expenditureRepository;
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.budgetService = budgetService;
        this.fixedAssetService = fixedAssetService;
        this.leaseContractRepository = leaseContractRepository;
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    /**
     * 새로운 지출결의서를 생성합니다.
     * 
     * @param requestDto 생성할 지출결의서 정보가 담긴 DTO
     * @return 저장된 ExpenditureResolution 엔티티
     */
    public ExpenditureResolution createResolution(ExpenditureResolutionRequestDto requestDto) {
        ExpenditureResolution resolution = new ExpenditureResolution();
        resolution.setTitle(requestDto.getTitle());
        resolution.setResolutionDate(requestDto.getResolutionDate());
        resolution.setPaymentDate(requestDto.getPaymentDate());

        // Validate and set Department
        Department department = departmentRepository.findByCode(requestDto.getDepartmentCode())
                .orElseThrow(
                        () -> new IllegalArgumentException("존재하지 않는 부서입니다. 코드: " + requestDto.getDepartmentCode()));
        resolution.setDepartment(department);

        // Validate and set Payment Account
        AccountSubject paymentAccount = accountSubjectRepository.findById(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지급 계정입니다. 코드: " + requestDto.getPaymentAccountCode()));
        resolution.setPaymentAccount(paymentAccount);

        // Set Tax Invoice if provided
        if (requestDto.getTaxInvoiceId() != null) {
            TaxInvoice taxInvoice = taxInvoiceRepository.findById(requestDto.getTaxInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "세금계산서를 찾을 수 없습니다. ID: " + requestDto.getTaxInvoiceId()));
            if (!"PURCHASE".equals(taxInvoice.getType())) {
                throw new IllegalArgumentException("지출결의서에 연결할 세금계산서는 'PURCHASE' 타입이어야 합니다.");
            }
            resolution.setTaxInvoice(taxInvoice);
        }

        // Add Expenditure Details
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = new ExpenditureDetail();
            detail.setDescription(detailDto.getDescription());
            detail.setAmount(detailDto.getAmount());

            // Validate and set Account Subject for detail
            AccountSubject detailAccount = accountSubjectRepository.findById(detailDto.getAccountSubjectCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 비용 계정입니다. 코드: " + detailDto.getAccountSubjectCode()));
            detail.setAccountSubject(detailAccount);

            // Validate and set Business Partner for detail
            BusinessPartner businessPartner = businessPartnerRepository
                    .findByBusinessPartnerCode(detailDto.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 거래처입니다. 코드: " + detailDto.getBusinessPartnerCode()));
            detail.setBusinessPartner(businessPartner);

            resolution.addDetail(detail);
            totalAmount = totalAmount.add(detail.getAmount());
        }
        resolution.setTotalAmount(totalAmount); // Ensure total amount is calculated

        return createResolution(resolution);
    }

    /**
     * 지출결의서 생성 (Entity 직접 입력)
     * LeaseService 등 내부 서비스에서 호출
     */
    public ExpenditureResolution createResolution(ExpenditureResolution resolution) {
        // Budget check
        String yearMonth = resolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (ExpenditureDetail detail : resolution.getDetails()) {
            budgetService.useBudget(yearMonth, resolution.getDepartment(), detail.getAccountSubject(),
                    detail.getAmount());
        }

        String resolutionNo = generateResolutionNo(resolution.getResolutionDate());
        resolution.setResolutionNo(resolutionNo);
        resolution.setStatus(ExpenditureResolutionStatus.DRAFT); // Initial status

        return expenditureRepository.save(resolution);
    }

    /**
     * 기존 지출결의서를 수정합니다.
     * 
     * @param id         수정할 지출결의서 ID
     * @param requestDto 수정할 지출결의서 정보가 담긴 DTO
     * @return 수정된 ExpenditureResolution 엔티티
     */
    public ExpenditureResolution updateResolution(Long id, ExpenditureResolutionRequestDto requestDto) {
        ExpenditureResolution existingResolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지출결의서를 찾을 수 없습니다. ID: " + id));

        // Only allow update if status is DRAFT or REJECTED
        if (existingResolution.getStatus() != ExpenditureResolutionStatus.DRAFT
                && existingResolution.getStatus() != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("작성중이거나 반려된 결의서만 수정할 수 있습니다.");
        }

        existingResolution.setTitle(requestDto.getTitle());
        existingResolution.setResolutionDate(requestDto.getResolutionDate());
        existingResolution.setPaymentDate(requestDto.getPaymentDate());

        // Validate and set Department
        Department department = departmentRepository.findByCode(requestDto.getDepartmentCode())
                .orElseThrow(
                        () -> new IllegalArgumentException("존재하지 않는 부서입니다. 코드: " + requestDto.getDepartmentCode()));
        existingResolution.setDepartment(department);

        // Validate and set Payment Account
        AccountSubject paymentAccount = accountSubjectRepository.findById(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지급 계정입니다. 코드: " + requestDto.getPaymentAccountCode()));
        existingResolution.setPaymentAccount(paymentAccount);

        // Set Tax Invoice if provided
        if (requestDto.getTaxInvoiceId() != null) {
            TaxInvoice taxInvoice = taxInvoiceRepository.findById(requestDto.getTaxInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "세금계산서를 찾을 수 없습니다. ID: " + requestDto.getTaxInvoiceId()));
            if (!"PURCHASE".equals(taxInvoice.getType())) {
                throw new IllegalArgumentException("지출결의서에 연결할 세금계산서는 'PURCHASE' 타입이어야 합니다.");
            }
            existingResolution.setTaxInvoice(taxInvoice);
        } else {
            existingResolution.setTaxInvoice(null); // Clear if not provided
        }

        // 지출 상세 업데이트(단순화를 위해 기존 항목 교체)
        existingResolution.getDetails().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = new ExpenditureDetail();
            detail.setDescription(detailDto.getDescription());
            detail.setAmount(detailDto.getAmount());

            // Validate and set Account Subject for detail
            AccountSubject detailAccount = accountSubjectRepository.findById(detailDto.getAccountSubjectCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 비용 계정입니다. 코드: " + detailDto.getAccountSubjectCode()));
            detail.setAccountSubject(detailAccount);

            // Validate and set Business Partner for detail
            BusinessPartner businessPartner = businessPartnerRepository
                    .findByBusinessPartnerCode(detailDto.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 거래처입니다. 코드: " + detailDto.getBusinessPartnerCode()));
            detail.setBusinessPartner(businessPartner);

            existingResolution.addDetail(detail); // addDetail handles setting parent resolution and calculating total
            totalAmount = totalAmount.add(detail.getAmount());
        }
        existingResolution.setTotalAmount(totalAmount); // Recalculate total amount

        // Re-check budget if details changed
        String yearMonth = existingResolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        // This would require a budgetService.updateBudget or similar, for now, just a
        // re-check
        for (ExpenditureDetail detail : existingResolution.getDetails()) {
            budgetService.useBudget(yearMonth, existingResolution.getDepartment(), detail.getAccountSubject(),
                    detail.getAmount());
        }

        return expenditureRepository.save(existingResolution);
    }

    // 승인 요청
    public void requestApproval(Long id) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        if (resolution.getStatus() != ExpenditureResolutionStatus.DRAFT
                && resolution.getStatus() != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("작성중이거나 반려된 결의서만 승인 요청할 수 있습니다.");
        }

        resolution.setStatus(ExpenditureResolutionStatus.REQUESTED);
        expenditureRepository.save(resolution);
    }

    // 결의서 승인 및 전표/자산/리스 자동 처리
    public void approveResolution(Long id) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        if (resolution.getStatus() != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("승인요청 상태의 결의서만 승인할 수 있습니다.");
        }

        // 1. 전표 생성 및 승인
        JournalEntry journalEntry = createJournalFromResolution(resolution);
        JournalEntry savedEntry = journalService.createJournalEntry(journalEntry);
        journalService.requestApproval(savedEntry.getId());
        journalService.approveJournalEntry(savedEntry.getId());

        // 2. 고정자산 자동 등록 체크
        for (ExpenditureDetail detail : resolution.getDetails()) {
            if (Boolean.TRUE.equals(detail.getAccountSubject().isFixedAsset())) {
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

        resolution.setStatus(ExpenditureResolutionStatus.APPROVED);
        resolution.setJournalEntry(savedEntry);
        resolution.setRejectionReason(null);
        expenditureRepository.save(resolution);
    }

    // 고정자산 자동 등록 로직
    private void createFixedAssetFromExpenditure(ExpenditureDetail detail, ExpenditureResolution resolution) {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode("FA-" + resolution.getResolutionNo() + "-" + detail.getId());
        asset.setAssetName(
                detail.getDescription() != null ? detail.getDescription() : detail.getAccountSubject().getName());
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

        if (resolution.getStatus() != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("승인요청 상태의 결의서만 반려할 수 있습니다.");
        }

        resolution.setStatus(ExpenditureResolutionStatus.REJECTED);
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
            journalDetail.setBusinessPartner(detail.getBusinessPartner());
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
