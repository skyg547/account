package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsCollateralMstRepository;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsCollateralMstEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsCollateralMstRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * [Persistence Adapter] 담보 마스터 헥사고날 어댑터
 */
@Component
@RequiredArgsConstructor
public class OdsCollateralMstPersistenceAdapter implements OdsCollateralMstRepository {

    private final JpaOdsCollateralMstRepository jpaRepository;

    @Override
    public Optional<OdsCollateralMst> findById(String collateralNo) {
        return jpaRepository.findById(collateralNo).map(this::toDomain);
    }

    @Override
    public Optional<OdsCollateralMst> findFirstByCustomerCode(String customerCode) {
        return jpaRepository.findFirstByCustomerCode(customerCode).map(this::toDomain);
    }

    @Override
    public List<OdsCollateralMst> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public OdsCollateralMst save(OdsCollateralMst domain) {
        OdsCollateralMstEntity entity = toEntity(domain);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void saveAll(Iterable<OdsCollateralMst> domains) {
        if (domains == null) return;
        List<OdsCollateralMstEntity> entities = StreamSupport.stream(domains.spliterator(), false)
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    private OdsCollateralMst toDomain(OdsCollateralMstEntity entity) {
        if (entity == null) return null;
        return OdsCollateralMst.builder()
                .collateralNo(entity.getCollateralNo())
                .customerCode(entity.getCustomerCode())
                .collateralType(entity.getCollateralType())
                .currency(entity.getCurrency())
                .appraisedValue(entity.getAppraisalAmount()) // 필드명 정합성 주의
                .pledgedAmount(entity.getPledgeAmount())
                .valuationDate(entity.getAppraisalDate())
                .isActive(entity.getIsActive())
                .build();
    }

    private OdsCollateralMstEntity toEntity(OdsCollateralMst domain) {
        if (domain == null) return null;
        return OdsCollateralMstEntity.builder()
                .collateralNo(domain.getCollateralNo())
                .customerCode(domain.getCustomerCode())
                .collateralType(domain.getCollateralType())
                .currency(domain.getCurrency())
                .appraisalAmount(domain.getAppraisedValue())
                .pledgeAmount(domain.getPledgedAmount())
                .appraisalDate(domain.getValuationDate())
                .isActive(domain.getIsActive())
                .build();
    }
}
