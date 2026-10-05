package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.AdvancePaymentCommand;
import com.ho.account.expenditure.application.port.in.ExecutePaymentCommand;
import com.ho.account.expenditure.application.port.in.OffsetPayableCommand;
import com.ho.account.expenditure.application.port.in.PaymentRunCommand;
import com.ho.account.expenditure.application.port.out.AdvancePaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.PaymentExecutionPort;
import com.ho.account.expenditure.application.port.out.PaymentRunPersistencePort;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.domain.PaymentRunStatus;
import com.ho.account.expenditure.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private AccountingPeriodStatusPort accountingPeriodStatusPort;

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
                paymentExecutionPort,
                accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("지급 전표 생성 시 지급 계정 매핑과 지급런 actor를 반영한다")
    void executePaymentUsesMappedAccountsAndRunActor() {
        Payment payment = payment();
        Payable payable = payable();

        when(paymentPersistencePort.findById(10L)).thenReturn(Optional.of(payment));
        when(accountingPeriodStatusPort.isClosed(payment.getPaymentDate())).thenReturn(false);
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

        service.executePayment(new ExecutePaymentCommand(10L, "BANK-001"));

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
        when(accountingPeriodStatusPort.isClosed(payment.getPaymentDate())).thenReturn(false);
        when(paymentExecutionPort.execute(any())).thenReturn(
                PaymentExecutionPort.PaymentExecutionResult.failed("BANK_TIMEOUT"));
        when(paymentPersistencePort.save(payment)).thenReturn(payment);

        Payment result = service.executePayment(new ExecutePaymentCommand(10L, "BANK-001"));

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("BANK_TIMEOUT");
        assertThat(result.getExecutionAttempts()).isEqualTo(1);
        verify(paymentPersistencePort).save(payment);
    }

    @Test
    @DisplayName("선급금 전표 생성 시 선급금 계정 매핑을 반영한다")
    void recordAdvancePaymentUsesMappedAccounts() {
        AdvancePayment advance = advancePayment();

        when(accountingPeriodStatusPort.isClosed(LocalDate.of(2026, 5, 29))).thenReturn(false);
        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(advancePaymentPersistencePort.save(any(AdvancePayment.class))).thenAnswer(invocation -> {
            AdvancePayment saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        when(payableAccountMappingPort.resolveAdvancePaymentAccounts(any(AdvancePayment.class)))
                .thenReturn(new PayableAccountMappingPort.AdvancePaymentAccounts("ADV-003", "CASH-003"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(30L, "SLIP-3", "DRAFT"));

        service.recordAdvancePayment(new AdvancePaymentCommand("V001", LocalDate.of(2026, 5, 29), new BigDecimal("200.00"), null));

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("ADV-003", "CASH-003");
    }

    @Test
    @DisplayName("동일 공급업체의 부분 상계는 잔액과 상태를 갱신하고 동일 금액의 상계 전표를 생성한다")
    void offsetPayableWithAdvancePaymentUsesMappedAccounts() {
        Payable payable = payable();
        AdvancePayment advance = advancePayment();

        when(accountingPeriodStatusPort.isClosed(any(LocalDate.class))).thenReturn(false);
        when(payablePersistencePort.findByIdForUpdate(100L)).thenReturn(Optional.of(payable));
        when(advancePaymentPersistencePort.findById(200L)).thenReturn(Optional.of(advance));
        when(payablePersistencePort.save(payable)).thenReturn(payable);
        when(advancePaymentPersistencePort.save(any(AdvancePayment.class))).thenAnswer(invocation -> {
            AdvancePayment saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        when(payableAccountMappingPort.resolveAdvanceOffsetAccounts(payable))
                .thenReturn(new PayableAccountMappingPort.AdvanceOffsetAccounts("AP-004", "ADV-004"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(40L, "SLIP-4", "DRAFT"));

        Payable result = service.offsetPayableWithAdvancePayment(
                new OffsetPayableCommand(100L, 200L, new BigDecimal("100.00")));

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(result).isSameAs(payable);
        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo("400.00");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.PARTIAL_PAID);
        assertThat(advance.getOutstandingAmount()).isEqualByComparingTo("100.00");
        assertThat(advance.getStatus()).isEqualTo(AdvancePaymentStatus.ACTIVE);
        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("AP-004", "ADV-004");
        assertThat(commandCaptor.getValue().lines()).extracting("amount")
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(commandCaptor.getValue().lines()).extracting("businessPartnerCode")
                .containsExactly("V001", "V001");
        verify(payablePersistencePort).findByIdForUpdate(100L);
    }

    @Test
    @DisplayName("다른 공급업체의 선급금은 채무 상계 전에 거부하고 잔액, 상태, 전표를 그대로 둔다")
    void offsetPayableWithAdvancePaymentRejectsDifferentVendorBeforeMutation() {
        Payable payable = payable();
        AdvancePayment advance = advancePayment();
        advance.setVendorCode("V002");

        when(payablePersistencePort.findByIdForUpdate(100L)).thenReturn(Optional.of(payable));
        when(advancePaymentPersistencePort.findById(200L)).thenReturn(Optional.of(advance));

        assertThatThrownBy(() -> service.offsetPayableWithAdvancePayment(
                new OffsetPayableCommand(100L, 200L, new BigDecimal("100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공급업체 코드가 일치하지 않습니다");

        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo("500.00");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
        assertThat(advance.getOutstandingAmount()).isEqualByComparingTo("200.00");
        assertThat(advance.getStatus()).isEqualTo(AdvancePaymentStatus.ACTIVE);
        verify(payablePersistencePort, org.mockito.Mockito.never()).save(any(Payable.class));
        verify(advancePaymentPersistencePort, org.mockito.Mockito.never()).save(any(AdvancePayment.class));
        verifyNoInteractions(payableAccountMappingPort, journalPostingPort);
    }

    @Test
    @DisplayName("공급업체 코드가 양쪽 모두 공백이면 상계 없이 거부한다")
    void offsetPayableWithAdvancePaymentRejectsBlankVendorCodes() {
        Payable payable = payable();
        AdvancePayment advance = advancePayment();
        payable.setVendorCode("  ");
        advance.setVendorCode("  ");

        when(payablePersistencePort.findByIdForUpdate(100L)).thenReturn(Optional.of(payable));
        when(advancePaymentPersistencePort.findById(200L)).thenReturn(Optional.of(advance));

        assertThatThrownBy(() -> service.offsetPayableWithAdvancePayment(
                new OffsetPayableCommand(100L, 200L, new BigDecimal("100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공급업체 코드가 일치하지 않습니다");

        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo("500.00");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
        assertThat(advance.getOutstandingAmount()).isEqualByComparingTo("200.00");
        assertThat(advance.getStatus()).isEqualTo(AdvancePaymentStatus.ACTIVE);
        verify(payablePersistencePort, org.mockito.Mockito.never()).save(any(Payable.class));
        verify(advancePaymentPersistencePort, org.mockito.Mockito.never()).save(any(AdvancePayment.class));
        verifyNoInteractions(payableAccountMappingPort, journalPostingPort);
    }

    @Test
    @DisplayName("동일 공급업체의 전액 상계는 양쪽 잔액을 0으로 만들고 전액의 상계 전표를 생성한다")
    void offsetPayableWithAdvancePaymentFullySettlesSameVendor() {
        Payable payable = payable();
        AdvancePayment advance = advancePayment();
        advance.setAmount(new BigDecimal("500.00"));
        advance.setOutstandingAmount(new BigDecimal("500.00"));

        when(payablePersistencePort.findByIdForUpdate(100L)).thenReturn(Optional.of(payable));
        when(advancePaymentPersistencePort.findById(200L)).thenReturn(Optional.of(advance));
        when(payableAccountMappingPort.resolveAdvanceOffsetAccounts(payable))
                .thenReturn(new PayableAccountMappingPort.AdvanceOffsetAccounts("AP-004", "ADV-004"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));

        service.offsetPayableWithAdvancePayment(
                new OffsetPayableCommand(100L, 200L, new BigDecimal("500.00")));

        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo("0");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.PAID);
        assertThat(advance.getOutstandingAmount()).isEqualByComparingTo("0");
        assertThat(advance.getStatus()).isEqualTo(AdvancePaymentStatus.OFFSET);
        verify(payablePersistencePort).save(payable);
        verify(advancePaymentPersistencePort).save(advance);
        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());
        assertThat(commandCaptor.getValue().lines()).extracting("amount")
                .containsExactly(new BigDecimal("500.00"), new BigDecimal("500.00"));
        assertThat(commandCaptor.getValue().lines()).extracting("businessPartnerCode")
                .containsExactly("V001", "V001");
    }

    @Test
    @DisplayName("마감된 회계기간에 대한 지급 실행 시 IllegalStateException이 발생한다")
    void executePaymentThrowsExceptionWhenAccountingPeriodClosed() {
        Payment payment = payment();
        when(paymentPersistencePort.findById(10L)).thenReturn(Optional.of(payment));
        when(accountingPeriodStatusPort.isClosed(payment.getPaymentDate())).thenReturn(true);

        assertThatThrownBy(() -> service.executePayment(new ExecutePaymentCommand(10L, "BANK-001")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간입니다");
    }

    @Test
    @DisplayName("마감된 회계기간에 대한 선급금 등록 시 IllegalStateException이 발생한다")
    void recordAdvancePaymentThrowsExceptionWhenAccountingPeriodClosed() {
        LocalDate closedDate = LocalDate.of(2026, 5, 29);
        when(accountingPeriodStatusPort.isClosed(closedDate)).thenReturn(true);

        assertThatThrownBy(() -> service.recordAdvancePayment(
                new AdvancePaymentCommand("V001", closedDate, new BigDecimal("200.00"), null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간입니다");
    }

    @Test
    @DisplayName("차변과 대변 합계가 일치하지 않는 전표 검증 시 IllegalArgumentException이 발생한다")
    void validateJournalBalanceThrowsExceptionWhenImbalanced() {
        JournalEntryCommand imbalancedCommand = new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Imbalanced Entry",
                "TEST",
                null, null, "user", "user",
                "TEST", "1",
                List.of(
                        new com.ho.account.contracts.journal.JournalLineCommand("DEBIT", "10100", new BigDecimal("100.00"), null, null, "V001", "Debit"),
                        new com.ho.account.contracts.journal.JournalLineCommand("CREDIT", "20100", new BigDecimal("90.00"), null, null, "V001", "Credit")));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service, "validateJournalBalance", imbalancedCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차변 합계(100.00)와 대변 합계(90.00)가 일치하지 않습니다");
    }

    @Test
    @DisplayName("initiatePaymentRun 호출 시 적격 채무 claim 성공 후 Payment를 생성한다")
    void initiatePaymentRunClaimsPayablesAndCreatesPayments() {
        LocalDate runDate = LocalDate.of(2026, 6, 30);
        PaymentRunCommand command = new PaymentRunCommand(runDate, "Batch Payment Run", "admin-user");

        when(accountingPeriodStatusPort.isClosed(runDate)).thenReturn(false);
        when(paymentRunPersistencePort.findByRunDateAndDescriptionAndCreatedBy(runDate, "Batch Payment Run", "admin-user"))
                .thenReturn(Optional.empty());

        PaymentRun savedRun = new PaymentRun();
        savedRun.setId(50L);
        savedRun.setRunDate(runDate);
        savedRun.setDescription("Batch Payment Run");
        savedRun.setCreatedBy("admin-user");
        savedRun.setStatus(PaymentRunStatus.INITIATED);
        when(paymentRunPersistencePort.save(any(PaymentRun.class))).thenReturn(savedRun);
        when(paymentRunPersistencePort.findById(50L)).thenReturn(Optional.of(savedRun));

        Payable payable = payable();
        payable.setId(101L);
        payable.setStatus(PayableStatus.OPEN);
        Payable claimedPayable = payable();
        claimedPayable.setId(101L);
        claimedPayable.setStatus(PayableStatus.IN_PAYMENT);

        when(payablePersistencePort.findByDueDateBeforeAndStatusNot(runDate.plusDays(1), PayableStatus.PAID))
                .thenReturn(List.of(payable));
        when(payablePersistencePort.findById(101L))
                .thenReturn(Optional.of(payable), Optional.of(claimedPayable));
        when(payablePersistencePort.claimForPayment(101L)).thenReturn(1);
        when(paymentPersistencePort.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRun result = service.initiatePaymentRun(command);

        assertThat(result).isNotNull();
        verify(payablePersistencePort).claimForPayment(101L);
        verify(payablePersistencePort, never()).save(payable);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentPersistencePort).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getPayableId()).isEqualTo(101L);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo("500.00");
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(savedPayment.getPaymentRun().getId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("서로 다른 지급 런이 같은 OPEN 채무를 읽어도 claim 실패한 런은 Payment를 만들지 않는다")
    void processPaymentRunChunkSkipsDuplicateWhenSecondClaimFails() {
        LocalDate runDate = LocalDate.of(2026, 6, 30);
        PaymentRun firstRun = new PaymentRun();
        firstRun.setId(50L);
        PaymentRun secondRun = new PaymentRun();
        secondRun.setId(51L);

        // 각 런이 DB 갱신 전 읽은 별도의 OPEN 스냅샷을 가진 상황을 재현한다.
        Payable firstSnapshot = payable();
        Payable claimedSnapshot = payable();
        claimedSnapshot.setStatus(PayableStatus.IN_PAYMENT);
        claimedSnapshot.setOutstandingAmount(new BigDecimal("450.00"));
        Payable secondSnapshot = payable();
        when(paymentRunPersistencePort.findById(50L)).thenReturn(Optional.of(firstRun));
        when(paymentRunPersistencePort.findById(51L)).thenReturn(Optional.of(secondRun));
        when(payablePersistencePort.findById(100L))
                .thenReturn(Optional.of(firstSnapshot), Optional.of(claimedSnapshot), Optional.of(secondSnapshot));
        when(payablePersistencePort.claimForPayment(100L)).thenReturn(1, 0);
        when(paymentPersistencePort.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processPaymentRunChunk(50L, runDate, List.of(100L));
        service.processPaymentRunChunk(51L, runDate, List.of(100L));

        verify(payablePersistencePort, times(2)).claimForPayment(100L);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentPersistencePort).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPayableId()).isEqualTo(100L);
        assertThat(paymentCaptor.getValue().getPaymentRun()).isSameAs(firstRun);
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("450.00");
        assertThat(secondSnapshot.getOutstandingAmount()).isEqualByComparingTo("500.00");
        verify(payablePersistencePort, never()).save(any(Payable.class));
        verify(paymentExecutionPort, never()).execute(any());
    }

    @Test
    @DisplayName("지급 claim 후 상계는 잔액과 전표를 변경하지 않고 두 번째 지급 claim도 중복 결제를 만들지 않는다")
    void offsetCannotChangeClaimedPayableBeforeSecondPaymentRun() {
        LocalDate runDate = LocalDate.of(2026, 6, 30);
        PaymentRun firstRun = new PaymentRun();
        firstRun.setId(50L);
        PaymentRun secondRun = new PaymentRun();
        secondRun.setId(51L);
        Payable firstSnapshot = payable();
        Payable claimedPayable = payable();
        claimedPayable.setStatus(PayableStatus.IN_PAYMENT);
        Payable staleSecondSnapshot = payable();

        when(paymentRunPersistencePort.findById(50L)).thenReturn(Optional.of(firstRun));
        when(paymentRunPersistencePort.findById(51L)).thenReturn(Optional.of(secondRun));
        when(payablePersistencePort.findById(100L)).thenReturn(
                Optional.of(firstSnapshot), Optional.of(claimedPayable), Optional.of(staleSecondSnapshot));
        when(payablePersistencePort.findByIdForUpdate(100L)).thenReturn(Optional.of(claimedPayable));
        when(payablePersistencePort.claimForPayment(100L)).thenReturn(1, 0);
        when(paymentPersistencePort.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountingPeriodStatusPort.isClosed(any(LocalDate.class))).thenReturn(false);
        service.processPaymentRunChunk(50L, runDate, List.of(100L));
        assertThatThrownBy(() -> service.offsetPayableWithAdvancePayment(
                new OffsetPayableCommand(100L, 200L, new BigDecimal("100.00"))))
                .isInstanceOf(IllegalStateException.class);
        service.processPaymentRunChunk(51L, runDate, List.of(100L));

        verify(payablePersistencePort).findByIdForUpdate(100L);
        verify(payablePersistencePort, times(2)).claimForPayment(100L);
        verify(payablePersistencePort, never()).save(any(Payable.class));
        verify(advancePaymentPersistencePort, never()).findById(200L);
        verify(advancePaymentPersistencePort, never()).save(any(AdvancePayment.class));
        verify(journalPostingPort, never()).createDraftEntry(any());
        verify(paymentExecutionPort, never()).execute(any());
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentPersistencePort).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("500.00");
        assertThat(paymentCaptor.getValue().getPaymentRun()).isSameAs(firstRun);
        assertThat(claimedPayable.getStatus()).isEqualTo(PayableStatus.IN_PAYMENT);
        assertThat(claimedPayable.getOutstandingAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("동일한 파라미터로 initiatePaymentRun 반복 호출 시 기존 PaymentRun을 반환하고 중복 결제/전표 생성을 방지한다")
    void initiatePaymentRunIdempotencyPreventsDuplicatePayments() {
        LocalDate runDate = LocalDate.of(2026, 6, 30);
        PaymentRunCommand command = new PaymentRunCommand(runDate, "Batch Payment Run", "admin-user");

        PaymentRun existingRun = new PaymentRun();
        existingRun.setId(50L);
        existingRun.setRunDate(runDate);
        existingRun.setDescription("Batch Payment Run");
        existingRun.setCreatedBy("admin-user");
        existingRun.setStatus(PaymentRunStatus.PROCESSING);

        when(accountingPeriodStatusPort.isClosed(runDate)).thenReturn(false);
        when(paymentRunPersistencePort.findByRunDateAndDescriptionAndCreatedBy(runDate, "Batch Payment Run", "admin-user"))
                .thenReturn(Optional.of(existingRun));

        PaymentRun result = service.initiatePaymentRun(command);

        assertThat(result).isSameAs(existingRun);
        verify(paymentPersistencePort, org.mockito.Mockito.never()).save(any(Payment.class));
        verify(payablePersistencePort, org.mockito.Mockito.never()).save(any(Payable.class));
    }

    @Test
    @DisplayName("이미 IN_PAYMENT 또는 PAID 상태인 채무는 지급 런 대상에서 제외된다")
    void initiatePaymentRunFiltersOutInPaymentAndPaidPayables() {
        LocalDate runDate = LocalDate.of(2026, 6, 30);
        PaymentRunCommand command = new PaymentRunCommand(runDate, "Batch Payment Run 2", "admin-user");

        when(accountingPeriodStatusPort.isClosed(runDate)).thenReturn(false);
        when(paymentRunPersistencePort.findByRunDateAndDescriptionAndCreatedBy(runDate, "Batch Payment Run 2", "admin-user"))
                .thenReturn(Optional.empty());

        PaymentRun savedRun = new PaymentRun();
        savedRun.setId(51L);
        savedRun.setRunDate(runDate);
        savedRun.setStatus(PaymentRunStatus.INITIATED);
        when(paymentRunPersistencePort.save(any(PaymentRun.class))).thenReturn(savedRun);
        when(paymentRunPersistencePort.findById(51L)).thenReturn(Optional.of(savedRun));

        Payable inPaymentPayable = payable();
        inPaymentPayable.setId(102L);
        inPaymentPayable.setStatus(PayableStatus.IN_PAYMENT);

        Payable paidPayable = payable();
        paidPayable.setId(103L);
        paidPayable.setStatus(PayableStatus.PAID);
        paidPayable.setOutstandingAmount(BigDecimal.ZERO);

        Payable eligiblePayable = payable();
        eligiblePayable.setId(104L);
        eligiblePayable.setStatus(PayableStatus.APPROVED);
        Payable claimedPayable = payable();
        claimedPayable.setId(104L);
        claimedPayable.setStatus(PayableStatus.IN_PAYMENT);

        when(payablePersistencePort.findByDueDateBeforeAndStatusNot(runDate.plusDays(1), PayableStatus.PAID))
                .thenReturn(List.of(inPaymentPayable, paidPayable, eligiblePayable));
        when(payablePersistencePort.findById(104L))
                .thenReturn(Optional.of(eligiblePayable), Optional.of(claimedPayable));
        when(payablePersistencePort.claimForPayment(104L)).thenReturn(1);
        when(paymentPersistencePort.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRun result = service.initiatePaymentRun(command);

        assertThat(result).isNotNull();
        verify(payablePersistencePort).claimForPayment(104L);
        verify(payablePersistencePort, never()).claimForPayment(102L);
        verify(payablePersistencePort, never()).claimForPayment(103L);

        // 104L만 처리되고 102L, 103L은 제외됨
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentPersistencePort, org.mockito.Mockito.times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPayableId()).isEqualTo(104L);
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
