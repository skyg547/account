package com.ho.account.masterdata.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.closing.infrastructure.external.HttpFiscalPeriodControlAdapter;
import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.application.service.FiscalPeriodTransitionService;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodTransitionAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaMasterDataAuditAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodTransitionRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/** Real client signing/JSON, ingress, controller and committed H2 receipts; no listening server. */
@DataJpaTest(properties = {
        "spring.config.name=fiscal-transition-http-test",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false"
})
@ContextConfiguration(classes = FiscalPeriodTransitionHttpIntegrationTest.TestApplication.class)
@Import({JpaFiscalPeriodPersistenceAdapter.class, FiscalPeriodMapper.class,
        MonolithFiscalPeriodControlAdapter.class, FiscalPeriodTransitionService.class, JpaFiscalPeriodTransitionAdapter.class, JpaMasterDataAuditAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FiscalPeriodTransitionHttpIntegrationTest {
    private static final String SECRET = "synthetic-transition-signing-key-for-http-773";
    private static final String BODY = "{\"expectedStatus\":\"CLOSED\",\"targetStatus\":\"OPEN\"}";

    // Explicit ContextConfiguration source only: no component stereotype or discoverable Boot root.
    // Otherwise neighboring SpringBootTest classes can select this fixture instead of the real API.
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    @EnableJpaRepositories(basePackageClasses = FiscalPeriodRepository.class)
    static class TestApplication {}

    @Autowired private FiscalPeriodControlPort provider;
    @Autowired private JpaFiscalPeriodPersistenceAdapter periods;
    @Autowired private FiscalPeriodRepository periodRepository;
    @Autowired private FiscalPeriodTransitionRepository transitions;
    @Autowired private PlatformTransactionManager transactionManager;
    private TransactionTemplate transaction;
    private Long periodId;
    private MockMvc mvc;
    private HttpFiscalPeriodControlAdapter client;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        periodId = transaction.execute(status -> periods.save(FiscalPeriod.reconstitute(
                null, "2026", "11", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                FiscalPeriod.ClosingStatus.CLOSED, null, null, "prior")).getId());
        mvc = MockMvcBuilders.standaloneSetup(new InternalFiscalPeriodController(provider))
                .setControllerAdvice(new MasterDataExceptionHandler())
                .addFilters(new MasterDataIngressAuthenticationFilter(
                        "different-synthetic-user-signing-key-773", "auth-service", "account-api", SECRET))
                .build();
        RestClient.Builder builder = spy(RestClient.builder()
                .requestFactory(new MockMvcClientHttpRequestFactory(mvc)));
        // Keep the real client contract but route transport into the in-memory servlet boundary.
        doReturn(builder).when(builder).requestFactory(any(ClientHttpRequestFactory.class));
        client = new HttpFiscalPeriodControlAdapter(builder, "http://master-data.test",
                Duration.ofSeconds(2), Duration.ofSeconds(5), SECRET);
    }

    @AfterEach
    void cleanFixtures() {
        transaction.executeWithoutResult(status -> {
            transitions.deleteAll();
            periodRepository.deleteById(periodId);
        });
    }

    static Stream<String> validOperationIds() {
        return Stream.of("reopen@773", "@" + "x".repeat(127), "reopen..773", ".reopen", "...");
    }

    @ParameterizedTest
    @MethodSource("validOperationIds")
    void realClientPrepareLookupApplyConfirmAndRetryKeepAdmissionFenced(String operationId) {
        var command = command(operationId, "operator");
        assertThat(client.findTransitionReceipt(operationId)).isEmpty();
        var prepared = client.prepareClosingTransition(command);
        assertThat(prepared.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.PREPARED);
        assertThat(client.prepareClosingTransition(command)).isEqualTo(prepared);
        assertThat(client.findTransitionReceipt(operationId)).contains(prepared);
        var applied = client.applyClosingTransition(operationId);
        assertThat(applied.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
        assertThat(client.applyClosingTransition(operationId)).isEqualTo(applied);
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.OPEN);
        assertThat(provider.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        var confirmed = client.confirmClosingTransition(operationId);
        assertThat(confirmed.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
        assertThat(client.confirmClosingTransition(operationId)).isEqualTo(confirmed);
        assertThat(client.prepareClosingTransition(command)).isEqualTo(confirmed);
        assertThat(client.findTransitionReceipt(operationId)).contains(confirmed);
        assertThat(provider.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("OPEN");
    }

    @Test
    void realClientCancelKeepsTombstoneAndRejectsDifferentSignedCommandWith409() throws Exception {
        String operationId = "cancel@773";
        var command = command(operationId, "operator");
        client.prepareClosingTransition(command);
        var cancelled = client.cancelClosingTransition(operationId, "recovery-operator");
        assertThat(cancelled.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.CANCELLED);
        assertThat(client.cancelClosingTransition(operationId, "recovery-operator")).isEqualTo(cancelled);
        assertThat(client.findTransitionReceipt(operationId)).contains(cancelled);
        assertThat(client.prepareClosingTransition(command)).isEqualTo(cancelled);
        String path = preparePath(operationId);
        mvc.perform(request(HttpMethod.PUT, URI.create(path))
                        .header("X-Service-Assertion", assertion("PUT", path, operationId, "another-actor"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        assertThat(provider.findTransitionReceipt(operationId)).contains(cancelled);
        assertThat(provider.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
    }

    @Test
    void invalidIdsAndEncodedSegmentsFailAllSignedRoutesWithoutWriting() throws Exception {
        for (String operationId : List.of("invalid:773", "", ".", "..", "x".repeat(129), "encoded%40773", "encoded%2F773")) {
            String receiptPath = receiptPath(operationId);
            for (String action : List.of("prepare", "lookup", "apply", "cancel", "confirm")) {
                String method = switch (action) {
                    case "prepare" -> "PUT";
                    case "lookup" -> "GET";
                    case "cancel" -> "DELETE";
                    default -> "POST";
                };
                String path = action.equals("prepare") ? preparePath(operationId)
                        : action.equals("confirm") ? receiptPath + "/confirm" : receiptPath;
                mvc.perform(request(HttpMethod.valueOf(method), URI.create(path))
                                .header("X-Service-Assertion", assertion(method, path, operationId, "operator"))
                                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                        .andExpect(status().isUnauthorized());
            }
        }
        assertThat(transitions.count()).isZero();
    }

    @Test
    void signedMethodPathOperationAndBodyCannotBeSubstitutedForAtIdentity() throws Exception {
        String operationId = "bound@773";
        String path = preparePath(operationId);
        for (String token : List.of(assertion("POST", path, operationId, "operator"),
                assertion("PUT", path + "x", operationId, "operator"),
                assertion("PUT", path, "other@773", "operator"))) {
            mvc.perform(request(HttpMethod.PUT, URI.create(path)).header("X-Service-Assertion", token)
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(request(HttpMethod.PUT, URI.create(path))
                        .header("X-Service-Assertion", assertion("PUT", path, operationId, "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(transitions.count()).isZero();
    }

    private FiscalPeriodTransitionCommand command(String operationId, String actor) {
        return new FiscalPeriodTransitionCommand(operationId, periodId,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, actor);
    }

    private String preparePath(String operationId) {
        return "/api/internal/fiscal-periods/" + periodId + "/transitions/" + operationId;
    }

    private String receiptPath(String operationId) {
        return "/api/internal/fiscal-periods/transitions/" + operationId;
    }

    private String assertion(String method, String path, String operationId, String actor) {
        Instant now = Instant.now();
        return Jwts.builder().setSubject("closing").setIssuer("closing-service")
                .setAudience("master-data-internal").setIssuedAt(Date.from(now.minusSeconds(1)))
                .setExpiration(Date.from(now.plusSeconds(60))).claim("actor", actor)
                .claim("serviceAuthority", "closing-fiscal-transition").claim("fiscalPeriodId", periodId)
                .claim("operationId", operationId).claim("expectedStatus", "CLOSED").claim("targetStatus", "OPEN")
                .claim("method", method).claim("path", path)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
