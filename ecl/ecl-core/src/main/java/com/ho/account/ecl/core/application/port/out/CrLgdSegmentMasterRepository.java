package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.CrLgdSegmentMaster;
import java.util.List;
import java.util.Optional;

/**
 * [Repository] LGD 세그먼트 마스터 저장소.
 */
public interface CrLgdSegmentMasterRepository {
    List<CrLgdSegmentMaster> findAll();
    Optional<CrLgdSegmentMaster> findByCustomerTypeAndCollateralType(String customerType, String collateralType);
}
