package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.AllowanceBalance;
import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/** Uses posted dual-currency contributions; GL ending_balance contains only the base unit. */
@Repository
@RequiredArgsConstructor
public class GlAllowanceBalanceLookupAdapter implements AllowanceBalanceLookupPort {

    private final JournalFxValuationBalanceSource balanceSource;

    @Override
    public AllowanceBalance findCreditBalance(
            String allowanceAccountCode,
            String transactionCurrencyCode,
            String functionalCurrencyCode,
            LocalDate balanceDate) {
        return balanceSource.findCreditBalance(
                allowanceAccountCode, transactionCurrencyCode, functionalCurrencyCode, balanceDate);
    }
}
