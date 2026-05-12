package com.ho.account.journalledger.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalPostingAdapterTest {

    @Mock
    private JournalUseCase journalUseCase;

    @Mock
    private AccountSubjectPersistencePort accountSubjectPersistencePort;

    @Mock
    private DepartmentPersistencePort departmentPersistencePort;

    @Mock
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;

    @Mock
    private CurrencyPersistencePort currencyPersistencePort;

    private JournalPostingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JournalPostingAdapter(
                journalUseCase,
                accountSubjectPersistencePort,
                departmentPersistencePort,
                businessPartnerPersistencePort,
                currencyPersistencePort
        );
    }

    @Test
    void createDraftEntryMapsCommandFieldsAndBaseAmount() {
        Currency currency = new Currency();
        currency.setCurrencyCode("KRW");
        AccountSubject debitAccount = accountSubject("131000");
        AccountSubject creditAccount = accountSubject("211000");

        when(currencyPersistencePort.findByCode("KRW")).thenReturn(Optional.of(currency));
        when(accountSubjectPersistencePort.findByCode("131000")).thenReturn(Optional.of(debitAccount));
        when(accountSubjectPersistencePort.findByCode("211000")).thenReturn(Optional.of(creditAccount));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(77L);
            entry.setSlipNo("JE-20260511-0001");
            entry.setStatus(JournalEntryStatus.DRAFT);
            return entry;
        });

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 11),
                "reconciliation adjustment",
                "ADJUSTMENT",
                "KRW",
                new BigDecimal("1.00"),
                "tester",
                "tester",
                "RECONCILIATION",
                "RECON_ADJ-1",
                List.of(
                        new JournalLineCommand("DEBIT", "131000", new BigDecimal("50.00"), new BigDecimal("50.00"), null, null, "debit"),
                        new JournalLineCommand("CREDIT", "211000", new BigDecimal("50.00"), new BigDecimal("50.00"), null, null, "credit")
                )
        );

        JournalPostingResult result = adapter.createDraftEntry(command);

        assertThat(result.journalEntryId()).isEqualTo(77L);
        assertThat(result.slipNo()).isEqualTo("JE-20260511-0001");
        assertThat(result.status()).isEqualTo("DRAFT");

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();

        assertThat(entry.getSlipDate()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(entry.getAccountingDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(entry.getEntryType()).isEqualTo("ADJUSTMENT");
        assertThat(entry.getExchangeRate()).isEqualByComparingTo("1.00");
        assertThat(entry.getCreatedBy()).isEqualTo("tester");
        assertThat(entry.getAuditUser()).isEqualTo("tester");
        assertThat(entry.getLineageSourceType()).isEqualTo("RECONCILIATION");
        assertThat(entry.getLineageSourceId()).isEqualTo("RECON_ADJ-1");
        assertThat(entry.getDetails()).hasSize(2);
        assertThat(entry.getDetails()).anySatisfy(detail -> {
            assertThat(detail.getSide()).isEqualTo(JournalSide.DEBIT);
            assertThat(detail.getAccountSubject()).isSameAs(debitAccount);
            assertThat(detail.getBaseAmount()).isEqualByComparingTo("50.00");
        });
        assertThat(entry.getDetails()).anySatisfy(detail -> {
            assertThat(detail.getSide()).isEqualTo(JournalSide.CREDIT);
            assertThat(detail.getAccountSubject()).isSameAs(creditAccount);
            assertThat(detail.getBaseAmount()).isEqualByComparingTo("50.00");
        });
    }

    private AccountSubject accountSubject(String code) {
        AccountSubject accountSubject = new AccountSubject();
        accountSubject.setCode(code);
        accountSubject.setName(code);
        return accountSubject;
    }
}
