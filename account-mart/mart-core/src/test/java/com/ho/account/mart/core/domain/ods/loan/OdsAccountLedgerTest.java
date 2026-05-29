package com.ho.account.mart.core.domain.ods.loan;

import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OdsAccountLedgerTest {

    @Test
    @DisplayName("연체일수 경계값으로 IFRS 9 스테이징을 판정한다")
    void determineStagingByDelinquentDaysBoundaries() {
        assertEquals(CrStaging.STAGE1, ledger(null).determineStaging());
        assertEquals(CrStaging.STAGE1, ledger(29).determineStaging());
        assertEquals(CrStaging.STAGE2, ledger(30).determineStaging());
        assertEquals(CrStaging.STAGE2, ledger(89).determineStaging());
        assertEquals(CrStaging.STAGE3, ledger(90).determineStaging());
    }

    private OdsAccountLedger ledger(Integer delinquentDays) {
        return OdsAccountLedger.builder()
                .accountNo("ACC001")
                .delinquentDays(delinquentDays)
                .build();
    }
}
