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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
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
                    || !List.of("DRAFT", "APPROVED", "POSTED").contains(Objects.toString(draft.status(), ""))) {
                throw invalidJournal("draft identity/state");
            }
            // The provider can return an existing journal solely by lineage, even for a different payload.
            // Confirm its entire financial content before any approval or posting write.
            operation = "pre-approval confirmation";
            JournalResponse existing = lookup(draft.slipNo());
            requireMatchingJournal(command, draft, existing, draft.status());
            if ("POSTED".equals(existing.status())) {
                return new PostedJournal(existing.id(), existing.slipNo());
            }
            if ("DRAFT".equals(existing.status())) {
                operation = "approval";
                successful(restClient.post().uri("/api/journals/{id}/approve", draft.journalEntryId())
                        .header("X-User-ID", command.actor()).retrieve()).toBodilessEntity();
            }
            operation = "posting after approval";
            successful(restClient.post().uri("/api/journals/{id}/post", draft.journalEntryId())
                    .header("X-User-ID", command.actor()).retrieve()).toBodilessEntity();
            operation = "posted-state confirmation";
            JournalResponse posted = lookup(draft.slipNo());
            requireMatchingJournal(command, draft, posted, "POSTED");
            return new PostedJournal(posted.id(), posted.slipNo());
        } catch (RestClientException error) {
            String status = error instanceof RestClientResponseException response
                    ? " (HTTP " + response.getStatusCode().value() + ")" : "";
            // No cause: transport exceptions can expose URLs or remote response bodies.
            throw new IllegalStateException("Journal Ledger " + operation + " failed" + status
                    + "; verify remote state before retrying");
        }
    }

    private JournalResponse lookup(String slipNo) {
        return successful(restClient.get().uri("/api/journals/{slipNo}", slipNo).retrieve())
                .body(JournalResponse.class);
    }

    private static void requireMatchingJournal(LoanJournalCommand command, PostingResponse identity,
            JournalResponse actual, String expectedStatus) {
        if (actual == null || !identity.journalEntryId().equals(actual.id())
                || !identity.slipNo().equals(actual.slipNo()) || !expectedStatus.equals(actual.status())
                || !command.accountingDate().equals(actual.slipDate())
                || !command.accountingDate().equals(actual.accountingDate())
                || !command.description().equals(actual.description())
                || !command.currencyCode().equals(actual.currencyCode())
                || !"NORMAL".equals(actual.entryType())
                || actual.exchangeRate() == null || actual.exchangeRate().compareTo(BigDecimal.ONE) != 0
                || !command.actor().equals(actual.createdBy()) || !command.actor().equals(actual.auditUser())
                || !command.lineageSourceType().equals(actual.lineageSourceType())
                || !command.lineageSourceId().equals(actual.lineageSourceId())
                || actual.lines() == null || actual.lines().size() != command.lines().size()
                || actual.lines().stream().anyMatch(line -> line == null || line.amount() == null
                        || line.baseAmount() == null || line.amount().signum() <= 0 || line.baseAmount().signum() <= 0
                        || line.accountCode() == null || !("DEBIT".equals(line.side()) || "CREDIT".equals(line.side())))) {
            throw invalidJournal("financial content/state confirmation");
        }
        // Equal multisets preserve each account, side and amount (and thus debit/credit totals),
        // while allowing provider line order and decimal scale to differ. Counts reject duplicate lines.
        Map<ComparedLine, Long> requested = command.lines().stream().map(line -> new ComparedLine(
                line.side(), line.accountCode(), line.amount().stripTrailingZeros(),
                line.amount().stripTrailingZeros(), null, null, line.description()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<ComparedLine, Long> stored = actual.lines().stream().map(line -> new ComparedLine(
                line.side(), line.accountCode(), line.amount().stripTrailingZeros(),
                line.baseAmount().stripTrailingZeros(), line.departmentCode(), line.businessPartnerCode(),
                line.description())).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (!requested.equals(stored)) {
            throw invalidJournal("financial line confirmation");
        }
    }

    private static IllegalStateException invalidJournal(String operation) {
        return new IllegalStateException("Journal Ledger " + operation
                + " failed; verify remote state before retrying");
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
    private record JournalResponse(Long id, String slipNo, LocalDate slipDate, LocalDate accountingDate,
            String description, String status, String entryType, String currencyCode, BigDecimal exchangeRate,
            String createdBy, String auditUser, String lineageSourceType, String lineageSourceId,
            List<JournalLineResponse> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalLineResponse(String side, String accountCode, BigDecimal amount, BigDecimal baseAmount,
            String departmentCode, String businessPartnerCode, String description) {
    }

    private record ComparedLine(String side, String accountCode, BigDecimal amount, BigDecimal baseAmount,
            String departmentCode, String businessPartnerCode, String description) {
    }
}
