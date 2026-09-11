package com.ho.account.deposit.infrastructure.adapter.out.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import java.math.BigDecimal;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 * Sends the existing Deposit outbox command to Journal Ledger without retrying remote writes.
 * The provider deduplicates by source lineage. Before acknowledging an outbox event, this adapter
 * verifies the returned journal against the original command, including monetary lines.
 * A remote commit and local outbox acknowledgement remain separate transactions.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "account.deposit.remote", name = "enabled", havingValue = "true")
public class HttpDepositJournalPostingAdapter implements JournalPostingPort {
    private static final Set<String> ACCEPTED_STATUSES = Set.of("DRAFT", "REQUESTED", "APPROVED", "POSTED");
    private final RestClient restClient;

    @Autowired
    public HttpDepositJournalPostingAdapter(
            RestClient.Builder builder,
            @Value("${account.deposit.journal-base-url}") String baseUrl,
            @Value("${account.deposit.http.connect-timeout:2s}") String connectTimeout,
            @Value("${account.deposit.http.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout), parseDuration(readTimeout));
    }

    public HttpDepositJournalPostingAdapter(
            RestClient.Builder builder, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("account.deposit.journal-base-url must not be blank");
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

    HttpDepositJournalPostingAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireText(command.lineageSourceType(), "lineageSourceType");
        requireText(command.lineageSourceId(), "lineageSourceId");
        requireText(command.currencyCode(), "currencyCode");
        requireText(command.entryType(), "entryType");
        requireText(command.createdBy(), "createdBy");
        requireText(command.auditUser(), "auditUser");
        try {
            JournalPostingResult result = successful(restClient.post()
                    .uri("/api/v1/journals/posting")
                    .header("X-User-ID", command.auditUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command).retrieve())
                    .body(JournalPostingResult.class);
            if (result == null || result.journalEntryId() == null || result.journalEntryId() < 1
                    || result.slipNo() == null || result.slipNo().isBlank()
                    || result.status() == null || !ACCEPTED_STATUSES.contains(result.status())
                    || (command.slipNo() != null && !command.slipNo().equals(result.slipNo()))) {
                throw invalidJournal();
            }
            JournalResponse journal = successful(restClient.get()
                    .uri("/api/journals/{slipNo}", result.slipNo()).retrieve())
                    .body(JournalResponse.class);
            if (!matches(command, result, journal)) {
                throw invalidJournal();
            }
            // Do not fabricate DRAFT when an idempotent replay returns an already processed journal.
            return result;
        } catch (RestClientException error) {
            String status = error instanceof RestClientResponseException response
                    ? " (HTTP " + response.getStatusCode().value() + ")" : "";
            // No cause: the outbox persists this message and must not retain remote response data.
            throw new IllegalStateException("Journal Ledger draft delivery failed" + status
                    + "; verify remote state before retrying");
        }
    }

    /**
     * Deposit's current outbox only creates drafts. The provider has no journal-state lookup by
     * numeric ID, so this contract cannot safely recover an interrupted approval/posting sequence.
     * Fail before any write until an ID-based status and recovery contract is available.
     */
    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive");
        }
        requireText(actor, "actor");
        throw new UnsupportedOperationException(
                "Deposit remote approval requires a Journal Ledger ID-based status and recovery contract");
    }

    private static boolean matches(JournalEntryCommand command, JournalPostingResult result, JournalResponse journal) {
        return journal != null
                && result.journalEntryId().equals(journal.id())
                && result.slipNo().equals(journal.slipNo())
                && result.status().equals(journal.status())
                && command.slipDate().equals(journal.slipDate())
                && command.accountingDate().equals(journal.accountingDate())
                && Objects.equals(command.description(), journal.description())
                && command.entryType().equals(journal.entryType())
                && command.currencyCode().equals(journal.currencyCode())
                && sameAmount(command.exchangeRate() == null ? BigDecimal.ONE : command.exchangeRate(), journal.exchangeRate())
                && command.createdBy().equals(journal.createdBy())
                // Approval/posting legitimately changes auditUser after an idempotent draft delivery.
                && journal.auditUser() != null && !journal.auditUser().isBlank()
                && command.lineageSourceType().equals(journal.lineageSourceType())
                && command.lineageSourceId().equals(journal.lineageSourceId())
                && sameLines(command.lines(), journal.lines());
    }

    private static boolean sameLines(List<JournalLineCommand> requested, List<LineResponse> returned) {
        if (returned == null || requested.size() != returned.size()) {
            return false;
        }
        List<LineResponse> unmatched = new ArrayList<>(returned);
        for (JournalLineCommand line : requested) {
            int match = -1;
            for (int index = 0; index < unmatched.size(); index++) {
                LineResponse candidate = unmatched.get(index);
                if (candidate != null && line.drcrType().equals(candidate.side())
                        && line.accountCode().equals(candidate.accountCode())
                        && sameAmount(line.amount(), candidate.amount())
                        && sameAmount(line.baseAmount() == null ? line.amount() : line.baseAmount(), candidate.baseAmount())
                        && Objects.equals(line.departmentCode(), candidate.departmentCode())
                        && Objects.equals(line.businessPartnerCode(), candidate.businessPartnerCode())
                        && Objects.equals(line.detailDescription(), candidate.description())) {
                    match = index;
                    break;
                }
            }
            if (match < 0) {
                return false;
            }
            unmatched.remove(match);
        }
        return unmatched.isEmpty();
    }

    private static boolean sameAmount(BigDecimal expected, BigDecimal actual) {
        return expected != null && actual != null && expected.compareTo(actual) == 0;
    }

    private static RestClient.ResponseSpec successful(RestClient.ResponseSpec response) {
        return response.onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
            throw new IllegalStateException("Journal Ledger request failed (HTTP "
                    + result.getStatusCode().value() + "); verify remote state before retrying");
        });
    }

    private static IllegalStateException invalidJournal() {
        return new IllegalStateException(
                "Journal Ledger returned an invalid or mismatched journal; verify remote state before retrying");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
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
        return new IllegalArgumentException("account.deposit.http timeout must be a positive bounded duration");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalResponse(Long id, String slipNo, String status, LocalDate slipDate,
                                   LocalDate accountingDate, String description, String entryType,
                                   String currencyCode, BigDecimal exchangeRate, String createdBy, String auditUser,
                                   String lineageSourceType, String lineageSourceId, List<LineResponse> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LineResponse(String side, String accountCode, BigDecimal amount, BigDecimal baseAmount,
                                String departmentCode, String businessPartnerCode, String description) {
    }
}
