package com.ho.account.expenditure.resolution.infrastructure.adapter;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
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

/**
 * HTTP REST outbound adapter for TaxInvoice queries in expenditure-resolution module.
 */
@Component
@ConditionalOnProperty(
        prefix = "expenditure.tax.remote",
        name = "enabled",
        havingValue = "true")
public class HttpExpenditureTaxAdapter implements TaxInvoiceQueryPort {

    private final RestClient restClient;

    @Autowired
    public HttpExpenditureTaxAdapter(
            RestClient.Builder builder,
            @Value("${expenditure.tax.base-url}") String baseUrl,
            @Value("${expenditure.tax.connect-timeout:2s}") String connectTimeout,
            @Value("${expenditure.tax.read-timeout:5s}") String readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("expenditure.tax.base-url must not be blank");
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

    public HttpExpenditureTaxAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("expenditure.tax.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));

        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpExpenditureTaxAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public Optional<TaxInvoiceRef> findById(Long taxInvoiceId) {
        if (taxInvoiceId == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/ap/invoices/{id}", taxInvoiceId)
                    .retrieve()
                    .body(TaxInvoiceResponse.class))
                    .map(res -> new TaxInvoiceRef(res.id(), res.issueId(), res.type(), res.status()));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new IllegalStateException("Tax invoice lookup returned a non-success status", e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Tax invoice lookup failed", e);
        }
    }

    public Optional<TaxInvoiceRef> findTaxInvoice(Long taxInvoiceId) {
        return findById(taxInvoiceId);
    }

    private static Duration parseDuration(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "expenditure.tax." + propertyName + " must not be blank");
        }
        try {
            return DurationStyle.detectAndParse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "expenditure.tax." + propertyName + " must be a valid duration: " + value, e);
        }
    }

    private int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isNegative()
                || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "expenditure.tax." + propertyName + " must be a positive bounded duration");
        }
        return Math.toIntExact(timeout.toMillis());
    }

    record TaxInvoiceResponse(
            Long id,
            String issueId,
            String type,
            String status) {
    }
}
