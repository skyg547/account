package com.ho.account.auth.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.infrastructure.security.JwtTokenIssuer;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the real HTTP, JWT verification, use case, and JPA boundaries together. */
@SpringBootTest(
        classes = AuthApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:pat-authority;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "auth.persistence.mode=jpa",
                "auth.master-data.enabled=false",
                "auth.login-security.store=memory"
        })
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PersonalAccessTokenHttpIntegrationTest {
    private static final String JWT_SECRET = randomSecret();
    private static final String INTERNAL_TOKEN = randomSecret();
    private static final String ALICE = "patAlice";
    private static final String CASE_VARIANT = "PatAlice";
    private static final String BOB = "patBob";
    private static final String ADMIN = "patAdmin";
    private static final String LONG_USER = "p".repeat(80);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TokenIssuerPort tokenIssuer;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> JWT_SECRET);
        registry.add("auth.internal-api.token", () -> INTERNAL_TOKEN);
    }

    @BeforeEach
    void resetUsersAndTokens() {
        jdbc.update("delete from auth_pat_lifecycle_events");
        jdbc.update("delete from auth_personal_access_tokens");
        jdbc.update("delete from auth_role_assignments where username in (?, ?, ?, ?, ?)",
                ALICE, CASE_VARIANT, BOB, ADMIN, LONG_USER);
        jdbc.update("delete from auth_users where username in (?, ?, ?, ?, ?)",
                ALICE, CASE_VARIANT, BOB, ADMIN, LONG_USER);
        insertUser(ALICE, "ROLE_USER");
        insertUser(CASE_VARIANT, "ROLE_USER");
        insertUser(BOB, "ROLE_USER");
        insertUser(ADMIN, "ROLE_SYSTEM_ADMIN");
        insertUser(LONG_USER, "ROLE_USER");
    }

    @Test
    void anonymousCannotCreateListOrRevokeEvenWithClaimedIdentityOrRoleHeaders() throws Exception {
        mockMvc.perform(post("/api/auth/pat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Auth-User", ADMIN)
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN")
                        .content("{\"username\":\"patAdmin\",\"tokenName\":\"spoofed\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/pat").param("username", ADMIN))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/admin/pat")
                        .header("X-Auth-User", ADMIN)
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/auth/pat/nonexistent").param("username", ADMIN))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/auth/admin/pat/nonexistent")
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from auth_personal_access_tokens", Integer.class))
                .isZero();
    }

    @Test
    void ownerCanCreateListAndRevokeWhileSpoofedBodyAndQueryOwnerAreIgnored() throws Exception {
        String aliceBearer = bearer(ALICE, "ROLE_USER");
        JsonNode created = create(aliceBearer, "{\"username\":\"patBob\",\"tokenName\":\"owner key\"}");
        String id = created.get("id").asText();

        assertThat(created.get("username").asText()).isEqualTo(ALICE);
        assertThat(created.get("rawToken").asText()).startsWith("pat_live_");
        mockMvc.perform(get("/api/auth/pat")
                        .header("Authorization", aliceBearer)
                        .param("username", BOB))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode listed = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(listed.size()).isEqualTo(1);
                    assertThat(listed.get(0).get("id").asText()).isEqualTo(id);
                    assertThat(listed.get(0).get("username").asText()).isEqualTo(ALICE);
                    assertThat(listed.get(0).has("rawToken")).isFalse();
                });
        mockMvc.perform(delete("/api/auth/pat/{id}", id)
                        .header("Authorization", aliceBearer)
                        .param("username", BOB))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject(
                "select status from auth_personal_access_tokens where id = ?", String.class, id))
                .isEqualTo("REVOKED");
        assertThat(eventsFor(id)).extracting(row -> row.get("action"))
                .containsExactlyInAnyOrder("CREATED", "REVOKED");
        assertThat(eventsFor(id)).allSatisfy(row -> {
            assertThat(row.get("actor_username")).isEqualTo(ALICE);
            assertThat(row.get("owner_username")).isEqualTo(ALICE);
            assertThat(row).doesNotContainKeys("raw_token", "token_hash");
        });
    }

    @Test
    void ordinaryUserCannotSeeOrRevokeAnotherUsersTokenOrUseAdminRoutes() throws Exception {
        String bobId = create(bearer(BOB, "ROLE_USER"), "{\"tokenName\":\"bob key\"}")
                .get("id").asText();
        String aliceBearer = bearer(ALICE, "ROLE_USER");

        mockMvc.perform(get("/api/auth/pat")
                        .header("Authorization", aliceBearer)
                        .param("username", BOB))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(objectMapper.readTree(
                        result.getResponse().getContentAsString()).size()).isZero());
        mockMvc.perform(delete("/api/auth/pat/{id}", bobId)
                        .header("Authorization", aliceBearer)
                        .param("username", BOB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/auth/admin/pat")
                        .header("Authorization", aliceBearer)
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/auth/admin/pat/{id}", bobId)
                        .header("Authorization", aliceBearer)
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject(
                "select status from auth_personal_access_tokens where id = ?", String.class, bobId))
                .isEqualTo("ACTIVE");
        assertThat(eventsFor(bobId)).hasSize(1);
    }

    @Test
    void signedAdministratorClaimDoesNotPromoteAnOrdinaryCurrentUser() throws Exception {
        // The credential is signed with the real issuer, but current AuthUser roles govern authority.
        String staleOrMisstatedRole = bearer(ALICE, "ROLE_SYSTEM_ADMIN");
        mockMvc.perform(get("/api/auth/admin/pat")
                        .header("Authorization", staleOrMisstatedRole))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/auth/admin/pat/nonexistent")
                        .header("Authorization", staleOrMisstatedRole))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifiedAdministratorCanListAndForceRevokeAcrossOwners() throws Exception {
        String bobId = create(bearer(BOB, "ROLE_USER"), "{\"tokenName\":\"bob key\"}")
                .get("id").asText();
        String adminBearer = bearer(ADMIN, "ROLE_SYSTEM_ADMIN");

        mockMvc.perform(get("/api/auth/admin/pat")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode listed = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(listed.size()).isEqualTo(1);
                    assertThat(listed.get(0).get("id").asText()).isEqualTo(bobId);
                });
        mockMvc.perform(delete("/api/auth/admin/pat/{id}", bobId)
                        .header("Authorization", adminBearer))
                .andExpect(status().isNoContent());
        assertThat(eventsFor(bobId)).extracting(row -> row.get("actor_username"))
                .containsExactlyInAnyOrder(BOB, ADMIN);
        assertThat(eventsFor(bobId)).extracting(row -> row.get("owner_username"))
                .containsExactly(BOB, BOB);
    }

    @Test
    void accountNamesDifferingOnlyByCaseRemainSeparateOwners() throws Exception {
        String aliceId = create(bearer(ALICE, "ROLE_USER"), "{\"tokenName\":\"case key\"}")
                .get("id").asText();
        String variantBearer = bearer(CASE_VARIANT, "ROLE_USER");

        mockMvc.perform(get("/api/auth/pat").header("Authorization", variantBearer))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(objectMapper.readTree(
                        result.getResponse().getContentAsString()).size()).isZero());
        mockMvc.perform(delete("/api/auth/pat/{id}", aliceId)
                        .header("Authorization", variantBearer))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject(
                "select status from auth_personal_access_tokens where id = ?", String.class, aliceId))
                .isEqualTo("ACTIVE");
    }

    @Test
    void maximumLengthCanonicalAuthUserCanOwnAPat() throws Exception {
        JsonNode created = create(bearer(LONG_USER, "ROLE_USER"),
                "{\"tokenName\":\"long owner key\"}");
        assertThat(created.get("username").asText()).isEqualTo(LONG_USER);
        assertThat(jdbc.queryForObject(
                "select username from auth_personal_access_tokens where id = ?", String.class,
                created.get("id").asText())).isEqualTo(LONG_USER);
    }

    @Test
    void invalidExpiredAndUnverifiableBearerCannotAccessPatRoutes() throws Exception {
        String expired = "Bearer " + tokenIssuer.issue(
                subject(ALICE, "ROLE_USER", 1L), Instant.now().minusSeconds(7200)).token();
        AuthModuleProperties attackerProperties = new AuthModuleProperties();
        attackerProperties.getJwt().setSecret(randomSecret());
        String forged = "Bearer " + new JwtTokenIssuer(attackerProperties).issue(
                subject(ADMIN, "ROLE_SYSTEM_ADMIN", 1L), Instant.now()).token();
        String absentUser = bearer("patMissing", "ROLE_USER");
        for (String authorization : List.of(
                "Bearer invalid.jwt.value", expired, forged, absentUser, "Bearer pat_live_fake")) {
            mockMvc.perform(post("/api/auth/pat")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenName\":\"forbidden\"}"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/auth/admin/pat")
                            .header("Authorization", authorization))
                    .andExpect(status().isUnauthorized());
        }
        assertThat(jdbc.queryForObject("select count(*) from auth_personal_access_tokens", Integer.class))
                .isZero();
    }

    @Test
    void currentAccountStateAndRoleVersionOverridePreviouslyIssuedClaims() throws Exception {
        String adminBearer = bearer(ADMIN, "ROLE_SYSTEM_ADMIN");
        jdbc.update("update auth_users set role_version = 2 where username = ?", ADMIN);
        mockMvc.perform(get("/api/auth/admin/pat").header("Authorization", adminBearer))
                .andExpect(status().isUnauthorized());

        String refreshed = bearer(ADMIN, "ROLE_SYSTEM_ADMIN", 2L);
        jdbc.update("update auth_users set account_locked = true where username = ?", ADMIN);
        mockMvc.perform(get("/api/auth/admin/pat").header("Authorization", refreshed))
                .andExpect(status().isUnauthorized());
        jdbc.update("update auth_users set account_locked = false, account_active = false where username = ?", ADMIN);
        mockMvc.perform(get("/api/auth/admin/pat").header("Authorization", refreshed))
                .andExpect(status().isUnauthorized());
        jdbc.update("update auth_users set account_active = true where username = ?", ADMIN);
        jdbc.update("delete from auth_role_assignments where username = ?", ADMIN);
        jdbc.update("insert into auth_role_assignments(username, role_code, data_scope, approved) values (?, ?, ?, ?)",
                ADMIN, "ROLE_USER", "GLOBAL", true);
        mockMvc.perform(get("/api/auth/admin/pat").header("Authorization", refreshed))
                .andExpect(status().isForbidden());
    }

    private JsonNode create(String bearer, String body) throws Exception {
        String response = mockMvc.perform(post("/api/auth/pat")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String bearer(String username, String role) {
        return bearer(username, role, 1L);
    }

    private String bearer(String username, String role, long version) {
        return "Bearer " + tokenIssuer.issue(subject(username, role, version), Instant.now()).token();
    }

    private TokenIssuerPort.TokenSubject subject(String username, String role, long version) {
        return new TokenIssuerPort.TokenSubject(username, null,
                List.of(RoleAssignment.approved(role)), version);
    }

    private List<Map<String, Object>> eventsFor(String id) {
        return jdbc.queryForList("select action, actor_username, owner_username from auth_pat_lifecycle_events "
                + "where token_id = ?", id);
    }

    private void insertUser(String username, String role) {
        jdbc.update("insert into auth_users(username, stored_password, account_active, account_locked, role_version) "
                        + "values (?, ?, ?, ?, ?)", username,
                PasswordEncoderPolicy.encode(UUID.randomUUID().toString(), 4), true, false, 1L);
        jdbc.update("insert into auth_role_assignments(username, role_code, data_scope, approved) "
                        + "values (?, ?, ?, ?)", username, role, "GLOBAL", true);
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
