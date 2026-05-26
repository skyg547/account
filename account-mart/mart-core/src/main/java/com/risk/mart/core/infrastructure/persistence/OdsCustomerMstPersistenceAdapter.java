package com.risk.mart.core.infrastructure.persistence;

import com.risk.mart.core.application.port.out.OdsCustomerMstRepository;
import com.risk.mart.core.domain.ods.common.OdsCustomerMst;
import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsCustomerMstEntity;
import com.risk.mart.core.infrastructure.persistence.jpa.JpaOdsCustomerMstRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class OdsCustomerMstPersistenceAdapter implements OdsCustomerMstRepository {

    private final JpaOdsCustomerMstRepository jpaRepository;

    @Override
    public Optional<OdsCustomerMst> findById(String customerCode) {
        return jpaRepository.findById(customerCode).map(e -> this.toDomain(e));
    }

    @Override
    public Optional<OdsCustomerMst> findByCustomerCode(String customerCode) {
        return jpaRepository.findByCustomerCode(customerCode).map(e -> this.toDomain(e));
    }

    @Override
    public List<OdsCustomerMst> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> this.toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public OdsCustomerMst save(OdsCustomerMst domain) {
        OdsCustomerMstEntity entity = toEntity(domain);
        OdsCustomerMstEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void saveAll(Iterable<OdsCustomerMst> domains) {
        if (domains == null) return;
        List<OdsCustomerMstEntity> entities = StreamSupport.stream(domains.spliterator(), false)
                .map(d -> this.toEntity(d))
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    private OdsCustomerMst toDomain(OdsCustomerMstEntity entity) {
        if (entity == null) return null;
        return OdsCustomerMst.builder()
                .customerCode(entity.getCustomerCode())
                .bizNo(entity.getBizNo())
                .customerName(entity.getCustomerName())
                .customerType(entity.getCustomerType())
                .countryCode(entity.getCountryCode())
                .internalRating(entity.getInternalRating())
                .ratingCode(entity.getRatingCode())
                .externalRating(entity.getExternalRating())
                .industryCode(entity.getIndustryCode())
                .industryName(entity.getIndustryName())
                .isSme(entity.getIsSme())
                .branchCode(entity.getBranchCode())
                .creditStatusCd(entity.getCreditStatusCd())
                .build();
    }

    private OdsCustomerMstEntity toEntity(OdsCustomerMst domain) {
        if (domain == null) return null;
        return OdsCustomerMstEntity.builder()
                .customerCode(domain.getCustomerCode())
                .bizNo(domain.getBizNo())
                .customerName(domain.getCustomerName())
                .customerType(domain.getCustomerType())
                .countryCode(domain.getCountryCode())
                .internalRating(domain.getInternalRating())
                .ratingCode(domain.getRatingCode())
                .externalRating(domain.getExternalRating())
                .industryCode(domain.getIndustryCode())
                .industryName(domain.getIndustryName())
                .isSme(domain.getIsSme())
                .branchCode(domain.getBranchCode())
                .creditStatusCd(domain.getCreditStatusCd())
                .build();
    }
}
