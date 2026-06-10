package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrAccountCollateralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CrAccountCollateralPersistenceAdapter implements CrAccountCollateralRepository {
    private final JpaCrAccountCollateralRepository jpaRepository;

    @Override public CrAccountCollateral save(CrAccountCollateral allocation) { return jpaRepository.save(allocation); }
    @Override public List<CrAccountCollateral> findByAccount(CrAccount account) { return jpaRepository.findByAccount(account); }
    @Override public List<CrAccountCollateral> findByAccountIn(List<CrAccount> accounts) { return jpaRepository.findByAccountIn(accounts); }
}
