package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.ods.loan.OdsCollateralMst;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 담보 마스터 데이터 접근 인터페이스
 */
public interface OdsCollateralMstRepository {
    Optional<OdsCollateralMst> findById(String collateralNo);
    Optional<OdsCollateralMst> findFirstByCustomerCode(String customerCode);
    List<OdsCollateralMst> findAll();
    OdsCollateralMst save(OdsCollateralMst collateral);
    void saveAll(Iterable<OdsCollateralMst> collaterals);
}
