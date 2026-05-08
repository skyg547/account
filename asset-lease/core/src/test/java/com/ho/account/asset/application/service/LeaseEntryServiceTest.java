package com.ho.account.asset.application.service;

import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseEntryServiceTest {

    @Mock
    private LeasePersistencePort persistencePort;
    @Mock
    private AssetEventPort eventPort;
    @Mock
    private LeasePaymentResolutionPort leasePaymentResolutionPort;

    @Test
    void processMonthlyLeasePaymentSplitsIfrs16PaymentIntoInterestAndPrincipal() {
        LeaseEntryService service = new LeaseEntryService(
                persistencePort,
                eventPort,
                leasePaymentResolutionPort);
        LeaseContract contract = createIfrs16LeaseContract();
        LeasePaymentSchedule schedule = createSchedule(contract);
        when(persistencePort.findActiveContracts("ACTIVE")).thenReturn(List.of(contract));
        when(persistencePort.findSchedulesByContract(contract)).thenReturn(List.of(schedule));

        service.processMonthlyLeasePayment(LocalDate.of(2026, 5, 25));

        ArgumentCaptor<LeasePaymentResolutionCommand> captor =
                ArgumentCaptor.forClass(LeasePaymentResolutionCommand.class);
        verify(leasePaymentResolutionPort).createLeasePaymentResolution(captor.capture());
        LeasePaymentResolutionCommand command = captor.getValue();
        assertEquals("21100", command.creditAccountCode());
        assertEquals(new BigDecimal("1200.00"), command.amount());
        assertEquals(2, command.debitLines().size());
        assertEquals("93100", command.debitLines().get(0).debitAccountCode());
        assertEquals(new BigDecimal("200.00"), command.debitLines().get(0).amount());
        assertEquals("25100", command.debitLines().get(1).debitAccountCode());
        assertEquals(new BigDecimal("1000.00"), command.debitLines().get(1).amount());
    }

    private LeaseContract createIfrs16LeaseContract() {
        LeaseContract contract = new LeaseContract();
        contract.setId(1L);
        contract.setContractNo("LC-2026-001");
        contract.setContractName("테스트 리스");
        contract.setLessorCode("V001");
        contract.setDepartmentCode("D001");
        contract.setExpenseAccountCode("51500");
        contract.setMonthlyPayment(new BigDecimal("1200.00"));
        contract.setPaymentDay(25);
        contract.setIfrs16Applicable(true);
        contract.setShortTermLease(false);
        contract.setLowValueLease(false);
        contract.setStatus("ACTIVE");
        return contract;
    }

    private LeasePaymentSchedule createSchedule(LeaseContract contract) {
        LeasePaymentSchedule schedule = new LeasePaymentSchedule();
        schedule.setLeaseContract(contract);
        schedule.setPaymentDate(LocalDate.of(2026, 5, 1));
        schedule.setScheduledPaymentAmount(new BigDecimal("1200.00"));
        schedule.setInterestPortion(new BigDecimal("200.00"));
        schedule.setPrincipalPortion(new BigDecimal("1000.00"));
        schedule.setRemainingLeaseLiability(new BigDecimal("9000.00"));
        schedule.setStatus("SCHEDULED");
        return schedule;
    }
}
