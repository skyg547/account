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

    /**
     * 일반 조회·테스트 용도로 미결 항목을 조회합니다.
     * 반제처럼 최신 상태에 대한 독점 처리가 필요한 쓰기 흐름에는 사용하지 않습니다.
     */
    Optional<UnsettledItem> findById(Long id);

    /**
     * 반제할 미결 항목의 최신 상태를 조회하고 호출자 트랜잭션이 끝날 때까지 독점 점유합니다.
     *
     * <p>호출자는 활성 쓰기 트랜잭션 안에서 이 메서드를 호출해야 합니다. 어댑터는 같은
     * 미결 항목의 동시 반제가 직렬화되도록 현재 aggregate를 반환하고, 점유는 호출자
     * 트랜잭션의 commit 또는 rollback까지 유지해야 합니다.</p>
     */
    Optional<UnsettledItem> findByIdForSettlement(Long id);

    List<UnsettledItem> findActive();

    List<UnsettledItem> findActiveByBusinessPartnerCode(String businessPartnerCode);
}
