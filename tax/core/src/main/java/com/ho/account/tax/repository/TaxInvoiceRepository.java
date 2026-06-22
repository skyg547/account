package com.ho.account.tax.repository;

import com.ho.account.tax.domain.TaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import java.util.Optional; // Optional 임포트

@Repository
public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {
    List<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<TaxInvoice> findByIssueId(String issueId); // 이 메서드 추가
}
