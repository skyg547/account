package com.ho.account.internalaudit.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Exercises the API filter with a local Auth endpoint and explicit module permission grants. */
class InternalAuditIdentityIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SECRET = "internal-audit-test-secret-at-least-32-bytes-long";
    private static final String RCM = "/api/v1/internalaudit/rcms/processes";
    private static final String EVALUATION = "/api/v1/internalaudit/evaluations/design";
    private static final String PROCESS_JSON = "{\"processId\":\"proc-1\",\"processName\":\"Ledger\"}";
    private static final String DESIGN_JSON = "{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"result\":\"EFFECTIVE\"}";

    private final AtomicBoolean roleVersionValid = new AtomicBoolean(true);
    private final AtomicInteger authRequestCount = new AtomicInteger();
    private final AtomicReference<String> lastAuthRequestBody = new AtomicReference<>();
    private HttpServer server;
    private RcmUseCase rcm;
    private EvaluationUseCase evaluations;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/validate-token-version", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastAuthRequestBody.set(requestBody);
            authRequestCount.incrementAndGet();
            JsonNode request = JSON.readTree(requestBody);
            boolean valid = "POST".equals(exchange.getRequestMethod()) && roleVersionValid.get()
                    && "signed_auditor".equals(request.path("username").asText())
                    && request.path("roleVersion").asLong(-1) == 1;
            byte[] body = ("{\"valid\":" + valid + "}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        rcm = mock(RcmUseCase.class);
        evaluations = mock(EvaluationUseCase.class);
        mvc = mvc(baseUrl, "ROLE_AUDITOR:INTERNAL_AUDIT.RCM:READ,ROLE_AUDITOR:INTERNAL_AUDIT.RCM:WRITE,"
                + "ROLE_AUDITOR:INTERNAL_AUDIT.EVALUATION:READ,ROLE_AUDITOR:INTERNAL_AUDIT.EVALUATION:WRITE");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        AuditActorContext.clear();
    }

    @Test
    void directHeaderOnlyCreateAndReadFailBeforeBusinessPorts() throws Exception {
        mvc.perform(post(RCM).header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON).content(PROCESS_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RCM).header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(EVALUATION).header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON).content(DESIGN_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design")
                        .header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(rcm, evaluations);
    }

    @Test
    void gatewayStyleBearerUsesVerifiedActorDespiteForgedHeaders() throws Exception {
        when(rcm.createProcess(any())).thenAnswer(invocation -> {
            assertThat(AuditActorContext.getActor()).isEqualTo("signed_auditor");
            return invocation.getArgument(0);
        });
        when(evaluations.submitDesignEvaluation(any())).thenAnswer(invocation -> {
            assertThat(AuditActorContext.getActor()).isEqualTo("signed_auditor");
            return invocation.getArgument(0);
        });
        mvc.perform(post(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET))
                        .header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(PROCESS_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value("signed_auditor"));
        mvc.perform(post(EVALUATION).header("Authorization", "Bearer " + token("signed_auditor", SECRET))
                        .header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(DESIGN_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evaluatorId").value("signed_auditor"));
        when(rcm.getAllProcesses()).thenReturn(List.of(new RcmProcess("proc-1", "Ledger", null, "signed_auditor")));
        mvc.perform(get(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].ownerId").value("signed_auditor"));
        when(evaluations.getDesignEvaluationsByControl("ctrl-1")).thenReturn(List.of(
                new DesignEvaluation("eval-1", "ctrl-1", "signed_auditor", null, "EFFECTIVE", null)));
        mvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design")
                        .header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].evaluatorId").value("signed_auditor"));
        verify(rcm).createProcess(any());
        verify(evaluations).submitDesignEvaluation(any());
        assertThat(authRequestCount.get()).isEqualTo(4);
        assertThat(JSON.readTree(lastAuthRequestBody.get()))
                .isEqualTo(JSON.readTree("{\"username\":\"signed_auditor\",\"roleVersion\":1}"));
    }

    @Test
    void invalidTokenAndRevokedRoleVersionRejectBothControllers() throws Exception {
        mvc.perform(post(RCM).header("Authorization", "Bearer " + token("signed_auditor", "different-signing-secret-at-least-32-bytes"))
                        .header("X-Auth-User", "forged").header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(PROCESS_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(EVALUATION).header("Authorization", "Bearer malformed")
                        .contentType(MediaType.APPLICATION_JSON).content(DESIGN_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET, "AUDITOR", 2)))
                .andExpect(status().isUnauthorized());
        assertThat(JSON.readTree(lastAuthRequestBody.get()))
                .isEqualTo(JSON.readTree("{\"username\":\"signed_auditor\",\"roleVersion\":2}"));
        roleVersionValid.set(false);
        mvc.perform(get(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design")
                        .header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(rcm, evaluations);
    }

    @Test
    void permittedRoleWithoutLiveFunctionGrantRejectsCreateAndRead() throws Exception {
        mvc = mvc("http://127.0.0.1:" + server.getAddress().getPort(), "");
        mvc.perform(post(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET))
                        .contentType(MediaType.APPLICATION_JSON).content(PROCESS_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(get(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isForbidden());
        mvc.perform(post(EVALUATION).header("Authorization", "Bearer " + token("signed_auditor", SECRET))
                        .contentType(MediaType.APPLICATION_JSON).content(DESIGN_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design")
                        .header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rcm, evaluations);
    }

    @Test
    void readGrantDoesNotAuthorizeWriteOrOtherFunction() throws Exception {
        mvc = mvc("http://127.0.0.1:" + server.getAddress().getPort(),
                "ROLE_AUDITOR:INTERNAL_AUDIT.RCM:READ");
        when(rcm.getAllProcesses()).thenReturn(List.of());
        mvc.perform(get(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isOk());
        mvc.perform(post(RCM).header("Authorization", "Bearer " + token("signed_auditor", SECRET))
                        .contentType(MediaType.APPLICATION_JSON).content(PROCESS_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design")
                        .header("Authorization", "Bearer " + token("signed_auditor", SECRET)))
                .andExpect(status().isForbidden());
        verify(rcm).getAllProcesses();
        verifyNoInteractions(evaluations);
    }

    @Test
    void forgedAuditorHeaderCannotGrantGuestTokenAccess() throws Exception {
        mvc.perform(post(EVALUATION).header("Authorization", "Bearer " + token("signed_auditor", SECRET, "USER"))
                        .header("X-Auth-User", "forged_auditor").header("X-Auth-Roles", "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON).content(DESIGN_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rcm, evaluations);
    }

    private MockMvc mvc(String baseUrl, String grants) {
        return MockMvcBuilders.standaloneSetup(new RcmController(rcm), new EvaluationController(evaluations))
                .addFilters(new InternalAuditIdentityFilter(SECRET, "auth-service", baseUrl,
                        new InternalAuditPermissionPolicy(grants)))
                .setControllerAdvice(new InternalAuditApiExceptionHandler()).build();
    }

    private static String token(String actor, String secret) {
        return token(actor, secret, "AUDITOR");
    }

    private static String token(String actor, String secret, String role) {
        return token(actor, secret, role, 1);
    }

    private static String token(String actor, String secret, String role, int roleVersion) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().setIssuer("auth-service").setSubject(actor)
                .setIssuedAt(Date.from(now.minusSeconds(1))).setExpiration(Date.from(now.plusSeconds(300)))
                .claim("roles", List.of(role)).claim("roleVersion", roleVersion)
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }
}
