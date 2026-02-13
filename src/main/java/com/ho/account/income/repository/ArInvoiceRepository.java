package com.ho.account.income.repository;

import com.ho.account.income.domain.ArInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArInvoiceRepository extends JpaRepository<ArInvoice, Long> {
    Optional<ArInvoice> findByInvoiceNo(String invoiceNo);
}
