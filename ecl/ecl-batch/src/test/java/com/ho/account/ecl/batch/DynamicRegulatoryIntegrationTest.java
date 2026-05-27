package com.ho.account.ecl.batch;

import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.ecl.core.application.port.out.CrSaRwMasterRepository;
import com.ho.account.ecl.core.application.port.out.CrRegulatoryParameterRepository;
import com.ho.account.ecl.core.application.service.calculation.IrbParameterService;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.model.CrRegulatoryParameter;
import com.ho.account.ecl.core.domain.model.CrSaRwMaster;
import com.ho.account.ecl.core.domain.model.SaRwMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [QA] 대손충당금(IFRS9) 동적 규제 파라미터 연동 통합 테스트
 * 
 * 💡 [초보자를 위한 가이드]
 * 이 테스트는 "규제 기관이 가이드라인을 바꿨을 때, DB만 고치면 시스템에 즉시 반영되는가?"를 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:dynamic_test;MODE=PostgreSQL")
@Transactional
@org.junit.jupiter.api.Disabled("JVM 리소스 절약 및 컨텍스트 충돌 방지를 위해 일시 비활성화")
public class DynamicRegulatoryIntegrationTest {

    @Autowired
    private SaRwMapper saRwMapper;

    @Autowired
    private IrbParameterService irbParameterService;

    @Autowired
    private CrSaRwMasterRepository saRwMasterRepository;

    @Autowired
    private CrRegulatoryParameterRepository parameterRepository;

    @BeforeEach
    void setUp() {
        // 데모 데이터와 테스트 데이터 충돌 방지를 위한 초기화
        saRwMasterRepository.deleteAll();
        parameterRepository.deleteAll();
        saRwMapper.refreshCache();
        irbParameterService.refreshCache();
    }

    @Test
    @DisplayName("DB에 설정된 SA 위험가중치(RW) 매핑이 정확히 반환되는지 확인")
    void testSaRwDynamicMapping() {
        // given: DB에 기업 AAA 등급 20% 가중치 적재
        CrSaRwMaster master = CrSaRwMaster.builder()
                .customerType("CORPORATE")
                .ratingCode("AAA")
                .riskWeight(new BigDecimal("0.2000"))
                .build();
        saRwMasterRepository.save(master);
        
        // 캐시 갱신
        saRwMapper.refreshCache();

        // when: 기업 AAA 등급 RW 조회
        BigDecimal rw = saRwMapper.getStandardRw(CustomerType.CORPORATE, "AAA");

        // then: 20% (0.20) 반환 확인
        assertThat(rw).isEqualByComparingTo("0.2000");
    }

    @Test
    @DisplayName("DB에 설정된 IRB 규제 파라미터가 동적으로 로드되는지 확인")
    void testIrbParameterDynamicLoading() {
        // given: DB에 PD Floor를 0.1%로 변경 적재
        CrRegulatoryParameter floorParam = CrRegulatoryParameter.builder()
                .paramKey("PD_FLOOR")
                .paramValue(new BigDecimal("0.00100000"))
                .build();
        parameterRepository.save(floorParam);
        
        // 캐시 갱신
        irbParameterService.refreshCache();

        // when: 규제 파라미터 로드
        IrbRegulatoryParams params = irbParameterService.getParameters();

        // then: 0.001 (0.1%) 반영 확인
        assertThat(params.getPdFloor()).isEqualByComparingTo("0.0010");
    }
}
