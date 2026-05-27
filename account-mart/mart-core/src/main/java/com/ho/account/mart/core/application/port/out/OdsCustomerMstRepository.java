package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.common.OdsCustomerMst;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 고객 마스터 데이터 접근 인터페이스
 */
public interface OdsCustomerMstRepository {
    Optional<OdsCustomerMst> findById(String customerCode);
    Optional<OdsCustomerMst> findByCustomerCode(String customerCode);
    List<OdsCustomerMst> findAll();
    OdsCustomerMst save(OdsCustomerMst customerMst);
    void saveAll(Iterable<OdsCustomerMst> customerMsts);
}
