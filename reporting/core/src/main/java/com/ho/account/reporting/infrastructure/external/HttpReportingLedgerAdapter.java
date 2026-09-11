package com.ho.account.reporting.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import java.math.BigDecimal;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/** Real ledger balances for Reporting JPA mode; enabled explicitly for standalone dev APIs. */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "account.reporting.remote", name = "enabled", havingValue = "true")
public class HttpReportingLedgerAdapter implements LedgerQueryPort {

    private final RestClient restClient;

    @Autowired
    public HttpReportingLedgerAdapter(
            RestClient.Builder builder,
            @Value("${account.reporting.journal-base-url}") String baseUrl,
            @Value("${account.reporting.http.connect-timeout:2s}") String connectTimeout,
            @Value("${account.reporting.http.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout), parseDuration(readTimeout));
    }

    public HttpReportingLedgerAdapter(
            RestClient.Builder builder, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (isBlank(baseUrl)) {
            throw new IllegalArgumentException("account.reporting.journal-base-url must not be blank");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // Preserve the original status: a followed GET redirect would bypass non-2xx rejection.
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(timeoutMillis(connectTimeout));
        factory.setReadTimeout(timeoutMillis(readTimeout));
        this.restClient = builder.baseUrl(baseUrl.trim()).requestFactory(factory).build();
    }

    HttpReportingLedgerAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public List<LedgerBalanceSummary> getGlBalanceSummaries(
            LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode) {
        Map<String, String> filters = new LinkedHashMap<>();
        addFilter(filters, "accountCode", accountCode);
        addFilter(filters, "currencyCode", currencyCode);
        return balances("/api/v1/ledger/gl/balances", startDate, endDate, filters);
    }

    @Override
    public List<LedgerBalanceSummary> getSlBalanceSummaries(
            LocalDate startDate, LocalDate endDate, String accountCode,
            String businessPartnerCode, String departmentCode, String currencyCode) {
        Map<String, String> filters = new LinkedHashMap<>();
        addFilter(filters, "accountCode", accountCode);
        addFilter(filters, "businessPartnerCode", businessPartnerCode);
        addFilter(filters, "deptCode", departmentCode);
        addFilter(filters, "currencyCode", currencyCode);
        return balances("/api/v1/ledger/sl/balances", startDate, endDate, filters);
    }

    @Override
    public LedgerAggregateSummary calculateLedgerSummary(
            LocalDate startDate, LocalDate endDate, String accountCode,
            String currencyCode, String amountBasis) {
        throw new UnsupportedOperationException(
                "Journal Ledger HTTP API does not expose database ledger aggregate queries for Reporting");
    }

    private List<LedgerBalanceSummary> balances(
            String path, LocalDate startDate, LocalDate endDate, Map<String, String> filters) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid inclusive ledger date range is required");
        }
        try {
            BalanceResponse[] responses = restClient.get().uri(builder -> {
                builder.path(path).queryParam("startDate", startDate).queryParam("endDate", endDate);
                filters.forEach(builder::queryParam);
                return builder.build();
            }).retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw new IllegalStateException("Journal Ledger balance lookup failed (HTTP "
                                + response.getStatusCode().value() + ")");
                    }).body(BalanceResponse[].class);
            if (responses == null) {
                throw new IllegalStateException("Journal Ledger balance lookup returned an empty response body");
            }
            return Arrays.stream(responses).map(response -> toSummary(response, startDate, endDate, filters))
                    .toList();
        } catch (RestClientException error) {
            String status = error instanceof RestClientResponseException response
                    ? " (HTTP " + response.getStatusCode().value() + ")" : "";
            // Do not expose remote URLs, response bodies or transport exception causes.
            throw new IllegalStateException("Journal Ledger balance lookup failed" + status);
        }
    }

    private static LedgerBalanceSummary toSummary(
            BalanceResponse response, LocalDate startDate, LocalDate endDate, Map<String, String> filters) {
        if (response == null || isBlank(response.accountCode()) || isBlank(response.currencyCode())
                || response.balanceDate() == null || response.balanceDate().isBefore(startDate)
                || response.balanceDate().isAfter(endDate) || response.debitAmount() == null
                || response.creditAmount() == null || response.endingBalance() == null
                || response.debitAmount().signum() < 0 || response.creditAmount().signum() < 0
                || !matches(filters, "accountCode", response.accountCode())
                || !matches(filters, "currencyCode", response.currencyCode())
                || !matches(filters, "businessPartnerCode", response.businessPartnerCode())
                || !matches(filters, "deptCode", response.departmentCode())) {
            throw new IllegalStateException("Journal Ledger balance lookup returned an invalid response");
        }
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        summary.setAccountCode(response.accountCode());
        summary.setCurrencyCode(response.currencyCode());
        summary.setBusinessPartnerCode(response.businessPartnerCode());
        summary.setDepartmentCode(response.departmentCode());
        summary.setDebitAmount(response.debitAmount());
        summary.setCreditAmount(response.creditAmount());
        summary.setEndingBalance(response.endingBalance());
        return summary;
    }

    private static boolean matches(Map<String, String> filters, String name, String actual) {
        return !filters.containsKey(name) || filters.get(name).equals(actual);
    }

    private static void addFilter(Map<String, String> filters, String name, String value) {
        if (value != null) {
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank when supplied");
            }
            filters.put(name, value.trim());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
        return new IllegalArgumentException("account.reporting.http timeout must be a positive bounded duration");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BalanceResponse(String accountCode, String currencyCode, LocalDate balanceDate,
                                   String businessPartnerCode, String departmentCode,
                                   BigDecimal debitAmount, BigDecimal creditAmount, BigDecimal endingBalance) {
    }
}
