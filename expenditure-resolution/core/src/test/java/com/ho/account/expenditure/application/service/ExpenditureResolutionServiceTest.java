package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionCommand;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenditureResolutionServiceTest {

    @Mock
    private ExpenditureResolutionPersistencePort resolutionPersistencePort;
    @Mock
    private JournalUseCase journalUseCase;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private BudgetService budgetService;
    @Mock
    private AssetRegistrationPort assetRegistrationPort;
    @Mock
    private TaxInvoiceQueryPort taxInvoiceQueryPort;

    @Test
    void createResolutionWithPurchaseTaxInvoiceSavesResolution() {
        ExpenditureResolutionService service = createService();
        LocalDate resolutionDate = LocalDate.of(2026, 4, 29);
        ExpenditureResolutionCommand command = createCommand(resolutionDate);

        givenMasterData();
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE", "ACTIVE")));
        when(resolutionPersistencePort.findByResolutionDate(resolutionDate)).thenReturn(List.of());
        when(resolutionPersistencePort.save(any(ExpenditureResolution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureResolution saved = service.createResolution(command);

        assertEquals("REQ-20260429-001", saved.getResolutionNo());
        assertEquals(0, saved.getTotalAmount().compareTo(new BigDecimal("1200.00")));
        verify(budgetService).useBudget(
                eq("202604"),
                eq("D001"),
                eq("EXP001"),
                eq(new BigDecimal("1200.00")));
    }

    @Test
    void createResolutionWithSalesTaxInvoiceThrows() {
        ExpenditureResolutionService service = createService();
        givenHeaderMasterData();
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "SALES", "ACTIVE")));

        assertThrows(IllegalArgumentException.class, () -> service.createResolution(createCommand(LocalDate.of(2026, 4, 29))));
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void createResolutionWithCancelledTaxInvoiceThrows() {
        ExpenditureResolutionService service = createService();
        givenHeaderMasterData();
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE", "CANCELLED")));

        assertThrows(IllegalArgumentException.class, () -> service.createResolution(createCommand(LocalDate.of(2026, 4, 29))));
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionThrowsWhenDepartmentIsMissing() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(masterDataQueryPort.findDepartment("D001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.approveResolution(1L));

        verify(journalUseCase, never()).createJournalEntry(any());
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionThrowsWhenDetailAccountIsMissing() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(masterDataQueryPort.findDepartment("D001")).thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", "COST_CENTER")));
        when(masterDataQueryPort.findAccountSubject("EXP001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.approveResolution(1L));

        verify(journalUseCase, never()).createJournalEntry(any());
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionThrowsWhenBusinessPartnerIsMissing() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(masterDataQueryPort.findDepartment("D001")).thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", "COST_CENTER")));
        when(masterDataQueryPort.findAccountSubject("EXP001")).thenReturn(Optional.of(new AccountSubjectRef("EXP001", "수선비", false, false)));
        when(masterDataQueryPort.findBusinessPartner("BP001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.approveResolution(1L));

        verify(journalUseCase, never()).createJournalEntry(any());
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionCreatesJournalAndApprovesResolution() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        givenMasterData();
        JournalEntry savedEntry = new JournalEntry();
        savedEntry.setId(100L);
        savedEntry.initializeDraft();

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenReturn(savedEntry);

        service.approveResolution(1L);

        assertEquals(ExpenditureResolutionStatus.APPROVED, resolution.getStatus());
        verify(journalUseCase).approveJournalEntry(100L, "SYSTEM");
        verify(resolutionPersistencePort).save(resolution);
    }

    @Test
    void updateResolutionRestoresExistingBudgetAndDeductsNewAmountWhenDraft() {
        ExpenditureResolutionService service = createService();
        LocalDate resolutionDate = LocalDate.of(2026, 4, 29);
        ExpenditureResolution existing = ExpenditureResolution.create(
                "REQ-20260429-001",
                "기존 결의서",
                resolutionDate,
                resolutionDate.plusDays(1),
                "D001",
                "PAY001",
                "tester");
        ExpenditureDetail detail = ExpenditureDetail.create("EXP001", new BigDecimal("1000.00"), "BP001", "기존 내역");
        existing.addDetail(detail);

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(existing));
        givenMasterData();
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE", "ACTIVE")));
        when(resolutionPersistencePort.save(any(ExpenditureResolution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureResolutionCommand updateCmd = new ExpenditureResolutionCommand(
                "수정 결의서",
                resolutionDate,
                resolutionDate.plusDays(1),
                "D001",
                "PAY001",
                10L,
                List.of(new ExpenditureResolutionCommand.DetailCommand("EXP001", new BigDecimal("1500.00"), "BP001", "수정 내역")));

        ExpenditureResolution updated = service.updateResolution(1L, updateCmd);

        verify(budgetService).restoreBudget("202604", "D001", "EXP001", new BigDecimal("1000.00"));
        verify(budgetService).useBudget("202604", "D001", "EXP001", new BigDecimal("1500.00"));
        assertEquals("수정 결의서", updated.getTitle());
    }

    @Test
    void rejectResolutionRestoresBudgetOnRejection() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));

        service.rejectResolution(1L, "서류 미비로 인한 반려");

        assertEquals(ExpenditureResolutionStatus.REJECTED, resolution.getStatus());
        assertEquals("서류 미비로 인한 반려", resolution.getRejectionReason());
        verify(budgetService).restoreBudget("202604", "D001", "EXP001", new BigDecimal("1200.00"));
        verify(resolutionPersistencePort).save(resolution);
    }

    private ExpenditureResolutionCommand createCommand(LocalDate resolutionDate) {
        return new ExpenditureResolutionCommand(
                "지출결의 테스트",
                resolutionDate,
                resolutionDate.plusDays(1),
                "D001",
                "PAY001",
                10L,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP001",
                        new BigDecimal("1200.00"),
                        "BP001",
                        "세금계산서 검증 테스트")));
    }

    private ExpenditureResolution requestedResolution() {
        ExpenditureResolution resolution = ExpenditureResolution.create(
                "REQ-20260429-001",
                "지출결의 테스트",
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                "tester");
        ExpenditureDetail detail = ExpenditureDetail.create(
                "EXP001",
                new BigDecimal("1200.00"),
                "BP001",
                "마스터 조회 검증");
        resolution.addDetail(detail);
        resolution.requestApproval();
        return resolution;
    }

    private void givenHeaderMasterData() {
        when(masterDataQueryPort.findDepartment("D001")).thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", "COST_CENTER")));
        when(masterDataQueryPort.findAccountSubject("PAY001")).thenReturn(Optional.of(new AccountSubjectRef("PAY001", "보통예금", false, false)));
    }

    private void givenMasterData() {
        givenHeaderMasterData();
        when(masterDataQueryPort.findAccountSubject("EXP001")).thenReturn(Optional.of(new AccountSubjectRef("EXP001", "수선비", false, false)));
        when(masterDataQueryPort.findBusinessPartner("BP001")).thenReturn(Optional.of(new BusinessPartnerRef("BP001", "테스트거래처", "VENDOR", true)));
    }

    private ExpenditureResolutionService createService() {
        return new ExpenditureResolutionService(
                resolutionPersistencePort,
                journalUseCase,
                masterDataQueryPort,
                budgetService,
                assetRegistrationPort,
                taxInvoiceQueryPort);
    }
}
