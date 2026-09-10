package com.ho.account.deposit.infrastructure.adapter.out.external;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Date-explicit reference lookup for the independently deployed dev Deposit API. */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "account.deposit.remote", name = "enabled", havingValue = "true")
public class HttpDepositMasterDataAdapter implements MasterDataQueryPort {
    private final RestClient restClient;

    @Autowired
    public HttpDepositMasterDataAdapter(
            RestClient.Builder builder,
            @Value("${account.deposit.master-data-base-url}") String baseUrl,
            @Value("${account.deposit.http.connect-timeout:2s}") String connectTimeout,
            @Value("${account.deposit.http.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout), parseDuration(readTimeout));
    }

    public HttpDepositMasterDataAdapter(
            RestClient.Builder builder, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("account.deposit.master-data-base-url must not be blank");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis(connectTimeout));
        factory.setReadTimeout(timeoutMillis(readTimeout));
        this.restClient = builder.baseUrl(baseUrl.trim()).requestFactory(factory).build();
    }

    HttpDepositMasterDataAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String code) {
        return findAccountSubjectAt(code, LocalDate.now());
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String code) {
        return findBusinessPartnerAt(code, LocalDate.now());
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String code) {
        return findDepartmentAt(code, LocalDate.now());
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubjectAt(String code, LocalDate effectiveDate) {
        return lookup("account-subjects", code, effectiveDate, AccountSubjectRef.class, AccountSubjectRef::code);
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartnerAt(String code, LocalDate effectiveDate) {
        return lookup("business-partners", code, effectiveDate, BusinessPartnerRef.class, BusinessPartnerRef::code);
    }

    @Override
    public Optional<DepartmentRef> findDepartmentAt(String code, LocalDate effectiveDate) {
        return lookup("departments", code, effectiveDate, DepartmentRef.class, DepartmentRef::code);
    }

    private <T> Optional<T> lookup(
            String resource, String code, LocalDate effectiveDate, Class<T> type, Function<T, String> referenceCode) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = code.trim();
        try {
            T result = restClient.get()
                    .uri("/api/basic/references/" + resource + "/{code}?effectiveDate={date}",
                            normalizedCode, effectiveDate)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful() && status.value() != 404,
                            (request, response) -> {
                                throw new IllegalStateException("Master Data lookup failed (HTTP "
                                        + response.getStatusCode().value() + ")");
                            })
                    .body(type);
            if (result == null || !normalizedCode.equals(referenceCode.apply(result))) {
                throw new IllegalStateException("Master Data returned an empty or mismatched reference");
            }
            return Optional.of(result);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("Master Data lookup failed (HTTP " + error.getStatusCode().value() + ")");
        } catch (RestClientException error) {
            // Transport exceptions can contain response bodies or URLs; do not retain their cause.
            throw new IllegalStateException("Master Data lookup failed");
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
}
