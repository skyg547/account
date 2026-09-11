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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                // A redirect must not become an apparently successful financial read from another host.
                connection.setInstanceFollowRedirects(false);
            }
        };
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
            JournalPostingResult result = successful(restClient.post()
                    .uri("/api/v1/journals/posting")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve())
                    .body(JournalPostingResult.class);
            if (result == null) {
                throw new IllegalStateException("Remote journal posting returned empty response body");
            }
            return result;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Remote journal posting failed with status " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal posting failed");
        }
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        if (journalEntryId == null) {
            throw new IllegalArgumentException("journalEntryId must not be null");
        }
        String effectiveActor = (actor == null || actor.isBlank()) ? "reconciliation" : actor.trim();
        try {
            successful(restClient.post()
                    .uri("/api/journals/{id}/approve", journalEntryId)
                    .header("X-User-ID", effectiveActor)
                    .retrieve())
                    .toBodilessEntity();

            successful(restClient.post()
                    .uri("/api/journals/{id}/post", journalEntryId)
                    .header("X-User-ID", effectiveActor)
                    .retrieve())
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Remote journal approve/post failed with status " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal approve/post failed");
        }
    }

    @Override
    public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        try {
            JournalViewResponse[] responses = successful(restClient.get()
                    .uri("/api/journals?startDate={startDate}&endDate={endDate}", startDate, endDate)
                    .retrieve())
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
            throw new IllegalStateException("Remote journal summaries lookup failed with status " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal summaries lookup failed");
        }
    }

    @Override
    public JournalSummary getJournalSummary(Long journalEntryId) {
        if (journalEntryId == null) {
            return null;
        }
        if (journalEntryId < 1) throw new IllegalArgumentException("journalEntryId must be positive");
        Optional<JournalSummary> result = fetchJournal("/api/journals/by-id/{id}", journalEntryId);
        if (result.isPresent() && !journalEntryId.equals(result.get().getId())) {
            throw invalidFinancialResponse();
        }
        return result.orElse(null);
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
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive");
        }
        try {
            JournalViewResponse entry = successful(restClient.get()
                    .uri("/api/journals/by-id/{id}", journalEntryId).retrieve()).body(JournalViewResponse.class);
            validateHeader(entry);
            if (!journalEntryId.equals(entry.id())) throw invalidFinancialResponse();
            return details(entry);
        } catch (RestClientException error) {
            throw financialReadFailure(error);
        }
    }

    @Override
    public List<JournalDetailSummary> getJournalDetailsByAccountCodes(
            LocalDate startDate, LocalDate endDate, List<String> accountCodes) {
        validateRange(startDate, endDate);
        // Match the local port contract: no requested accounts means no item-level selection.
        if (accountCodes == null || accountCodes.isEmpty()) return List.of();
        if (accountCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new IllegalArgumentException("accountCodes must contain nonblank account codes");
        }
        Set<String> requested = new HashSet<>(accountCodes);
        return postedDetails(startDate, endDate).stream()
                .filter(line -> requested.contains(line.getAccountCode())).toList();
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregate(
            LocalDate startDate, LocalDate endDate, JournalSide side) {
        return getJournalDetailAggregateByAccount(startDate, endDate, side, null);
    }

    @Override
    public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(
            LocalDate startDate, LocalDate endDate, JournalSide side, String accountCode) {
        Objects.requireNonNull(side, "side must not be null");
        long count = 0;
        BigDecimal amount = BigDecimal.ZERO;
        for (JournalDetailSummary line : postedDetails(startDate, endDate)) {
            if (line.getSide() == side && (accountCode == null || accountCode.equals(line.getAccountCode()))) {
                count++;
                // Same accounting measure as the provider's local aggregate SQL COALESCE(baseAmount, amount).
                amount = amount.add(line.getBaseAmount() == null ? line.getAmount() : line.getBaseAmount());
            }
        }
        return new JournalDetailAggregateSummary(count, amount);
    }

    private List<JournalDetailSummary> postedDetails(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        try {
            // The existing API returns headers and lines together: avoid one remote call per journal.
            JournalViewResponse[] entries = successful(restClient.get()
                    .uri("/api/journals?startDate={startDate}&endDate={endDate}", startDate, endDate)
                    .retrieve()).body(JournalViewResponse[].class);
            if (entries == null) throw invalidFinancialResponse();
            List<JournalDetailSummary> result = new ArrayList<>();
            Set<Long> journalIds = new HashSet<>();
            Set<Long> lineIds = new HashSet<>();
            for (JournalViewResponse entry : entries) {
                validateHeader(entry);
                if (!journalIds.add(entry.id())) throw invalidFinancialResponse();
                if (!"POSTED".equals(entry.status()) || entry.accountingDate().isBefore(startDate)
                        || entry.accountingDate().isAfter(endDate)) continue;
                for (JournalDetailSummary line : details(entry)) {
                    if (!lineIds.add(line.getId())) throw invalidFinancialResponse();
                    result.add(line);
                }
            }
            return List.copyOf(result);
        } catch (RestClientException error) {
            // A failed/malformed response is not a zero balance. Never conceal it as successful matching.
            throw financialReadFailure(error);
        }
    }

    private static void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid inclusive accounting date range is required");
        }
    }

    private static void validateHeader(JournalViewResponse entry) {
        if (entry == null || entry.id() == null || entry.id() < 1 || entry.slipNo() == null
                || entry.slipNo().isBlank() || entry.accountingDate() == null || entry.status() == null
                || !List.of("DRAFT", "REQUESTED", "APPROVED", "POSTED", "REJECTED", "REVERSED").contains(entry.status())) {
            throw invalidFinancialResponse();
        }
    }

    private static List<JournalDetailSummary> details(JournalViewResponse entry) {
        if (entry.lines() == null || entry.lines().isEmpty()) throw invalidFinancialResponse();
        Set<Long> identities = new HashSet<>();
        return entry.lines().stream().map(line -> {
            if (line == null || line.id() == null || line.id() < 1 || !identities.add(line.id())
                    || line.accountCode() == null || line.accountCode().isBlank()
                    || !("DEBIT".equals(line.side()) || "CREDIT".equals(line.side())) || line.amount() == null) {
                throw invalidFinancialResponse();
            }
            JournalDetailSummary detail = new JournalDetailSummary();
            detail.setId(line.id());
            detail.setSide(JournalSide.valueOf(line.side()));
            detail.setAccountCode(line.accountCode());
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount());
            detail.setDepartmentCode(line.departmentCode());
            detail.setBusinessPartnerCode(line.businessPartnerCode());
            detail.setDetailDescription(line.description());
            detail.setAccountingDate(entry.accountingDate());
            detail.setSlipNo(entry.slipNo());
            detail.setHeaderDescription(entry.description());
            return detail;
        }).toList();
    }

    private static RestClient.ResponseSpec successful(RestClient.ResponseSpec response) {
        return response.onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
            throw new RestClientResponseException("Remote journal returned a non-success status",
                    result.getStatusCode().value(), "", null, null, null);
        });
    }

    private static IllegalStateException invalidFinancialResponse() {
        return new IllegalStateException("Remote journal financial lookup returned invalid or incomplete data");
    }

    private static IllegalStateException financialReadFailure(RestClientException error) {
        String status = error instanceof RestClientResponseException response
                ? " (HTTP " + response.getStatusCode().value() + ")" : "";
        // Omit cause/body/headers: transport diagnostics can contain credentials or provider URLs.
        return new IllegalStateException("Remote journal financial lookup failed" + status);
    }

    private Optional<JournalSummary> fetchJournal(String path, Object uriVariable) {
        try {
            JournalViewResponse response = successful(restClient.get()
                    .uri(path, uriVariable)
                    .retrieve())
                    .body(JournalViewResponse.class);
            return Optional.ofNullable(response).map(JournalViewResponse::toJournalSummary);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new IllegalStateException("Remote journal lookup failed with status " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote journal lookup failed");
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
            String lineageSourceId,
            List<JournalLineResponse> lines) {

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JournalLineResponse(Long id, String side, String accountCode, BigDecimal amount,
            BigDecimal baseAmount, String departmentCode, String businessPartnerCode, String description) {
    }

}
