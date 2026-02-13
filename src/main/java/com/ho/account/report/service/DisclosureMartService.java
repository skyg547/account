package com.ho.account.report.service;

import com.ho.account.report.domain.DisclosureMart;
import com.ho.account.report.repository.DisclosureMartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DisclosureMartService {

    private final DisclosureMartRepository disclosureMartRepository;

    @Autowired
    public DisclosureMartService(DisclosureMartRepository disclosureMartRepository) {
        this.disclosureMartRepository = disclosureMartRepository;
    }

    /**
     * 공시 마트 데이터를 생성합니다.
     */
    public DisclosureMart createMartData(String martType, LocalDate baseDate, String category1, String category2,
            BigDecimal amount, Integer count) {
        DisclosureMart mart = new DisclosureMart();
        mart.setMartType(martType);
        mart.setBaseDate(baseDate);
        mart.setCategory1(category1);
        mart.setCategory2(category2);
        mart.setAmount(amount);
        mart.setCount(count);
        return disclosureMartRepository.save(mart);
    }

    /**
     * 특정 기준일의 마트 데이터를 조회합니다.
     */
    public List<DisclosureMart> getMartEntries(String martType, LocalDate baseDate) {
        return disclosureMartRepository.findByMartTypeAndBaseDate(martType, baseDate);
    }

    /**
     * 만기별 공시 마트를 자동 집계합니다 (단순 예시 로직).
     */
    public void generateMaturityMart(LocalDate baseDate) {
        // 실제로는 LoanContract 등을 조회하여 만기 구간별로 집계하는 복잡한 로직이 들어감
        // 예: 1년 이내, 1~3년, 3~5년, 5년 초과
        createMartData("MATURITY", baseDate, "WITHIN_1Y", null, new BigDecimal("1000000"), 10);
        createMartData("MATURITY", baseDate, "1Y_TO_3Y", null, new BigDecimal("2000000"), 5);
    }
}
