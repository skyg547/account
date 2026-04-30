package com.ho.account.closing.infrastructure.external;

import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * [ClosingStatusAdapter]
 * 타 모듈(Journal 등)에서 결산 상태를 조회하기 위한 어댑터.
 * contracts 모듈의 AccountingPeriodStatusPort를 구현합니다.
 */
@Component
public class ClosingStatusAdapter implements AccountingPeriodStatusPort {

    private final ClosingUseCase closingUseCase;

    public ClosingStatusAdapter(ClosingUseCase closingUseCase) {
        this.closingUseCase = closingUseCase;
    }

    @Override
    public boolean isClosed(LocalDate accountingDate) {
        return closingUseCase.isClosed(accountingDate);
    }
}
