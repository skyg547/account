package com.ho.account.expenditure.application.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ExpenditureResolutionService implements ExpenditureResolutionUseCase {

    private final ExpenditureResolutionPersistencePort resolutionPersistencePort;
    private final JournalUseCase journalUseCase;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final BudgetService budgetService;
    private final AssetRegistrationPort assetRegistrationPort;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public ExpenditureResolutionService(
            ExpenditureResolutionPersistencePort resolutionPersistencePort,
            JournalUseCase journalUseCase,
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            DepartmentPersistencePort departmentPersistencePort,
            BusinessPartnerPersistencePort businessPartnerPersistencePort,
            BudgetService budgetService,
            AssetRegistrationPort assetRegistrationPort,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.resolutionPersistencePort = resolutionPersistencePort;
        this.journalUseCase = journalUseCase;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.budgetService = budgetService;
        this.assetRegistrationPort = assetRegistrationPort;
        this.taxInvoiceQueryPort = taxInvoiceQueryPort;
    }

    @Override
    public ExpenditureResolution createResolution(ExpenditureResolutionRequestDto requestDto) {
        Department department = departmentPersistencePort.findActiveByCode(requestDto.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다. 코드: " + requestDto.getDepartmentCode()));

        AccountSubject paymentAccount = accountSubjectPersistencePort.findByCode(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지급 계정과목입니다. 코드: " + requestDto.getPaymentAccountCode()));

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());

        ExpenditureResolution resolution = ExpenditureResolution.create(
                null, // No will be generated later
                requestDto.getTitle(),
                requestDto.getResolutionDate(),
                requestDto.getPaymentDate(),
                department.getCode(),
                paymentAccount.getCode(),
                "SYSTEM" // Default user for now
        );
        resolution.setTaxInvoiceId(requestDto.getTaxInvoiceId());

        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = buildDetail(detailDto);
            resolution.addDetail(detail);
        }

        return createResolution(resolution);
    }

    @Override
    public ExpenditureResolution createResolution(ExpenditureResolution resolution) {
        String yearMonth = resolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        
        // 부서 정보 조회 (ID 기반)
        Department department = departmentPersistencePort.findActiveByCode(resolution.getDeptCode())
                .orElseThrow(() -> new IllegalStateException("Department not found: " + resolution.getDeptCode()));

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(detail.getAccountCode())
                    .orElseThrow(() -> new IllegalStateException("Account subject not found: " + detail.getAccountCode()));
            budgetService.useBudget(yearMonth, department, accountSubject, detail.getAmount());
        }
        String resolutionNo = generateResolutionNo(resolution.getResolutionDate());
        resolution.setResolutionNo(resolutionNo);
        
        return resolutionPersistencePort.save(resolution);
    }

    @Override
    public ExpenditureResolution updateResolution(Long id, ExpenditureResolutionRequestDto requestDto) {
        ExpenditureResolution existing = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지출결의서를 찾을 수 없습니다. ID: " + id));

        if (existing.getStatus() != com.ho.account.expenditure.domain.ExpenditureResolutionStatus.DRAFT
                && existing.getStatus() != com.ho.account.expenditure.domain.ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("DRAFT 또는 REJECTED 상태의 결의서만 수정할 수 있습니다.");
        }

        // 도메인 엔티티 업데이트 (ID 기반)
        existing.updateInfo(requestDto.getTitle(), requestDto.getResolutionDate(), requestDto.getPaymentDate(),
                requestDto.getDepartmentCode(), requestDto.getPaymentAccountCode());

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());
        existing.setTaxInvoiceId(requestDto.getTaxInvoiceId());

        existing.getDetails().clear();
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = buildDetail(detailDto);
            existing.addDetail(detail);
        }

        // 예산 확인
        Department department = departmentPersistencePort.findActiveByCode(existing.getDeptCode())
                .orElseThrow(() -> new IllegalStateException("Department not found: " + existing.getDeptCode()));
        String yearMonth = existing.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (ExpenditureDetail detail : existing.getDetails()) {
            AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(detail.getAccountCode())
                    .orElseThrow(() -> new IllegalStateException("Account subject not found: " + detail.getAccountCode()));
            budgetService.useBudget(yearMonth, department, accountSubject, detail.getAmount());
        }

        return resolutionPersistencePort.save(existing);
    }

    @Override
    public void requestApproval(Long id) {
        ExpenditureResolution resolution = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));
        resolution.requestApproval();
        resolutionPersistencePort.save(resolution);
    }

    @Override
    public void approveResolution(Long id) {
        ExpenditureResolution resolution = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        JournalEntry journalEntry = buildJournalEntry(resolution);
        JournalEntry savedEntry = journalUseCase.createJournalEntry(journalEntry);
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(detail.getAccountCode())
                    .orElseThrow(() -> new IllegalStateException("Account subject not found: " + detail.getAccountCode()));
            if (Boolean.TRUE.equals(accountSubject.isFixedAsset())) {
                registerFixedAsset(detail, resolution, accountSubject);
            }
        }

        if (resolution.getLeaseContractId() != null) {
            assetRegistrationPort.activateLeaseContract(resolution.getLeaseContractId());
        }

        resolution.approve(savedEntry.getId());
        resolutionPersistencePort.save(resolution);
    }

    @Override
    public void rejectResolution(Long id, String reason) {
        ExpenditureResolution resolution = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));
        resolution.reject(reason);
        resolutionPersistencePort.save(resolution);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenditureResolution getResolution(Long id) {
        return resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenditureResolution> getResolutionsByDate(LocalDate startDate, LocalDate endDate) {
        return resolutionPersistencePort.findByResolutionDateBetween(startDate, endDate);
    }

    private ExpenditureDetail buildDetail(ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto) {
        return ExpenditureDetail.create(
                detailDto.getAccountSubjectCode(),
                detailDto.getAmount(),
                detailDto.getBusinessPartnerCode(),
                detailDto.getDescription()
        );
    }

    private JournalEntry buildJournalEntry(ExpenditureResolution resolution) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(resolution.getResolutionDate());
        entry.setAccountingDate(resolution.getPaymentDate());
        entry.setDescription("지출결의: " + resolution.getTitle());
        entry.setLineageSourceType("EXPENDITURE_RESOLUTION");
        entry.setLineageSourceId(resolution.getResolutionNo());

        // 부서 정보 (결의서 헤더 부서)
        Department department = departmentPersistencePort.findActiveByCode(resolution.getDeptCode())
                .orElseThrow(() -> new IllegalStateException("부서 정보를 찾을 수 없습니다. 코드: " + resolution.getDeptCode()));

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubject detailAccount = accountSubjectPersistencePort.findByCode(detail.getAccountCode())
                    .orElseThrow(() -> new IllegalStateException("계정과목을 찾을 수 없습니다. 코드: " + detail.getAccountCode()));
            
            // 거래처는 선택 사항일 수 있으나, 지출결의 상세에서는 보통 필수임. 여기서는 필수인 것으로 간주하여 예외 처리.
            BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(detail.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalStateException("거래처 정보를 찾을 수 없습니다. 코드: " + detail.getBusinessPartnerCode()));

            JournalDetail debitLine = new JournalDetail();
            debitLine.setSide(JournalSide.DEBIT);
            debitLine.setAccountSubject(detailAccount);
            debitLine.setAmount(detail.getAmount());
            debitLine.setBaseAmount(detail.getAmount());
            debitLine.setDepartment(department);
            debitLine.setBusinessPartner(businessPartner);
            debitLine.setDetailDescription(detail.getDescription());
            entry.addDetail(debitLine);
        }

        JournalDetail creditLine = new JournalDetail();
        creditLine.setSide(JournalSide.CREDIT);
        AccountSubject paymentAccount = accountSubjectPersistencePort.findByCode(resolution.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalStateException("지급 계정과목을 찾을 수 없습니다. 코드: " + resolution.getPaymentAccountCode()));
        creditLine.setAccountSubject(paymentAccount);
        creditLine.setAmount(resolution.getTotalAmount());
        creditLine.setBaseAmount(resolution.getTotalAmount());
        creditLine.setDepartment(department);
        creditLine.setDetailDescription("Expenditure payment");
        entry.addDetail(creditLine);

        return entry;
    }

    private void registerFixedAsset(ExpenditureDetail detail, ExpenditureResolution resolution, AccountSubject accountSubject) {
        assetRegistrationPort.registerAcquiredAsset(new AssetAcquisitionCommand(
                "FA-" + resolution.getResolutionNo() + "-" + detail.getId(),
                detail.getDescription() != null ? detail.getDescription() : accountSubject.getName(),
                accountSubject.getCode(),
                resolution.getPaymentDate(),
                detail.getAmount(),
                resolution.getDeptCode(),
                5,
                "STRAIGHT_LINE",
                "REGISTERED"));
    }

    private String generateResolutionNo(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<ExpenditureResolution> list = resolutionPersistencePort.findByResolutionDate(date);
        int seq = list.size() + 1;
        return String.format("REQ-%s-%03d", dateStr, seq);
    }

    private void validatePurchaseTaxInvoice(Long taxInvoiceId) {
        if (taxInvoiceId == null) return;
        TaxInvoiceRef taxInvoice = taxInvoiceQueryPort.findById(taxInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + taxInvoiceId));
        if (!"PURCHASE".equals(taxInvoice.type())) {
            throw new IllegalArgumentException("지출결의서에 연결하는 세금계산서는 PURCHASE 타입이어야 합니다.");
        }
    }
}
