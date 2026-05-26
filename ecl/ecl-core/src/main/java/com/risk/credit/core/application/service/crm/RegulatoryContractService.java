package com.risk.credit.core.application.service.crm;

import com.risk.credit.core.application.port.out.CrCollateralBulkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * [도메인 서비스] 규제 계약 관리 서비스 (Regulatory Contract Service)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 리스크 산출의 '첫 단추'를 꿰는 역할을 합니다. 
 * 은행에 흩어져 있는 대출(계약)과 담보 데이터를 가져와서 서로 연결해 주는 '법적 매핑 테이블'을 만듭니다.
 * 이 매핑이 정확해야 나중에 담보 배분도 정확하게 이루어집니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryContractService {

    private final CrCollateralBulkPort collateralBulkPort;

    /**
     * [Phase 1-3] 기준일자의 계약-담보 간 규제 매핑 관계를 초기화하고 재생성합니다.
     * 
     * 💡 [비즈니스 가이드]
     * 담보 배분(Allocation)을 수행하기 위해서는 어떤 대출(계약)이 어떤 담보를 사용할 수 있는지에 대한 '후보군' 명세가 필요합니다.
     * 이 메서드는 시스템에 등록된 고객 정보를 바탕으로 대출과 담보를 1차적으로 자동 매칭합니다.
     * 
     * @param baseDate 산출 기준일
     * @return 생성된 매핑 건수
     */
    @Transactional
    public int initializeRegulatoryContracts(LocalDate baseDate) {
        log.info("🚀 [Domain Service] 규제 계약-담보 매핑 관계 생성 시작 (BaseDate: {})", baseDate);

        // [아키텍처 포인트]
        // 억 단위 데이터를 관리하기 위해 JPA 대신 인프라 레이어의 Bulk SQL 어댑터를 직접 호출합니다.
        // 서비스 레이어는 저수준의 SQL 쿼리 대신 포트 인터페이스를 통해 대량 처리의 의도만 전달합니다.

        // 1. 기존 매핑 데이터 초기화 (Idempotency 보장)
        // 재실행 시 데이터가 중첩되어 중복 가산되는 현상을 방지하기 위해 기존 매핑 정보를 Truncate 처리합니다.
        log.info("  -> [1/2] 기존 매핑 데이터(cr_account_collaterals) 초기화 중...");
        collateralBulkPort.truncateAccountCollateralMappings();

        // 2. 계좌-담보 자동 매핑 (차주 ID 기반 조인)
        // CustomerID가 동일한 대출계좌와 담보를 DB 레벨에서 직접 조인하여 삽입합니다.
        // 이는 바젤 III 규제상 동일 차주 내부의 담보 대체성을 가장 빠르게 구현하는 방식입니다.
        log.info("  -> [2/2] 차주(Customer) ID 기반 계약-담보 일괄 매칭 수행 중...");
        int insertedCount = collateralBulkPort.createMappingsByCustomerId();
        
        log.info("✅ [Domain Service] 총 {}건의 규제 매핑 관계 생성 완료.", insertedCount);
        
        return insertedCount;
    }
}
