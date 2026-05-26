package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.exposure.CrCustomer;
import java.util.List;

/**
 * [Port] 고객 대량 처리를 위한 아웃고잉 포트
 */
public interface CrCustomerBulkPort {
    /**
     * [고도화] 데이터 마트 동기화를 위한 고객 대량 Upsert
     */
    void bulkUpsert(List<CrCustomer> customers);
}
