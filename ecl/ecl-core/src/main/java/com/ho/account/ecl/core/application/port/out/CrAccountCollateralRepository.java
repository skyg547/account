package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface CrAccountCollateralRepository extends JpaRepository<CrAccountCollateral, Long> {
    List<CrAccountCollateral> findByAccount(CrAccount account);
    List<CrAccountCollateral> findByAccountIn(List<CrAccount> accounts);
}
