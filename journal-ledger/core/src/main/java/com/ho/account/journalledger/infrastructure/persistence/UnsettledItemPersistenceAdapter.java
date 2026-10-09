package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.infrastructure.persistence.repository.UnsettledItemRepository;
import com.ho.account.journalledger.application.port.out.UnsettledItemPersistencePort;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 미결 항목 JPA 출력 어댑터.
 *
 * <p>거래처별 미결 조회를 DB 조건으로 실행하여, 전체 미결 데이터를 메모리에 올린 뒤
 * 필터링하던 방식의 메모리 사용량과 불필요한 데이터 전송을 줄입니다.</p>
 */
@Component
@RequiredArgsConstructor
public class UnsettledItemPersistenceAdapter implements UnsettledItemPersistencePort {

    private final UnsettledItemRepository repository;
    private final EntityManager entityManager;

    @Override
    public UnsettledItem save(UnsettledItem item) {
        return repository.save(item);
    }

    @Override
    public Optional<UnsettledItem> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 반제 대상 부모 행을 잠근 뒤 최신 aggregate를 반환합니다.
     *
     * <p>{@code READ_COMMITTED}에서는 선행 반제의 commit/rollback까지 기다린 다음 refresh하여
     * 이미 1차 캐시에 있던 금액과 참조 컬렉션도 현재 상태로 교체합니다. 잠금은 호출자
     * 트랜잭션 종료까지 유지되며, deadlock/serialization 실패는 전체 반제를 새 트랜잭션에서
     * 재시도할 수 있도록 그대로 전파합니다.</p>
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UnsettledItem> findByIdForSettlement(Long id) {
        entityManager.flush();
        if (repository.lockByIdForSettlement(id).isEmpty()) {
            return Optional.empty();
        }
        return repository.findById(id).map(item -> {
            // 행 잠금만으로는 대기 전에 관리되던 snapshot이 갱신되지 않으므로 명시적으로 다시 읽습니다.
            entityManager.refresh(item);
            item.getSettlementReferences().size();
            return item;
        });
    }

    @Override
    public List<UnsettledItem> findActive() {
        return repository.findByResolvedFalse();
    }

    @Override
    public List<UnsettledItem> findActiveByBusinessPartnerCode(String businessPartnerCode) {
        return repository.findByResolvedFalseAndBusinessPartnerCode(businessPartnerCode);
    }
}
