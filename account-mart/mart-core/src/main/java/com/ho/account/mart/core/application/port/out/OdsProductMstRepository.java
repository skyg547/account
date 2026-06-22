package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.common.OdsProductMst;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 상품 마스터 데이터 접근 인터페이스입니다.
 */
public interface OdsProductMstRepository {

    Optional<OdsProductMst> findById(String productCode);

    List<OdsProductMst> findAll();

    OdsProductMst save(OdsProductMst productMst);

    void saveAll(Iterable<OdsProductMst> productMsts);
}
