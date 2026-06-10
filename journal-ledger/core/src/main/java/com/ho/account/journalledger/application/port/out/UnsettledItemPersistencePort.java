package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.unsettled.UnsettledItem;

import java.util.List;
import java.util.Optional;

/**
 * 미결 항목 영속성 출력 포트.
 *
 * <p>애플리케이션 서비스는 "미결 항목을 저장하고 조회한다"는 업무 의도에만 의존합니다.
 * 실제 DB 조회 방식과 Spring Data JPA는 출력 어댑터가 담당합니다.</p>
 *
 * <p>의존 방향:
 * {@code UnsettledService -> UnsettledItemPersistencePort <- UnsettledItemPersistenceAdapter}</p>
 */
public interface UnsettledItemPersistencePort {

    UnsettledItem save(UnsettledItem item);

    Optional<UnsettledItem> findById(Long id);

    List<UnsettledItem> findActive();

    List<UnsettledItem> findActiveByBusinessPartnerCode(String businessPartnerCode);
}
