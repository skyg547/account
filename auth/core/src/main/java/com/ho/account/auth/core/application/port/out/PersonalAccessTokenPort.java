package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.PersonalAccessToken;
import java.util.List;
import java.util.Optional;

/**
 * 개인용 액세스 토큰 (PAT) 영속성을 위한 아웃바운드 포트 (Outbound Port) 인터페이스.
 *
 * <p>🐣 [초보자를 위한 헥사고날 아키텍처 & DIP 설명]
 * 1. 헥사고날 아키텍처 (Hexagonal Architecture / Ports and Adapters):
 *    - 핵심 비즈니스 로직(Application Service)은 데이터가 데이터베이스(JPA), 파일 시스템, 외부 API 중 어디에 저장되는지 알 필요가 없습니다.
 *    - 이 인터페이스는 비즈니스 계층이 외부(영속성 계층)로 나가는 '출구 포트(Outbound Port)' 역할을 합니다.
 *
 * 2. 의존관계 역전 원칙 (DIP: Dependency Inversion Principle):
 *    - 서비스(상위 모듈)가 JPA Entity나 JPA Repository 같은 특정 세부 기술 구현체(하위 모듈)를 직접 참조하지 않습니다.
 *    - 대신 추상화된 이 포트 인터페이스에 의존합니다.
 *    - 인프라 어댑터가 이 포트 인터페이스를 구현(Implement)함으로써, 의존성의 방향이 '서비스 -> 인프라'에서 '인프라 -> 서비스(포트)'로 역전(Inversion)됩니다.
 *    - 이로 인해 단위 테스트 시 Mocking이나 In-Memory 구현체 대체가 매우 용이해집니다.
 * </p>
 */
public interface PersonalAccessTokenPort {

    /**
     * PAT 도메인 객체를 저장하거나 업데이트합니다.
     *
     * @param token 저장할 PAT 도메인 객체
     * @return 저장 완료된 PAT 도메인 객체
     */
    PersonalAccessToken save(PersonalAccessToken token);

    /**
     * 특정 사용자(username)의 모든 PAT 목록을 생성일시 역순으로 조회합니다.
     *
     * @param username 사용자명
     * @return PAT 도메인 목록
     */
    List<PersonalAccessToken> findByUsername(String username);

    /**
     * 전사 모든 PAT 목록을 생성일시 역순으로 조회합니다 (관리자용).
     *
     * @return PAT 도메인 목록
     */
    List<PersonalAccessToken> findAll();

    /**
     * 식별자(ID)로 PAT 도메인 객체를 조회합니다.
     *
     * @param id 토큰 식별자 UUID
     * @return 조회된 PAT 도메인 객체 (Optional)
     */
    Optional<PersonalAccessToken> findById(String id);

    /**
     * Atomically changes an ACTIVE PAT to REVOKED. Returns true only for the transaction that
     * changed the row, so concurrent revocations produce one lifecycle event.
     */
    boolean markRevokedIfActive(String tokenId);

    /**
     * 해시값(tokenHash)으로 PAT 도메인 객체를 조회합니다.
     *
     * @param tokenHash SHA-256 해시값
     * @return 조회된 PAT 도메인 객체 (Optional)
     */
    Optional<PersonalAccessToken> findByTokenHash(String tokenHash);
}
