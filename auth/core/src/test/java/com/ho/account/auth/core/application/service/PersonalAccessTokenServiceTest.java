package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ho.account.auth.core.application.exception.PatAccessDeniedException;
import com.ho.account.auth.core.application.exception.PatAuthenticationException;
import com.ho.account.auth.core.application.model.PatActor;
import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.port.out.PatCredentialVerifierPort;
import com.ho.account.auth.core.application.port.out.PatLifecycleEventPort;
import com.ho.account.auth.core.application.port.out.PersonalAccessTokenPort;
import com.ho.account.auth.core.domain.model.PersonalAccessToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PersonalAccessTokenServiceTest {
    private static final String ALICE_BEARER = "Bearer alice-verified";
    private static final String CASE_BEARER = "Bearer Alice-verified";
    private static final String BOB_BEARER = "Bearer bob-verified";
    private static final String ADMIN_BEARER = "Bearer admin-verified";

    private PersonalAccessTokenPort patPort;
    private PatCredentialVerifierPort verifier;
    private PatLifecycleEventPort events;
    private PersonalAccessTokenService service;

    @BeforeEach
    void setUp() {
        patPort = mock(PersonalAccessTokenPort.class);
        verifier = mock(PatCredentialVerifierPort.class);
        events = mock(PatLifecycleEventPort.class);
        service = new PersonalAccessTokenService(patPort, verifier, events);
        when(verifier.verify(ALICE_BEARER)).thenReturn(new PatActor("alice", false));
        when(verifier.verify(CASE_BEARER)).thenReturn(new PatActor("Alice", false));
        when(verifier.verify(BOB_BEARER)).thenReturn(new PatActor("bob", false));
        when(verifier.verify(ADMIN_BEARER)).thenReturn(new PatActor("administrator", true));
    }

    @Test
    void creationUsesVerifiedCanonicalOwnerAndRecordsActorWithoutRawToken() {
        when(patPort.save(any(PersonalAccessToken.class))).thenAnswer(call -> call.getArgument(0));

        PersonalAccessTokenCreateResponse created = service.createToken(ALICE_BEARER, "Agent Key", 30);

        ArgumentCaptor<PersonalAccessToken> saved = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(patPort).save(saved.capture());
        assertThat(created.username()).isEqualTo("alice");
        assertThat(created.rawToken()).startsWith("pat_live_");
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getTokenHash()).doesNotContain(created.rawToken());
        assertThat(saved.getValue().getTokenPrefix()).doesNotContain(created.rawToken());
        verify(events).recordCreated(eq("alice"), eq(created.id()), eq("alice"), any(LocalDateTime.class));
        verify(verifier).verify(ALICE_BEARER);
    }

    @Test
    void userListingUsesOnlyVerifiedPrincipalAndExactOwnerKey() {
        PersonalAccessToken aliceToken = activeToken("alice");
        when(patPort.findByUsername("alice")).thenReturn(List.of(aliceToken));

        var listed = service.getUserTokens(ALICE_BEARER);

        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).id()).isEqualTo(aliceToken.getId());
        assertThat(listed.get(0).username()).isEqualTo("alice");
        verify(patPort).findByUsername("alice");
        verify(patPort, never()).findByUsername("bob");
        verify(verifier).verify(ALICE_BEARER);
    }

    @Test
    void ordinaryUserCannotListAllOrForceRevokeEvenWithKnownTokenId() {
        assertThatThrownBy(() -> service.getAllTokensForAdmin(ALICE_BEARER))
                .isInstanceOf(PatAccessDeniedException.class);
        assertThatThrownBy(() -> service.revokeTokenForAdmin(ALICE_BEARER, "id-1"))
                .isInstanceOf(PatAccessDeniedException.class);

        verify(patPort, never()).findAll();
        verify(patPort, never()).findById(any());
        verifyNoInteractions(events);
    }

    @Test
    void verifiedAdministratorCanListAndForceRevokeWithActorAttribution() {
        PersonalAccessToken bobToken = activeToken("bob");
        when(patPort.findAll()).thenReturn(List.of(bobToken));
        when(patPort.findById("id-1")).thenReturn(Optional.of(bobToken));
        when(patPort.markRevokedIfActive("id-1")).thenReturn(true);

        assertThat(service.getAllTokensForAdmin(ADMIN_BEARER)).hasSize(1);
        assertThat(service.revokeTokenForAdmin(ADMIN_BEARER, "id-1")).isTrue();

        verify(patPort).markRevokedIfActive("id-1");
        verify(events).recordRevoked(eq("administrator"), eq("id-1"), eq("bob"), any(LocalDateTime.class));
    }

    @Test
    void nonOwnerIncludingCaseVariantCannotRevokeAndCannotEmitEvent() {
        PersonalAccessToken aliceToken = activeToken("alice");
        when(patPort.findById("id-1")).thenReturn(Optional.of(aliceToken));

        assertThat(service.revokeOwnToken(BOB_BEARER, "id-1")).isFalse();
        assertThat(service.revokeOwnToken(CASE_BEARER, "id-1")).isFalse();

        assertThat(aliceToken.getStatus()).isEqualTo("ACTIVE");
        verify(patPort, never()).markRevokedIfActive(any());
        verifyNoInteractions(events);
    }

    @Test
    void ownerCanRevokeAndRepeatedRevokeDoesNotDuplicateEvent() {
        PersonalAccessToken aliceToken = activeToken("alice");
        when(patPort.findById("id-1")).thenReturn(Optional.of(aliceToken));
        when(patPort.markRevokedIfActive("id-1")).thenReturn(true, false);

        assertThat(service.revokeOwnToken(ALICE_BEARER, "id-1")).isTrue();
        assertThat(service.revokeOwnToken(ALICE_BEARER, "id-1")).isTrue();

        verify(patPort, times(2)).markRevokedIfActive("id-1");
        verify(events).recordRevoked(eq("alice"), eq("id-1"), eq("alice"), any(LocalDateTime.class));
    }

    @Test
    void missingOrInvalidCredentialIsRejectedBeforePatStorageAccess() {
        when(verifier.verify(null)).thenThrow(new PatAuthenticationException());

        assertThatThrownBy(() -> service.createToken(null, "Key", 30))
                .isInstanceOf(PatAuthenticationException.class);
        assertThatThrownBy(() -> service.getUserTokens(null))
                .isInstanceOf(PatAuthenticationException.class);
        assertThatThrownBy(() -> service.getAllTokensForAdmin(null))
                .isInstanceOf(PatAuthenticationException.class);
        assertThatThrownBy(() -> service.revokeOwnToken(null, "id-1"))
                .isInstanceOf(PatAuthenticationException.class);
        assertThatThrownBy(() -> service.revokeTokenForAdmin(null, "id-1"))
                .isInstanceOf(PatAuthenticationException.class);
        verifyNoInteractions(patPort, events);
    }

    private PersonalAccessToken activeToken(String username) {
        LocalDateTime now = LocalDateTime.now();
        return PersonalAccessToken.create(
                "id-1", username, "Key", "pat_live_123...", "hash1", now.plusDays(10), now);
    }
}
