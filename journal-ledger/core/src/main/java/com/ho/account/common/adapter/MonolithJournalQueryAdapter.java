package com.ho.account.common.adapter;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * [MonolithJournalQueryAdapter]
 * 모놀리스 환경에서 Journal Ledger 모듈의 데이터를 조회하기 위한 어댑터.
 */
@Component
@RequiredArgsConstructor
public class MonolithJournalQueryAdapter implements JournalQueryPort {

    private final JournalUseCase journalUseCase;
    private final JournalDetailRepository journalDetailRepository;
    private final MasterDataQueryPort masterDataQueryPort;

    @Override
    public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
        return journalUseCase.getJournalEntriesByDate(startDate, endDate).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
        JournalEntry entry = journalUseCase.getJournalEntryWithDetails(journalEntryId)
                .orElseThrow(() -> new NoSuchElementException("Journal entry not found: " + journalEntryId));
        return entry.getDetails().stream()
                .map(this::mapToDetailSummary)
                .collect(Collectors.toList());
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregate(LocalDate startDate, LocalDate endDate,
            JournalSide side) {
        JournalDetailRepository.JournalDetailAggregateProjection projection =
                journalDetailRepository.summarizeByAccountingDateBetweenAndSide(startDate, endDate, mapJournalSide(side));
        if (projection == null) {
            return new JournalDetailAggregateSummary(0, BigDecimal.ZERO);
        }
        Long detailCount = projection.getDetailCount();
        BigDecimal totalAmount = projection.getTotalAmount();
        return new JournalDetailAggregateSummary(
                detailCount == null ? 0L : detailCount,
                totalAmount == null ? BigDecimal.ZERO : totalAmount);
    }

    @Override
    public JournalSummary getJournalSummary(Long journalEntryId) {
        return journalUseCase.getJournalEntry(journalEntryId)
                .map(this::mapToSummary)
                .orElseThrow(() -> new NoSuchElementException("Journal entry not found: " + journalEntryId));
    }

    private JournalSummary mapToSummary(JournalEntry entry) {
        JournalSummary summary = new JournalSummary();
        summary.setId(entry.getId());
        summary.setSlipNo(entry.getSlipNo());
        summary.setAccountingDate(entry.getAccountingDate());
        summary.setDescription(entry.getDescription());
        summary.setEntryType(entry.getEntryType());
        summary.setStatus(entry.getStatus().name());
        return summary;
    }

    private JournalDetailSummary mapToDetailSummary(JournalDetail detail) {
        JournalDetailSummary summary = new JournalDetailSummary();
        summary.setId(detail.getId());
        summary.setSide(detail.getSide() == com.ho.account.journalledger.domain.journal.domain.JournalSide.DEBIT ? JournalSide.DEBIT : JournalSide.CREDIT);
        
        String accountCode = detail.getAccountCode();
        summary.setAccountCode(accountCode);
        
        masterDataQueryPort.findAccountSubject(accountCode).ifPresent(acc -> {
            summary.setAccountName(acc.name());
        });

        if (detail.getJournalEntry() != null) {
            summary.setAccountingDate(detail.getJournalEntry().getAccountingDate());
        }
        summary.setAmount(detail.getAmount());
        summary.setBaseAmount(detail.getBaseAmount());
        return summary;
    }

    private com.ho.account.journalledger.domain.journal.domain.JournalSide mapJournalSide(JournalSide side) {
        if (side == null) {
            throw new IllegalArgumentException("Journal side must not be null.");
        }
        return side == JournalSide.DEBIT
                ? com.ho.account.journalledger.domain.journal.domain.JournalSide.DEBIT
                : com.ho.account.journalledger.domain.journal.domain.JournalSide.CREDIT;
    }
}