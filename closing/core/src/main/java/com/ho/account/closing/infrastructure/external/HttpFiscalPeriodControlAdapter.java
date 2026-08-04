package com.ho.account.closing.infrastructure.external;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 컨테이너 분리 환경에서 Closing 서비스가 MasterData 서비스의 회계기간 상태 변경 및 조회를
 * 서비스 인증 헤더(X-Service-Identity: closing)와 함께 호출하는 원격 HTTP 어댑터입니다.
 */
@Component
@ConditionalOnProperty(
        prefix = "closing.master-data.remote",
        name = "enabled",
        havingValue = "true")
public class HttpFiscalPeriodControlAdapter implements FiscalPeriodControlPort {

    private static final String SERVICE_IDENTITY_HEADER = "X-Service-Identity";
    private static final String SERVICE_IDENTITY_VALUE = "closing";

    private final RestClient restClient;

    @Autowired
    public HttpFiscalPeriodControlAdapter(
            RestClient.Builder builder,
            @Value("${closing.master-data.base-url}") String baseUrl,
            @Value("${closing.master-data.connect-timeout:2s}") Duration connectTimeout,
            @Value("${closing.master-data.read-timeout:5s}") Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("closing.master-data.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    HttpFiscalPeriodControlAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/basic/fiscal-periods/id/{id}", id)
                    .retrieve()
                    .body(FiscalPeriodRef.class));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new IllegalStateException("master-data lookup failed", e);
        }
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        if (fiscalYear == null || fiscalYear.isBlank() || fiscalPeriod == null || fiscalPeriod.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/basic/fiscal-periods/{year}/{period}", fiscalYear.trim(), fiscalPeriod.trim())
                    .retrieve()
                    .body(FiscalPeriodRef.class));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new IllegalStateException("master-data lookup failed", e);
        }
    }

    @Override
    public FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser) {
        if (fiscalPeriodId == null || closingStatus == null || auditUser == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }
        try {
            return restClient.put()
                    .uri("/api/internal/fiscal-periods/{id}/closing-status", fiscalPeriodId)
                    .header(SERVICE_IDENTITY_HEADER, SERVICE_IDENTITY_VALUE)
                    .body(new UpdateStatusRequest(closingStatus, auditUser))
                    .retrieve()
                    .body(FiscalPeriodRef.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new SecurityException("Service authentication rejected by master-data: " + e.getMessage(), e);
            }
            throw new IllegalStateException("master-data closing status update failed: " + e.getMessage(), e);
        }
    }

    private record UpdateStatusRequest(String closingStatus, String auditUser) {
    }
}
