package com.risk.credit.core.infrastructure.adapter.persistence;

import com.risk.credit.core.application.port.out.CrCollateralRepository;
import com.risk.credit.core.domain.collateral.CrCollateral;
import com.risk.credit.core.infrastructure.adapter.persistence.jpa.JpaCrCollateralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * [Adapter] CrCollateralRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class CrCollateralPersistenceAdapter implements CrCollateralRepository {

    private final JpaCrCollateralRepository jpaRepository;

    @Override
    public Optional<CrCollateral> findByCollateralCode(String collateralCode) {
        return jpaRepository.findByCollateralCode(collateralCode);
    }

    @Override
    public List<CrCollateral> findByCustomer_IdAndIsActiveTrue(Long customerId) {
        return jpaRepository.findByCustomer_IdAndIsActiveTrue(customerId);
    }

    @Override
    public CrCollateral save(CrCollateral collateral) {
        return jpaRepository.save(collateral);
    }

    @Override
    public void saveAll(Iterable<CrCollateral> collaterals) {
        jpaRepository.saveAll(collaterals);
    }

    @Override
    public Optional<CrCollateral> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
