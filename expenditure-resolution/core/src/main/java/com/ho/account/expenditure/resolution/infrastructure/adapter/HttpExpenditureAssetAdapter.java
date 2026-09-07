package com.ho.account.expenditure.resolution.infrastructure.adapter;

import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
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

/**
 * HTTP REST outbound adapter for Asset registration and lease activation in expenditure-resolution module.
 */
@Component
@ConditionalOnProperty(
        prefix = "expenditure.asset.remote",
        name = "enabled",
        havingValue = "true")
public class HttpExpenditureAssetAdapter implements AssetRegistrationPort {

    private final RestClient restClient;

    @Autowired
    public HttpExpenditureAssetAdapter(
            RestClient.Builder builder,
            @Value("${expenditure.asset.base-url}") String baseUrl,
            @Value("${expenditure.asset.connect-timeout:2s}") String connectTimeout,
            @Value("${expenditure.asset.read-timeout:5s}") String readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("expenditure.asset.base-url must not be blank");
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

    public HttpExpenditureAssetAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("expenditure.asset.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));

        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpExpenditureAssetAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public void registerAcquiredAsset(AssetAcquisitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String accountCode = command.accountCode() != null && !command.accountCode().isBlank()
                ? command.accountCode().trim()
                : "12000";
        Integer usefulLife = command.usefulLife() != null ? command.usefulLife() : 5;
        String depMethod = command.depreciationMethod() != null && !command.depreciationMethod().isBlank()
                ? command.depreciationMethod().trim()
                : "STRAIGHT_LINE";

        FixedAssetPayload payload = new FixedAssetPayload(
                command.assetCode(),
                command.assetName(),
                command.acquisitionDate(),
                command.acquisitionCost(),
                usefulLife,
                depMethod,
                BigDecimal.ZERO,
                accountCode,
                "12099",
                "83100",
                command.departmentCode()
        );

        try {
            restClient.post()
                    .uri("/api/fixed-assets")
                    .header("X-User-ID", "system")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IllegalStateException("Fixed asset registration failed for " + command.assetCode(), e);
        }
    }

    @Override
    public void activateLeaseContract(Long leaseContractId) {
        Objects.requireNonNull(leaseContractId, "leaseContractId must not be null");
        try {
            restClient.post()
                    .uri("/api/ifrs16/leases/{id}/activate", leaseContractId)
                    .header("X-User-ID", "system")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Safe fallback if activate endpoint is not yet deployed on asset service
                return;
            }
            throw new IllegalStateException("Lease contract activation returned a non-success status", e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Lease contract activation failed for " + leaseContractId, e);
        }
    }

    private static Duration parseDuration(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "expenditure.asset." + propertyName + " must not be blank");
        }
        try {
            return DurationStyle.detectAndParse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "expenditure.asset." + propertyName + " must be a valid duration: " + value, e);
        }
    }

    private int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isNegative()
                || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "expenditure.asset." + propertyName + " must be a positive bounded duration");
        }
        return Math.toIntExact(timeout.toMillis());
    }

    public record FixedAssetPayload(
            String assetCode,
            String assetName,
            LocalDate acquisitionDate,
            BigDecimal acquisitionCost,
            Integer usefulLife,
            String depreciationMethod,
            BigDecimal residualValue,
            String accountSubjectCode,
            String accumulatedAccountCode,
            String expenseAccountCode,
            String departmentCode
    ) {}
}
