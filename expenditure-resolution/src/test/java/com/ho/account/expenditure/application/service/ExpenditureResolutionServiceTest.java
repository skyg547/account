package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
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
        ExpenditureResolutionService service = new ExpenditureResolutionService(
                resolutionPersistencePort,
                journalUseCase,
                accountSubjectPersistencePort,
                departmentPersistencePort,
                businessPartnerPersistencePort,
                budgetService,
                assetRegistrationPort,
                taxInvoiceQueryPort);

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

        BusinessPartner partner = new BusinessPartner();
        partner.setBusinessPartnerCode("BP001");
        partner.setBusinessPartnerName("테스트거래처");

        when(departmentPersistencePort.findByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("PAY001")).thenReturn(Optional.of(paymentAccount));
        when(accountSubjectPersistencePort.findByCode("EXP001")).thenReturn(Optional.of(expenseAccount));
        when(businessPartnerPersistencePort.findByBusinessPartnerCode("BP001")).thenReturn(Optional.of(partner));
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
        ExpenditureResolutionService service = new ExpenditureResolutionService(
                resolutionPersistencePort,
                journalUseCase,
                accountSubjectPersistencePort,
                departmentPersistencePort,
                businessPartnerPersistencePort,
                budgetService,
                assetRegistrationPort,
                taxInvoiceQueryPort);

        ExpenditureResolutionRequestDto request = createRequest(LocalDate.of(2026, 4, 29));

        Department department = new Department();
        department.setCode("D001");
        department.setName("재무팀");

        AccountSubject paymentAccount = new AccountSubject();
        paymentAccount.setCode("PAY001");
        paymentAccount.setName("보통예금");

        when(departmentPersistencePort.findByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("PAY001")).thenReturn(Optional.of(paymentAccount));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "SALES")));

        assertThrows(IllegalArgumentException.class, () -> service.createResolution(request));
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
}
