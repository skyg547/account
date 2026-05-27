package com.ho.account.ecl.core.application.port.out;

import java.time.LocalDate;

/**
 * [Port] 담보 및 계약 매핑 대량 처리를 위한 아웃고잉 포트
 */
public interface CrCollateralBulkPort {
    
    /**
     * 기존 매핑 데이터를 초기화합니다 (Truncate)
     */
    void truncateAccountCollateralMappings();

    /**
     * 차주(Customer) 기반으로 계좌와 담보를 자동 매핑합니다
     */
    int createMappingsByCustomerId();
}
