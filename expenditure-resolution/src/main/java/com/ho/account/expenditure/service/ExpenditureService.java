package com.ho.account.expenditure.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
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
    private final JournalUseCase journalUseCase;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final BudgetService budgetService;
    private final AssetRegistrationPort assetRegistrationPort;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public ExpenditureService(ExpenditureResolutionRepository expenditureRepository,
            JournalUseCase journalUseCase,
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            DepartmentPersistencePort departmentPersistencePort,
            BusinessPartnerPersistencePort businessPartnerPersistencePort,
            BudgetService budgetService,
            AssetRegistrationPort assetRegistrationPort,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.expenditureRepository = expenditureRepository;
        this.journalUseCase = journalUseCase;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.budgetService = budgetService;
        this.assetRegistrationPort = assetRegistrationPort;
        this.taxInvoiceQueryPort = taxInvoiceQueryPort;
    }

    public ExpenditureResolution createResolution(ExpenditureResolutionRequestDto requestDto) {
        ExpenditureResolution resolution = new ExpenditureResolution();
        resolution.setTitle(requestDto.getTitle());
        resolution.setResolutionDate(requestDto.getResolutionDate());
        resolution.setPaymentDate(requestDto.getPaymentDate());

        Department department = departmentPersistencePort.findByCode(requestDto.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "議댁옱?섏? ?딅뒗 遺?쒖엯?덈떎. 肄붾뱶: " + requestDto.getDepartmentCode()));
        resolution.setDepartment(department);

        AccountSubject paymentAccount = accountSubjectPersistencePort.findByCode(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "議댁옱?섏? ?딅뒗 吏湲?怨꾩젙?낅땲?? 肄붾뱶: " + requestDto.getPaymentAccountCode()));
        resolution.setPaymentAccount(paymentAccount);

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());
        resolution.setTaxInvoiceId(requestDto.getTaxInvoiceId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = new ExpenditureDetail();
            detail.setDescription(detailDto.getDescription());
            detail.setAmount(detailDto.getAmount());

            AccountSubject detailAccount = accountSubjectPersistencePort.findByCode(detailDto.getAccountSubjectCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "議댁옱?섏? ?딅뒗 鍮꾩슜 怨꾩젙?낅땲?? 肄붾뱶: " + detailDto.getAccountSubjectCode()));
            detail.setAccountSubject(detailAccount);

            BusinessPartner businessPartner = businessPartnerPersistencePort
                    .findByBusinessPartnerCode(detailDto.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "議댁옱?섏? ?딅뒗 嫄곕옒泥섏엯?덈떎. 肄붾뱶: " + detailDto.getBusinessPartnerCode()));
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
                .orElseThrow(() -> new IllegalArgumentException("吏異쒓껐?섏꽌瑜?李얠쓣 ???놁뒿?덈떎. ID: " + id));

        if (existingResolution.getStatus() != ExpenditureResolutionStatus.DRAFT
                && existingResolution.getStatus() != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("?묒꽦以묒씠嫄곕굹 諛섎젮??寃곗쓽?쒕쭔 ?섏젙?????덉뒿?덈떎.");
        }

        existingResolution.setTitle(requestDto.getTitle());
        existingResolution.setResolutionDate(requestDto.getResolutionDate());
        existingResolution.setPaymentDate(requestDto.getPaymentDate());

        Department department = departmentPersistencePort.findByCode(requestDto.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "議댁옱?섏? ?딅뒗 遺?쒖엯?덈떎. 肄붾뱶: " + requestDto.getDepartmentCode()));
        existingResolution.setDepartment(department);

        AccountSubject paymentAccount = accountSubjectPersistencePort.findByCode(requestDto.getPaymentAccountCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "議댁옱?섏? ?딅뒗 吏湲?怨꾩젙?낅땲?? 肄붾뱶: " + requestDto.getPaymentAccountCode()));
        existingResolution.setPaymentAccount(paymentAccount);

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());
        existingResolution.setTaxInvoiceId(requestDto.getTaxInvoiceId());

        existingResolution.getDetails().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detailDto : requestDto.getDetails()) {
            ExpenditureDetail detail = new ExpenditureDetail();
            detail.setDescription(detailDto.getDescription());
            detail.setAmount(detailDto.getAmount());

            AccountSubject detailAccount = accountSubjectPersistencePort.findByCode(detailDto.getAccountSubjectCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "議댁옱?섏? ?딅뒗 鍮꾩슜 怨꾩젙?낅땲?? 肄붾뱶: " + detailDto.getAccountSubjectCode()));
            detail.setAccountSubject(detailAccount);

            BusinessPartner businessPartner = businessPartnerPersistencePort
                    .findByBusinessPartnerCode(detailDto.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "議댁옱?섏? ?딅뒗 嫄곕옒泥섏엯?덈떎. 肄붾뱶: " + detailDto.getBusinessPartnerCode()));
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
                .orElseThrow(() -> new IllegalArgumentException("寃곗쓽?쒕? 李얠쓣 ???놁뒿?덈떎. ID: " + id));

        if (resolution.getStatus() != ExpenditureResolutionStatus.DRAFT
                && resolution.getStatus() != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("?묒꽦以묒씠嫄곕굹 諛섎젮??寃곗쓽?쒕쭔 ?뱀씤 ?붿껌?????덉뒿?덈떎.");
        }

        resolution.setStatus(ExpenditureResolutionStatus.REQUESTED);
        expenditureRepository.save(resolution);
    }

    public void approveResolution(Long id) {
        ExpenditureResolution resolution = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("寃곗쓽?쒕? 李얠쓣 ???놁뒿?덈떎. ID: " + id));

        if (resolution.getStatus() != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("?뱀씤?붿껌 ?곹깭??寃곗쓽?쒕쭔 ?뱀씤?????덉뒿?덈떎.");
        }

        JournalEntry journalEntry = createJournalFromResolution(resolution);
        JournalEntry savedEntry = journalUseCase.createJournalEntry(journalEntry);
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");

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
                .orElseThrow(() -> new IllegalArgumentException("寃곗쓽?쒕? 李얠쓣 ???놁뒿?덈떎. ID: " + id));

        if (resolution.getStatus() != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("?뱀씤?붿껌 ?곹깭??寃곗쓽?쒕쭔 諛섎젮?????덉뒿?덈떎.");
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
                .orElseThrow(() -> new IllegalArgumentException("寃곗쓽?쒕? 李얠쓣 ???놁뒿?덈떎."));
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
        entry.setDescription("吏異쒓껐??" + resolution.getTitle());

        for (ExpenditureDetail detail : resolution.getDetails()) {
            JournalDetail journalDetail = new JournalDetail();
            journalDetail.setSide(JournalSide.DEBIT);
            journalDetail.setAccountSubject(detail.getAccountSubject());
            journalDetail.setAmount(detail.getAmount());
            journalDetail.setBaseAmount(detail.getAmount());
            journalDetail.setDepartment(resolution.getDepartment());
            journalDetail.setBusinessPartner(detail.getBusinessPartner());
            journalDetail.setDetailDescription(detail.getDescription());
            entry.addDetail(journalDetail);
        }

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setSide(JournalSide.CREDIT);
        creditDetail.setAccountSubject(resolution.getPaymentAccount());
        creditDetail.setAmount(resolution.getTotalAmount());
        creditDetail.setBaseAmount(resolution.getTotalAmount());
        creditDetail.setDepartment(resolution.getDepartment());
        creditDetail.setDetailDescription("Expenditure payment");
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
                .orElseThrow(() -> new IllegalArgumentException("?멸툑怨꾩궛?쒕? 李얠쓣 ???놁뒿?덈떎. ID: " + taxInvoiceId));
        if (!"PURCHASE".equals(taxInvoice.type())) {
            throw new IllegalArgumentException("吏異쒓껐?섏꽌???곌껐?섎뒗 ?멸툑怨꾩궛?쒕뒗 PURCHASE ??낆씠?댁빞 ?⑸땲??");
        }
    }
}
