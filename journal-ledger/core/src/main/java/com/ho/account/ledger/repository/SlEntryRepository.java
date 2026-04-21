package com.ho.account.ledger.repository;

import com.ho.account.ledger.domain.SlEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlEntryRepository extends JpaRepository<SlEntry, Long> {

    List<SlEntry> findByBusinessPartner_BusinessPartnerCodeAndPostingDateBetween(String bpCode, LocalDate startDate, LocalDate endDate);
    
    List<SlEntry> findByBusinessPartner_BusinessPartnerCodeAndAccount_CodeAndPostingDateBetween(String bpCode, String accountCode, LocalDate startDate, LocalDate endDate);
    
    List<SlEntry> findByLineageSourceTypeAndLineageSourceId(String sourceType, String sourceId);

    List<SlEntry> findByAccount_CodeAndPostingDateBetween(String accountCode, LocalDate startDate, LocalDate endDate);
}
