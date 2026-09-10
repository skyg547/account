package com.ho.account.deposit.infrastructure.adapter.out.external;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpDepositMasterDataAdapterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String ROOT = "http://reference.invalid/api/basic/references/";
    private MockRestServiceServer server;
    private HttpDepositMasterDataAdapter adapter;

    @BeforeEach
    void setUp() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class, RestClientAutoConfiguration.class)).run(context -> {
            var builder = context.getBean(RestClient.Builder.class).baseUrl("http://reference.invalid");
            server = MockRestServiceServer.bindTo(builder).build();
            adapter = new HttpDepositMasterDataAdapter(builder.build());
        });
    }

    @Test
    void historicalReferencesUseActualProviderEndpointsAndPreserveCreditDirection() {
        server.expect(requestTo(ROOT + "account-subjects/20200?effectiveDate=" + DATE)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":\"20200\",\"name\":\"Deposit liability\",\"normalBalanceSide\":\"CREDIT\",\"accountCategory\":\"LIABILITY\",\"unsettled\":false,\"fixedAsset\":false}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ROOT + "business-partners/C1?effectiveDate=" + DATE))
                .andRespond(withSuccess("{\"code\":\"C1\",\"name\":\"Customer\",\"partnerType\":\"CUSTOMER\",\"active\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ROOT + "departments/D1?effectiveDate=" + DATE))
                .andRespond(withSuccess("{\"code\":\"D1\",\"name\":\"Deposit\",\"type\":\"BUSINESS\"}", MediaType.APPLICATION_JSON));
        assertThat(adapter.findAccountSubjectAt("20200", DATE).orElseThrow().normalBalanceSide()).isEqualTo("CREDIT");
        assertThat(adapter.findBusinessPartnerAt("C1", DATE).orElseThrow().code()).isEqualTo("C1");
        assertThat(adapter.findDepartmentAt("D1", DATE).orElseThrow().code()).isEqualTo("D1");
        server.verify();
    }

    @Test
    void notFoundIsAbsenceForAllReferenceTypes() {
        for (String path : new String[]{"account-subjects/A", "business-partners/B", "departments/C"})
            server.expect(requestTo(ROOT + path + "?effectiveDate=" + DATE)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(adapter.findAccountSubjectAt("A", DATE)).isEmpty();
        assertThat(adapter.findBusinessPartnerAt("B", DATE)).isEmpty();
        assertThat(adapter.findDepartmentAt("C", DATE)).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json", "{\"code\":\"OTHER\",\"name\":\"Wrong\",\"normalBalanceSide\":\"CREDIT\"}"})
    void malformedOrMismatchedReferenceNeverBecomesValid(String body) {
        server.expect(requestTo(ROOT + "account-subjects/20200?effectiveDate=" + DATE))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.findAccountSubjectAt("20200", DATE)).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 401, 500, 503})
    void providerFailureDoesNotBecomeAbsenceOrExposeProviderBody(int status) {
        server.expect(requestTo(ROOT + "account-subjects/20200?effectiveDate=" + DATE))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitive-provider-detail").contentType(MediaType.TEXT_PLAIN));
        assertThatThrownBy(() -> adapter.findAccountSubjectAt("20200", DATE)).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("sensitive-provider-detail").hasNoCause();
        server.verify();
    }

    @Test
    void blankCodesAreAbsentAndMissingDateFailsBeforeHttp() {
        assertThat(adapter.findAccountSubjectAt(" ", DATE)).isEmpty();
        assertThat(adapter.findBusinessPartnerAt(null, DATE)).isEmpty();
        assertThat(adapter.findDepartmentAt("", DATE)).isEmpty();
        assertThatThrownBy(() -> adapter.findAccountSubjectAt("20200", null)).isInstanceOf(NullPointerException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "dev"})
    void existingLocalSelectionStaysUsableWithoutRemoteOptIn(String profile) {
        new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withUserConfiguration(HttpDepositMasterDataAdapter.class, HttpDepositJournalPostingAdapter.class,
                        com.ho.account.deposit.infrastructure.adapter.out.local.LocalDepositMasterDataAdapter.class,
                        com.ho.account.deposit.infrastructure.adapter.out.local.LocalDepositJournalPostingAdapter.class)
                .withPropertyValues("account.deposit.local-adapters.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(HttpDepositMasterDataAdapter.class)
                            .doesNotHaveBean(HttpDepositJournalPostingAdapter.class);
                    assertThat(context).hasSingleBean(com.ho.account.contracts.masterdata.MasterDataQueryPort.class)
                            .hasSingleBean(com.ho.account.contracts.journal.JournalPostingPort.class);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "invalid"})
    void rejectsUnboundedTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpDepositMasterDataAdapter(RestClient.builder(), "http://reference.invalid", timeout, "5s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpDepositMasterDataAdapter(RestClient.builder(), "http://reference.invalid", "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
