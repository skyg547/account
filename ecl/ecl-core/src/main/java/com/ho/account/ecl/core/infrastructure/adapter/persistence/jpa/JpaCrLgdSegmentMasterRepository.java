package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.model.CrLgdSegmentMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCrLgdSegmentMasterRepository extends JpaRepository<CrLgdSegmentMaster, Long> {
    Optional<CrLgdSegmentMaster> findByCustomerTypeAndCollateralType(String customerType, String collateralType);
}
