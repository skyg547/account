package com.ho.account.reporting.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Dev runtime adapter for the Journal Ledger HTTP contract.
 * ID/detail/aggregate queries remain unavailable until the provider exposes them.
 * Missing detail/aggregate contracts fail explicitly instead of fabricating report drill-down data.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "account.reporting.remote", name = "enabled", havingValue = "true")
public class HttpReportingJournalAdapter implements JournalQueryPort {

    private static final String UNSUPPORTED_QUERY =
            "Journal Ledger HTTP API does not support this Reporting query";

    private final RestClient restClient;

    @Autowired
    public HttpReportingJournalAdapter(
            RestClient.Builder builder,
            @Value("${account.reporting.journal-base-url}") String baseUrl,
            @Value("${account.reporting.http.connect-timeout:2s}") String connectTimeout,
            @Value("${account.reporting.http.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout, "connect-timeout"),
                parseDuration(readTimeout, "read-timeout"));
    }

    public HttpReportingJournalAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("account.reporting.journal-base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));
        this.restClient = builder.baseUrl(baseUrl.trim()).requestFactory(requestFactory).build();
    }

    HttpReportingJournalAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid inclusive journal date range is required");
        }
        try {
            JournalViewResponse[] responses = restClient.get()
                    .uri("/api/journals?startDate={startDate}&endDate={endDate}", startDate, endDate)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw new IllegalStateException("Journal Ledger summaries lookup failed (HTTP "
                                + response.getStatusCode().value() + ")");
                    }).body(JournalViewResponse[].class);
            if (responses == null) {
                throw new IllegalStateException("Journal Ledger summaries returned an empty response body");
            }
            return Arrays.stream(responses).map(HttpReportingJournalAdapter::toSummary).toList();
        } catch (RestClientException e) {
            throw remoteFailure("summaries lookup", e);
        }
    }

    @Override
    public Optional<JournalSummary> findBySlipNo(String slipNo) {
        if (isBlank(slipNo)) {
            throw new IllegalArgumentException("slipNo must not be blank");
        }
        try {
            JournalViewResponse response = restClient.get()
                    .uri("/api/journals/{slipNo}", slipNo.trim())
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful() && status.value() != 404,
                            (request, result) -> {
                                throw new IllegalStateException("Journal Ledger slip lookup failed (HTTP "
                                        + result.getStatusCode().value() + ")");
                            }).body(JournalViewResponse.class);
            JournalSummary summary = toSummary(response);
            if (!slipNo.trim().equals(summary.getSlipNo())) {
                throw new IllegalStateException("Journal Ledger lookup returned a different slip number");
            }
            return Optional.of(summary);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw remoteFailure("slip lookup", e);
        } catch (RestClientException e) {
            throw remoteFailure("slip lookup", e);
        }
    }

    @Override
    public JournalSummary getJournalSummary(Long journalEntryId) {
        throw new UnsupportedOperationException(UNSUPPORTED_QUERY);
    }

    @Override
    public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
        throw new UnsupportedOperationException(UNSUPPORTED_QUERY);
    }

    @Override
    public List<JournalDetailSummary> getJournalDetailsByAccountCodes(
            LocalDate startDate, LocalDate endDate, List<String> accountCodes) {
        throw new UnsupportedOperationException(UNSUPPORTED_QUERY);
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregate(
            LocalDate startDate, LocalDate endDate, JournalSide side) {
        throw new UnsupportedOperationException(UNSUPPORTED_QUERY);
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(
            LocalDate startDate, LocalDate endDate, JournalSide side, String accountCode) {
        throw new UnsupportedOperationException(UNSUPPORTED_QUERY);
    }

    private static JournalSummary toSummary(JournalViewResponse response) {
        if (response == null || response.id() == null || response.id() < 1
                || isBlank(response.slipNo()) || response.slipDate() == null
                || response.accountingDate() == null || isBlank(response.status())) {
            throw new IllegalStateException("Journal Ledger lookup returned an invalid response");
        }
        JournalSummary summary = new JournalSummary();
        summary.setId(response.id());
        summary.setSlipNo(response.slipNo());
        summary.setSlipDate(response.slipDate());
        summary.setAccountingDate(response.accountingDate());
        summary.setDescription(response.description());
        summary.setStatus(response.status());
        summary.setEntryType(response.entryType());
        summary.setCurrencyCode(response.currencyCode());
        summary.setLineageSourceType(response.lineageSourceType());
        summary.setLineageSourceId(response.lineageSourceId());
        return summary;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static IllegalStateException remoteFailure(String operation, RestClientException error) {
        // RestClient causes can contain URLs and response bodies; expose only the HTTP status.
        String status = error instanceof RestClientResponseException response
                ? " (HTTP " + response.getStatusCode().value() + ")" : "";
        return new IllegalStateException("Journal Ledger " + operation + " failed" + status);
    }

    private static Duration parseDuration(String value, String propertyName) {
        try {
            return DurationStyle.detectAndParse(value == null ? "" : value.trim());
        } catch (RuntimeException e) {
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
                "account.reporting.http." + propertyName + " must be a positive bounded duration");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalViewResponse(
            Long id, String slipNo, LocalDate slipDate, LocalDate accountingDate,
            String description, String status, String entryType, String currencyCode,
            String lineageSourceType, String lineageSourceId) {
    }
}
