package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local reporting adapter used when a beginner starts reporting without the journal-ledger service.
 *
 * <p>운영에서는 `LedgerClientAdapter`가 `LedgerQueryPort`를 통해 실제 GL 잔액을 읽습니다.
 * 로컬 학습 모드에서는 `account.reporting.persistence.mode=memory`를 켜고 이 어댑터가
 * 작은 샘플 잔액을 반환하게 하여 API/BATCH 애플리케이션 기동과 데이터 흐름을 먼저 확인합니다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class InMemoryLedgerBalanceAdapter implements LoadLedgerPort {

    @Override
    public Map<String, BigDecimal> getAccountBalances(LocalDateTime baseDate) {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        balances.put("101", new BigDecimal("1200000.00"));
        balances.put("102", new BigDecimal("300000.00"));
        balances.put("201", new BigDecimal("700000.00"));
        balances.put("202", new BigDecimal("250000.00"));
        balances.put("401", new BigDecimal("90000.00"));
        balances.put("501", new BigDecimal("35000.00"));
        return balances;
    }
}
