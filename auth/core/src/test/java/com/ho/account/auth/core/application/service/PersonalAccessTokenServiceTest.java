package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.application.port.out.PersonalAccessTokenPort;
import com.ho.account.auth.core.domain.model.PersonalAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PersonalAccessTokenService 단위 테스트.
 *
 * <p>🐣 [초보자를 위한 단위 테스트 설명]
 * PersonalAccessTokenService가 PersonalAccessTokenPort 인터페이스에 의존하도록 DIP를 적용했기 때문에,
 * 실제 데이터베이스(JPA) 없이도 Mockito를 이용해 Port를 가짜 객체(Mock)로 대체하여
 * 초단위의 빠른 단위 테스트(Unit Test)를 작성할 수 있습니다.
 * </p>
 */
class PersonalAccessTokenServiceTest {

    private PersonalAccessTokenPort patPort;
    private PersonalAccessTokenService patService;

    @BeforeEach
    void setUp() {
        patPort = mock(PersonalAccessTokenPort.class);
        patService = new PersonalAccessTokenService(patPort);
    }

    @Test
    @DisplayName("PAT 생성 시 보안 rawToken이 생성되고, Port를 통해 Pure POJO 도메인 모델이 저장된다.")
    void createToken_success() {
        // given
        when(patPort.save(any(PersonalAccessToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PersonalAccessTokenCreateResponse response = patService.createToken("user1", "Agent Key", 30);

        // then
        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("user1");
        assertThat(response.tokenName()).isEqualTo("Agent Key");
        assertThat(response.rawToken()).startsWith("pat_live_");
        assertThat(response.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<PersonalAccessToken> captor = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(patPort, times(1)).save(captor.capture());
        PersonalAccessToken savedToken = captor.getValue();
        assertThat(savedToken.getUsername()).isEqualTo("user1");
        assertThat(savedToken.getTokenName()).isEqualTo("Agent Key");
        assertThat(savedToken.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("사용자의 PAT 목록을 조회하면 DTO 목록으로 올바르게 변환된다.")
    void getUserTokens_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PersonalAccessToken token1 = PersonalAccessToken.create(
                "id-1", "user1", "Key 1", "pat_live_123...", "hash1", now.plusDays(10), now);
        when(patPort.findByUsername("user1")).thenReturn(List.of(token1));

        // when
        List<PersonalAccessTokenDto> dtos = patService.getUserTokens("user1");

        // then
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).id()).isEqualTo("id-1");
        assertThat(dtos.get(0).username()).isEqualTo("user1");
        assertThat(dtos.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("토큰 소유자가 맞으면 revokeToken이 성공하고 status가 REVOKED로 저장된다.")
    void revokeToken_byOwner_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PersonalAccessToken token = PersonalAccessToken.create(
                "id-1", "user1", "Key 1", "pat_live_123...", "hash1", now.plusDays(10), now);
        when(patPort.findById("id-1")).thenReturn(Optional.of(token));

        // when
        boolean result = patService.revokeToken("user1", "id-1", false);

        // then
        assertThat(result).isTrue();
        assertThat(token.getStatus()).isEqualTo("REVOKED");
        verify(patPort, times(1)).save(token);
    }

    @Test
    @DisplayName("다른 사용자의 토큰을 비관리자가 폐기하려 하면 실패한다.")
    void revokeToken_byOtherUser_fail() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PersonalAccessToken token = PersonalAccessToken.create(
                "id-1", "user1", "Key 1", "pat_live_123...", "hash1", now.plusDays(10), now);
        when(patPort.findById("id-1")).thenReturn(Optional.of(token));

        // when
        boolean result = patService.revokeToken("otherUser", "id-1", false);

        // then
        assertThat(result).isFalse();
        assertThat(token.getStatus()).isEqualTo("ACTIVE");
        verify(patPort, never()).save(any());
    }
}
