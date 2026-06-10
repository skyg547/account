package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;
import com.ho.account.journalledger.application.port.out.UnsettledItemPersistencePort;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    @Override
    public UnsettledItem save(UnsettledItem item) {
        return repository.save(item);
    }

    @Override
    public Optional<UnsettledItem> findById(Long id) {
        return repository.findById(id);
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
