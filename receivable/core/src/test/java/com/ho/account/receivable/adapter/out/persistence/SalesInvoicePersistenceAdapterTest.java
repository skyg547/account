package com.ho.account.receivable.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.SalesInvoiceJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.SalesInvoiceMapper;
import com.ho.account.receivable.repository.SalesInvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesInvoicePersistenceAdapterTest {

    @Mock
    private SalesInvoiceRepository salesInvoiceRepository;

    private SalesInvoicePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SalesInvoicePersistenceAdapter(salesInvoiceRepository, new SalesInvoiceMapper());
    }

    @Test
    void findAllMapsJpaEntitiesToDomainInvoices() {
        SalesInvoiceJpaEntity entity = createEntity(10L, SalesInvoiceStatus.ISSUED);
        when(salesInvoiceRepository.findAll()).thenReturn(List.of(entity));

        List<SalesInvoice> result = adapter.findAll();

        assertThat(result).singleElement().satisfies(invoice -> {
            assertThat(invoice.getId()).isEqualTo(10L);
            assertThat(invoice.getInvoiceNo()).isEqualTo("INV-QUERY");
            assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.ISSUED);
        });
        verify(salesInvoiceRepository).findAll();
    }

    @Test
    void findByStatusUsesTypedRepositoryQueryAndMapsResults() {
        SalesInvoiceJpaEntity entity = createEntity(11L, SalesInvoiceStatus.PAID);
        when(salesInvoiceRepository.findByStatus(SalesInvoiceStatus.PAID)).thenReturn(List.of(entity));

        List<SalesInvoice> result = adapter.findByStatus(SalesInvoiceStatus.PAID);

        assertThat(result).singleElement().satisfies(invoice -> {
            assertThat(invoice.getId()).isEqualTo(11L);
            assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.PAID);
        });
        verify(salesInvoiceRepository).findByStatus(SalesInvoiceStatus.PAID);
    }

    private SalesInvoiceJpaEntity createEntity(Long id, SalesInvoiceStatus status) {
        SalesInvoiceJpaEntity entity = new SalesInvoiceJpaEntity();
        entity.setId(id);
        entity.setInvoiceNo("INV-QUERY");
        entity.setCustomerCode("C001");
        entity.setIssueDate(LocalDate.of(2026, 5, 1));
        entity.setDueDate(LocalDate.of(2026, 5, 31));
        entity.setNetAmount(new BigDecimal("1000.00"));
        entity.setTaxAmount(new BigDecimal("100.00"));
        entity.setTotalAmount(new BigDecimal("1100.00"));
        entity.setStatus(status);
        entity.setCreatedBy("sales-user");
        return entity;
    }
}
