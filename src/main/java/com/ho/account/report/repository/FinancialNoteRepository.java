package com.ho.account.report.repository;

import com.ho.account.report.domain.FinancialNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialNoteRepository extends JpaRepository<FinancialNote, Long> {
    List<FinancialNote> findByYearMonth(String yearMonth);
}
