package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCrAccountCollateralRepository extends JpaRepository<CrAccountCollateral, Long> {
    List<CrAccountCollateral> findByAccount(CrAccount account);
    List<CrAccountCollateral> findByAccountIn(List<CrAccount> accounts);
}
