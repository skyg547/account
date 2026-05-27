package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [여신 도메인] 아파트 담보 상세 정보 저장소
 */
@Repository
public interface OdsApartCollDetailRepository extends JpaRepository<OdsApartCollDetail, String> {
}
