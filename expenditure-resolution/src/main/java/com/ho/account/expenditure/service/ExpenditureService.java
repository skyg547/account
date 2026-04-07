package com.ho.account.expenditure.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenditureService {

    private final ExpenditureResolutionRepository expenditureRepository;
    private final JournalService journalService;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final BudgetService budgetService;
    private final AssetRegistrationPort assetRegistrationPort;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public ExpenditureService(ExpenditureResolutionRepository expenditureRepository,
            JournalService journalService,
            AccountSubjectRepository accountSubjectRepository,
            DepartmentRepository departmentRepository,
            BusinessPartnerRepository businessPartnerRepository,
            BudgetService budgetService,
            AssetRegistrationPort assetRegistrationPort,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.expenditureRepository = expenditureRepository;
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.budgetService = budgetService;
        this.assetRegistrationPort = assetRegistrationPort;
        this.taxInvoiceQueryPort = taxInvoiceQueryPort;
    }

    public ExpenditureResolution createResolution(ExpenditureResolutionRequestDto requestDto) {
        ExpenditureResolution resolution = new ExpenditureResolution();
        resolution.setTitle(requestDto.getTitle());
        resolution.setResolutionDate(requestDto.getResolutionDate());
        resolution.setPaymentDate(requestDto.getPaymentDate());

        Department department = departmentRepository.findByCode(requestDto.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 부서입니다. 코드: " + requestDto.getDepartmentCode()));
        resolution.setDepartment(department);

        AccountSubject paymentAccount = accountSubjectRepository.findById(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지급 계정입니다. 코드: " + requestDto.getPaymentAccountCode()));
        resolution.setPaymentAccount(paymentAccount);

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());
        resolution.setTaxInvoiceId(requestDto.getTaxInvoiceId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = new ExpenditureDetail();
            detail.setDescription(detailDto.getDescription());
            detail.setAmount(detailDto.getAmount());

            AccountSubject detailAccount = accountSubjectRepository.findById(detailDto.getAccountSubjectCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 비용 계정입니다. 코드: " + detailDto.getAccountSubjectCode()));
            detail.setAccountSubject(detailAccount);

            BusinessPartner businessPartner = businessPartnerRepository
                    .findByBusinessPartnerCode(detailDto.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 거래처입니다. 코드: " + detailDto.getBusinessPartnerCode()));
            detail.setBusinessPartner(businessPartner);

            resolution.addDetail(detail);
            totalAmount = totalAmount.add(detail.getAmount());
        }
        resolution.setTotalAmount(totalAmount);

        return createResolution(resolution);
    }

    public ExpenditureResolution createResolution(ExpenditureResolution resolution) {
        String yearMonth = resolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (ExpenditureDetail detail : resolution.getDetails()) {
            budgetService.useBudget(yearMonth, resolution.getDepartment(), detail.getAccountSubject(),
                    detail.getAmount());
        }

        String resolutionNo = generateResolutionNo(resolution.getResolutionDate());
        resolution.setResolutionNo(resolutionNo);
        resolution.setStatus(ExpenditureResolutionStatus.DRAFT);

        return expenditureRepository.save(resolution);
    }

    public ExpenditureResolution updateResolution(Long id, ExpenditureResolutionRequestDto requestDto) {
        ExpenditureResolution existingResolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지출결의서를 찾을 수 없습니다. ID: " + id));

        if (existingResolution.getStatus() != ExpenditureResolutionStatus.DRAFT
                && existingResolution.getStatus() != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("작성중이거나 반려된 결의서만 수정할 수 있습니다.");
        }

        existingResolution.setTitle(requestDto.getTitle());
        existingResolution.setResolutionDate(requestDto.getResolutionDate());
        existingResolution.setPaymentDate(requestDto.getPaymentDate());

        Department department = departmentRepository.findByCode(requestDto.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 부서입니다. 코드: " + requestDto.getDepartmentCode()));
        existingResolution.setDepartment(department);

        AccountSubject paymentAccount = accountSubjectRepository.findById(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지급 계정입니다. 코드: " + requestDto.getPaymentAccountCode()));
        existingResolution.setPaymentAccount(paymentAccount);

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());
        existingResolution.setTaxInvoiceId(requestDto.getTaxInvoiceId());

        existingResolution.getDetails().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = new ExpenditureDetail();
            detail.setDescription(detailDto.getDescription());
            detail.setAmount(detailDto.getAmount());

            AccountSubject detailAccount = accountSubjectRepository.findById(detailDto.getAccountSubjectCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 비용 계정입니다. 코드: " + detailDto.getAccountSubjectCode()));
            detail.setAccountSubject(detailAccount);

            BusinessPartner businessPartner = businessPartnerRepository
                    .findByBusinessPartnerCode(detailDto.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 거래처입니다. 코드: " + detailDto.getBusinessPartnerCode()));
            detail.setBusinessPartner(businessPartner);

            existingResolution.addDetail(detail);
            totalAmount = totalAmount.add(detail.getAmount());
        }
        existingResolution.setTotalAmount(totalAmount);

        String yearMonth = existingResolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (ExpenditureDetail detail : existingResolution.getDetails()) {
            budgetService.useBudget(yearMonth, existingResolution.getDepartment(), detail.getAccountSubject(),
                    detail.getAmount());
        }

        return expenditureRepository.save(existingResolution);
    }

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

    public void approveResolution(Long id) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        if (resolution.getStatus() != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("승인요청 상태의 결의서만 승인할 수 있습니다.");
        }

        JournalEntry journalEntry = createJournalFromResolution(resolution);
        JournalEntry savedEntry = journalService.createJournalEntry(journalEntry);
        journalService.requestApproval(savedEntry.getId());
        journalService.approveJournalEntry(savedEntry.getId());

        for (ExpenditureDetail detail : resolution.getDetails()) {
            if (Boolean.TRUE.equals(detail.getAccountSubject().isFixedAsset())) {
                createFixedAssetFromExpenditure(detail, resolution);
            }
        }

        if (resolution.getLeaseContract() != null) {
            LeaseContract contract = resolution.getLeaseContract();
            assetRegistrationPort.activateLeaseContract(contract.getId());
        }

        resolution.setStatus(ExpenditureResolutionStatus.APPROVED);
        resolution.setJournalEntry(savedEntry);
        resolution.setRejectionReason(null);
        expenditureRepository.save(resolution);
    }

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

    @Transactional(readOnly = true)
    public List<ExpenditureResolution> getResolutionsByDate(LocalDate startDate, LocalDate endDate) {
        return expenditureRepository.findByResolutionDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public ExpenditureResolution getResolution(Long id) {
        return expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다."));
    }

    private void createFixedAssetFromExpenditure(ExpenditureDetail detail, ExpenditureResolution resolution) {
        assetRegistrationPort.registerAcquiredAsset(new AssetAcquisitionCommand(
                "FA-" + resolution.getResolutionNo() + "-" + detail.getId(),
                detail.getDescription() != null ? detail.getDescription() : detail.getAccountSubject().getName(),
                detail.getAccountSubject().getCode(),
                resolution.getPaymentDate(),
                detail.getAmount(),
                resolution.getDepartment().getCode(),
                5,
                "STRAIGHT_LINE",
                "REGISTERED"));
    }

    private JournalEntry createJournalFromResolution(ExpenditureResolution resolution) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(resolution.getResolutionDate());
        entry.setAccountingDate(resolution.getPaymentDate());
        entry.setDescription("지출결의 " + resolution.getTitle());

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

    private void validatePurchaseTaxInvoice(Long taxInvoiceId) {
        if (taxInvoiceId == null) {
            return;
        }

        TaxInvoiceRef taxInvoice = taxInvoiceQueryPort.findById(taxInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + taxInvoiceId));
        if (!"PURCHASE".equals(taxInvoice.type())) {
            throw new IllegalArgumentException("지출결의서에 연결하는 세금계산서는 PURCHASE 타입이어야 합니다.");
        }
    }
}
