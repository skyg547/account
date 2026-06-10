package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.CrProductMaster;
import java.util.List;
import java.util.Optional;

/**
 * [Repository] 대손충당금(IFRS9)용 상품 마스터(Product Master) 저장소.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 은행의 모든 대출 상품 종류(예: 주택담보대출, 전세자금대출 등)를 관리하는 창구입니다.
 * 각 상품별로 부도가 났을 때 한도 대비 얼마나 더 인출될지(CCF) 등의 기준 정보를 가져올 때 사용됩니다.
 */
public interface CrProductMasterRepository {
    List<CrProductMaster> findAll();
    Optional<CrProductMaster> findByProductCode(String productCode);
}
