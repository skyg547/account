package com.ho.account.journalledger.infrastructure.adapter.masterdata;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.LocalDate;
import java.time.Duration;
import java.util.Optional;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** HTTP adapter used by containerized Journal Ledger processes. */
@Component
@ConditionalOnProperty(
        prefix = "journal-ledger.master-data.remote",
        name = "enabled",
        havingValue = "true")
public class HttpJournalMasterDataQueryAdapter
        implements MasterDataQueryPort, FiscalPeriodControlPort {

    private final RestClient restClient;

    @Autowired
    public HttpJournalMasterDataQueryAdapter(
            RestClient.Builder builder,
            @Value("${journal-ledger.master-data.base-url}") String baseUrl,
            @Value("${journal-ledger.master-data.connect-timeout:2s}") Duration connectTimeout,
            @Value("${journal-ledger.master-data.read-timeout:5s}") Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("journal-ledger.master-data.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));
        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpJournalMasterDataQueryAdapter(RestClient restClient) {
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

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return get("/api/basic/fiscal-periods/id/{id}", FiscalPeriodResponse.class, id)
                .map(FiscalPeriodResponse::toRef);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        if (fiscalYear == null || fiscalYear.isBlank()
                || fiscalPeriod == null || fiscalPeriod.isBlank()) {
            return Optional.empty();
        }
        return get(
                        "/api/basic/fiscal-periods/{year}/{period}",
                        FiscalPeriodResponse.class,
                        fiscalYear.trim(),
                        fiscalPeriod.trim())
                .map(FiscalPeriodResponse::toRef);
    }

    @Override
    public FiscalPeriodRef updateClosingStatus(
            Long fiscalPeriodId,
            String closingStatus,
            String auditUser) {
        throw new UnsupportedOperationException(
                "journal-ledger master-data adapter is read-only");
    }

    private <T> Optional<T> get(String path, Class<T> responseType, Object... uriVariables) {
        if (java.util.Arrays.stream(uriVariables).anyMatch(java.util.Objects::isNull)) {
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

    private int timeoutMillis(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isNegative()
                || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "journal-ledger.master-data." + propertyName + " must be a positive bounded duration");
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

    record FiscalPeriodResponse(
            Long id,
            String fiscalYear,
            String fiscalPeriod,
            LocalDate startDate,
            LocalDate endDate,
            String closingStatus) {

        FiscalPeriodRef toRef() {
            return new FiscalPeriodRef(
                    id, fiscalYear, fiscalPeriod, startDate, endDate, closingStatus);
        }
    }

}
