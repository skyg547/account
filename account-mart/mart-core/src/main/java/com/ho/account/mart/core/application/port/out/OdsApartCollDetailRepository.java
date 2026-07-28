package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;

import java.util.Optional;

/**
 * [Outbound Port] 아파트 담보 상세 데이터 접근 인터페이스.
 *
 * <p>초보자 설명: core 업무 규칙은 JPA나 SQL을 직접 알면 안 됩니다. 이 port는
 * "담보번호로 아파트 상세를 찾는다"는 업무 계약만 표현하고, 실제 DB 조회 방식은
 * infrastructure adapter가 담당합니다.</p>
 */
public interface OdsApartCollDetailRepository {
    Optional<OdsApartCollDetail> findByCollateralId(String collateralId);

    OdsApartCollDetail save(OdsApartCollDetail detail);

    void saveAll(Iterable<OdsApartCollDetail> details);
}
