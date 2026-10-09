package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지출결의 유즈케이스 서비스입니다.
 *
 * 🎓 [교육적 주석 / DDD Bounded Context & MSA 헥사고날 아키텍처 결합 해제 원칙]
 * 본 서비스는 지출결의(Expenditure Resolution) Bounded Context의 핵심 비즈니스 유즈케이스를 담당합니다.
 * 
 * 1. DDD Bounded Context 경계 보존 및 Core 간 컴파일 타임 격리:
 *    - 각 모듈의 Core 영역(`expenditure-resolution:core`, `journal-ledger:core`, `asset-lease:core` 등)은
 *      타 Bounded Context의 도메인 엔티티나 인커밍 포트(UseCase)를 직접 참조(implementation project)하지 않습니다.
 *    - 모듈 간 직접 컴파일 타임 의존성을 맺으면 한 Bounded Context의 변경이 타 Context로 전파되어
 *      MSA 독립 배포 및 자율성을 훼손하는 '스파게티 모놀리스' 위험이 발생합니다.
 *
 * 2. Shared Kernel (`contracts`) 및 헥사고날 아웃바운드 포트 패턴:
 *    - 타 Bounded Context와의 협력이 필요한 경우, 공동 계약 모듈(`contracts`)에 정의된 DTO 및 Outbound Port
 *      (`JournalPostingPort`, `MasterDataQueryPort`, `AssetRegistrationPort`, `TaxInvoiceQueryPort`)만을 사용합니다.
 *    - 서비스는 포트 인터페이스에만 의존하며, 실제 외부 통신 어댑터(Feign Client, REST Template, Kafka Producer, Local Mock 등)는
 *      Infrastructure 또는 Spring Configuration에서 주입(DI)받아 런타임에 실행됩니다.
 *
 * 3. 업무 조율 흐름:
 *    - 지출결의 생성/수정/반려 시 예산 차감 및 복원 통제 (`BudgetService`)
 *    - 결의 승인 시 전표 발행 계약 명령(`JournalEntryCommand`) 생성 후 `JournalPostingPort` 호출
 *    - 고정자산 대상 결의 승인 시 `AssetRegistrationPort` 호출을 통한 자산 등록 연동
 */
@Service
@Transactional
public class ExpenditureResolutionService implements ExpenditureResolutionUseCase {

    private final ExpenditureResolutionPersistencePort resolutionPersistencePort;
    private final JournalPostingPort journalPostingPort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final BudgetService budgetService;
    private final AssetRegistrationPort assetRegistrationPort;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public ExpenditureResolutionService(
            ExpenditureResolutionPersistencePort resolutionPersistencePort,
            JournalPostingPort journalPostingPort,
            MasterDataQueryPort masterDataQueryPort,
            BudgetService budgetService,
            AssetRegistrationPort assetRegistrationPort,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.resolutionPersistencePort = resolutionPersistencePort;
        this.journalPostingPort = journalPostingPort;
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

    /**
     * 지출결의서를 최종 승인하고 회계 전표 발행 및 자산 등록 조율을 수행합니다.
     * 
     * 🎓 [교육적 주석 / MSA 헥사고날 포트 패턴을 통한 전표 연동]
     * 타 Bounded Context(journal-ledger)의 인커밍 포트(`JournalUseCase`) 및 도메인 엔티티(`JournalEntry`)를 직접 참조하지 않고,
     * Shared Kernel(`contracts`) 모듈의 `JournalPostingPort` 및 `JournalEntryCommand`를 통해 아웃바운드 전표 발행을 요청합니다.
     * 
     * [아키텍처적 이점]
     * 1. 컴파일 타임 격리: expenditure-resolution 모듈은 journal-ledger 내부 도메인 변경에 영향을 받지 않습니다.
     * 2. MSA 유연성: 단일 프로세스(Monolith/Local) 실행 시 Spring Bean으로 간편히 바인딩되고,
     *    분산 환경(MSA) 전환 시 REST Feign Client나 Event Driven Broker(Kafka) 어댑터로 손쉽게 교체할 수 있습니다.
     */
    @Override
    public void approveResolution(Long id) {
        ExpenditureResolution resolution = resolutionPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다. ID: " + id));

        JournalEntryCommand command = buildJournalEntryCommand(resolution);
        JournalPostingResult postingResult = journalPostingPort.createDraftEntry(command);
        // Reject malformed responses from every port implementation before asset/lease side effects or approval.
        validateDraftPostingResult(postingResult);

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubjectRef accountSubject = requireAccountSubject(detail.getAccountCode());
            if (accountSubject.fixedAsset()) {
                registerFixedAsset(detail, resolution, accountSubject);
            }
        }

        if (resolution.getLeaseContractId() != null) {
            assetRegistrationPort.activateLeaseContract(resolution.getLeaseContractId());
        }

        resolution.approve(postingResult.journalEntryId());
        resolutionPersistencePort.save(resolution);
    }

    private static void validateDraftPostingResult(JournalPostingResult result) {
        if (result == null) {
            throw new IllegalStateException("Journal ledger posting returned null result");
        }
        if (result.journalEntryId() == null || result.journalEntryId() <= 0) {
            throw new IllegalStateException("Journal ledger posting returned invalid journal entry ID");
        }
        if (!"DRAFT".equals(result.status())) {
            throw new IllegalStateException("Journal ledger posting returned invalid draft status");
        }
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

    private JournalEntryCommand buildJournalEntryCommand(ExpenditureResolution resolution) {
        DepartmentRef department = requireDepartment(resolution.getDeptCode());
        List<JournalLineCommand> lines = new ArrayList<>();

        for (ExpenditureDetail detail : resolution.getDetails()) {
            AccountSubjectRef detailAccount = requireAccountSubject(detail.getAccountCode());
            BusinessPartnerRef businessPartner = requireBusinessPartner(detail.getBusinessPartnerCode());

            lines.add(new JournalLineCommand(
                    "DEBIT",
                    detailAccount.code(),
                    detail.getAmount(),
                    detail.getAmount(),
                    department.code(),
                    businessPartner.code(),
                    detail.getDescription()
            ));
        }

        AccountSubjectRef paymentAccount = requireAccountSubject(resolution.getPaymentAccountCode());
        lines.add(new JournalLineCommand(
                "CREDIT",
                paymentAccount.code(),
                resolution.getTotalAmount(),
                resolution.getTotalAmount(),
                department.code(),
                null,
                "Expenditure payment"
        ));

        String actor = resolution.getCreatedBy() != null && !resolution.getCreatedBy().isBlank()
                ? resolution.getCreatedBy().trim()
                : "SYSTEM";

        return new JournalEntryCommand(
                resolution.getResolutionDate(),
                resolution.getPaymentDate(),
                "지출결의: " + resolution.getTitle(),
                "EXPENDITURE_RESOLUTION",
                null,
                null,
                actor,
                actor,
                "EXPENDITURE_RESOLUTION",
                resolution.getResolutionNo(),
                lines
        );
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
