package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductPersistencePort productPersistencePort;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productPersistencePort);
    }

    @Test
    void updateProductCreatesNewScd2VersionAndClosesCurrentVersion() {
        Product current = product(10L, "LN-PROD", "Old loan", "old", "EA",
                new BigDecimal("100.00"), Product.ProductType.LOAN,
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31));
        ProductCommand command = new ProductCommand(
                "LN-PROD",
                "New loan",
                null,
                null,
                new BigDecimal("120.00"),
                null,
                LocalDate.of(2026, 6, 1),
                null
        );

        when(productPersistencePort.findById(10L)).thenReturn(Optional.of(current));
        when(productPersistencePort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = service.updateProduct(10L, command);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productPersistencePort, times(2)).save(productCaptor.capture());

        assertThat(current.getValidTo()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(productCaptor.getAllValues().get(0)).isSameAs(current);
        assertThat(productCaptor.getAllValues().get(1)).isSameAs(updated);
        assertThat(updated).isNotSameAs(current);
        assertThat(updated.getId()).isNull();
        assertThat(updated.getProductCode()).isEqualTo("LN-PROD");
        assertThat(updated.getName()).isEqualTo("New loan");
        assertThat(updated.getDescription()).isEqualTo("old");
        assertThat(updated.getUnitOfMeasure()).isEqualTo("EA");
        assertThat(updated.getPrice()).isEqualByComparingTo("120.00");
        assertThat(updated.getProductType()).isEqualTo(Product.ProductType.LOAN);
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
        assertThat(productCaptor.getAllValues()).hasSize(2);
    }

    @Test
    void updateProductRejectsProductCodeChange() {
        Product current = product(10L, "LN-PROD", "Old loan", "old", "EA",
                BigDecimal.TEN, Product.ProductType.LOAN,
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31));
        ProductCommand command = new ProductCommand(
                "OTHER",
                "New loan",
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 1),
                null
        );

        when(productPersistencePort.findById(10L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.updateProduct(10L, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 코드는");
    }

    @Test
    void getProductByProductCodeReturnsActiveVersionOnly() {
        Product active = product(11L, "LN-PROD", "Active", null, "EA",
                BigDecimal.TEN, Product.ProductType.LOAN,
                LocalDate.now().minusDays(1), LocalDate.of(9999, 12, 31));
        when(productPersistencePort.findActiveByProductCode("LN-PROD")).thenReturn(Optional.of(active));

        Optional<Product> found = service.getProductByProductCode("LN-PROD");

        assertThat(found).contains(active);
    }

    @Test
    void getAllActiveProductsDelegatesValidityFilteringToPersistence() {
        LocalDate today = LocalDate.now();
        Product active = product(11L, "LN-PROD", "Active", null, "EA",
                BigDecimal.TEN, Product.ProductType.LOAN,
                today.minusDays(1), LocalDate.of(9999, 12, 31));
        when(productPersistencePort.findAllActive(today)).thenReturn(List.of(active));

        assertThat(service.getAllActiveProducts()).containsExactly(active);

        verify(productPersistencePort).findAllActive(today);
    }

    private Product product(Long id, String code, String name, String description, String unitOfMeasure,
            BigDecimal price, Product.ProductType type, LocalDate validFrom, LocalDate validTo) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(code);
        product.setName(name);
        product.setDescription(description);
        product.setUnitOfMeasure(unitOfMeasure);
        product.setPrice(price);
        product.setProductType(type);
        product.setValidFrom(validFrom);
        product.setValidTo(validTo);
        return product;
    }
}
