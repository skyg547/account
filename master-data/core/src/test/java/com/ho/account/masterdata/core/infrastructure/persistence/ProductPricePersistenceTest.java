package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.ProductMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = ProductPricePersistenceTest.TestApplication.class)
@Import({JpaProductPersistenceAdapter.class, ProductMapper.class})
class ProductPricePersistenceTest {

    @Autowired private JpaProductPersistenceAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication { }

    @Test
    void h2JpaSaveReloadPreservesValidatedPriceExactly() {
        Product product = new Product();
        product.setProductCode("EXACT-PRICE");
        product.setName("exact price");
        product.setProductType(Product.ProductType.PHYSICAL);
        product.setPrice(new BigDecimal("999999999999999.999900"));
        product.setValidFrom(LocalDate.now());
        product.setValidTo(LocalDate.of(9999, 12, 31));

        Product saved = adapter.save(product);
        entityManager.flush();
        entityManager.clear();

        Product reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(reloaded.getPrice()).isEqualByComparingTo("999999999999999.9999");
    }
}
