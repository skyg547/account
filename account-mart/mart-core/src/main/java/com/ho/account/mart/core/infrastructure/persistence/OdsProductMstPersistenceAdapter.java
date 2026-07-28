package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsProductMstRepository;
import com.ho.account.mart.core.domain.ods.common.OdsProductMst;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsProductMstEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsProductMstRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OdsProductMstPersistenceAdapter implements OdsProductMstRepository {

    private final JpaOdsProductMstRepository jpaRepository;

    @Override
    public Optional<OdsProductMst> findById(String productCode) {
        return jpaRepository.findById(productCode).map(this::toDomain);
    }

    @Override
    public List<OdsProductMst> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public OdsProductMst save(OdsProductMst productMst) {
        OdsProductMstEntity entity = toEntityForSave(productMst);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void saveAll(Iterable<OdsProductMst> productMsts) {
        if (productMsts == null) {
            return;
        }
        List<OdsProductMstEntity> entities = StreamSupport.stream(productMsts.spliterator(), false)
                .map(this::toEntityForSave)
                .toList();
        jpaRepository.saveAll(entities);
    }

    private OdsProductMst toDomain(OdsProductMstEntity entity) {
        if (entity == null) {
            return null;
        }
        return OdsProductMst.builder()
                .productCode(entity.getProductCode())
                .productName(entity.getProductName())
                .productType(entity.getProductCategory())
                .assetLiabilityType(entity.getAssetLiabilityType())
                .isActive(entity.getIsActive())
                .build();
    }

    private OdsProductMstEntity toEntityForSave(OdsProductMst domain) {
        if (domain == null) {
            return null;
        }
        OdsProductMstEntity entity = jpaRepository.findById(domain.getProductCode())
                .orElseGet(() -> OdsProductMstEntity.builder()
                        .productCode(domain.getProductCode())
                        .build());
        entity.setProductName(domain.getProductName());
        entity.setProductCategory(domain.getProductType());
        entity.setAssetLiabilityType(domain.getAssetLiabilityType());
        entity.setIsActive(domain.getIsActive());
        return entity;
    }
}
