package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.model.CrLgdSegmentMaster;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * [Repository] LGD 세그먼트 마스터 저장소.
 */
@Repository
public interface CrLgdSegmentMasterRepository extends JpaRepository<CrLgdSegmentMaster, Long> {
    @Cacheable(value = "lgdSegment", key = "#customerType + ':' + #collateralType")
    Optional<CrLgdSegmentMaster> findByCustomerTypeAndCollateralType(String customerType, String collateralType);
}
