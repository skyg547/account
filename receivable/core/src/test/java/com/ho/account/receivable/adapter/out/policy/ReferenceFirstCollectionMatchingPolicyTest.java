package com.ho.account.receivable.adapter.out.policy;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import com.ho.account.receivable.domain.SalesInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceFirstCollectionMatchingPolicyTest {

    private final ReferenceFirstCollectionMatchingPolicy policy = new ReferenceFirstCollectionMatchingPolicy();

    @Test
    void choosesUniqueReferenceMatchBeforeAmountCandidates() {
        Collection collection = collection("INV-2");
        Receivable first = receivable(1L, "INV-1", LocalDate.of(2026, 6, 9));
        Receivable second = receivable(2L, "INV-2", LocalDate.of(2026, 7, 31));

        assertThat(policy.selectMatch(collection, List.of(first, second))).contains(second);
    }

    @Test
    void failsClosedWhenMultipleCandidatesHaveSameConfidence() {
        Collection collection = collection(null);
        Receivable first = receivable(1L, "INV-1", LocalDate.of(2026, 6, 9));
        Receivable second = receivable(2L, "INV-2", LocalDate.of(2026, 6, 9));

        assertThat(policy.selectMatch(collection, List.of(first, second))).isEmpty();
    }

    private Collection collection(String referenceNo) {
        Collection collection = new Collection();
        collection.setCollectionDate(LocalDate.of(2026, 6, 9));
        collection.setCustomerCode("C001");
        collection.setAmount(new BigDecimal("100.00"));
        collection.setReferenceNo(referenceNo);
        collection.setStatus(CollectionStatus.RECEIVED);
        return collection;
    }

    private Receivable receivable(Long id, String invoiceNo, LocalDate dueDate) {
        SalesInvoice invoice = SalesInvoice.create(
                invoiceNo, "C001", LocalDate.of(2026, 5, 1), dueDate,
                new BigDecimal("90.00"), new BigDecimal("10.00"), "tester");
        Receivable receivable = new Receivable();
        receivable.setId(id);
        receivable.setSalesInvoice(invoice);
        receivable.setCustomerCode("C001");
        receivable.setOriginalAmount(new BigDecimal("100.00"));
        receivable.setOutstandingAmount(new BigDecimal("100.00"));
        receivable.setDueDate(dueDate);
        receivable.setStatus(ReceivableStatus.OPEN);
        return receivable;
    }
}
