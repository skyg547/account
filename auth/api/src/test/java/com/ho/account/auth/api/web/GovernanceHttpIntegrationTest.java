package com.ho.account.auth.api.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import com.ho.account.shared.infrastructure.security.web.AuditController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Auth ingress/JWT/Flyway/JPA boundary; all users and credentials are disposable fixtures. */
@SpringBootTest(classes = AuthApplication.class, properties = {
        "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
        "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false", "management.tracing.enabled=false",
        "spring.data.redis.repositories.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:governance;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false", "spring.flyway.enabled=true", "auth.persistence.mode=jpa",
        "auth.master-data.enabled=false", "auth.login-security.store=memory", "auth.jwt.issuer=governance-test", "auth.jwt.audience=account-api"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GovernanceHttpIntegrationTest {
    private static final String SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final String LONG_USER = "u".repeat(80);
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired TokenIssuerPort issuer;
    @Autowired ApplicationContext context;
    @Autowired AuthUserQueryPort users;
    @Autowired TransactionTemplate transactions;

    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> SECRET);
        registry.add("auth.internal-api.token", () -> UUID.randomUUID().toString());
    }

    @BeforeEach
    void reset() {
        for (String table : List.of("audit_log", "master_approval", "audit_authorizations", "system_role",
                "auth_role_assignment_apply_log", "auth_role_assignments", "auth_users")) jdbc.update("delete from " + table);
        user("maker", "ROLE_SYSTEM_ADMIN"); user("checker", "ROLE_SYSTEM_ADMIN");
        user("ordinary", "ROLE_USER"); user("other", "ROLE_USER"); user(LONG_USER, "ROLE_USER");
    }

    @Test
    void onlyNarrowAuthControllerIsRegisteredAndAnonymousHintsNeverAuthenticate() throws Exception {
        assertThat(context.getBeansOfType(AuditController.class)).isEmpty();
        http.perform(get("/api/audit/roles").header("X-Auth-User", "maker").header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isUnauthorized());
        http.perform(post("/api/audit/approvals/requests").contentType(MediaType.APPLICATION_JSON)
                .content(request("ordinary", "USER"))).andExpect(status().isUnauthorized());
        http.perform(get("/api/audit/roles").header("Authorization", bearer("maker"), bearer("maker")))
                .andExpect(status().isUnauthorized());
        assertThat(count("master_approval")).isZero();
    }

    @Test
    void verifiedSelfRequestBindsActorsAndApprovalCommitsVersionApplyLogAndAudit() throws Exception {
        long id = requestAs("ordinary", "ordinary", "AUDITOR");
        assertThat(id).isPositive();
        assertThat(jdbc.queryForObject("select request_user from master_approval where id=?", String.class, id)).isEqualTo("ordinary");
        decide("ordinary", id, "approve").andExpect(status().isForbidden());
        decide("checker", id, "approve").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.approverUser").value("checker"));
        assertThat(jdbc.queryForObject("select role_version from auth_users where username='ordinary'", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("select role_code from auth_role_assignments where username='ordinary'", String.class)).isEqualTo("ROLE_AUDITOR");
        assertThat(count("auth_role_assignment_apply_log")).isEqualTo(1);
        assertThat(count("audit_log")).isEqualTo(2);
        decide("maker", id, "reject").andExpect(status().isConflict());
        http.perform(get("/api/audit/permissions/check").header("Authorization", bearer("ordinary"))
                .param("roleCode", "ROLE_USER").param("functionCode", "ledger.read").param("accessType", "READ"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managementAndCrossUserRequestsRequireCurrentAdminAndSodAppliesToBothDecisions() throws Exception {
        http.perform(post("/api/audit/approvals/requests").header("Authorization", bearer("ordinary"))
                .header("X-User-ID", "maker").header("X-Auth-Roles", "SYSTEM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON).content(request("other", "SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());
        for (String path : List.of("/roles", "/approvals/pending", "/logs/user/ordinary"))
            http.perform(get("/api/audit" + path).header("Authorization", bearer("ordinary"))).andExpect(status().isForbidden());
        long id = requestAs("maker", "other", "USER");
        decide("maker", id, "approve").andExpect(status().isForbidden());
        decide("maker", id, "reject").andExpect(status().isForbidden());
        decide("checker", id, "reject").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(count("auth_role_assignment_apply_log")).isZero();
    }

    @Test
    void permissionsAreLimitedToOwnCurrentGlobalRolesWithoutDefaultGrants() throws Exception {
        http.perform(get("/api/audit/roles/ROLE_USER/authorizations").header("Authorization", bearer("ordinary")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        http.perform(get("/api/audit/roles/ROLE_SYSTEM_ADMIN/authorizations").header("Authorization", bearer("ordinary")))
                .andExpect(status().isForbidden());
        permission("ordinary", "ROLE_USER").andExpect(status().isOk()).andExpect(content().string("false"));
        permission("ordinary", "ROLE_SYSTEM_ADMIN").andExpect(status().isForbidden());
        long roleRequest = json.readTree(http.perform(post("/api/audit/roles").header("Authorization", bearer("maker"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleCode\":\"ROLE_USER\",\"roleName\":\"User\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        assertThat(count("system_role")).isZero();
        decide("checker", roleRequest, "approve").andExpect(status().isOk());
        long grant = json.readTree(http.perform(post("/api/audit/roles/ROLE_USER/authorizations")
                .header("Authorization", bearer("maker")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"functionCode\":\"ledger.read\",\"accessType\":\"READ\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        decide("checker", grant, "approve").andExpect(status().isOk());
        permission("ordinary", "ROLE_USER").andExpect(status().isOk()).andExpect(content().string("true"));
        http.perform(get("/api/audit/roles/ROLE_USER").header("Authorization", bearer("maker")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorizations.length()").value(1));
        http.perform(get("/api/audit/roles/ROLE_USER/authorizations").header("Authorization", bearer("maker")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].roleCode").value("ROLE_USER"));
        long authorizationId = jdbc.queryForObject("select id from audit_authorizations", Long.class);
        long revoke = json.readTree(http.perform(delete("/api/audit/authorizations/" + authorizationId)
                .header("Authorization", bearer("maker"))).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        decide("checker", revoke, "approve").andExpect(status().isOk());
        permission("ordinary", "ROLE_USER").andExpect(status().isOk()).andExpect(content().string("false"));
    }

    @Test
    void invalidOrUnsupportedCommandsNeverPersist() throws Exception {
        for (String payload : List.of("{}", "{\"username\":\"other\",\"role\":\"USER\"}",
                "{\"role\":\"USER\",\"dataScope\":\"DEPARTMENT\"}", "{\"roles\":[]}",
                "{\"role\":\"USER\",\"role\":\"SYSTEM_ADMIN\"}")) {
            http.perform(post("/api/audit/approvals/requests").header("Authorization", bearer("ordinary"))
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                            "masterType", "AUTH_USER_ROLE", "masterKey", "ordinary", "requestType", "UPDATE", "payload", payload))))
                    .andExpect(status().isBadRequest());
        }
        http.perform(post("/api/audit/approvals/requests").header("Authorization", bearer("maker"))
                .contentType(MediaType.APPLICATION_JSON).content(request("ordinary", "USER").replace("AUTH_USER_ROLE", "DEPARTMENT")))
                .andExpect(status().isBadRequest());
        assertThat(count("master_approval")).isZero(); assertThat(count("audit_log")).isZero();
        decide("checker", 999999, "approve").andExpect(status().isNotFound());
    }

    @Test
    void eightyCharacterActorSurvivesRequestDecisionAndAudit() throws Exception {
        long id = requestAs(LONG_USER, LONG_USER, "USER");
        decide("checker", id, "approve").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select request_user from master_approval where id=?", String.class, id)).isEqualTo(LONG_USER);
        assertThat(jdbc.queryForObject("select user_id from audit_log where event_type='APPROVAL_REQUEST'", String.class)).isEqualTo(LONG_USER);
        jdbc.update("update auth_role_assignments set role_code='ROLE_SYSTEM_ADMIN' where username=?", LONG_USER);
        jdbc.update("update auth_users set role_version=1 where username=?", LONG_USER);
        long another = requestAs("maker", "other", "USER");
        decide(LONG_USER, another, "approve").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select approver_user from master_approval where id=?", String.class, another)).isEqualTo(LONG_USER);
    }

    @Test
    void auditFailureRollsBackDecisionRolesVersionAndApplyLog() throws Exception {
        long id = requestAs("ordinary", "ordinary", "AUDITOR");
        jdbc.execute("alter table audit_log add constraint fixture_audit_failure check (event_type <> 'APPROVAL_APPROVE')");
        try {
            decide("checker", id, "approve").andExpect(status().isConflict());
            assertThat(jdbc.queryForObject("select status from master_approval where id=?", String.class, id)).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select role_version from auth_users where username='ordinary'", Long.class)).isEqualTo(1L);
            assertThat(jdbc.queryForObject("select role_code from auth_role_assignments where username='ordinary'", String.class)).isEqualTo("ROLE_USER");
            assertThat(count("auth_role_assignment_apply_log")).isZero(); assertThat(count("audit_log")).isEqualTo(1);
        } finally { jdbc.execute("alter table audit_log drop constraint fixture_audit_failure"); }
        decide("checker", id, "approve").andExpect(status().isOk());
    }

    @Test
    void competingApproveAndRejectHaveExactlyOneWinner() throws Exception {
        long id = requestAs("ordinary", "ordinary", "AUDITOR");
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            var approve = pool.submit(() -> { ready.countDown(); start.await(); return decide("checker", id, "approve").andReturn().getResponse().getStatus(); });
            var reject = pool.submit(() -> { ready.countDown(); start.await(); return decide("maker", id, "reject").andReturn().getResponse().getStatus(); });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(approve.get(20, TimeUnit.SECONDS), reject.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            assertThat(count("audit_log")).isEqualTo(2);
            String status = jdbc.queryForObject("select status from master_approval where id=?", String.class, id);
            assertThat(count("auth_role_assignment_apply_log")).isEqualTo(status.equals("APPROVED") ? 1 : 0);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test
    void differentApprovalsForOnePreloadedTargetIncrementTwiceAndReplaceTheCompleteRoleSet() throws Exception {
        long first = requestAs("ordinary", "ordinary", "AUDITOR");
        long second = requestAs("ordinary", "ordinary", "USER");
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch preloaded = new CountDownLatch(2), apply = new CountDownLatch(1);
        try {
            // Both real persistence contexts hold version 1 and its role collection before either lock.
            // The HTTP service joins that transaction, reproducing validation/JWT preloading reliably.
            var firstResult = pool.submit(() -> approveAfterPreload(first, "checker", preloaded, apply));
            var secondResult = pool.submit(() -> approveAfterPreload(second, "maker", preloaded, apply));
            assertThat(preloaded.await(10, TimeUnit.SECONDS)).isTrue();
            apply.countDown();
            assertThat(firstResult.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(secondResult.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(jdbc.queryForObject("select role_version from auth_users where username='ordinary'", Long.class)).isEqualTo(3L);
            assertThat(jdbc.queryForList("select applied_role_version from auth_role_assignment_apply_log order by applied_role_version", Long.class))
                    .containsExactly(2L, 3L);
            String lastTrace = jdbc.queryForObject("select approval_trace_id from auth_role_assignment_apply_log where applied_role_version=3", String.class);
            String expectedRole = lastTrace.equals("governance-approval-id=" + first) ? "ROLE_AUDITOR" : "ROLE_USER";
            assertThat(jdbc.queryForList("select role_code from auth_role_assignments where username='ordinary'", String.class))
                    .containsExactly(expectedRole);
            assertThat(jdbc.queryForObject("select count(*) from master_approval where status='APPROVED'", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from audit_log where event_type='APPROVAL_APPROVE'", Integer.class)).isEqualTo(2);
        } finally { apply.countDown(); pool.shutdownNow(); }
    }

    private int approveAfterPreload(long id, String checker, CountDownLatch preloaded, CountDownLatch apply) {
        return transactions.execute(status -> {
            assertThat(users.findByUsername("ordinary").orElseThrow().getRoleVersion()).isEqualTo(1L);
            preloaded.countDown();
            try {
                if (!apply.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Fixture barrier timed out");
                return decide(checker, id, "approve").andReturn().getResponse().getStatus();
            } catch (Exception exception) { throw new IllegalStateException("Concurrent approval fixture failed", exception); }
        });
    }

    @Test
    void concurrentWriteAndExecuteApprovalsForOneRoleCannotBothCommit() throws Exception {
        long role = json.readTree(http.perform(post("/api/audit/roles").header("Authorization", bearer("maker"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleCode\":\"ROLE_USER\",\"roleName\":\"User\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        decide("checker", role, "approve").andExpect(status().isOk());
        long write = grantRequest("WRITE", "ledger.write");
        long execute = grantRequest("EXECUTE", "ledger.execute");
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            var writeResult = pool.submit(() -> { ready.countDown(); start.await(); return decide("checker", write, "approve").andReturn().getResponse().getStatus(); });
            var executeResult = pool.submit(() -> { ready.countDown(); start.await(); return decide("checker", execute, "approve").andReturn().getResponse().getStatus(); });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(writeResult.get(20, TimeUnit.SECONDS), executeResult.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(count("audit_authorizations")).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from master_approval where master_type='AUTHORIZATION' and status='APPROVED'", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from master_approval where master_type='AUTHORIZATION' and status='PENDING'", Integer.class)).isEqualTo(1);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    private long grantRequest(String accessType, String function) throws Exception {
        return json.readTree(http.perform(post("/api/audit/roles/ROLE_USER/authorizations")
                .header("Authorization", bearer("maker")).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("functionCode", function, "accessType", accessType))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void currentVersionScopeAccountAndRoleStateOverrideSignedAdminClaims() throws Exception {
        String token = bearer("maker");
        for (String mutation : List.of("role_version=2", "role_version=1, account_active=false", "account_active=true, account_locked=true")) {
            jdbc.update("update auth_users set " + mutation + " where username='maker'");
            http.perform(get("/api/audit/roles").header("Authorization", token)).andExpect(status().isUnauthorized());
        }
        jdbc.update("update auth_users set account_locked=false where username='maker'");
        jdbc.update("update auth_role_assignments set data_scope='DEPARTMENT' where username='maker'");
        http.perform(get("/api/audit/roles").header("Authorization", token)).andExpect(status().isUnauthorized());
        jdbc.update("update auth_role_assignments set data_scope='GLOBAL', role_code='ROLE_USER' where username='maker'");
        http.perform(get("/api/audit/roles").header("Authorization", token)).andExpect(status().isForbidden());
        jdbc.update("update auth_role_assignments set approved=false where username='maker'");
        http.perform(get("/api/audit/roles").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAudienceIssuerTimesSignatureAndFractionalVersionsAreUnauthorized() throws Exception {
        for (String fault : List.of("audience", "issuer", "iat", "exp", "fraction", "signature", "subject")) {
            var builder = Jwts.builder().setSubject(fault.equals("subject") ? "Maker" : "maker")
                    .setIssuer(fault.equals("issuer") ? "wrong" : "governance-test")
                    .setAudience(fault.equals("audience") ? "wrong" : "account-api")
                    .claim("roleVersion", fault.equals("fraction") ? 1.5 : 1);
            if (!fault.equals("iat")) builder.setIssuedAt(Date.from(Instant.now().minusSeconds(1)));
            if (!fault.equals("exp")) builder.setExpiration(Date.from(Instant.now().plusSeconds(600)));
            String secret = fault.equals("signature") ? UUID.randomUUID().toString() + UUID.randomUUID() : SECRET;
            String token = builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), io.jsonwebtoken.SignatureAlgorithm.HS256).compact();
            http.perform(get("/api/audit/roles").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }

    private org.springframework.test.web.servlet.ResultActions permission(String actor, String role) throws Exception {
        return http.perform(get("/api/audit/permissions/check").header("Authorization", bearer(actor))
                .param("roleCode", role).param("functionCode", "ledger.read").param("accessType", "READ"));
    }
    private org.springframework.test.web.servlet.ResultActions decide(String actor, long id, String action) throws Exception {
        return http.perform(post("/api/audit/approvals/" + id + "/" + action).header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content("{\"approverUser\":\"forged\",\"remarks\":\"reviewed\"}"));
    }
    private long requestAs(String actor, String target, String role) throws Exception {
        JsonNode response = json.readTree(http.perform(post("/api/audit/approvals/requests").header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content(request(target, role))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.effectiveDate").isString())
                .andReturn().getResponse().getContentAsString());
        return response.get("id").asLong();
    }
    private String request(String target, String role) throws Exception {
        return json.writeValueAsString(Map.of("masterType", "AUTH_USER_ROLE", "masterKey", target, "requestType", "UPDATE",
                "requestUser", "forged", "payload", json.writeValueAsString(Map.of("username", target, "role", role))));
    }
    private String bearer(String user) {
        return "Bearer " + issuer.issue(new TokenIssuerPort.TokenSubject(user, null,
                List.of(RoleAssignment.approved("ROLE_SYSTEM_ADMIN")), 1L), Instant.now()).token();
    }
    private int count(String table) { return jdbc.queryForObject("select count(*) from " + table, Integer.class); }
    private void user(String name, String role) {
        jdbc.update("insert into auth_users(username,stored_password,account_active,account_locked,role_version) values(?,?,true,false,1)",
                name, PasswordEncoderPolicy.encode(UUID.randomUUID().toString(), 4));
        jdbc.update("insert into auth_role_assignments(username,role_code,data_scope,approved) values(?,?,'GLOBAL',true)", name, role);
    }
}
