package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.exception.PatAccessDeniedException;
import com.ho.account.auth.core.application.model.PatActor;
import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.application.port.in.PersonalAccessTokenUseCase;
import com.ho.account.auth.core.application.port.out.PatCredentialVerifierPort;
import com.ho.account.auth.core.application.port.out.PatLifecycleEventPort;
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

/** Coordinates PAT lifecycle changes after resolving the actor from a verified credential. */
@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService implements PersonalAccessTokenUseCase {

    private final PersonalAccessTokenPort patPort;
    private final PatCredentialVerifierPort credentialVerifier;
    private final PatLifecycleEventPort lifecycleEvents;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public PersonalAccessTokenCreateResponse createToken(String authorizationHeader, String tokenName, int expireDays) {
        PatActor actor = credentialVerifier.verify(authorizationHeader);
        String id = UUID.randomUUID().toString();

        // 1. 보안 랜덤 16바이트 헥사 문자열 생성 (pat_live_...)
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
                id, actor.username(), tokenName, tokenPrefix, tokenHash, expiresAt, now);

        patPort.save(token);
        lifecycleEvents.recordCreated(actor.username(), id, token.getUsername(), now);

        return new PersonalAccessTokenCreateResponse(
                id, actor.username(), tokenName, rawToken, tokenPrefix, "ACTIVE", expiresAt, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDto> getUserTokens(String authorizationHeader) {
        PatActor actor = credentialVerifier.verify(authorizationHeader);
        return patPort.findByUsername(actor.username()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDto> getAllTokensForAdmin(String authorizationHeader) {
        requireAdministrator(credentialVerifier.verify(authorizationHeader));
        return patPort.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public boolean revokeOwnToken(String authorizationHeader, String tokenId) {
        PatActor actor = credentialVerifier.verify(authorizationHeader);
        return revokeAuthorizedToken(actor, tokenId, false);
    }

    @Override
    @Transactional
    public boolean revokeTokenForAdmin(String authorizationHeader, String tokenId) {
        PatActor actor = credentialVerifier.verify(authorizationHeader);
        requireAdministrator(actor);
        return revokeAuthorizedToken(actor, tokenId, true);
    }

    private boolean revokeAuthorizedToken(PatActor actor, String tokenId, boolean administratorRoute) {
        return patPort.findById(tokenId).map(token -> {
            // AuthUser identity keys are case-sensitive; a case variant never owns this PAT.
            if (!administratorRoute && !token.getUsername().equals(actor.username())) {
                return false;
            }
            // The conditional write resolves races after this ownership read.
            if (patPort.markRevokedIfActive(token.getId())) {
                lifecycleEvents.recordRevoked(actor.username(), token.getId(), token.getUsername(), LocalDateTime.now());
            }
            return true;
        }).orElse(false);
    }

    private void requireAdministrator(PatActor actor) {
        if (!actor.systemAdmin()) {
            throw new PatAccessDeniedException();
        }
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
