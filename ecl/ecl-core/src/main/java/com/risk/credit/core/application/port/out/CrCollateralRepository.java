package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.collateral.CrCollateral;
import java.util.List;
import java.util.Optional;

/**
 * [Port] 신용리스크용 담보(Collateral) 데이터소스 인터페이스.
 */
public interface CrCollateralRepository {
    Optional<CrCollateral> findByCollateralCode(String collateralCode);

    List<CrCollateral> findByCustomer_IdAndIsActiveTrue(Long customerId);

    CrCollateral save(CrCollateral collateral);

    void saveAll(Iterable<CrCollateral> collaterals);

    Optional<CrCollateral> findById(Long id);
}
