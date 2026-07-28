package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsAccountLedgerRepository;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsAccountLedgerEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsAccountLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * [Persistence Adapter] 여신 원장 헥사고날 어댑터
 */
@Component
@RequiredArgsConstructor
public class OdsAccountLedgerPersistenceAdapter implements OdsAccountLedgerRepository {

    private final JpaOdsAccountLedgerRepository jpaRepository;

    @Override
    public Optional<OdsAccountLedger> findById(String accountNo) {
        return jpaRepository.findById(accountNo).map(this::main);
    }

    @Override
    public List<OdsAccountLedger> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public OdsAccountLedger save(OdsAccountLedger domain) {
        OdsAccountLedgerEntity entity = toEntity(domain);
        OdsAccountLedgerEntity savedEntity = jpaRepository.save(entity);
        return main(savedEntity);
    }

    @Override
    public void saveAll(List<OdsAccountLedger> domains) {
        if (domains == null || domains.isEmpty()) return;
        List<OdsAccountLedgerEntity> entities = domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    /**
     * Entity -> Domain POJO Mapper
     */
    private OdsAccountLedger main(OdsAccountLedgerEntity entity) {
        if (entity == null) return null;
        return OdsAccountLedger.builder()
                .accountNo(entity.getAccountNo())
                .customerCode(entity.getCustomerCode())
                .productCode(entity.getProductCode())
                .currency(entity.getCurrency())
                .outstandingAmount(entity.getOutstandingAmount())
                .limitAmount(entity.getLimitAmount())
                .interestRate(entity.getInterestRate())
                .baseRateCode(entity.getBaseRateCode())
                .spread(entity.getSpread())
                .nextResetDate(entity.getNextResetDate())
                .openDate(entity.getOpenDate())
                .maturityDate(entity.getMaturityDate())
                .delinquentDays(entity.getDelinquentDays())
                .repaymentMethod(entity.getRepaymentMethod())
                .gracePeriod(entity.getGracePeriod())
                .repaymentFreq(entity.getRepaymentFreq())
                .branchCd(entity.getBranchCd())
                .bizUnitCd(entity.getBizUnitCd())
                .isActive(entity.getIsActive())
                .build();
    }

    /**
     * Domain POJO -> Entity Mapper
     */
    private OdsAccountLedgerEntity toEntity(OdsAccountLedger domain) {
        if (domain == null) return null;
        return OdsAccountLedgerEntity.builder()
                .accountNo(domain.getAccountNo())
                .customerCode(domain.getCustomerCode())
                .productCode(domain.getProductCode())
                .currency(domain.getCurrency())
                .outstandingAmount(domain.getOutstandingAmount())
                .limitAmount(domain.getLimitAmount())
                .interestRate(domain.getInterestRate())
                .baseRateCode(domain.getBaseRateCode())
                .spread(domain.getSpread())
                .nextResetDate(domain.getNextResetDate())
                .openDate(domain.getOpenDate())
                .maturityDate(domain.getMaturityDate())
                .delinquentDays(domain.getDelinquentDays())
                .repaymentMethod(domain.getRepaymentMethod())
                .gracePeriod(domain.getGracePeriod())
                .repaymentFreq(domain.getRepaymentFreq())
                .branchCd(domain.getBranchCd())
                .bizUnitCd(domain.getBizUnitCd())
                .isActive(domain.getIsActive())
                .build();
    }
}
