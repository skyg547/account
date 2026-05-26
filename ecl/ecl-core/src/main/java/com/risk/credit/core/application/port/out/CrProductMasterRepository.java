package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.model.CrProductMaster;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * [Repository] 신용 리스크용 상품 마스터(Product Master) 저장소.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 은행의 모든 대출 상품 종류(예: 주택담보대출, 전세자금대출 등)를 관리하는 창구입니다.
 * 각 상품별로 부도가 났을 때 한도 대비 얼마나 더 인출될지(CCF) 등의 기준 정보를 가져올 때 사용됩니다.
 */
public interface CrProductMasterRepository extends JpaRepository<CrProductMaster, Long> {
    @Cacheable(value = "productMaster", key = "#productCode")
    Optional<CrProductMaster> findByProductCode(String productCode);
}
