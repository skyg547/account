package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MasterDataDepartmentValidationAdapterTest {

    private static final String ENDPOINT = "https://department-validation.invalid";
    private static final String DEPARTMENT = "SYNTHETIC-DEPARTMENT";
    private static final String REMOTE_DETAIL = "synthetic-remote-private-detail";
    private static final String SAFE_MESSAGE = "Department validation is unavailable";

    private final Logger logger = (Logger) LoggerFactory.getLogger(MasterDataDepartmentValidationAdapter.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private MockRestServiceServer server;
    private MasterDataDepartmentValidationAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(ENDPOINT);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new MasterDataDepartmentValidationAdapter(builder.build());
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logs.stop();
        server.verify();
    }

    @Test
    void acceptsMatchingCodeInFullResponseObject() {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":\"" + DEPARTMENT
                        + "\",\"name\":\"Synthetic department\",\"active\":true}\n\t ", MediaType.APPLICATION_JSON));

        assertThat(adapter.existsDepartmentCode(DEPARTMENT)).isTrue();
    }

    @Test
    void returnsFalseOnlyForNotFound() {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withStatus(HttpStatusCode.valueOf(404)).body(REMOTE_DETAIL));

        assertThat(adapter.existsDepartmentCode(DEPARTMENT)).isFalse();
        assertNoRemoteDetailsInLogs();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingInputWithoutHttpRequest(String departmentCode) {
        assertThat(adapter.existsDepartmentCode(departmentCode)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 202, 204, 206, 301, 302, 400, 401, 403, 409, 429, 500, 502, 503})
    void rejectsEveryUnverifiedStatusEvenWithMatchingBody(int status) {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"" + DEPARTMENT + "\",\"detail\":\"" + REMOTE_DETAIL + "\"}"));

        assertUnavailable();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null", "{}", "[]", "123", "true", "\"SYNTHETIC-DEPARTMENT\"",
            "{broken", "{\"code\":null}", "{\"code\":123}", "{\"code\":true}", "{\"code\":{}}",
            "{\"code\":[\"SYNTHETIC-DEPARTMENT\"]}", "{\"code\":\"OTHER\"}",
            "{\"code\":\"synthetic-department\"}", "{\"code\":\" SYNTHETIC-DEPARTMENT \"}"})
    void rejectsMissingMalformedOrMismatchedSuccessBody(String body) {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertUnavailable();
    }

    @ParameterizedTest
    @ValueSource(strings = {" this-is-not-json", " {\"second\":\"object\"}"})
    void rejectsTrailingContentAfterMatchingObject(String trailingContent) {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess("{\"code\":\"" + DEPARTMENT + "\",\"detail\":\""
                        + REMOTE_DETAIL + "\"}" + trailingContent, MediaType.APPLICATION_JSON));

        assertUnavailable();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTHER", DEPARTMENT})
    void rejectsDuplicateCodeEvenWhenLastValueMatches(String firstCode) {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess("{\"code\":\"" + firstCode + "\",\"code\":\"" + DEPARTMENT
                        + "\",\"detail\":\"" + REMOTE_DETAIL + "\"}", MediaType.APPLICATION_JSON));

        assertUnavailable();
    }

    @Test
    void doesNotCoerceNumericCodeToRequestedString() {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/123"))
                .andRespond(withSuccess("{\"code\":123}", MediaType.APPLICATION_JSON));

        assertSafeFailure(catchThrowable(() -> adapter.existsDepartmentCode("123")));
    }

    @Test
    void rejectsUnreadableContentType() {
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withSuccess(REMOTE_DETAIL, MediaType.TEXT_HTML));

        assertUnavailable();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sanitizesConnectionAndTimeoutFailures(boolean timeout) {
        String unsafeMessage = ENDPOINT + "/" + DEPARTMENT + " " + REMOTE_DETAIL;
        IOException failure = timeout ? new SocketTimeoutException(unsafeMessage) : new IOException(unsafeMessage);
        server.expect(requestTo(ENDPOINT + "/api/basic/departments/" + DEPARTMENT))
                .andRespond(withException(failure));

        assertUnavailable();
    }

    private void assertUnavailable() {
        assertSafeFailure(catchThrowable(() -> adapter.existsDepartmentCode(DEPARTMENT)));
    }

    private void assertSafeFailure(Throwable failure) {
        // A future typed availability exception may extend RuntimeException without weakening this boundary.
        assertThat(failure).isInstanceOf(RuntimeException.class).hasMessage(SAFE_MESSAGE).hasNoCause();
        assertThat(failure.getSuppressed()).isEmpty();
        assertNoRemoteDetailsInLogs();
    }

    private void assertNoRemoteDetailsInLogs() {
        for (ILoggingEvent event : logs.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(ENDPOINT, DEPARTMENT, REMOTE_DETAIL);
            if (event.getThrowableProxy() != null) {
                assertThat(ThrowableProxyUtil.asString(event.getThrowableProxy()))
                        .doesNotContain(ENDPOINT, DEPARTMENT, REMOTE_DETAIL);
            }
        }
    }
}
