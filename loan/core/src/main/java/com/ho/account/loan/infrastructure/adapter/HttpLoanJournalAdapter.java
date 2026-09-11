package com.ho.account.loan.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Real remote draft/approval/posting flow for dev. Each write is a separate remote transaction;
 * failures require reconciliation before retrying, and this adapter never retries writes.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "account.loan.remote", name = "enabled", havingValue = "true")
public class HttpLoanJournalAdapter implements LoanJournalPort {

    private final RestClient restClient;

    @Autowired
    public HttpLoanJournalAdapter(
            RestClient.Builder builder,
            @Value("${account.loan.journal-base-url}") String baseUrl,
            @Value("${account.loan.http.connect-timeout:2s}") String connectTimeout,
            @Value("${account.loan.http.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout), parseDuration(readTimeout));
    }

    public HttpLoanJournalAdapter(
            RestClient.Builder builder, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("account.loan.journal-base-url must not be blank");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(timeoutMillis(connectTimeout));
        factory.setReadTimeout(timeoutMillis(readTimeout));
        this.restClient = builder.baseUrl(baseUrl.trim()).requestFactory(factory).build();
    }

    HttpLoanJournalAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public PostedJournal post(LoanJournalCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String operation = "draft creation";
        try {
            PostingResponse draft = successful(restClient.post()
                    .uri("/api/v1/journals/posting")
                    .header("X-User-ID", command.actor())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toContract(command)).retrieve())
                    .body(PostingResponse.class);
            if (draft == null || draft.journalEntryId() == null || draft.journalEntryId() < 1
                    || draft.slipNo() == null || draft.slipNo().isBlank()
                    || !"DRAFT".equals(draft.status())) {
                throw new IllegalStateException(
                        "Journal Ledger returned an invalid or non-draft response; verify remote state before retrying");
            }
            // Creation returns only a summary. Verify the stored lines before authorizing any write.
            operation = "draft-state confirmation";
            JournalResponse storedDraft = successful(restClient.get()
                    .uri("/api/journals/{slipNo}", draft.slipNo()).retrieve())
                    .body(JournalResponse.class);
            validateJournal(storedDraft, draft, command, "DRAFT");
            operation = "approval";
            successful(restClient.post().uri("/api/journals/{id}/approve", draft.journalEntryId())
                    .header("X-User-ID", command.actor()).retrieve()).toBodilessEntity();
            operation = "posting after approval";
            successful(restClient.post().uri("/api/journals/{id}/post", draft.journalEntryId())
                    .header("X-User-ID", command.actor()).retrieve()).toBodilessEntity();
            operation = "posted-state confirmation";
            JournalResponse posted = successful(restClient.get()
                    .uri("/api/journals/{slipNo}", draft.slipNo()).retrieve())
                    .body(JournalResponse.class);
            validateJournal(posted, draft, command, "POSTED");
            return new PostedJournal(posted.id(), posted.slipNo());
        } catch (RestClientException error) {
            String status = error instanceof RestClientResponseException response
                    ? " (HTTP " + response.getStatusCode().value() + ")" : "";
            // No cause: transport exceptions can expose URLs or remote response bodies.
            throw new IllegalStateException("Journal Ledger " + operation + " failed" + status
                    + "; verify remote state before retrying");
        }
    }

    private static void validateJournal(
            JournalResponse journal, PostingResponse draft, LoanJournalCommand command, String expectedStatus) {
        if (journal == null || !draft.journalEntryId().equals(journal.id())
                || !draft.slipNo().equals(journal.slipNo()) || !expectedStatus.equals(journal.status())
                || !command.accountingDate().equals(journal.accountingDate())
                || !command.currencyCode().equals(journal.currencyCode())
                || !command.lineageSourceType().equals(journal.lineageSourceType())
                || !command.lineageSourceId().equals(journal.lineageSourceId())) {
            throw invalidJournal();
        }
        if (journal.lines() == null || journal.lines().isEmpty()) {
            throw invalidJournal();
        }
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (JournalLineResponse line : journal.lines()) {
            if (line == null || line.amount() == null || line.amount().signum() <= 0) {
                throw invalidJournal();
            }
            if ("DEBIT".equals(line.side())) {
                debits = debits.add(line.amount());
            } else if ("CREDIT".equals(line.side())) {
                credits = credits.add(line.amount());
            } else {
                throw invalidJournal();
            }
        }
        BigDecimal requestedDebits = command.lines().stream()
                .filter(line -> "DEBIT".equals(line.side())).map(LoanJournalLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal requestedCredits = command.lines().stream()
                .filter(line -> "CREDIT".equals(line.side())).map(LoanJournalLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Scale is representational: compare exact numeric values without rounding or double conversion.
        if (debits.compareTo(requestedDebits) != 0 || credits.compareTo(requestedCredits) != 0) {
            throw invalidJournal();
        }
    }

    private static IllegalStateException invalidJournal() {
        return new IllegalStateException(
                "Journal Ledger identity, state or amount confirmation failed; verify remote state before retrying");
    }

    private static RestClient.ResponseSpec successful(RestClient.ResponseSpec response) {
        return response.onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
            throw new IllegalStateException("Journal Ledger request failed (HTTP "
                    + result.getStatusCode().value() + "); verify remote state before retrying");
        });
    }

    private static JournalEntryCommand toContract(LoanJournalCommand command) {
        return new JournalEntryCommand(command.accountingDate(), command.accountingDate(),
                command.description(), "NORMAL", command.currencyCode(), null,
                command.actor(), command.actor(), command.lineageSourceType(), command.lineageSourceId(),
                command.lines().stream().map(line -> new JournalLineCommand(
                        line.side(), line.accountCode(), line.amount(), line.amount(),
                        null, null, line.description())).toList());
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
    private record PostingResponse(Long journalEntryId, String slipNo, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalResponse(Long id, String slipNo, String status, LocalDate accountingDate,
                                   String currencyCode, String lineageSourceType, String lineageSourceId,
                                   List<JournalLineResponse> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalLineResponse(String side, BigDecimal amount) {
    }
}
