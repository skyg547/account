package com.ho.account.receivable.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP REST outbound adapter for Journal Ledger posting in receivable module.
 */
@Component
@ConditionalOnProperty(
        prefix = "receivable.journal-ledger.remote",
        name = "enabled",
        havingValue = "true")
public class HttpReceivableJournalAdapter implements JournalPostingPort {

    private final RestClient restClient;

    @Autowired
    public HttpReceivableJournalAdapter(
            RestClient.Builder builder,
            @Value("${receivable.journal-ledger.base-url}") String baseUrl,
            @Value("${receivable.journal-ledger.connect-timeout:2s}") String connectTimeout,
            @Value("${receivable.journal-ledger.read-timeout:5s}") String readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("receivable.journal-ledger.base-url must not be blank");
        }
        Duration cTimeout = parseDuration(connectTimeout, "connect-timeout");
        Duration rTimeout = parseDuration(readTimeout, "read-timeout");

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(cTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(rTimeout, "read-timeout"));

        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    public HttpReceivableJournalAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("receivable.journal-ledger.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));

        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpReceivableJournalAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        try {
            JournalPostingResult result = restClient.post()
                    .uri("/api/v1/journals/posting")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(JournalPostingResult.class);

            if (result == null) {
                throw new IllegalStateException("Journal ledger posting returned null result");
            }
            return result;
        } catch (RestClientException e) {
            throw new IllegalStateException("Journal ledger posting failed", e);
        }
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        Objects.requireNonNull(journalEntryId, "journalEntryId must not be null");
        String safeActor = actor != null && !actor.isBlank() ? actor.trim() : "system";

        try {
            restClient.post()
                    .uri("/api/journals/{id}/approve", journalEntryId)
                    .header("X-User-ID", safeActor)
                    .retrieve()
                    .toBodilessEntity();

            restClient.post()
                    .uri("/api/journals/{id}/post", journalEntryId)
                    .header("X-User-ID", safeActor)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IllegalStateException("Journal ledger approval or posting failed for entry " + journalEntryId, e);
        }
    }

    private static Duration parseDuration(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "receivable.journal-ledger." + propertyName + " must not be blank");
        }
        try {
            return DurationStyle.detectAndParse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "receivable.journal-ledger." + propertyName + " must be a valid duration: " + value, e);
        }
    }

    private int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isNegative()
                || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "receivable.journal-ledger." + propertyName + " must be a positive bounded duration");
        }
        return Math.toIntExact(timeout.toMillis());
    }
}
