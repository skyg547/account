package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.ods.common.OdsProductMst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * OdsProductMst 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 * 은행의 금융 상품 마스터 정보를 관리합니다.
 */
@Repository
public interface OdsProductMstRepository extends JpaRepository<OdsProductMst, String> {
}
