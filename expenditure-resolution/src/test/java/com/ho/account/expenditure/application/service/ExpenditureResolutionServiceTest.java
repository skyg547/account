package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private AccountSubjectPersistencePort accountSubjectPersistencePort;
    @Mock
    private DepartmentPersistencePort departmentPersistencePort;
    @Mock
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;
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
        ExpenditureResolutionRequestDto request = createRequest(resolutionDate);

        Department department = new Department();
        department.setCode("D001");
        department.setName("재무팀");

        AccountSubject paymentAccount = new AccountSubject();
        paymentAccount.setCode("PAY001");
        paymentAccount.setName("보통예금");

        AccountSubject expenseAccount = new AccountSubject();
        expenseAccount.setCode("EXP001");
        expenseAccount.setName("수선비");

        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("PAY001")).thenReturn(Optional.of(paymentAccount));
        when(accountSubjectPersistencePort.findByCode("EXP001")).thenReturn(Optional.of(expenseAccount));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE")));
        when(resolutionPersistencePort.findByResolutionDate(resolutionDate)).thenReturn(List.of());
        when(resolutionPersistencePort.save(any(ExpenditureResolution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureResolution saved = service.createResolution(request);

        assertEquals("REQ-20260429-001", saved.getResolutionNo());
        assertEquals(0, saved.getTotalAmount().compareTo(new BigDecimal("1200.00")));
        verify(budgetService).useBudget(
                eq("202604"),
                eq(department),
                eq(expenseAccount),
                eq(new BigDecimal("1200.00")));
    }

    @Test
    void createResolutionWithSalesTaxInvoiceThrows() {
        ExpenditureResolutionService service = createService();

        ExpenditureResolutionRequestDto request = createRequest(LocalDate.of(2026, 4, 29));

        Department department = new Department();
        department.setCode("D001");
        department.setName("재무팀");

        AccountSubject paymentAccount = new AccountSubject();
        paymentAccount.setCode("PAY001");
        paymentAccount.setName("보통예금");

        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("PAY001")).thenReturn(Optional.of(paymentAccount));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "SALES")));

        assertThrows(IllegalArgumentException.class, () -> service.createResolution(request));
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionThrowsWhenDepartmentIsMissing() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.approveResolution(1L));

        verify(journalUseCase, never()).createJournalEntry(any());
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionThrowsWhenDetailAccountIsMissing() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        Department department = department();
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("EXP001")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.approveResolution(1L));

        verify(journalUseCase, never()).createJournalEntry(any());
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    @Test
    void approveResolutionThrowsWhenBusinessPartnerIsMissing() {
        ExpenditureResolutionService service = createService();
        ExpenditureResolution resolution = requestedResolution();
        Department department = department();
        AccountSubject expenseAccount = account("EXP001", "수선비");
        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(resolution));
        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("EXP001")).thenReturn(Optional.of(expenseAccount));
        when(businessPartnerPersistencePort.findByBusinessPartnerCode("BP001")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.approveResolution(1L));

        verify(journalUseCase, never()).createJournalEntry(any());
        verify(resolutionPersistencePort, never()).save(any(ExpenditureResolution.class));
    }

    private ExpenditureResolutionRequestDto createRequest(LocalDate resolutionDate) {
        ExpenditureResolutionRequestDto request = new ExpenditureResolutionRequestDto();
        request.setTitle("지출결의 테스트");
        request.setResolutionDate(resolutionDate);
        request.setPaymentDate(resolutionDate.plusDays(1));
        request.setDepartmentCode("D001");
        request.setPaymentAccountCode("PAY001");
        request.setTaxInvoiceId(10L);

        ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto detail =
                new ExpenditureResolutionRequestDto.ExpenditureDetailRequestDto();
        detail.setAccountSubjectCode("EXP001");
        detail.setAmount(new BigDecimal("1200.00"));
        detail.setBusinessPartnerCode("BP001");
        detail.setDescription("세금계산서 검증 테스트");
        request.setDetails(List.of(detail));
        return request;
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

    private Department department() {
        Department department = new Department();
        department.setCode("D001");
        department.setName("재무팀");
        return department;
    }

    private AccountSubject account(String code, String name) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName(name);
        return account;
    }

    private ExpenditureResolutionService createService() {
        return new ExpenditureResolutionService(
                resolutionPersistencePort,
                journalUseCase,
                accountSubjectPersistencePort,
                departmentPersistencePort,
                businessPartnerPersistencePort,
                budgetService,
                assetRegistrationPort,
                taxInvoiceQueryPort);
    }
}
