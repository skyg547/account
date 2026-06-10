package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrLgdSegmentMasterRepository;
import com.ho.account.ecl.core.domain.model.CrLgdSegmentMaster;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrLgdSegmentMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CrLgdSegmentMasterPersistenceAdapter implements CrLgdSegmentMasterRepository {
    private final JpaCrLgdSegmentMasterRepository jpaRepository;

    @Override public List<CrLgdSegmentMaster> findAll() { return jpaRepository.findAll(); }

    @Override
    @Cacheable(value = "lgdSegment", key = "#customerType + ':' + #collateralType")
    public Optional<CrLgdSegmentMaster> findByCustomerTypeAndCollateralType(String customerType, String collateralType) {
        return jpaRepository.findByCustomerTypeAndCollateralType(customerType, collateralType);
    }
}
