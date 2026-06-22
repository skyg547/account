package com.ho.account.mart.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ho.account.mart.core.domain.ods.common.OdsProductMst;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsProductMstEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsProductMstRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OdsProductMstPersistenceAdapterTest {

    private final JpaOdsProductMstRepository jpaRepository = org.mockito.Mockito.mock(JpaOdsProductMstRepository.class);
    private final OdsProductMstPersistenceAdapter adapter = new OdsProductMstPersistenceAdapter(jpaRepository);

    @Test
    void saveUpdatesApiFieldsAndPreservesExistingOperationalFields() {
        OdsProductMstEntity existing = OdsProductMstEntity.builder()
                .productCode("CORP_LOAN")
                .productName("Old")
                .productCategory("OLD")
                .subjectCode("11100")
                .defaultCcf(new BigDecimal("0.7500"))
                .build();
        when(jpaRepository.findById("CORP_LOAN")).thenReturn(Optional.of(existing));
        when(jpaRepository.save(existing)).thenReturn(existing);

        OdsProductMst saved = adapter.save(OdsProductMst.builder()
                .productCode("CORP_LOAN")
                .productName("Corporate Loan")
                .productType("LOAN")
                .assetLiabilityType("ASSET")
                .isActive(true)
                .build());

        assertThat(existing.getSubjectCode()).isEqualTo("11100");
        assertThat(existing.getDefaultCcf()).isEqualByComparingTo("0.7500");
        assertThat(saved.getProductName()).isEqualTo("Corporate Loan");
        assertThat(saved.getProductType()).isEqualTo("LOAN");
        assertThat(saved.getAssetLiabilityType()).isEqualTo("ASSET");
    }
}
