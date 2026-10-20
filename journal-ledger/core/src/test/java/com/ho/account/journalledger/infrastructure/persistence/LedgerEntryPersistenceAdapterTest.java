package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlEntryRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.SlEntryRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerEntryPersistenceAdapterTest {

    @Mock
    private GlEntryRepository glEntryRepository;
    @Mock
    private SlEntryRepository slEntryRepository;
    @Mock
    private EntityManager entityManager;

    private LedgerEntryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LedgerEntryPersistenceAdapter(
                glEntryRepository,
                slEntryRepository,
                entityManager);
    }

    @Test
    @DisplayName("JPA adapter는 한 Aggregate의 같은 posting 값으로 GL과 SL 엔티티를 만든다.")
    void mapsSameAggregateSnapshotToGlAndSl() {
        JournalDetail debitReference = new JournalDetail();
        debitReference.setId(11L);
        JournalDetail creditReference = new JournalDetail();
        creditReference.setId(12L);
        when(entityManager.getReference(JournalDetail.class, 11L)).thenReturn(debitReference);
        when(entityManager.getReference(JournalDetail.class, 12L)).thenReturn(creditReference);

        adapter.save(generalLedger());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GlEntry>> glCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SlEntry>> slCaptor = ArgumentCaptor.forClass(List.class);
        verify(glEntryRepository).saveAll(glCaptor.capture());
        verify(slEntryRepository).saveAll(slCaptor.capture());

        assertThat(glCaptor.getValue()).hasSize(2);
        assertThat(slCaptor.getValue()).hasSize(2);
        GlEntry glDebit = glCaptor.getValue().get(0);
        SlEntry slDebit = slCaptor.getValue().get(0);
        assertThat(glDebit.getDrAmount()).isEqualByComparingTo(slDebit.getDrAmount());
        assertThat(glDebit.getBaseDrAmount()).isEqualByComparingTo(slDebit.getBaseDrAmount());
        assertThat(glDebit.getJournalDetail()).isSameAs(debitReference);
        assertThat(slDebit.getBusinessPartnerCode()).isEqualTo("BP-001");
    }

    private GeneralLedger generalLedger() {
        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260730-0044");
        entry.setSlipDate(LocalDate.of(2026, 7, 30));
        entry.setAccountingDate(LocalDate.of(2026, 7, 30));
        entry.setCurrencyCode("KRW");
        entry.setLineageSourceType("UNIT_TEST");
        entry.setLineageSourceId("SRC-44");
        entry.setCreatedBy("maker");
        entry.addDetail(detail(11L, JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(12L, JournalSide.CREDIT, "40100"));
        entry.initializeDraft();
        entry.requestApproval("maker");
        entry.approve("approver");
        return GeneralLedger.fromApproved(entry);
    }

    private JournalDetail detail(Long id, JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setId(id);
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        detail.setBusinessPartnerCode("BP-001");
        detail.setDepartmentCode("D-10");
        return detail;
    }
}
