package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@ContextConfiguration(classes = JpaMasterDataAuditAdapterTest.TestApplication.class)
@Import(JpaMasterDataAuditAdapter.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JpaMasterDataAuditAdapterTest {
    @Autowired JpaMasterDataAuditAdapter audit;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication { }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackRemovesAuditWithUncommittedBusinessOperation() {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            ProductEntity product = new ProductEntity();
            product.setProductCode("P-924");
            product.setName("synthetic product");
            product.setPrice(BigDecimal.ONE);
            product.setProductType(Product.ProductType.PHYSICAL);
            entityManager.persist(product);
            audit.append(event("APPLIED", product.getId()));
            entityManager.flush();
            throw new IllegalStateException("synthetic business rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(audit.exportAfterId(0, 10)).isEmpty();
        assertThat(entityManager.createQuery("select count(p) from ProductEntity p", Long.class)
                .getSingleResult()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void exportIsOrderedPagedAndRetainedAfterLaterTransactions() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> {
            audit.append(event("APPROVED", null));
            audit.append(event("APPLIED", 41L));
        });

        var first = audit.exportAfterId(0, 1);
        assertThat(first).hasSize(1);
        assertThat(first.get(0).event().eventType()).isEqualTo("APPROVED");
        var second = audit.exportAfterId(first.get(0).id(), 1);
        assertThat(second).hasSize(1);
        assertThat(second.get(0).event().afterVersionId()).isEqualTo(41L);
        assertThat(second.get(0).event().sourceReference()).isEqualTo("source-924");
        assertThat(second.get(0).toString()).doesNotContain("synthetic credential");
        assertThat(audit.exportAfterId(second.get(0).id(), 1)).isEmpty();

        transaction.executeWithoutResult(status -> { });
        assertThat(audit.exportAfterId(0, 10)).hasSize(2);
        assertThatThrownBy(() -> audit.exportAfterId(0, 1001)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void exportDetectsChangedEvidence() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> audit.append(event("REJECTED", null)));
        Long id = audit.exportAfterId(0, 1).get(0).id();

        transaction.executeWithoutResult(status -> entityManager.createNativeQuery(
                        "UPDATE master_data_audit_events SET business_actor = 'tampered' WHERE id = :id")
                .setParameter("id", id).executeUpdate());

        assertThatThrownBy(() -> audit.exportAfterId(0, 10))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("integrity mismatch");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void exportDetectsRenumberedOrderingIdentifier() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> audit.append(event("APPROVED", null)));
        Long originalId = audit.exportAfterId(0, 1).get(0).id();

        transaction.executeWithoutResult(status -> entityManager.createNativeQuery(
                        "UPDATE master_data_audit_events SET id = :newId WHERE id = :oldId")
                .setParameter("newId", originalId + 1000)
                .setParameter("oldId", originalId).executeUpdate());

        assertThatThrownBy(() -> audit.exportAfterId(0, 10))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("integrity mismatch");
    }

    private MasterDataAuditEvent event(String type, Long afterId) {
        // Older-than-policy synthetic evidence stays exportable because the application has no TTL purge.
        return new MasterDataAuditEvent(type, Instant.parse("2010-10-20T00:00:00.123456789Z"),
                MasterDataType.PRODUCT, "P-924", 9L, "source-924", null,
                "actor-924", "request:sha256:" + "a".repeat(64), null, afterId);
    }
}
