package com.ho.account.auth.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.ho.account.auth.core.application.exception.DepartmentValidationUnavailableException;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.application.port.out.OtpVerificationPort;
import com.ho.account.auth.core.application.port.out.SsoAuthenticationPort;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.application.service.AuthService;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.infrastructure.persistence.MasterDataDepartmentValidationAdapter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

class DepartmentValidationLoginIntegrationTest {

    private static final String ENDPOINT = "https://department-login.invalid";
    private static final String DEPARTMENT = "SYNTHETIC-PRIVATE-DEPARTMENT";
    private static final String USERNAME = "synthetic-login-user";
    private static final String REMOTE_DETAIL = "synthetic-private-response";
    private static final Instant NOW = Instant.parse("2026-09-28T00:00:00Z");

    private final String password = UUID.randomUUID().toString();
    private final String storedPassword = UUID.randomUUID().toString();
    private final Logger logger = (Logger) LoggerFactory.getLogger("com.ho.account.auth");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final TokenIssuerPort tokenIssuer = mock(TokenIssuerPort.class);
    private final LoginAttemptPort attempts = mock(LoginAttemptPort.class);
    private MockRestServiceServer server;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(ENDPOINT);
        server = MockRestServiceServer.bindTo(builder).build();
        AuthUser user = new AuthUser(USERNAME, storedPassword, DEPARTMENT, true, false, List.of("ROLE_USER"));
        AuthUserQueryPort users = mock(AuthUserQueryPort.class);
        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        AuthService service = new AuthService(users, new MasterDataDepartmentValidationAdapter(builder.build()),
                (raw, stored) -> password.equals(raw) && storedPassword.equals(stored),
                mock(OtpVerificationPort.class), mock(SsoAuthenticationPort.class), tokenIssuer, attempts,
                Clock.fixed(NOW, ZoneOffset.UTC));
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(service,
                        mock(AuthUserRoleAssignmentUseCase.class), new AuthModuleProperties()))
                .setControllerAdvice(new AuthExceptionHandler()).build();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logs.stop();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 204, 301, 401, 403, 500, 503})
    void remoteNon200CannotIssueToken(int statusCode) throws Exception {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withStatus(HttpStatusCode.valueOf(statusCode)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"" + DEPARTMENT + "\",\"detail\":\"" + REMOTE_DETAIL + "\"}"));

        assertUnavailableLogin();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "[]", "{broken", "{\"code\":123}", "{\"code\":\"OTHER\"}"})
    void unverifiableSuccessBodyCannotIssueToken(String body) throws Exception {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertUnavailableLogin();
    }

    @ParameterizedTest
    @ValueSource(strings = {" this-is-not-json", " {\"second\":\"object\"}"})
    void trailingContentAfterMatchingObjectCannotIssueToken(String trailingContent) throws Exception {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess("{\"code\":\"" + DEPARTMENT + "\",\"detail\":\""
                        + REMOTE_DETAIL + "\"}" + trailingContent, MediaType.APPLICATION_JSON));

        assertUnavailableLogin();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTHER", DEPARTMENT})
    void duplicateCodeCannotIssueTokenEvenWhenLastValueMatches(String firstCode) throws Exception {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess("{\"code\":\"" + firstCode + "\",\"code\":\"" + DEPARTMENT
                        + "\",\"detail\":\"" + REMOTE_DETAIL + "\"}", MediaType.APPLICATION_JSON));

        assertUnavailableLogin();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void connectionAndTimeoutFailuresCannotIssueToken(boolean timeout) throws Exception {
        String unsafeMessage = ENDPOINT + "/" + DEPARTMENT + " " + REMOTE_DETAIL + " " + password;
        IOException failure = timeout ? new SocketTimeoutException(unsafeMessage) : new IOException(unsafeMessage);
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withException(failure));

        assertUnavailableLogin();
    }

    @Test
    void notFoundDeniesWithoutEchoingDepartment() throws Exception {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withStatus(HttpStatusCode.valueOf(404)).body(REMOTE_DETAIL));

        MvcResult result = login().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Department code is invalid"))
                .andExpect(jsonPath("$.token").doesNotExist()).andReturn();

        verifyNoInteractions(tokenIssuer);
        verify(attempts).recordFailure(USERNAME, "INVALID_DEPARTMENT");
        verify(attempts, never()).recordSuccess(any());
        assertNoSensitiveText(result);
    }

    @Test
    void verifiedDepartmentReachesIssuerWithSameRoleSnapshot() throws Exception {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess("{\"code\":\"" + DEPARTMENT
                        + "\",\"name\":\"Synthetic department\"}", MediaType.APPLICATION_JSON));
        String issuedToken = UUID.randomUUID().toString();
        when(tokenIssuer.issue(any(), any())).thenReturn(new TokenIssuerPort.IssuedToken(issuedToken, 60));

        login().andExpect(status().isOk()).andExpect(jsonPath("$.token").value(issuedToken))
                .andExpect(jsonPath("$.departmentCode").value(DEPARTMENT))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        ArgumentCaptor<TokenIssuerPort.TokenSubject> subject = ArgumentCaptor.forClass(TokenIssuerPort.TokenSubject.class);
        verify(tokenIssuer).issue(subject.capture(), org.mockito.ArgumentMatchers.eq(NOW));
        assertThat(subject.getValue().departmentCode()).isEqualTo(DEPARTMENT);
        assertThat(subject.getValue().effectiveRoleAssignments()).extracting(RoleAssignment::roleCode)
                .containsExactly("ROLE_USER");
        verify(attempts).recordSuccess(USERNAME);
        verify(attempts, never()).recordFailure(any(), any());
    }

    @Test
    void unrelatedFailureRetainsGenericServerErrorMapping() throws Exception {
        when(attempts.isLocked(USERNAME)).thenThrow(new IllegalStateException("synthetic generic failure"));

        login().andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error: synthetic generic failure"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verifyNoInteractions(tokenIssuer);
    }

    private void assertUnavailableLogin() throws Exception {
        MvcResult result = login().andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_VALIDATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Department validation is unavailable"))
                .andExpect(jsonPath("$.token").doesNotExist()).andReturn();
        verifyNoInteractions(tokenIssuer);
        verify(attempts, never()).recordSuccess(any());
        verify(attempts, never()).recordFailure(any(), any());
        assertThat(result.getResolvedException()).isInstanceOf(DepartmentValidationUnavailableException.class)
                .hasMessage("Department validation is unavailable").hasNoCause();
        assertThat(result.getResolvedException().getSuppressed()).isEmpty();
        assertNoSensitiveText(result);
    }

    private org.springframework.test.web.servlet.ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + password
                        + "\",\"loginType\":\"NORMAL\"}"));
    }

    private void assertNoSensitiveText(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(USERNAME, password, storedPassword, DEPARTMENT, REMOTE_DETAIL, ENDPOINT);
        for (ILoggingEvent event : logs.list) {
            assertThat(event.getFormattedMessage())
                    .doesNotContain(USERNAME, password, storedPassword, DEPARTMENT, REMOTE_DETAIL, ENDPOINT);
            if (event.getThrowableProxy() != null) {
                assertThat(ThrowableProxyUtil.asString(event.getThrowableProxy()))
                        .doesNotContain(USERNAME, password, storedPassword, DEPARTMENT, REMOTE_DETAIL, ENDPOINT);
            }
        }
    }
}
