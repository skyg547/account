package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlEntryRepository extends JpaRepository<SlEntry, Long> {

    List<SlEntry> findByPostingDateBetween(LocalDate startDate, LocalDate endDate);

    List<SlEntry> findByBusinessPartnerCodeAndPostingDateBetween(String bpCode, LocalDate startDate, LocalDate endDate);

    List<SlEntry> findByBusinessPartnerCodeAndAccountCodeAndPostingDateBetween(String bpCode, String accountCode, LocalDate startDate, LocalDate endDate);
}