package com.ho.account.reporting.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * [아웃바운드 포트] LoadLedgerPort
 * 초보자 가이드: '아웃바운드 포트'는 출구와 같습니다.
 * 우리 모듈(Reporting)이 다른 모듈(Journal Ledger)이나 DB에 데이터를 달라고 요청할 때 사용합니다.
 * 이 인터페이스 덕분에 실제 원장이 어떤 DB를 쓰는지 몰라도 "데이터만 줘!"라고 할 수 있습니다.
 */
public interface LoadLedgerPort {

    /**
     * 특정 기준일의 계정별 잔액 지도를 가져옵니다.
     * @param baseDate 기준일
     * @return Map<계정코드, 잔액>
     */
    Map<String, BigDecimal> getAccountBalances(LocalDateTime baseDate);
}
