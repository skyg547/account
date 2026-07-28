package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionCommand;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지출결의 유즈케이스 서비스입니다.
 *
 * 초보자용 설명:
 * 이 서비스는 지출결의 생성, 예산 사용, 승인 전표 생성의 업무 순서를 조율합니다.
 * master-data 내부 Repository/Entity를 직접 쓰지 않고 `MasterDataQueryPort`로 코드의 유효성을 확인합니다.
 */
@Service
@Transactional
public class ExpenditureResolutionService implements ExpenditureResolutionUseCase {

    private final ExpenditureResolutionPersistencePort resolutionPersistencePort;
    private final JournalUseCase journalUseCase;
    private final MasterDataQueryPort masterDataQueryPort;
    private final BudgetService budgetService;
    private final AssetRegistrationPort assetRegistrationPort;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public ExpenditureResolutionService(
            ExpenditureResolutionPersistencePort resolutionPersistencePort,
            JournalUseCase journalUseCase,
            MasterDataQueryPort masterDataQueryPort,
            BudgetService budgetService,
            AssetRegistrationPort assetRegistrationPort,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.resolutionPersistencePort = resolutionPersistencePort;
        this.journalUseCase = journalUseCase;
        this.masterDataQueryPort = masterDataQueryPort;
        this.budgetService = budgetService;
        this.assetRegistrationPort = assetRegistrationPort;
        this.taxInvoiceQueryPort = taxInvoiceQueryPort;
    }

    @Override
    public ExpenditureResolution createResolution(ExpenditureResolutionCommand command) {
        DepartmentRef department = requireDepartment(command.departmentCode());
        AccountSubjectRef paymentAccount = requireAccountSubject(command.paymentAccountCode());
        validatePurchaseTaxInvoice(command.taxInvoiceId());

        ExpenditureResolution resolution = ExpenditureResolution.create(
                null,
                command.title(),
                command.resolutionDate(),
                command.paymentDate(),
                department.code(),
                paymentAccount.code(),
                "SYSTEM"
        );
        resolution.setTaxInvoiceId(command.taxInvoiceId());

        for (ExpenditureResolutionCommand.DetailCommand detailCommand : command.details()) {
            ExpenditureDetail detail = buildDetail(detailCommand);
            resolution.addDetail(detail);
        }

        return createResolution(resolution);
    }

    @Override
    public ExpenditureResolution createResolution(ExpenditureResolution resolution) {
        String yearMonth = resolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        DepartmentRef department = requireDepartment(resolution.getDeptCode());

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubjectRef accountSubject = requireAccountSubject(detail.getAccountCode());
            budgetService.useBudget(yearMonth, department.code(), accountSubject.code(), detail.getAmount());
        }
        String resolutionNo = generateResolutionNo(resolution.getResolutionDate());
        resolution.setResolutionNo(resolutionNo);

        return resolutionPersistencePort.save(resolution);
    }

    @Override
    public ExpenditureResolution updateResolution(Long id, ExpenditureResolutionCommand command) {
        ExpenditureResolution existing = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지출결의서를 찾을 수 없습니다. ID: " + id));

        if (existing.getStatus() != com.ho.account.expenditure.domain.ExpenditureResolutionStatus.DRAFT
                && existing.getStatus() != com.ho.account.expenditure.domain.ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("DRAFT 또는 REJECTED 상태의 결의서만 수정할 수 있습니다.");
        }

        DepartmentRef department = requireDepartment(command.departmentCode());
        AccountSubjectRef paymentAccount = requireAccountSubject(command.paymentAccountCode());
        validatePurchaseTaxInvoice(command.taxInvoiceId());

        existing.updateInfo(command.title(), command.resolutionDate(), command.paymentDate(),
                department.code(), paymentAccount.code());
        existing.setTaxInvoiceId(command.taxInvoiceId());

        existing.getDetails().clear();
        for (ExpenditureResolutionCommand.DetailCommand detailCommand : command.details()) {
            ExpenditureDetail detail = buildDetail(detailCommand);
            existing.addDetail(detail);
        }

        String yearMonth = existing.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (ExpenditureDetail detail : existing.getDetails()) {
            AccountSubjectRef accountSubject = requireAccountSubject(detail.getAccountCode());
            budgetService.useBudget(yearMonth, department.code(), accountSubject.code(), detail.getAmount());
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
            AccountSubjectRef accountSubject = requireAccountSubject(detail.getAccountCode());
            if (accountSubject.fixedAsset()) {
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

    private ExpenditureDetail buildDetail(ExpenditureResolutionCommand.DetailCommand detailCommand) {
        requireAccountSubject(detailCommand.accountSubjectCode());
        requireBusinessPartner(detailCommand.businessPartnerCode());
        return ExpenditureDetail.create(
                detailCommand.accountSubjectCode(),
                detailCommand.amount(),
                detailCommand.businessPartnerCode(),
                detailCommand.description()
        );
    }

    private JournalEntry buildJournalEntry(ExpenditureResolution resolution) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(resolution.getResolutionDate());
        entry.setAccountingDate(resolution.getPaymentDate());
        entry.setDescription("지출결의: " + resolution.getTitle());
        entry.setLineageSourceType("EXPENDITURE_RESOLUTION");
        entry.setLineageSourceId(resolution.getResolutionNo());

        DepartmentRef department = requireDepartment(resolution.getDeptCode());

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubjectRef detailAccount = requireAccountSubject(detail.getAccountCode());
            BusinessPartnerRef businessPartner = requireBusinessPartner(detail.getBusinessPartnerCode());

            JournalDetail debitLine = new JournalDetail();
            debitLine.setSide(JournalSide.DEBIT);
            debitLine.setAccountCode(detailAccount.code());
            debitLine.setAmount(detail.getAmount());
            debitLine.setBaseAmount(detail.getAmount());
            debitLine.setDepartmentCode(department.code());
            debitLine.setBusinessPartnerCode(businessPartner.code());
            debitLine.setDetailDescription(detail.getDescription());
            entry.addDetail(debitLine);
        }

        JournalDetail creditLine = new JournalDetail();
        creditLine.setSide(JournalSide.CREDIT);
        AccountSubjectRef paymentAccount = requireAccountSubject(resolution.getPaymentAccountCode());
        creditLine.setAccountCode(paymentAccount.code());
        creditLine.setAmount(resolution.getTotalAmount());
        creditLine.setBaseAmount(resolution.getTotalAmount());
        creditLine.setDepartmentCode(department.code());
        creditLine.setDetailDescription("Expenditure payment");
        entry.addDetail(creditLine);

        return entry;
    }

    private void registerFixedAsset(ExpenditureDetail detail, ExpenditureResolution resolution, AccountSubjectRef accountSubject) {
        assetRegistrationPort.registerAcquiredAsset(new AssetAcquisitionCommand(
                "FA-" + resolution.getResolutionNo() + "-" + detail.getId(),
                detail.getDescription() != null ? detail.getDescription() : accountSubject.name(),
                accountSubject.code(),
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

    private DepartmentRef requireDepartment(String departmentCode) {
        return masterDataQueryPort.findDepartment(departmentCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다. 코드: " + departmentCode));
    }

    private AccountSubjectRef requireAccountSubject(String accountCode) {
        return masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정과목입니다. 코드: " + accountCode));
    }

    private BusinessPartnerRef requireBusinessPartner(String businessPartnerCode) {
        BusinessPartnerRef partner = masterDataQueryPort.findBusinessPartner(businessPartnerCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래처입니다. 코드: " + businessPartnerCode));
        if (Boolean.FALSE.equals(partner.active())) {
            throw new IllegalArgumentException("비활성 거래처는 지출결의에 사용할 수 없습니다. 코드: " + businessPartnerCode);
        }
        return partner;
    }

    private void validatePurchaseTaxInvoice(Long taxInvoiceId) {
        if (taxInvoiceId == null) return;
        TaxInvoiceRef taxInvoice = taxInvoiceQueryPort.findById(taxInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + taxInvoiceId));
        if (!taxInvoice.purchase()) {
            throw new IllegalArgumentException("지출결의서에 연결하는 세금계산서는 PURCHASE 타입이어야 합니다.");
        }
        if (!taxInvoice.active()) {
            throw new IllegalArgumentException("지출결의서에는 취소된 세금계산서를 연결할 수 없습니다.");
        }
    }
}