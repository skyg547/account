package com.risk.mart.core.domain.marketdata.service;

import com.risk.common.enums.CurrencyCode;
import com.risk.mart.core.domain.marketdata.ExchangeRate;
import com.risk.mart.core.application.port.out.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * [마트 서비스] 환율(Exchange Rate) 관리 서비스
 * 
 * 💡 [초보자를 위한 금융 개념 설명]
 * 이 서비스는 서로 다른 나라의 돈을 바꿀 때 기준이 되는 '환율' 데이터를 관리합니다.
 * 리스크 시스템에서 환율은 매우 중요합니다.
 * 은행이 미국 기업에 100만 달러를 빌려줬다면, 환율이 1,000원일 때는 자산이 10억 원이지만
 * 환율이 1,300원이 되면 13억 원으로 가치가 변합니다. 
 * 이렇게 환율 변동에 따라 은행 자산의 가치가 출렁이는 '환리스크'를 측정하기 위해 매일의 정확한 환율 정보가 필요합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {

    /** 💡 [초보자 가이드] 전 세계 통화별 환율 정보가 저장된 DB 테이블 저장소입니다. */
    private final ExchangeRateRepository repository;

    /**
     * 새로운 환율 정보를 시스템에 저장합니다.
     * 
     * @param rate 저장할 환율 엔티티 (기준통화, 상대통화, 환율값 등)
     * @return 저장된 환율 정보
     */
    @Transactional
    @SuppressWarnings("null")
    public ExchangeRate saveRate(ExchangeRate rate) {
        log.info("💱 [시장 데이터] 신규 환율 정보를 저장합니다. ({} -> {}, 기준일: {})", 
                rate.getBaseCurrency(), rate.getQuoteCurrency(), rate.getBaseDate());
        return Objects.requireNonNull(repository.save(rate));
    }

    /**
     * 특정 날짜의 모든 환율 정보를 조회합니다.
     * 💡 [비즈니스 시뮬레이션] "오늘 아침 환율이 모두 정상적으로 수신되었는가?"를 확인할 때 사용합니다.
     * 
     * @param baseDate 조회 기준일자
     * @return 해당 날짜의 환율 리스트
     */
    public List<ExchangeRate> getRatesByDate(LocalDate baseDate) {
        return repository.findByBaseDate(baseDate);
    }

    /**
     * 특정 날짜의 특정 통화 쌍(예: USD/KRW)에 대한 환율을 조회합니다.
     * 
     * @param baseDate 기준일자
     * @param baseCurrency 기준 통화 (예: USD)
     * @param quoteCurrency 상대 통화 (예: KRW)
     * @return 조회된 환율 정보 (Optional)
     */
    public Optional<ExchangeRate> getRate(LocalDate baseDate, CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        return repository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(baseDate, baseCurrency, quoteCurrency);
    }

    /**
     * 특정 통화 쌍의 가장 최신(전영업일 등) 환율 정보를 조회합니다.
     * 
     * @param baseCurrency 기준 통화
     * @param quoteCurrency 상대 통화
     * @return 가장 최근 환율 정보 (Optional)
     */
    public Optional<ExchangeRate> getLatestRate(CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        return repository.findLatest(baseCurrency, quoteCurrency);
    }

    /**
     * 특정 통화 쌍의 과거 일정 기간 동안의 환율 변동 이력(History)을 조회합니다.
     * 💡 [비즈니스 분석] 특정 시기에 환율이 급등했을 때 우리 은행의 손실이 어떠했는지 사후 분석할 때 씁니다.
     * 
     * @param baseCurrency 기준 통화
     * @param quoteCurrency 상대 통화
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과거 환율 변동 리스트
     */
    public List<ExchangeRate> getHistorical(CurrencyCode baseCurrency, CurrencyCode quoteCurrency, LocalDate startDate, LocalDate endDate) {
        return repository.findHistorical(baseCurrency, quoteCurrency, startDate, endDate);
    }
}