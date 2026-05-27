package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * [Infrastructure] JPA를 사용한 담보 데이터 접근 인터페이스.
 */
public interface JpaCrCollateralRepository extends JpaRepository<CrCollateral, Long> {
    Optional<CrCollateral> findByCollateralCode(String collateralCode);

    List<CrCollateral> findByCustomer_IdAndIsActiveTrue(Long customerId);
}
