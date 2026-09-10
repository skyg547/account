package com.ho.account.loan.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Date-explicit Master Data HTTP lookup for independently deployed Loan. */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "account.loan.remote", name = "enabled", havingValue = "true")
public class HttpLoanReferenceDataAdapter implements LoanReferenceDataPort {

    private final RestClient restClient;

    @Autowired
    public HttpLoanReferenceDataAdapter(
            RestClient.Builder builder,
            @Value("${account.loan.master-data-base-url}") String baseUrl,
            @Value("${account.loan.http.connect-timeout:2s}") String connectTimeout,
            @Value("${account.loan.http.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout), parseDuration(readTimeout));
    }

    public HttpLoanReferenceDataAdapter(
            RestClient.Builder builder, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("account.loan.master-data-base-url must not be blank");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis(connectTimeout));
        factory.setReadTimeout(timeoutMillis(readTimeout));
        this.restClient = builder.baseUrl(baseUrl.trim()).requestFactory(factory).build();
    }

    HttpLoanReferenceDataAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public LoanReferenceSnapshot requireLoanReferences(
            Long businessPartnerId, String currencyCode, LocalDate effectiveDate) {
        if (businessPartnerId == null || businessPartnerId < 1) {
            throw new IllegalArgumentException("businessPartnerId must be positive.");
        }
        requireDate(effectiveDate);
        String currency = requireText(currencyCode, "currencyCode");
        if (!currency.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code.");
        }
        // The provider exposes partner codes, not Loan's numeric IDs, and no currency endpoint.
        throw new IllegalStateException(
                "Master Data HTTP API does not expose business partner lookup by numeric ID "
                        + "or effective-date currency lookup; Loan reference validation is unavailable");
    }

    @Override
    public AccountReference requireAccount(String accountCode, LocalDate effectiveDate) {
        String code = requireText(accountCode, "accountCode");
        requireDate(effectiveDate);
        try {
            AccountResponse response = restClient.get()
                    .uri("/api/basic/references/account-subjects/{code}?effectiveDate={date}",
                            code, effectiveDate)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
                        throw new IllegalStateException("Master Data account lookup failed (HTTP "
                                + result.getStatusCode().value() + ")");
                    })
                    .body(AccountResponse.class);
            if (response == null || !code.equals(response.code())
                    || response.name() == null || response.name().isBlank()) {
                throw new IllegalStateException("Master Data account lookup returned an invalid response");
            }
            return new AccountReference(response.code(), response.name());
        } catch (RestClientException error) {
            String status = error instanceof RestClientResponseException response
                    ? " (HTTP " + response.getStatusCode().value() + ")" : "";
            // Transport causes may include remote response bodies or URLs.
            throw new IllegalStateException("Master Data account lookup failed" + status);
        }
    }

    private static void requireDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("effectiveDate is required.");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private static Duration parseDuration(String value) {
        try {
            return DurationStyle.detectAndParse(value == null ? "" : value.trim());
        } catch (RuntimeException error) {
            throw invalidTimeout();
        }
    }

    private static int timeoutMillis(Duration timeout) {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0
                || timeout.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
            throw invalidTimeout();
        }
        return (int) timeout.toMillis();
    }

    private static IllegalArgumentException invalidTimeout() {
        return new IllegalArgumentException("account.loan.http timeout must be a positive bounded duration");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AccountResponse(String code, String name) {
    }
}
