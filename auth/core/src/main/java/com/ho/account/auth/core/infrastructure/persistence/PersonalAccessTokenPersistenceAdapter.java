package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.PersonalAccessTokenPort;
import com.ho.account.auth.core.domain.model.PersonalAccessToken;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인용 액세스 토큰 (PAT) 영속성 어댑터 (Persistence Adapter).
 *
 * <p>🐣 [초보자를 위한 헥사고날 아키텍처 & 어댑터 패턴 설명]
 * 1. 아웃바운드 어댑터 (Outbound Persistence Adapter):
 *    - 헥사고날 아키텍처에서 아웃바운드 포트({@link PersonalAccessTokenPort})를 실제로 구현하는 기술 세부 구현체입니다.
 *    - 본 클래스는 JPA 기술({@link PersonalAccessTokenJpaRepository})을 사용해서 데이터베이스 데이터를 영속화하고 조회합니다.
 *
 * 2. Data Mapper 역할 (Entity <-> Domain POJO 변환):
 *    - DB 테이블과 직접 매핑되는 JPA 엔티티({@link PersonalAccessTokenJpaEntity})와
 *      순수 비즈니스 객체인 도메인 POJO({@link PersonalAccessToken}) 사이의 격리를 담당합니다.
 *    - 영속성 계층 내부에서 일어나는 기술적 변경(예: 테이블 칼럼 추가/변경, ORM 기술 변경)이 도메인 모델에 영향을 주지 않도록 보호합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class PersonalAccessTokenPersistenceAdapter implements PersonalAccessTokenPort {

    private final PersonalAccessTokenJpaRepository repository;

    @Override
    public PersonalAccessToken save(PersonalAccessToken token) {
        PersonalAccessTokenJpaEntity entity = PersonalAccessTokenJpaEntity.fromDomain(token);
        PersonalAccessTokenJpaEntity savedEntity = repository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public boolean markRevokedIfActive(String tokenId) {
        // The conditional update serializes competing revocations at the token row.
        return repository.markRevokedIfActive(tokenId) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalAccessToken> findByUsername(String username) {
        return repository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(PersonalAccessTokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalAccessToken> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(PersonalAccessTokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonalAccessToken> findById(String id) {
        return repository.findById(id).map(PersonalAccessTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonalAccessToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(PersonalAccessTokenJpaEntity::toDomain);
    }
}
