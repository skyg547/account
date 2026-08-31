package com.ho.account.expenditure.adapter.in.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.application.service.BudgetService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetControlAdapterTest {

    @Mock
    private BudgetService budgetService;
    @Mock
    private BudgetPersistencePort budgetPersistencePort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;

    private BudgetControlAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BudgetControlAdapter(budgetService, budgetPersistencePort, masterDataQueryPort);
    }

    @Test
    void checkBudgetAvailabilityValidatesMasterDataAndDelegates() {
        when(masterDataQueryPort.findDepartment("D001"))
                .thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", "COST_CENTER")));
        when(masterDataQueryPort.findAccountSubject("EXP001"))
                .thenReturn(Optional.of(new AccountSubjectRef("EXP001", "수선비", false, false)));

        assertDoesNotThrow(() -> adapter.checkBudgetAvailability("202604", "D001", "EXP001", new BigDecimal("1000.00")));
        verify(budgetService).checkBudgetAvailability("202604", "D001", "EXP001", new BigDecimal("1000.00"));
    }

    @Test
    void checkBudgetAvailabilityThrowsWhenDepartmentNotFound() {
        when(masterDataQueryPort.findDepartment("D001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> adapter.checkBudgetAvailability("202604", "D001", "EXP001", new BigDecimal("1000.00")));
    }

    @Test
    void useBudgetValidatesMasterDataAndDelegates() {
        when(masterDataQueryPort.findDepartment("D001"))
                .thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", "COST_CENTER")));
        when(masterDataQueryPort.findAccountSubject("EXP001"))
                .thenReturn(Optional.of(new AccountSubjectRef("EXP001", "수선비", false, false)));

        adapter.useBudget("202604", "D001", "EXP001", new BigDecimal("1000.00"));
        verify(budgetService).useBudget("202604", "D001", "EXP001", new BigDecimal("1000.00"));
    }

    @Test
    void restoreBudgetDelegatesDirectly() {
        adapter.restoreBudget("202604", "D001", "EXP001", new BigDecimal("1000.00"));
        verify(budgetService).restoreBudget("202604", "D001", "EXP001", new BigDecimal("1000.00"));
    }
}
