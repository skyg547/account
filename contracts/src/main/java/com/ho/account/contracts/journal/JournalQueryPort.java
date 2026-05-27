package com.ho.account.contracts.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [JournalQueryPort]
 * 전표 데이터를 조회하기 위한 인터페이스.
 */
public interface JournalQueryPort {
    
    /**
     * 특정 기간의 전표 요약 정보를 조회합니다.
     */
    List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate);
    
    /**
     * 특정 전표의 상세 내역을 조회합니다.
     */
    List<JournalDetailSummary> getJournalDetails(Long journalEntryId);

    /**
     * 특정 기간의 전표 상세를 DB 집계 기준으로 요약합니다.
     */
    JournalDetailAggregateSummary getJournalDetailAggregate(LocalDate startDate, LocalDate endDate, JournalSide side);

    /**
     * 특정 기간과 계정코드에 해당하는 전표 상세를 DB 집계 기준으로 요약합니다.
     */
    JournalDetailAggregateSummary getJournalDetailAggregateByAccount(LocalDate startDate, LocalDate endDate, JournalSide side, String accountCode);

    /**
     * 특정 전표의 요약 정보를 조회합니다.
     */
    JournalSummary getJournalSummary(Long journalEntryId);
}
