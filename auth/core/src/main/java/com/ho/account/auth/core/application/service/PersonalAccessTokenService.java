package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.application.port.out.PersonalAccessTokenPort;
import com.ho.account.auth.core.domain.model.PersonalAccessToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인용 액세스 토큰 (PAT) 발급 및 관리를 위한 애플리케이션 서비스.
 *
 * <p>🐣 [초보자를 위한 헥사고날 아키텍처 & DIP 리팩토링 설명]
 *
 * 기존 구조의 문제점 (헥사고날 아키텍처 및 DIP 위반):
 * - 기존 서비스 코드는 Infrastructure 계층의 {@code PersonalAccessTokenJpaRepository}와 {@code PersonalAccessTokenJpaEntity}에 직접 의존했습니다.
 * - 이로 인해 비즈니스 로직 계층(Service)이 특정 DB 기술(JPA/Hibernate)에 강하게 결합되어 계층적격리가 깨지고 순수한 단위 테스트 완성이 어려웠습니다.
 *
 * 개선된 구조 (의존관계 역전 원칙 DIP 적용):
 * 1. Outbound Port 의존:
 *    - 서비스는 JPA Repository가 아닌 아웃바운드 포트 인터페이스인 {@link PersonalAccessTokenPort}에 의존합니다.
 * 2. Pure Domain POJO 활용:
 *    - 서비스는 JPA 엔티티 대신 Pure POJO 도메인 모델인 {@link PersonalAccessToken}을 생성하고 상태를 조작합니다.
 * 3. 유연성 및 테스트 용이성 확보:
 *    - 영속성 인프라(JPA Entity / DB)가 변경되더라도 비즈니스 로직 서비스 및 컨트롤러는 아무런 영향을 받지 않습니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService {

    private final PersonalAccessTokenPort patPort;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public PersonalAccessTokenCreateResponse createToken(String username, String tokenName, int expireDays) {
        String id = UUID.randomUUID().toString();

        // 1. 보안 랜덤 32바이트 헥사 문자열 생성 (pat_live_...)
        byte[] randomBytes = new byte[16];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomHex = HexFormat.of().formatHex(randomBytes);
        String rawToken = "pat_live_" + randomHex;

        String tokenPrefix = rawToken.substring(0, 13) + "...";
        String tokenHash = hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();
        int days = (expireDays <= 0) ? 90 : expireDays;
        LocalDateTime expiresAt = now.plusDays(days);

        // 2. Pure POJO 도메인 객체 생성 및 포트를 통한 저장
        PersonalAccessToken token = PersonalAccessToken.create(
                id, username, tokenName, tokenPrefix, tokenHash, expiresAt, now);

        patPort.save(token);

        return new PersonalAccessTokenCreateResponse(
                id, username, tokenName, rawToken, tokenPrefix, "ACTIVE", expiresAt, now);
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDto> getUserTokens(String username) {
        return patPort.findByUsername(username).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDto> getAllTokensForAdmin() {
        return patPort.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public boolean revokeToken(String currentUsername, String tokenId, boolean isAdmin) {
        return patPort.findById(tokenId).map(token -> {
            if (isAdmin || token.getUsername().equalsIgnoreCase(currentUsername)) {
                token.revoke();
                patPort.save(token);
                return true;
            }
            return false;
        }).orElse(false);
    }

    private PersonalAccessTokenDto toDto(PersonalAccessToken token) {
        LocalDateTime now = LocalDateTime.now();
        String status = token.getEffectiveStatus(now);
        return new PersonalAccessTokenDto(
                token.getId(),
                token.getUsername(),
                token.getTokenName(),
                token.getTokenPrefix(),
                status,
                token.getExpiresAt(),
                token.getLastUsedAt(),
                token.getCreatedAt());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
