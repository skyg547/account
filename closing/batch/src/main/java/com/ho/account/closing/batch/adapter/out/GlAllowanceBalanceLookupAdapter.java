package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlBalanceType;
import com.ho.account.journalledger.domain.ledger.repository.GlAccountBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class GlAllowanceBalanceLookupAdapter implements AllowanceBalanceLookupPort {

    private final GlAccountBalanceRepository glAccountBalanceRepository;

    @Override
    public BigDecimal findCreditEndingBalance(String allowanceAccountCode, String currencyCode, LocalDate balanceDate) {
        return glAccountBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
                        allowanceAccountCode,
                        currencyCode,
                        balanceDate,
                        GlBalanceType.CREDIT)
                .map(GlAccountBalance::getEndingBalance)
                .orElse(BigDecimal.ZERO);
    }
}