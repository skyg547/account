package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.out.AdvancePaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.PaymentExecutionPort;
import com.ho.account.expenditure.application.port.out.PaymentRunPersistencePort;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentPersistencePort paymentPersistencePort;
    @Mock
    private PayablePersistencePort payablePersistencePort;
    @Mock
    private PaymentRunPersistencePort paymentRunPersistencePort;
    @Mock
    private AdvancePaymentPersistencePort advancePaymentPersistencePort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private PayableAccountMappingPort payableAccountMappingPort;
    @Mock
    private PaymentExecutionPort paymentExecutionPort;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(
                paymentPersistencePort,
                payablePersistencePort,
                paymentRunPersistencePort,
                advancePaymentPersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                payableAccountMappingPort,
                paymentExecutionPort);
    }

    @Test
    @DisplayName("지급 전표 생성 시 지급 계정 매핑과 지급런 actor를 반영한다")
    void executePaymentUsesMappedAccountsAndRunActor() {
        Payment payment = payment();
        Payable payable = payable();

        when(paymentPersistencePort.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentExecutionPort.execute(any())).thenReturn(
                PaymentExecutionPort.PaymentExecutionResult.completed("BANK-REF-10"));
        when(paymentPersistencePort.save(payment)).thenReturn(payment);
        when(payablePersistencePort.findById(100L)).thenReturn(Optional.of(payable));
        when(payablePersistencePort.save(payable)).thenReturn(payable);
        when(payableAccountMappingPort.resolvePaymentExecutionAccounts(payment))
                .thenReturn(new PayableAccountMappingPort.PaymentExecutionAccounts("AP-002", "CASH-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(20L, "SLIP-2", "DRAFT"));

        service.executePayment(10L, "BANK-001");

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());
        JournalEntryCommand command = commandCaptor.getValue();

        assertThat(command.createdBy()).isEqualTo("payment-user");
        assertThat(command.auditUser()).isEqualTo("payment-user");
        assertThat(command.lines()).extracting("accountCode")
                .containsExactly("AP-002", "CASH-002");
        assertThat(payment.getReferenceNo()).isEqualTo("BANK-REF-10");
        assertThat(payment.getExecutionAttempts()).isEqualTo(1);
        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("외부 지급 실패 시 채무를 차감하지 않고 실패 사유를 보존한다")
    void executePaymentPreservesFailureWithoutSettlingPayable() {
        Payment payment = payment();
        when(paymentPersistencePort.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentExecutionPort.execute(any())).thenReturn(
                PaymentExecutionPort.PaymentExecutionResult.failed("BANK_TIMEOUT"));
        when(paymentPersistencePort.save(payment)).thenReturn(payment);

        Payment result = service.executePayment(10L, "BANK-001");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("BANK_TIMEOUT");
        assertThat(result.getExecutionAttempts()).isEqualTo(1);
        verify(paymentPersistencePort).save(payment);
    }

    @Test
    @DisplayName("선급금 전표 생성 시 선급금 계정 매핑을 반영한다")
    void recordAdvancePaymentUsesMappedAccounts() {
        AdvancePayment advance = advancePayment();

        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(advancePaymentPersistencePort.save(advance)).thenReturn(advance);
        when(payableAccountMappingPort.resolveAdvancePaymentAccounts(advance))
                .thenReturn(new PayableAccountMappingPort.AdvancePaymentAccounts("ADV-003", "CASH-003"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(30L, "SLIP-3", "DRAFT"));

        service.recordAdvancePayment(advance);

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("ADV-003", "CASH-003");
    }

    @Test
    @DisplayName("채무와 선급금 상계 전표 생성 시 상계 계정 매핑을 반영한다")
    void offsetPayableWithAdvancePaymentUsesMappedAccounts() {
        Payable payable = payable();
        AdvancePayment advance = advancePayment();

        when(payablePersistencePort.findById(100L)).thenReturn(Optional.of(payable));
        when(advancePaymentPersistencePort.findById(200L)).thenReturn(Optional.of(advance));
        when(payablePersistencePort.save(payable)).thenReturn(payable);
        when(advancePaymentPersistencePort.save(advance)).thenReturn(advance);
        when(payableAccountMappingPort.resolveAdvanceOffsetAccounts(payable))
                .thenReturn(new PayableAccountMappingPort.AdvanceOffsetAccounts("AP-004", "ADV-004"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(40L, "SLIP-4", "DRAFT"));

        service.offsetPayableWithAdvancePayment(100L, 200L, new BigDecimal("100.00"));

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("AP-004", "ADV-004");
    }

    private Payment payment() {
        PaymentRun run = new PaymentRun();
        run.setCreatedBy("payment-user");

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setPaymentDate(LocalDate.of(2026, 5, 29));
        payment.setVendorCode("V001");
        payment.setPayableId(100L);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setPaymentRun(run);
        return payment;
    }

    private Payable payable() {
        Payable payable = new Payable();
        payable.setId(100L);
        payable.setPurchaseInvoiceNo("PI-001");
        payable.setPurchaseInvoiceVendorCode("V001");
        payable.setVendorCode("V001");
        payable.setOriginalAmount(new BigDecimal("500.00"));
        payable.setOutstandingAmount(new BigDecimal("500.00"));
        payable.setDueDate(LocalDate.of(2026, 6, 30));
        payable.setStatus(PayableStatus.OPEN);
        return payable;
    }

    private AdvancePayment advancePayment() {
        AdvancePayment advance = new AdvancePayment();
        advance.setId(200L);
        advance.setVendorCode("V001");
        advance.setPaymentDate(LocalDate.of(2026, 5, 29));
        advance.setAmount(new BigDecimal("200.00"));
        advance.setOutstandingAmount(new BigDecimal("200.00"));
        return advance;
    }

}
