package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
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
