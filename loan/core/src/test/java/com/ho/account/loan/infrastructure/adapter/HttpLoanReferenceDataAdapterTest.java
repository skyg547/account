package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpLoanReferenceDataAdapterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private MockRestServiceServer server;
    private HttpLoanReferenceDataAdapter adapter;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://reference.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpLoanReferenceDataAdapter(builder.build());
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 303, 307, 308})
    void productionTransportRejectsRedirectInsteadOfAcceptingReference(int status) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger followed = new AtomicInteger();
        provider.createContext("/", exchange -> {
            requests.incrementAndGet();
            if (exchange.getRequestURI().getPath().equals("/redirect-target")) {
                followed.incrementAndGet();
                byte[] body = ("{\"code\":\"131000\",\"name\":\"Loan receivable\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.getResponseHeaders().set("Location", "/redirect-target");
                exchange.sendResponseHeaders(status, -1);
            }
            exchange.close();
        });
        provider.start();
        try {
            var remote = new HttpLoanReferenceDataAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + provider.getAddress().getPort(),
                    Duration.ofSeconds(2), Duration.ofSeconds(2));
            // Use the production request factory: a mock response cannot reveal automatic redirects.
            assertThatThrownBy(() -> remote.requireAccount("131000", DATE)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP " + status).hasNoCause();
            assertThat(requests.get()).isEqualTo(1);
            assertThat(followed.get()).isZero();
        } finally {
            provider.stop(0);
        }
    }

    @Test
    void looksUpAccountByCodeAtRequestedBusinessDate() {
        expectAccount().andRespond(withSuccess("""
                {"code":"131000","name":"Loan receivable","normalBalanceSide":"DEBIT",
                 "unsettled":false,"fixedAsset":false,"accountCategory":"ASSET"}
                """, MediaType.APPLICATION_JSON));
        var result = adapter.requireAccount("131000", DATE);
        assertThat(result.code()).isEqualTo("131000");
        assertThat(result.name()).isEqualTo("Loan receivable");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 404, 500, 503})
    void providerFailuresDoNotBecomeValidReferences(int status) {
        expectAccount().andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> adapter.requireAccount("131000", DATE)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json",
            "{\"code\":\"999999\",\"name\":\"Wrong account\"}",
            "{\"code\":\"131000\",\"name\":\" \"}"})
    void rejectsMissingMalformedOrMismatchedAccount(String response) {
        expectAccount().andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.requireAccount("131000", DATE)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test
    void missingNumericPartnerAndCurrencyContractsFailWithoutInventingAnHttpRequest() {
        assertThatThrownBy(() -> adapter.requireLoanReferences(7L, "KRW", DATE))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void rejectsInvalidInputsBeforeCallingProvider() {
        assertThatThrownBy(() -> adapter.requireAccount(" ", DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.requireAccount("131000", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.requireLoanReferences(0L, "KRW", DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.requireLoanReferences(7L, "US", DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.requireLoanReferences(7L, "KRW", null)).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "invalid"})
    void rejectsUnboundedOrInvalidTransportTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpLoanReferenceDataAdapter(RestClient.builder(), "http://provider.invalid", timeout, "5s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpLoanReferenceDataAdapter(RestClient.builder(), "http://provider.invalid", "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transportFailureDoesNotExposeProviderResponse() {
        expectAccount().andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("sensitive-provider-detail").contentType(MediaType.TEXT_PLAIN));
        assertThatThrownBy(() -> adapter.requireAccount("131000", DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("sensitive-provider-detail")
                .hasNoCause();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "false"})
    void devWithoutExplicitRemoteOptInKeepsEmbeddedPortsWithoutHttpConfiguration(String setting) {
        var runner = adapterContext()
                .withBean(BusinessPartnerPersistencePort.class, () -> mock(BusinessPartnerPersistencePort.class))
                .withBean(CurrencyPersistencePort.class, () -> mock(CurrencyPersistencePort.class))
                .withBean(AccountSubjectPersistencePort.class, () -> mock(AccountSubjectPersistencePort.class))
                .withBean(JournalUseCase.class, () -> mock(JournalUseCase.class));
        if (!setting.equals("missing")) runner = runner.withPropertyValues("account.loan.remote.enabled=false");
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LoanReferenceDataPort.class).hasSingleBean(LoanJournalPort.class);
            assertThat(context.getBean(LoanReferenceDataPort.class)).isInstanceOf(LoanReferenceDataAdapter.class);
            assertThat(context.getBean(LoanJournalPort.class)).isInstanceOf(LoanJournalAdapter.class);
            assertThat(context).doesNotHaveBean(HttpLoanReferenceDataAdapter.class)
                    .doesNotHaveBean(HttpLoanJournalAdapter.class);
        });
    }

    @Test
    void explicitDevRemoteOptInSelectsOnlyHttpPortsWithoutProviderPersistenceDependencies() {
        adapterContext().withPropertyValues("account.loan.remote.enabled=true",
                        "account.loan.master-data-base-url=http://reference.invalid",
                        "account.loan.journal-base-url=http://journal.invalid")
                .withBean(RestClient.Builder.class, RestClient::builder)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LoanReferenceDataPort.class).hasSingleBean(LoanJournalPort.class);
                    assertThat(context.getBean(LoanReferenceDataPort.class)).isInstanceOf(HttpLoanReferenceDataAdapter.class);
                    assertThat(context.getBean(LoanJournalPort.class)).isInstanceOf(HttpLoanJournalAdapter.class);
                    assertThat(context).doesNotHaveBean(LoanReferenceDataAdapter.class)
                            .doesNotHaveBean(LoanJournalAdapter.class);
                });
    }

    private ApplicationContextRunner adapterContext() {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
                .withUserConfiguration(LoanReferenceDataAdapter.class, LoanJournalAdapter.class,
                        HttpLoanReferenceDataAdapter.class, HttpLoanJournalAdapter.class);
    }

    private org.springframework.test.web.client.ResponseActions expectAccount() {
        return server.expect(requestTo("http://reference.invalid/api/basic/references/account-subjects/131000?effectiveDate=2026-09-10"))
                .andExpect(method(HttpMethod.GET));
    }
}
