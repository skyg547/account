package com.ho.account.closing.infrastructure.external;

import com.ho.account.closing.application.port.in.ClosingAdmissionQuery;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * [ClosingStatusAdapter]
 * 타 모듈(Journal 등)에서 일반 전표 진입 차단 여부를 조회하기 위한 어댑터.
 * contracts 모듈의 AccountingPeriodStatusPort를 구현합니다.
 */
@Component
public class ClosingStatusAdapter implements AccountingPeriodStatusPort {

    private final ClosingAdmissionQuery closingAdmissionQuery;

    public ClosingStatusAdapter(ClosingAdmissionQuery closingAdmissionQuery) {
        this.closingAdmissionQuery = closingAdmissionQuery;
    }

    @Override
    public boolean isClosed(LocalDate accountingDate) {
        return closingAdmissionQuery.isClosed(accountingDate);
    }
}
