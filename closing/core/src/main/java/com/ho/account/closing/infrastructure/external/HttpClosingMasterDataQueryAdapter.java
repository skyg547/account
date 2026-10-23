package com.ho.account.closing.infrastructure.external;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Dated account-reference adapter for independently deployed Closing processes. */
@Component
@Profile("dev")
@ConditionalOnProperty(
        prefix = "closing.master-data.remote",
        name = "enabled",
        havingValue = "true")
public class HttpClosingMasterDataQueryAdapter implements MasterDataQueryPort {

    private final RestClient restClient;

    @Autowired
    public HttpClosingMasterDataQueryAdapter(
            RestClient.Builder builder,
            @Value("${closing.master-data.base-url}") String baseUrl,
            @Value("${closing.master-data.connect-timeout:2s}") String connectTimeout,
            @Value("${closing.master-data.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout, "connect-timeout"),
                parseDuration(readTimeout, "read-timeout"));
    }

    public HttpClosingMasterDataQueryAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        Objects.requireNonNull(builder, "builder must not be null");
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("closing.master-data.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                // Never forward a reference lookup through an untrusted redirect target.
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));
        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .defaultStatusHandler(status -> !status.is2xxSuccessful(), (request, response) -> {
                    // URLs, Location headers and provider bodies are not propagated to callers.
                    throw new RestClientResponseException("Master Data returned a non-success status",
                            response.getStatusCode().value(), "", null, null, null);
                })
                .build();
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubjectAt(
            String accountCode,
            LocalDate effectiveDate) {
        String normalizedCode = requireCode(accountCode);
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        try {
            AccountSubjectRef response = restClient.get()
                    .uri("/api/basic/references/account-subjects/{code}?effectiveDate={date}",
                            normalizedCode, effectiveDate)
                    .retrieve()
                    .body(AccountSubjectRef.class);
            requireValidAccount(response, normalizedCode);
            return Optional.of(response);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
        throw new UnsupportedOperationException(
                "Closing master-data account lookup requires an explicit effective date");
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        throw new UnsupportedOperationException(
                "Closing master-data adapter does not support undated business partner lookup");
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String departmentCode) {
        throw new UnsupportedOperationException(
                "Closing master-data adapter does not support undated department lookup");
    }

    private static void requireValidAccount(AccountSubjectRef response, String requestedCode) {
        if (response == null) {
            throw new IllegalStateException("master-data account lookup returned an empty response body");
        }
        if (response.accountCategory() == null || response.accountCategory().isBlank()) {
            throw new IllegalStateException(
                    "source classification completeness failed: master-data response has no category");
        }
        if (response.code() == null || !requestedCode.equals(response.code().trim())
                || response.name() == null || response.name().isBlank()) {
            throw new IllegalStateException("master-data account lookup returned an invalid response");
        }
    }

    private static String requireCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("accountCode must not be blank");
        }
        return value.trim();
    }

    private static IllegalStateException remoteFailure(RestClientException error) {
        String status = error instanceof RestClientResponseException response
                ? " (HTTP " + response.getStatusCode().value() + ")" : "";
        return new IllegalStateException("master-data dated account lookup failed" + status);
    }

    private static Duration parseDuration(String value, String propertyName) {
        try {
            return DurationStyle.detectAndParse(value == null ? "" : value.trim());
        } catch (RuntimeException exception) {
            throw invalidTimeout(propertyName);
        }
    }

    private static int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0
                || timeout.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
            throw invalidTimeout(propertyName);
        }
        return (int) timeout.toMillis();
    }

    private static IllegalArgumentException invalidTimeout(String propertyName) {
        return new IllegalArgumentException(
                "closing.master-data." + propertyName + " must be a positive bounded duration");
    }
}
