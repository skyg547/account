package com.ho.account.closing.infrastructure.external;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
 * 컨테이너 분리 환경에서 Closing 서비스가 MasterData 서비스의 회계기간 상태 변경 및 조회를
 * 서명된 서비스 권한과 원본 전이 ID를 함께 전달하는 원격 HTTP 어댑터입니다.
 */
@Component
@ConditionalOnProperty(
        prefix = "closing.master-data.remote",
        name = "enabled",
        havingValue = "true")
public class HttpFiscalPeriodControlAdapter implements FiscalPeriodControlPort {

    private static final String SERVICE_IDENTITY_HEADER = "X-Service-Identity";
    private static final String SERVICE_IDENTITY_VALUE = "closing";
    private static final String SERVICE_ASSERTION_HEADER = "X-Service-Assertion";

    private final RestClient restClient;
    private final String serviceSecret;

    @Autowired
    public HttpFiscalPeriodControlAdapter(
            RestClient.Builder builder,
            @Value("${closing.master-data.base-url}") String baseUrl,
            @Value("${closing.master-data.connect-timeout:2s}") String connectTimeout,
            @Value("${closing.master-data.read-timeout:5s}") String readTimeout,
            @Value("${closing.master-data.internal-auth.secret:}") String serviceSecret) {
        this(builder, baseUrl, parseDuration(connectTimeout, "connect-timeout"),
                parseDuration(readTimeout, "read-timeout"), serviceSecret);
        requireConfiguredSecret(serviceSecret);
    }

    public HttpFiscalPeriodControlAdapter(RestClient.Builder builder, String baseUrl,
            String connectTimeout, String readTimeout) {
        this(builder, baseUrl, parseDuration(connectTimeout, "connect-timeout"),
                parseDuration(readTimeout, "read-timeout"), "");
    }

    public HttpFiscalPeriodControlAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        this(builder, baseUrl, connectTimeout, readTimeout, "");
    }

    public HttpFiscalPeriodControlAdapter(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            String serviceSecret) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("closing.master-data.base-url must not be blank");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // A redirect is neither proof of a status transition nor permission to forward service identity.
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(timeoutMillis(connectTimeout, "connect-timeout"));
        requestFactory.setReadTimeout(timeoutMillis(readTimeout, "read-timeout"));
        this.restClient = builder
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .defaultStatusHandler(status -> !status.is2xxSuccessful(), (request, response) -> {
                    // Do not retain Location, response bodies or URLs in surfaced exceptions.
                    throw new RestClientResponseException("Master Data returned a non-success status",
                            response.getStatusCode().value(), "", null, null, null);
                })
                .build();
        this.serviceSecret = serviceSecret;
    }

    HttpFiscalPeriodControlAdapter(RestClient restClient) {
        this.restClient = restClient;
        this.serviceSecret = "";
    }

    @Override
    public FiscalPeriodTransitionReceipt prepareClosingTransition(FiscalPeriodTransitionCommand command) {
        if (command == null) throw new IllegalArgumentException("command must not be null");
        String path = "/api/internal/fiscal-periods/" + command.fiscalPeriodId()
                + "/transitions/" + command.operationId();
        try {
            FiscalPeriodTransitionReceipt receipt = restClient.put().uri(path)
                    .header(SERVICE_IDENTITY_HEADER, SERVICE_IDENTITY_VALUE)
                    .header(SERVICE_ASSERTION_HEADER, assertion("PUT", path, command.operationId(),
                            command.auditUser(), command))
                    .body(new PrepareTransitionRequest(command.expectedStatus().name(), command.targetStatus().name()))
                    .retrieve().body(FiscalPeriodTransitionReceipt.class);
            requireReceiptOperation(command.operationId(), receipt);
            if (!receipt.matches(command)) throw new IllegalStateException("Master receipt does not match request");
            return receipt;
        } catch (RestClientResponseException e) {
            throw transitionFailure("prepare", e);
        } catch (RestClientException e) {
            throw remoteFailure("transition prepare", e);
        }
    }

    @Override
    public FiscalPeriodTransitionReceipt applyClosingTransition(String operationId) {
        String path = operationPath(operationId);
        try {
            FiscalPeriodTransitionReceipt receipt = restClient.post().uri(path)
                    .header(SERVICE_IDENTITY_HEADER, SERVICE_IDENTITY_VALUE)
                    .header(SERVICE_ASSERTION_HEADER, assertion("POST", path, operationId, "closing-service", null))
                    .retrieve().body(FiscalPeriodTransitionReceipt.class);
            requireReceiptOperation(operationId, receipt);
            return receipt;
        } catch (RestClientResponseException e) {
            throw transitionFailure("apply", e);
        } catch (RestClientException e) {
            throw remoteFailure("transition apply", e);
        }
    }

    @Override
    public Optional<FiscalPeriodTransitionReceipt> findTransitionReceipt(String operationId) {
        String path = operationPath(operationId);
        try {
            FiscalPeriodTransitionReceipt receipt = restClient.get().uri(path)
                    .header(SERVICE_IDENTITY_HEADER, SERVICE_IDENTITY_VALUE)
                    .header(SERVICE_ASSERTION_HEADER, assertion("GET", path, operationId, "closing-service", null))
                    .retrieve().body(FiscalPeriodTransitionReceipt.class);
            if (receipt != null) requireReceiptOperation(operationId, receipt);
            return Optional.ofNullable(receipt);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            throw transitionFailure("receipt lookup", e);
        } catch (RestClientException e) {
            throw remoteFailure("transition receipt lookup", e);
        }
    }

    @Override
    public FiscalPeriodTransitionReceipt cancelClosingTransition(String operationId, String cancellationActor) {
        String path = operationPath(operationId);
        try {
            FiscalPeriodTransitionReceipt receipt = restClient.delete().uri(path)
                    .header(SERVICE_IDENTITY_HEADER, SERVICE_IDENTITY_VALUE)
                    .header(SERVICE_ASSERTION_HEADER, assertion("DELETE", path, operationId, cancellationActor, null))
                    .retrieve().body(FiscalPeriodTransitionReceipt.class);
            requireReceiptOperation(operationId, receipt);
            if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.CANCELLED) {
                throw new IllegalStateException("Master did not cancel the transition");
            }
            return receipt;
        } catch (RestClientResponseException e) {
            throw transitionFailure("cancel", e);
        } catch (RestClientException e) {
            throw remoteFailure("transition cancel", e);
        }
    }

    @Override
    public FiscalPeriodTransitionReceipt confirmClosingTransition(String operationId) {
        String path = operationPath(operationId) + "/confirm";
        try {
            FiscalPeriodTransitionReceipt receipt = restClient.post().uri(path)
                    .header(SERVICE_IDENTITY_HEADER, SERVICE_IDENTITY_VALUE)
                    .header(SERVICE_ASSERTION_HEADER, assertion("POST", path, operationId, "closing-service", null))
                    .retrieve().body(FiscalPeriodTransitionReceipt.class);
            requireReceiptOperation(operationId, receipt);
            if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.CONFIRMED) {
                throw new IllegalStateException("Master did not confirm the transition");
            }
            return receipt;
        } catch (RestClientResponseException e) {
            throw transitionFailure("confirm", e);
        } catch (RestClientException e) {
            throw remoteFailure("transition confirm", e);
        }
    }

    private String operationPath(String operationId) {
        if (operationId == null || !operationId.matches("[A-Za-z0-9._@-]{1,128}")) {
            throw new IllegalArgumentException("Invalid transition operation ID");
        }
        return "/api/internal/fiscal-periods/transitions/" + operationId;
    }

    private String assertion(String method, String path, String operationId, String actor,
            FiscalPeriodTransitionCommand command) {
        requireConfiguredSecret(serviceSecret);
        Instant now = Instant.now();
        var builder = Jwts.builder().setIssuer("closing-service").setAudience("master-data-internal")
                .setSubject("closing").setIssuedAt(Date.from(now)).setExpiration(Date.from(now.plusSeconds(60)))
                .claim("actor", actor).claim("operationId", operationId)
                // The caller's role is checked at Closing ingress; this attests Closing's service authority.
                .claim("serviceAuthority", "closing-fiscal-transition")
                .claim("method", method).claim("path", path);
        if (command != null) {
            builder.claim("fiscalPeriodId", command.fiscalPeriodId())
                    .claim("expectedStatus", command.expectedStatus().name())
                    .claim("targetStatus", command.targetStatus().name());
        }
        return builder.signWith(Keys.hmacShaKeyFor(serviceSecret.getBytes(StandardCharsets.UTF_8)),
                SignatureAlgorithm.HS256).compact();
    }

    private void requireReceiptOperation(String operationId, FiscalPeriodTransitionReceipt receipt) {
        if (receipt == null || !operationId.equals(receipt.operationId())) {
            throw new IllegalStateException("Master returned a different or empty transition receipt");
        }
    }

    private RuntimeException transitionFailure(String action, RestClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return new SecurityException("Service authentication rejected by master-data");
        }
        return remoteFailure("transition " + action, e);
    }

    private record PrepareTransitionRequest(String expectedStatus, String targetStatus) { }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            FiscalPeriodRef response = restClient.get()
                    .uri("/api/basic/fiscal-periods/id/{id}", id)
                    .retrieve()
                    .body(FiscalPeriodRef.class);
            if (response != null) {
                requireValidPeriod(response);
                if (!id.equals(response.id())) {
                    throw new IllegalStateException("master-data lookup returned a different fiscal period ID");
                }
            }
            return Optional.ofNullable(response);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw remoteFailure("lookup", e);
        } catch (RestClientException e) {
            throw remoteFailure("lookup", e);
        }
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        if (fiscalYear == null || fiscalYear.isBlank() || fiscalPeriod == null || fiscalPeriod.isBlank()) {
            return Optional.empty();
        }
        try {
            FiscalPeriodRef response = restClient.get()
                    .uri("/api/basic/fiscal-periods/{year}/{period}", fiscalYear.trim(), fiscalPeriod.trim())
                    .retrieve()
                    .body(FiscalPeriodRef.class);
            if (response != null) {
                requireValidPeriod(response);
                if (!fiscalYear.trim().equals(response.fiscalYear())
                        || !fiscalPeriod.trim().equals(response.fiscalPeriod())) {
                    throw new IllegalStateException("master-data lookup returned a different fiscal year or period");
                }
            }
            return Optional.ofNullable(response);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw remoteFailure("lookup", e);
        } catch (RestClientException e) {
            throw remoteFailure("lookup", e);
        }
    }

    @Override
    public FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser) {
        throw new UnsupportedOperationException(
                "Direct remote fiscal status update is retired; use an operation-bound transition.");
    }

    private static void requireValidPeriod(FiscalPeriodRef response) {
        if (response.id() == null || response.id() < 1
                || response.fiscalYear() == null || response.fiscalYear().isBlank()
                || response.fiscalPeriod() == null || response.fiscalPeriod().isBlank()
                || response.startDate() == null || response.endDate() == null
                || response.startDate().isAfter(response.endDate())
                || response.closingStatus() == null || response.closingStatus().isBlank()) {
            throw new IllegalStateException("master-data returned an invalid fiscal period");
        }
    }

    private static IllegalStateException remoteFailure(String operation, RestClientException error) {
        // RestClient causes can contain URLs and response bodies; expose only the HTTP status.
        String status = error instanceof RestClientResponseException response
                ? " (HTTP " + response.getStatusCode().value() + ")" : "";
        return new IllegalStateException("master-data " + operation + " failed" + status);
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
                "closing.master-data." + propertyName + " must be a positive bounded duration");
    }

    private static void requireConfiguredSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Closing Master internal assertion key is not configured");
        }
    }

}
