package com.ho.account.journalledger.infrastructure.adapter.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JournalMasterDataQueryAdapterTest {

    @Test
    void localAdapterSupportsNonBlankStandaloneReferencesOnly() {
        LocalJournalMasterDataQueryAdapter adapter = new LocalJournalMasterDataQueryAdapter();

        assertThat(adapter.findAccountSubject(" 1100 ")).get().extracting(ref -> ref.code()).isEqualTo("1100");
        assertThat(adapter.findBusinessPartner("BP-1")).get().extracting(ref -> ref.active()).isEqualTo(true);
        assertThat(adapter.findAccountSubjectAt("1100", java.time.LocalDate.of(2024, 12, 31)))
                .get()
                .extracting(ref -> ref.code())
                .isEqualTo("1100");
        assertThat(adapter.findDepartment(" ")).isEmpty();
        assertThat(adapter.findFiscalPeriod("2026", "08"))
                .get()
                .satisfies(period -> {
                    assertThat(period.startDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
                    assertThat(period.endDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 31));
                    assertThat(period.closingStatus()).isEqualTo("OPEN");
                });
    }

    @Test
    void httpAdapterMapsMasterDataResponsesAndTreatsNotFoundAsEmpty() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpJournalMasterDataQueryAdapter adapter =
                new HttpJournalMasterDataQueryAdapter(builder.build());
        server.expect(requestTo("http://master-data.test/api/basic/account-subjects/1100"))
                .andRespond(withSuccess("""
                        {"code":"1100","name":"Cash","unsettled":false,"fixedAsset":false,
                         "balanceType":"DEBIT","category":"ASSETS"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://master-data.test/api/basic/departments/UNKNOWN"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://master-data.test/api/basic/fiscal-periods/2026/08"))
                .andRespond(withSuccess("""
                        {"id":8,"fiscalYear":"2026","fiscalPeriod":"08",
                         "startDate":"2026-08-01","endDate":"2026-08-31","closingStatus":"OPEN"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "http://master-data.test/api/basic/references/account-subjects/1100"
                                + "?effectiveDate=2024-12-31"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(new AccountSubjectRef(
                        "1100", "Historical cash", false, false, "DEBIT", "ASSETS")),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "http://master-data.test/api/basic/references/business-partners/BP-1"
                                + "?effectiveDate=2024-12-31"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(new BusinessPartnerRef(
                        "BP-1", "Historical partner", "CORPORATION", true)),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "http://master-data.test/api/basic/references/departments/D-1"
                                + "?effectiveDate=2024-12-31"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(new DepartmentRef(
                        "D-1", "Historical department", "COST_CENTER")),
                        MediaType.APPLICATION_JSON));

        assertThat(adapter.findAccountSubject("1100"))
                .get()
                .satisfies(ref -> {
                    assertThat(ref.name()).isEqualTo("Cash");
                    assertThat(ref.normalBalanceSide()).isEqualTo("DEBIT");
                    assertThat(ref.accountCategory()).isEqualTo("ASSETS");
                });
        assertThat(adapter.findDepartment("UNKNOWN")).isEmpty();
        assertThat(adapter.findFiscalPeriod("2026", "08"))
                .get()
                .extracting(period -> period.closingStatus())
                .isEqualTo("OPEN");
        assertThat(adapter.findAccountSubjectAt("1100", java.time.LocalDate.of(2024, 12, 31)))
                .get()
                .extracting(ref -> ref.name())
                .isEqualTo("Historical cash");
        assertThat(adapter.findBusinessPartnerAt("BP-1", java.time.LocalDate.of(2024, 12, 31)))
                .get()
                .extracting(ref -> ref.name())
                .isEqualTo("Historical partner");
        assertThat(adapter.findDepartmentAt("D-1", java.time.LocalDate.of(2024, 12, 31)))
                .get()
                .extracting(ref -> ref.name())
                .isEqualTo("Historical department");
        server.verify();
    }

    @Test
    void httpAdapterFailsClosedOnServerErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpJournalMasterDataQueryAdapter adapter =
                new HttpJournalMasterDataQueryAdapter(builder.build());
        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/BP-1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.findBusinessPartner("BP-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("master-data lookup returned a non-success status");
        server.verify();
    }

    @Test
    void httpAdapterFailsClosedWithinConfiguredReadTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/basic/account-subjects/1100", exchange -> {
            try {
                Thread.sleep(500);
                byte[] body = "{\"code\":\"1100\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (java.io.IOException ignored) {
                // Expected when the client closes the timed-out connection.
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            HttpJournalMasterDataQueryAdapter adapter = new HttpJournalMasterDataQueryAdapter(
                    RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofMillis(100),
                    Duration.ofMillis(100));
            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> adapter.findAccountSubject("1100"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("master-data lookup failed");
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                    .isLessThan(Duration.ofSeconds(2));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void httpAdapterRejectsSubMillisecondTimeoutInsteadOfConvertingItToUnlimited() {
        assertThatThrownBy(() -> new HttpJournalMasterDataQueryAdapter(
                        RestClient.builder(),
                        "http://master-data.test",
                        Duration.ofNanos(1),
                        Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");
        assertThatThrownBy(() -> new HttpJournalMasterDataQueryAdapter(
                        RestClient.builder(),
                        "http://master-data.test",
                        Duration.ofSeconds(1),
                        Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-timeout");
    }
}
