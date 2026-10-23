package com.ho.account.closing.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.io.IOException;
import java.net.HttpURLConnection;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Dev runtime adapter for the Journal Ledger HTTP contract.
 * ID/detail queries preserve the financial content checked on deterministic closing reruns.
 * Financial writes are never retried. The remote approval and posting sequence requires
 * distinct trusted actors, so this adapter only creates drafts for human review.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "closing.sources.enabled", havingValue = "true")
public class HttpClosingJournalAdapter implements JournalPostingPort, JournalQueryPort {

    private static final String UNSUPPORTED_QUERY =
            "Journal Ledger HTTP API does not support this Closing query";
    private static final String AUTH_USER_HEADER = "X-Auth-User";
    private static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    private static final String JOURNAL_MAKER_ROLE = "ROLE_JOURNAL_MAKER";
    private static final String AUTO_POST_UNAVAILABLE =
            "Closing HTTP Journal auto-post is unavailable: trusted maker, checker, and poster "
                    + "identities are required; disable account.closing.accounting.auto-post-adjustments";

    private final RestClient restClient;
    private final boolean autoPostAdjustments;

    @Autowired
    public HttpClosingJournalAdapter(
            RestClient.Builder builder,
            @Value("${closing.journal-ledger.base-url}") String baseUrl,
            @Value("${closing.journal-ledger.connect-timeout:2s}") String connectTimeout,
            @Value("${closing.journal-ledger.read-timeout:5s}") String readTimeout,
            @Value("${account.closing.accounting.auto-post-adjustments:false}") boolean autoPostAdjustments) {
        this(builder, baseUrl, parseDuration(connectTimeout, "connect-timeout"),
                parseDuration(readTimeout, "read-timeout"), autoPostAdjustments);
    }

    public HttpClosingJournalAdapter(
            RestClient.Builder builder,
            String baseUrl,
            String connectTimeout,
            String readTimeout) {
        this(builder, baseUrl, connectTimeout, readTimeout, false);
    }

    public HttpClosingJournalAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        this(builder, baseUrl, connectTimeout, readTimeout, false);
    }

    public HttpClosingJournalAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            boolean autoPostAdjustments) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("closing.journal-ledger.base-url must not be blank");
        }
        this.autoPostAdjustments = autoPostAdjustments;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                // Inspect the original response, including GET redirects, without a second request.
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));
        this.restClient = builder.baseUrl(baseUrl.trim()).requestFactory(requestFactory)
                .defaultStatusHandler(status -> !status.is2xxSuccessful(), (request, response) -> {
                    // Keep only status: Location headers, URLs and response bodies may be sensitive.
                    throw new RestClientResponseException("Journal Ledger returned a non-success status",
                            response.getStatusCode().value(), "", null, null, null);
                }).build();
    }

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        // Reject before the first remote write. A failed auto-post after draft creation
        // would leave an unapproved financial draft while the Closing run reports failure.
        rejectUnsupportedAutoPost();
        try {
            PostingResponse response = successful(restClient.post()
                    .uri("/api/v1/journals/posting")
                    // Journal replaces the untrusted body actor with these gateway-style headers.
                    .header(AUTH_USER_HEADER, command.createdBy())
                    .header(AUTH_ROLES_HEADER, JOURNAL_MAKER_ROLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve())
                    .body(PostingResponse.class);
            if (response == null || response.journalEntryId() == null
                    || response.journalEntryId() < 1 || isBlank(response.slipNo())
                    || (command.slipNo() != null && !command.slipNo().equals(response.slipNo()))
                    || !"DRAFT".equals(response.status())) {
                throw new IllegalStateException("Journal Ledger posting returned an invalid response");
            }
            return new JournalPostingResult(response.journalEntryId(), response.slipNo(), response.status());
        } catch (RestClientException e) {
            throw remoteFailure("posting", e);
        }
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive");
        }
        if (isBlank(actor)) {
            throw new IllegalArgumentException("X-User-ID is required");
        }
        // Journal requires maker request-approval, a different checker, and a poster.
        // The caller's single actor cannot satisfy this workflow safely. This also
        // prevents a rerun from advancing an existing draft by an ambiguous state.
        throw new IllegalStateException(AUTO_POST_UNAVAILABLE);
    }

    @Override
    public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid inclusive journal date range is required");
        }
        try {
            JournalViewResponse[] responses = successful(restClient.get()
                    .uri("/api/journals?startDate={startDate}&endDate={endDate}", startDate, endDate)
                    .retrieve()).body(JournalViewResponse[].class);
            if (responses == null) {
                throw new IllegalStateException("Journal Ledger summaries returned an empty response body");
            }
            return Arrays.stream(responses).map(HttpClosingJournalAdapter::toSummary).toList();
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
            JournalViewResponse response = successful(restClient.get()
                    .uri("/api/journals/{slipNo}", slipNo.trim())
                    .retrieve()).body(JournalViewResponse.class);
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
        return toSummary(findById(journalEntryId));
    }

    @Override
    public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
        JournalViewResponse response = findById(journalEntryId);
        if (response.lines() == null || response.lines().isEmpty()) {
            throw new IllegalStateException("Journal Ledger detail lookup returned no journal lines");
        }
        return response.lines().stream().map(line -> toDetail(response, line)).toList();
    }

    private JournalViewResponse findById(Long journalEntryId) {
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive");
        }
        try {
            JournalViewResponse response = successful(restClient.get()
                    .uri("/api/journals/by-id/{id}", journalEntryId).retrieve()).body(JournalViewResponse.class);
            JournalSummary summary = toSummary(response);
            if (!journalEntryId.equals(summary.getId())) {
                throw new IllegalStateException("Journal Ledger lookup returned a different journal ID");
            }
            return response;
        } catch (RestClientException e) {
            throw remoteFailure("ID lookup", e);
        }
    }

    private static JournalDetailSummary toDetail(JournalViewResponse header, JournalLineResponse line) {
        if (line == null || line.id() == null || line.id() < 1 || isBlank(line.accountCode())
                || !("DEBIT".equals(line.side()) || "CREDIT".equals(line.side()))
                || line.amount() == null || line.baseAmount() == null) {
            throw new IllegalStateException("Journal Ledger detail lookup returned an invalid journal line");
        }
        JournalDetailSummary detail = new JournalDetailSummary();
        detail.setId(line.id());
        detail.setSide(JournalSide.valueOf(line.side()));
        detail.setAccountCode(line.accountCode());
        detail.setAccountCategory(line.accountCategory());
        detail.setAmount(line.amount());
        detail.setBaseAmount(line.baseAmount());
        detail.setDepartmentCode(line.departmentCode());
        detail.setBusinessPartnerCode(line.businessPartnerCode());
        detail.setDetailDescription(line.description());
        detail.setAccountingDate(header.accountingDate());
        detail.setSlipNo(header.slipNo());
        detail.setHeaderDescription(header.description());
        return detail;
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

    private static RestClient.ResponseSpec successful(RestClient.ResponseSpec response) {
        // Route redirects through the same operation-aware catch blocks as 4xx/5xx failures.
        // Keep only the status: response bodies, headers and Location can expose sensitive data.
        return response.onStatus(status -> !status.is2xxSuccessful() && !status.isError(), (request, result) -> {
            throw new RestClientResponseException("Journal Ledger returned a non-success status",
                    result.getStatusCode().value(), "", null, null, null);
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void rejectUnsupportedAutoPost() {
        if (autoPostAdjustments) {
            throw new IllegalStateException(AUTO_POST_UNAVAILABLE);
        }
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
                "closing.journal-ledger." + propertyName + " must be a positive bounded duration");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PostingResponse(Long journalEntryId, String slipNo, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalViewResponse(
            Long id, String slipNo, LocalDate slipDate, LocalDate accountingDate,
            String description, String status, String entryType, String currencyCode,
            String lineageSourceType, String lineageSourceId, List<JournalLineResponse> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JournalLineResponse(Long id, String side, String accountCode, String accountCategory,
            java.math.BigDecimal amount, java.math.BigDecimal baseAmount,
            String departmentCode, String businessPartnerCode, String description) {
    }
}
