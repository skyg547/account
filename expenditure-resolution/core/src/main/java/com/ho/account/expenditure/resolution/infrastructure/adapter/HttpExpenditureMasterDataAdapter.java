package com.ho.account.expenditure.resolution.infrastructure.adapter;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.Duration;
import java.time.LocalDate;
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
 * HTTP REST outbound adapter for MasterData in expenditure-resolution module.
 */
@Component
@ConditionalOnProperty(
        prefix = "expenditure.master-data.remote",
        name = "enabled",
        havingValue = "true")
public class HttpExpenditureMasterDataAdapter implements MasterDataQueryPort {

    private final RestClient restClient;

    @Autowired
    public HttpExpenditureMasterDataAdapter(
            RestClient.Builder builder,
            @Value("${expenditure.master-data.base-url}") String baseUrl,
            @Value("${expenditure.master-data.connect-timeout:2s}") String connectTimeout,
            @Value("${expenditure.master-data.read-timeout:5s}") String readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("expenditure.master-data.base-url must not be blank");
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

    public HttpExpenditureMasterDataAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("expenditure.master-data.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));

        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpExpenditureMasterDataAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
        return get("/api/basic/account-subjects/{code}", AccountSubjectResponse.class, normalized(accountCode))
                .map(response -> new AccountSubjectRef(
                        response.code(),
                        response.name(),
                        response.unsettled(),
                        response.fixedAsset(),
                        response.balanceType(),
                        response.category()));
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubjectAt(
            String accountCode,
            LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new NullPointerException("effectiveDate must not be null");
        }
        return get(
                "/api/basic/references/account-subjects/{code}?effectiveDate={date}",
                AccountSubjectRef.class,
                normalized(accountCode),
                effectiveDate);
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        return get(
                "/api/basic/businesspartners/{code}",
                BusinessPartnerResponse.class,
                normalized(businessPartnerCode))
                .map(response -> new BusinessPartnerRef(
                        response.businessPartnerCode(),
                        response.businessPartnerName(),
                        response.partnerType(),
                        response.useYn()));
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartnerAt(
            String businessPartnerCode,
            LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new NullPointerException("effectiveDate must not be null");
        }
        return get(
                "/api/basic/references/business-partners/{code}?effectiveDate={date}",
                BusinessPartnerRef.class,
                normalized(businessPartnerCode),
                effectiveDate);
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String departmentCode) {
        return get("/api/basic/departments/{code}", DepartmentResponse.class, normalized(departmentCode))
                .map(response -> new DepartmentRef(response.code(), response.name(), response.type()));
    }

    @Override
    public Optional<DepartmentRef> findDepartmentAt(
            String departmentCode,
            LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new NullPointerException("effectiveDate must not be null");
        }
        return get(
                "/api/basic/references/departments/{code}?effectiveDate={date}",
                DepartmentRef.class,
                normalized(departmentCode),
                effectiveDate);
    }

    private <T> Optional<T> get(String path, Class<T> responseType, Object... uriVariables) {
        if (java.util.Arrays.stream(uriVariables).anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(path, uriVariables)
                    .retrieve()
                    .body(responseType));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new IllegalStateException("master-data lookup returned a non-success status", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("master-data lookup failed", exception);
        }
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Duration parseDuration(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "expenditure.master-data." + propertyName + " must not be blank");
        }
        try {
            return DurationStyle.detectAndParse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "expenditure.master-data." + propertyName + " must be a valid duration: " + value, e);
        }
    }

    private int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isNegative()
                || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "expenditure.master-data." + propertyName + " must be a positive bounded duration");
        }
        return Math.toIntExact(timeout.toMillis());
    }

    record AccountSubjectResponse(
            String code,
            String name,
            boolean unsettled,
            boolean fixedAsset,
            String balanceType,
            String category) {
    }

    record BusinessPartnerResponse(
            String businessPartnerCode,
            String businessPartnerName,
            String partnerType,
            Boolean useYn) {
    }

    record DepartmentResponse(String code, String name, String type) {
    }
}
