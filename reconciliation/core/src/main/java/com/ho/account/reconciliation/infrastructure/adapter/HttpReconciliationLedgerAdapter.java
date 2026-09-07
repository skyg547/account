package com.ho.account.reconciliation.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatus;
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

/**
 * Reconciliation containerized HTTP adapter for remote General and Sub Ledger queries.
 */
@Component
@ConditionalOnProperty(
        prefix = "reconciliation.journal-ledger.remote",
        name = "enabled",
        havingValue = "true")
public class HttpReconciliationLedgerAdapter implements LedgerQueryPort {

    private final RestClient restClient;

    @Autowired
    public HttpReconciliationLedgerAdapter(
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

    public HttpReconciliationLedgerAdapter(
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

    HttpReconciliationLedgerAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public List<LedgerBalanceSummary> getGlBalanceSummaries(
            LocalDate startDate,
            LocalDate endDate,
            String accountCode,
            String currencyCode) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        try {
            LedgerBalanceResponse[] responses = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v1/ledger/gl/balances")
                                .queryParam("startDate", startDate)
                                .queryParam("endDate", endDate);
                        if (accountCode != null && !accountCode.isBlank()) {
                            builder.queryParam("accountCode", accountCode.trim());
                        }
                        if (currencyCode != null && !currencyCode.isBlank()) {
                            builder.queryParam("currencyCode", currencyCode.trim());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(LedgerBalanceResponse[].class);
            if (responses == null || responses.length == 0) {
                return List.of();
            }
            return Arrays.stream(responses)
                    .map(LedgerBalanceResponse::toSummary)
                    .toList();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return List.of();
            }
            throw new IllegalStateException("Remote GL balances lookup failed with status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote GL balances lookup failed", e);
        }
    }

    @Override
    public List<LedgerBalanceSummary> getSlBalanceSummaries(
            LocalDate startDate,
            LocalDate endDate,
            String accountCode,
            String businessPartnerCode,
            String departmentCode,
            String currencyCode) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        try {
            LedgerBalanceResponse[] responses = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v1/ledger/sl/balances")
                                .queryParam("startDate", startDate)
                                .queryParam("endDate", endDate);
                        if (accountCode != null && !accountCode.isBlank()) {
                            builder.queryParam("accountCode", accountCode.trim());
                        }
                        if (businessPartnerCode != null && !businessPartnerCode.isBlank()) {
                            builder.queryParam("businessPartnerCode", businessPartnerCode.trim());
                        }
                        if (departmentCode != null && !departmentCode.isBlank()) {
                            builder.queryParam("deptCode", departmentCode.trim());
                        }
                        if (currencyCode != null && !currencyCode.isBlank()) {
                            builder.queryParam("currencyCode", currencyCode.trim());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(LedgerBalanceResponse[].class);
            if (responses == null || responses.length == 0) {
                return List.of();
            }
            return Arrays.stream(responses)
                    .map(LedgerBalanceResponse::toSummary)
                    .toList();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return List.of();
            }
            throw new IllegalStateException("Remote SL balances lookup failed with status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Remote SL balances lookup failed", e);
        }
    }

    @Override
    public LedgerAggregateSummary calculateLedgerSummary(
            LocalDate startDate,
            LocalDate endDate,
            String accountCode,
            String currencyCode,
            String amountBasis) {
        List<LedgerBalanceSummary> balances = getGlBalanceSummaries(startDate, endDate, accountCode, currencyCode);
        if (balances == null || balances.isEmpty()) {
            return new LedgerAggregateSummary(0L, BigDecimal.ZERO);
        }
        long count = balances.size();
        BigDecimal total = BigDecimal.ZERO;
        String basis = amountBasis == null ? "DEBIT" : amountBasis.toUpperCase();
        for (LedgerBalanceSummary balance : balances) {
            BigDecimal amount = switch (basis) {
                case "CREDIT" -> balance.getCreditAmount() != null ? balance.getCreditAmount() : BigDecimal.ZERO;
                case "ENDING_BALANCE" -> balance.getEndingBalance() != null ? balance.getEndingBalance() : BigDecimal.ZERO;
                case "ABS_ENDING_BALANCE" -> balance.getEndingBalance() != null ? balance.getEndingBalance().abs() : BigDecimal.ZERO;
                case "DEBIT" -> balance.getDebitAmount() != null ? balance.getDebitAmount() : BigDecimal.ZERO;
                default -> balance.getDebitAmount() != null ? balance.getDebitAmount() : BigDecimal.ZERO;
            };
            total = total.add(amount);
        }
        return new LedgerAggregateSummary(count, total);
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
    record LedgerBalanceResponse(
            String accountCode,
            @JsonAlias({"bpCode", "businessPartnerCode"}) String businessPartnerCode,
            @JsonAlias({"deptCode", "departmentCode"}) String departmentCode,
            String currencyCode,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal endingBalance) {

        LedgerBalanceSummary toSummary() {
            LedgerBalanceSummary summary = new LedgerBalanceSummary();
            summary.setAccountCode(accountCode);
            summary.setBusinessPartnerCode(businessPartnerCode);
            summary.setDepartmentCode(departmentCode);
            summary.setCurrencyCode(currencyCode);
            summary.setDebitAmount(debitAmount);
            summary.setCreditAmount(creditAmount);
            summary.setEndingBalance(endingBalance);
            return summary;
        }
    }
}
