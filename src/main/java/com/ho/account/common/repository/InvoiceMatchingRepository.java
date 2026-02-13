package com.ho.account.common.repository;

import com.ho.account.common.domain.InvoiceMatching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceMatchingRepository extends JpaRepository<InvoiceMatching, Long> {
    List<InvoiceMatching> findByMatchTypeAndInvoiceId(String matchType, Long invoiceId);

    List<InvoiceMatching> findByMatchTypeAndPaymentId(String matchType, Long paymentId);
}
