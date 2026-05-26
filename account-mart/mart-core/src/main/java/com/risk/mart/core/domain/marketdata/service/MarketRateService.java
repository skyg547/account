package com.risk.mart.core.domain.marketdata.service;

import com.risk.mart.core.domain.marketdata.MarketRate;
import com.risk.mart.core.application.port.out.MarketRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * [마트 서비스] 시장 금리(Market Rate) 관리 서비스
 * 
 * 💡 [초보자를 위한 금융 개념 설명]
 * 이 서비스는 리스크 산출에 필요한 '시장 금리' 데이터를 관리합니다.
 * 리스크 시스템에서 금리는 매우 중요합니다. 예를 들어, CD 금리나 국고채 금리가 변하면 
 * 은행이 빌려준 돈의 가치도 변하고, 고객에게 받을 이자도 달라지기 때문입니다.
 * 이 서비스는 매일의 기준 금리를 저장하고, 과거의 금리 추이를 조회하는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketRateService {

    /** 💡 [초보자 가이드] 시장 금리 정보가 실제로 저장된 DB 테이블 저장소입니다. */
    private final MarketRateRepository repository;

    /**
     * 새로운 시장 금리 데이터를 저장합니다.
     * 
     * @param rate 저장할 금리 정보 엔티티 (금리명, 만기, 금리값 등)
     * @return 저장된 금리 정보
     */
    @Transactional
    public MarketRate saveRate(MarketRate rate) {
        log.info("📊 [시장 데이터] 신규 금리 정보를 저장합니다. (금리명: {}, 기준일: {})", rate.getRateName(), rate.getBaseDate());
        return Objects.requireNonNull(repository.save(Objects.requireNonNull(rate)));
    }

    /**
     * 여러 개의 금리 데이터를 한꺼번에 저장합니다.
     * 💡 [고도화] 매일 아침 수천 개의 금리 정보를 일괄적으로 DB에 밀어 넣을 때 사용합니다.
     * 
     * @param rates 저장할 금리 정보 리스트
     * @return 저장된 금리 정보 리스트
     */
    @Transactional
    public List<MarketRate> saveAllRates(List<MarketRate> rates) {
        log.info("📊 [시장 데이터] 총 {}건의 금리 정보를 일괄 저장합니다.", rates.size());
        return Objects.requireNonNull(repository.saveAll(Objects.requireNonNull(rates)));
    }

    /**
     * 특정 기준일자의 활성화된 모든 금리 정보를 조회합니다.
     * 
     * @param baseDate 조회 기준일자
     * @return 금리 정보 리스트
     */
    public List<MarketRate> getRatesByDate(LocalDate baseDate) {
        return repository.findByBaseDateAndIsActiveTrue(baseDate);
    }

    /**
     * 특정 기준일자 및 금리 유형(예: CD, KORIBOR 등)에 해당하는 금리 정보를 조회합니다.
     * 
     * @param baseDate 조회 기준일자
     * @param rateType 금리 유형 (CD, BOND, LIBOR 등)
     * @return 금리 정보 리스트
     */
    public List<MarketRate> getRatesByType(LocalDate baseDate, String rateType) {
        return repository.findByBaseDateAndRateTypeAndIsActiveTrue(baseDate, rateType);
    }

    /**
     * 특정 금리의 과거 이력(History)을 조회합니다.
     * 💡 [비즈니스 시뮬레이션] "지난 10년간 금리가 가장 폭등했을 때"의 데이터를 분석할 때 유용합니다.
     * 
     * @param rateName 금리 명칭
     * @param tenor 기간 (예: 3개월물=3, 1년물=12 등)
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 과거 금리 추이 리스트
     */
    public List<MarketRate> getRateHistory(String rateName, Integer tenor, LocalDate startDate, LocalDate endDate) {
        return repository.findHistorical(rateName, tenor, startDate, endDate);
    }

    /**
     * 현재 시스템에서 관리 중인 활성 금리 명칭 목록을 조회합니다.
     * 
     * @return 금리 명칭 리스트
     */
    public List<String> getActiveRateNames() {
        return repository.findDistinctActiveRateNames();
    }
}