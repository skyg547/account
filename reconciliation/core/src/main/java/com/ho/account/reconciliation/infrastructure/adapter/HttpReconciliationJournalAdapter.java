package com.ho.account.reconciliation.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reconciliation containerized HTTP adapter for remote Journal Ledger service.
 */
@Component
@ConditionalOnProperty(
        prefix = "reconciliation.journal-ledger.remote",
        name = "enabled",
        havingValue = "true")
public class HttpReconciliationJournalAdapter implements JournalQueryPort, JournalPostingPort {

    private final RestClient restClient;

    @Autowired
    public HttpReconciliationJournalAdapter(
            RestClient.Builder builder,
            @Value("${reconciliation.journal-ledger.base-url}") String baseUrl,
            @Value("${reconciliation.journal-ledger.connect-timeout:2s}") String connectTimeout,
            @Value("${reconciliation.journal-ledger.read-timeout:5s}") String readTimeout) {
        this(
                builder,
                baseUrl,
                parseDuration(connectTimeout, "connect-timeout"),
                parseDuration(readTimeout, "read-timeout")
        );
    }

    public HttpReconciliationJournalAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("reconciliation.journal-ledger.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));
        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpReconciliationJournalAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("JournalEntryCommand must not be null");
        }
        try {
            JournalPostingResult result = restClient.post()
                    .uri("/api/v1/journals/posting")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(JournalPostingResult.class);
            if (result == null) {
                throw new IllegalStateException("Remote journal posting returned empty response body");
            }
            return result;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Remote journal posting failed with status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal posting failed", e);
        }
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        if (journalEntryId == null) {
            throw new IllegalArgumentException("journalEntryId must not be null");
        }
        String effectiveActor = (actor == null || actor.isBlank()) ? "reconciliation" : actor.trim();
        try {
            restClient.post()
                    .uri("/api/journals/{id}/approve", journalEntryId)
                    .header("X-User-ID", effectiveActor)
                    .retrieve()
                    .toBodilessEntity();

            restClient.post()
                    .uri("/api/journals/{id}/post", journalEntryId)
                    .header("X-User-ID", effectiveActor)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Remote journal approve/post failed with status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal approve/post failed", e);
        }
    }

    @Override
    public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        try {
            JournalViewResponse[] responses = restClient.get()
                    .uri("/api/journals?startDate={startDate}&endDate={endDate}", startDate, endDate)
                    .retrieve()
                    .body(JournalViewResponse[].class);
            if (responses == null || responses.length == 0) {
                return List.of();
            }
            return Arrays.stream(responses)
                    .map(JournalViewResponse::toJournalSummary)
                    .toList();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return List.of();
            }
            throw new IllegalStateException("Remote journal summaries lookup failed with status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal summaries lookup failed", e);
        }
    }

    @Override
    public JournalSummary getJournalSummary(Long journalEntryId) {
        if (journalEntryId == null) {
            return null;
        }
        return fetchJournal("/api/journals/{id}", journalEntryId).orElse(null);
    }

    @Override
    public Optional<JournalSummary> findBySlipNo(String slipNo) {
        if (slipNo == null || slipNo.isBlank()) {
            return Optional.empty();
        }
        return fetchJournal("/api/journals/{slipNo}", slipNo.trim());
    }

    @Override
    public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
        return List.of();
    }

    @Override
    public List<JournalDetailSummary> getJournalDetailsByAccountCodes(
            LocalDate startDate,
            LocalDate endDate,
            List<String> accountCodes) {
        return List.of();
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregate(
            LocalDate startDate,
            LocalDate endDate,
            JournalSide side) {
        return new JournalDetailAggregateSummary(0L, BigDecimal.ZERO);
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(
            LocalDate startDate,
            LocalDate endDate,
            JournalSide side,
            String accountCode) {
        return new JournalDetailAggregateSummary(0L, BigDecimal.ZERO);
    }

    private Optional<JournalSummary> fetchJournal(String path, Object uriVariable) {
        try {
            JournalViewResponse response = restClient.get()
                    .uri(path, uriVariable)
                    .retrieve()
                    .body(JournalViewResponse.class);
            return Optional.ofNullable(response).map(JournalViewResponse::toJournalSummary);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new IllegalStateException("Remote journal lookup failed with status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal lookup failed", e);
        }
    }

    private static Duration parseDuration(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "reconciliation.journal-ledger." + propertyName + " must be a positive bounded duration");
        }
        try {
            return DurationStyle.detectAndParse(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "reconciliation.journal-ledger." + propertyName + " must be a valid duration: " + value, e);
        }
    }

    private int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isNegative()
                || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "reconciliation.journal-ledger." + propertyName + " must be a positive bounded duration");
        }
        return Math.toIntExact(timeout.toMillis());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JournalViewResponse(
            Long id,
            String slipNo,
            LocalDate slipDate,
            LocalDate accountingDate,
            String description,
            String status,
            String entryType,
            String currencyCode,
            String lineageSourceType,
            String lineageSourceId) {

        JournalSummary toJournalSummary() {
            JournalSummary summary = new JournalSummary();
            summary.setId(id);
            summary.setSlipNo(slipNo);
            summary.setSlipDate(slipDate);
            summary.setAccountingDate(accountingDate);
            summary.setDescription(description);
            summary.setStatus(status);
            summary.setEntryType(entryType);
            summary.setCurrencyCode(currencyCode);
            summary.setLineageSourceType(lineageSourceType);
            summary.setLineageSourceId(lineageSourceId);
            return summary;
        }
    }
}
