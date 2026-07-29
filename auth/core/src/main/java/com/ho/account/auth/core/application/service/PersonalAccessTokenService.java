package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.infrastructure.persistence.PersonalAccessTokenJpaEntity;
import com.ho.account.auth.core.infrastructure.persistence.PersonalAccessTokenJpaRepository;
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

@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService {

    private final PersonalAccessTokenJpaRepository repository;
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

        PersonalAccessTokenJpaEntity entity = new PersonalAccessTokenJpaEntity(
                id, username, tokenName, tokenPrefix, tokenHash, "ACTIVE", expiresAt, now);

        repository.save(entity);

        return new PersonalAccessTokenCreateResponse(
                id, username, tokenName, rawToken, tokenPrefix, "ACTIVE", expiresAt, now);
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDto> getUserTokens(String username) {
        return repository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDto> getAllTokensForAdmin() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public boolean revokeToken(String currentUsername, String tokenId, boolean isAdmin) {
        return repository.findById(tokenId).map(entity -> {
            if (isAdmin || entity.getUsername().equalsIgnoreCase(currentUsername)) {
                entity.revoke();
                return true;
            }
            return false;
        }).orElse(false);
    }

    private PersonalAccessTokenDto toDto(PersonalAccessTokenJpaEntity entity) {
        String status = entity.getStatus();
        if ("ACTIVE".equals(status) && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            status = "EXPIRED";
        }
        return new PersonalAccessTokenDto(
                entity.getId(),
                entity.getUsername(),
                entity.getTokenName(),
                entity.getTokenPrefix(),
                status,
                entity.getExpiresAt(),
                entity.getLastUsedAt(),
                entity.getCreatedAt());
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
