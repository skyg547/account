package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.domain.model.PersonalAccessToken;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
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
 * PersonalAccessTokenPersistenceAdapter 단위 테스트.
 *
 * <p>🐣 [초보자를 위한 설명]
 * 이 클래스는 영속성 어댑터가 JPA 저장소와 도메인 모델 간 변환(Data Mapping) 및 상호작용을
 * 올바르게 수행하는지 검증하는 단위 테스트입니다.
 * </p>
 */
class PersonalAccessTokenPersistenceAdapterTest {

    private PersonalAccessTokenJpaRepository repository;
    private PersonalAccessTokenPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(PersonalAccessTokenJpaRepository.class);
        adapter = new PersonalAccessTokenPersistenceAdapter(repository);
    }

    @Test
    @DisplayName("Domain POJO를 저장하면 JPA Entity로 변환되어 Repository.save()가 호출되고 Domain POJO가 반환된다.")
    void save_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PersonalAccessToken token = PersonalAccessToken.create(
                "id-123", "user1", "Key 1", "pat_live_abc...", "hash123", now.plusDays(30), now);

        PersonalAccessTokenJpaEntity entity = PersonalAccessTokenJpaEntity.fromDomain(token);
        when(repository.save(any(PersonalAccessTokenJpaEntity.class))).thenReturn(entity);

        // when
        PersonalAccessToken saved = adapter.save(token);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("id-123");
        assertThat(saved.getUsername()).isEqualTo("user1");

        ArgumentCaptor<PersonalAccessTokenJpaEntity> captor = ArgumentCaptor.forClass(PersonalAccessTokenJpaEntity.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("id-123");
    }

    @Test
    @DisplayName("findByUsername 호출 시 Entity 목록을 조회하여 Domain POJO 목록으로 매핑하여 반환한다.")
    void findByUsername_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PersonalAccessTokenJpaEntity entity = new PersonalAccessTokenJpaEntity(
                "id-123", "user1", "Key 1", "pat_live_abc...", "hash123", "ACTIVE", now.plusDays(30), now);
        when(repository.findByUsernameOrderByCreatedAtDesc("user1")).thenReturn(List.of(entity));

        // when
        List<PersonalAccessToken> result = adapter.findByUsername("user1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("id-123");
        assertThat(result.get(0).getUsername()).isEqualTo("user1");
    }

    @Test
    @DisplayName("findById 호출 시 Entity를 Domain POJO로 정상 변환하여 반환한다.")
    void findById_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PersonalAccessTokenJpaEntity entity = new PersonalAccessTokenJpaEntity(
                "id-123", "user1", "Key 1", "pat_live_abc...", "hash123", "ACTIVE", now.plusDays(30), now);
        when(repository.findById("id-123")).thenReturn(Optional.of(entity));

        // when
        Optional<PersonalAccessToken> result = adapter.findById("id-123");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("id-123");
    }
}

/** Verifies the conditional SQL write against H2 rather than a repository mock. */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = PersonalAccessTokenConditionalRevokeJpaTest.JpaTestConfiguration.class)
class PersonalAccessTokenConditionalRevokeJpaTest {

    @Autowired private PersonalAccessTokenPersistenceAdapter adapter;
    @Autowired private PersonalAccessTokenJpaRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    void activeTokenHasOnlyOneSuccessfulConditionalRevoke() {
        LocalDateTime now = LocalDateTime.now();
        repository.saveAndFlush(new PersonalAccessTokenJpaEntity(
                "conditional-id", "owner", "Key", "pat_live_123...", "conditional-hash",
                "ACTIVE", now.plusDays(1), now));

        assertThat(adapter.markRevokedIfActive("conditional-id")).isTrue();
        assertThat(adapter.markRevokedIfActive("conditional-id")).isFalse();
        assertThat(adapter.markRevokedIfActive("missing-id")).isFalse();

        // A bulk update bypasses the first-level cache; reloading proves persisted state.
        entityManager.clear();
        assertThat(repository.findById("conditional-id").orElseThrow().getStatus())
                .isEqualTo("REVOKED");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = PersonalAccessTokenJpaRepository.class)
    @EntityScan(basePackageClasses = PersonalAccessTokenJpaEntity.class)
    @Import(PersonalAccessTokenPersistenceAdapter.class)
    static class JpaTestConfiguration {
    }
}
