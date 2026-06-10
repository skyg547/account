package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import java.util.List;

/**
 * 계좌별 담보 배분 조회·저장 출력 포트입니다.
 */
public interface CrAccountCollateralRepository {
    CrAccountCollateral save(CrAccountCollateral allocation);
    List<CrAccountCollateral> findByAccount(CrAccount account);
    List<CrAccountCollateral> findByAccountIn(List<CrAccount> accounts);
}
