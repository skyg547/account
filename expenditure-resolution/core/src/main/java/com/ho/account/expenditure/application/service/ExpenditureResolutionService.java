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

    /**
     * 지출결의서를 수정합니다.
     * 
     * 🎓 [교육적 주석 / Budget Control & Re-deduction Strategy]
     * 지출결의서 수정 시 기존에 차감했던 예산 금액을 먼저 복원(restoreBudget)한 뒤, 신규 작성 금액으로 예산을 재차감(useBudget)합니다.
     * 
     * [이중 차감 결함 제거 원리]
     * 이전 코드에서는 기존에 차감된 예산을 복원하지 않은 채 신규 금액만 useBudget하여 동일 결의서에 대해 예산이 이중 차감되는 결함이 있었습니다.
     * 1) 결의서가 DRAFT 상태인 경우: 이미 createResolution 시점에 예산이 차감되었으므로, 기존 내역(기존 연월, 기존 부서, 기존 내역별 계정/금액)의 예산을 복원합니다.
     *    (단, REJECTED 상태인 경우는 rejectResolution 호출 시점에 이미 예산이 전액 복원되었으므로 사전 복원을 수행하지 않습니다.)
     * 2) 결의서 수정 적용: 기본 정보 및 상세 내역(Details) 갱신.
     * 3) 신규 금액 재차감: 갱신된 내역(신규 연월, 신규 부서, 신규 내역별 계정/금액)에 대해 useBudget으로 예산을 재차감합니다.
     * 이를 통해 결의서 변경 시에도 잔여 예산의 정합성과 금융 통제를 정확히 이행합니다.
     */
    @Override
    public ExpenditureResolution updateResolution(Long id, ExpenditureResolutionCommand command) {
        ExpenditureResolution existing = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지출결의서를 찾을 수 없습니다. ID: " + id));

        if (existing.getStatus() != com.ho.account.expenditure.domain.ExpenditureResolutionStatus.DRAFT
                && existing.getStatus() != com.ho.account.expenditure.domain.ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("DRAFT 또는 REJECTED 상태의 결의서만 수정할 수 있습니다.");
        }

        // DRAFT 상태인 경우, 작성 시점에 차감되었던 기존 결의 금액에 대해 예산을 선-복원(restoreBudget)
        if (existing.getStatus() == com.ho.account.expenditure.domain.ExpenditureResolutionStatus.DRAFT) {
            String existingYearMonth = existing.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String existingDeptCode = existing.getDeptCode();
            for (ExpenditureDetail detail : existing.getDetails()) {
                budgetService.restoreBudget(existingYearMonth, existingDeptCode, detail.getAccountCode(), detail.getAmount());
            }
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

    /**
     * 지출결의서를 반려 처리합니다.
     * 
     * 🎓 [교육적 주석 / Budget Restoration on Rejection]
     * 승인 요청된 결의서가 반려(REJECTED)되면 차감되었던 예산 금액을 즉시 전액 복원(restoreBudget)합니다.
     * 
     * [금융 통제 이점]
     * 결의서 작성을 통해 잠겼던 예산 자원을 결의 반려 시 즉시 해제함으로써, 부서의 실제 잔여 예산을
     * 정확히 반영하고 타 유효 결의 생성을 방해하지 않도록 보장합니다.
     */
    @Override
    public void rejectResolution(Long id, String reason) {
        ExpenditureResolution resolution = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));
        resolution.reject(reason);

        // 반려 시 차감되었던 예산을 전액 복원
        String yearMonth = resolution.getResolutionDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String deptCode = resolution.getDeptCode();
        for (ExpenditureDetail detail : resolution.getDetails()) {
            budgetService.restoreBudget(yearMonth, deptCode, detail.getAccountCode(), detail.getAmount());
        }

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