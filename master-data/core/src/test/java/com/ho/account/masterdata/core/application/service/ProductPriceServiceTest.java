package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.ArgumentCaptor;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.model.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductPriceServiceTest {

    private final ProductPersistencePort persistence = mock(ProductPersistencePort.class);
    private final ProductService service = new ProductService(persistence, mock(MasterDataBusinessKeyLockPort.class));

    @Test
    void directCreateRejectsInvalidPriceBeforeSaving() {
        ProductCommand command = command("1.23456", LocalDate.now());

        assertThatThrownBy(() -> service.createProduct(command))
                .isInstanceOf(IllegalArgumentException.class);
        verify(persistence, never()).save(any());
    }

    @Test
    void directUpdateRejectsInvalidPriceBeforeClosingOldVersion() {
        LocalDate today = LocalDate.now();
        Product old = existing(today.minusDays(30));
        when(persistence.findBusinessKeyById(7L)).thenReturn(Optional.of("P-7"));
        when(persistence.findByIdForUpdate(7L)).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.updateProduct(7L, command("-0.0001", today.plusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(old.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
        verify(persistence, never()).save(any());
    }

    @Test
    void directCreateUsesVerifiedActorAndEmitsIdentifierOnlyEvent() {
        MasterDataAuditPort audit = mock(MasterDataAuditPort.class);
        ProductService audited = new ProductService(persistence,
                mock(MasterDataBusinessKeyLockPort.class), audit);
        when(persistence.save(any())).thenAnswer(invocation -> {
            Product row = invocation.getArgument(0);
            row.setId(924L);
            return row;
        });

        try (MasterDataActorContext.Scope ignored = MasterDataActorContext.enter(null, "actor-924")) {
            assertThat(audited.createProduct(command("10.0000", LocalDate.now())).getAuditUser())
                    .isEqualTo("actor-924");
        }

        ArgumentCaptor<MasterDataAuditEvent> events = ArgumentCaptor.forClass(MasterDataAuditEvent.class);
        verify(audit).append(events.capture());
        assertThat(events.getValue().businessActor()).isEqualTo("actor-924");
        assertThat(events.getValue().afterVersionId()).isEqualTo(924L);
        assertThat(events.getValue().beforeVersionId()).isNull();
        assertThat(events.getValue().reason()).isNull();
        assertThat(MasterDataActorContext.current()).isNull();
    }

    private static Product existing(LocalDate from) {
        Product product = new Product();
        product.setId(7L);
        product.setProductCode("P-7");
        product.setName("original");
        product.setProductType(Product.ProductType.PHYSICAL);
        product.setPrice(new BigDecimal("1.0000"));
        product.setValidFrom(from);
        product.setValidTo(LocalDate.of(9999, 12, 31));
        return product;
    }

    private static ProductCommand command(String price, LocalDate from) {
        return new ProductCommand("P-7", "product", null, "EA", new BigDecimal(price),
                Product.ProductType.PHYSICAL, from, LocalDate.of(9999, 12, 31));
    }
}
