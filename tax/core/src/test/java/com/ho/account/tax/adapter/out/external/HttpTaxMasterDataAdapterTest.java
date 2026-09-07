package com.ho.account.tax.adapter.out.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpTaxMasterDataAdapterTest {

    @Test
    void mapsMasterDataResponsesAndTreatsNotFoundAsEmpty() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpTaxMasterDataAdapter adapter = new HttpTaxMasterDataAdapter(builder.build());

        server.expect(requestTo("http://master-data.test/api/basic/account-subjects/1100"))
                .andRespond(withSuccess("""
                        {"code":"1100","name":"Cash","unsettled":false,"fixedAsset":false,
                         "balanceType":"DEBIT","category":"ASSETS"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://master-data.test/api/basic/departments/UNKNOWN"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/BP-1"))
                .andRespond(withSuccess("""
                        {"businessPartnerCode":"BP-1","businessPartnerName":"Partner 1","partnerType":"VENDOR","useYn":true}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://master-data.test/api/basic/departments/D-1"))
                .andRespond(withSuccess("""
                        {"code":"D-1","name":"Finance","type":"COST_CENTER"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/account-subjects/1100?effectiveDate=2024-12-31"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(new AccountSubjectRef(
                        "1100", "Historical cash", false, false, "DEBIT", "ASSETS")),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://master-data.test/api/basic/references/business-partners/BP-1?effectiveDate=2024-12-31"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(new BusinessPartnerRef(
                        "BP-1", "Historical partner", "CORPORATION", true)),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://master-data.test/api/basic/references/departments/D-1?effectiveDate=2024-12-31"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(new DepartmentRef(
                        "D-1", "Historical department", "COST_CENTER")),
                        MediaType.APPLICATION_JSON));

        Optional<AccountSubjectRef> account = adapter.findAccountSubject("1100");
        assertThat(account).isPresent();
        assertThat(account.get().name()).isEqualTo("Cash");
        assertThat(account.get().normalBalanceSide()).isEqualTo("DEBIT");

        assertThat(adapter.findDepartment("UNKNOWN")).isEmpty();

        Optional<BusinessPartnerRef> partner = adapter.findBusinessPartner("BP-1");
        assertThat(partner).isPresent();
        assertThat(partner.get().name()).isEqualTo("Partner 1");

        Optional<DepartmentRef> dept = adapter.findDepartment("D-1");
        assertThat(dept).isPresent();
        assertThat(dept.get().name()).isEqualTo("Finance");

        assertThat(adapter.findAccountSubjectAt("1100", LocalDate.of(2024, 12, 31)))
                .get().extracting(AccountSubjectRef::name).isEqualTo("Historical cash");
        assertThat(adapter.findBusinessPartnerAt("BP-1", LocalDate.of(2024, 12, 31)))
                .get().extracting(BusinessPartnerRef::name).isEqualTo("Historical partner");
        assertThat(adapter.findDepartmentAt("D-1", LocalDate.of(2024, 12, 31)))
                .get().extracting(DepartmentRef::name).isEqualTo("Historical department");

        server.verify();
    }

    @Test
    void failsClosedOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpTaxMasterDataAdapter adapter = new HttpTaxMasterDataAdapter(builder.build());

        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/BP-1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.findBusinessPartner("BP-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("master-data lookup returned a non-success status");
        server.verify();
    }

    @Test
    void failsClosedWithinConfiguredReadTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/basic/account-subjects/1100", exchange -> {
            try {
                Thread.sleep(500);
                byte[] body = "{\"code\":\"1100\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            HttpTaxMasterDataAdapter adapter = new HttpTaxMasterDataAdapter(
                    RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofMillis(100),
                    Duration.ofMillis(100)
            );
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
    void rejectsInvalidTimeout() {
        assertThatThrownBy(() -> new HttpTaxMasterDataAdapter(
                RestClient.builder(),
                "http://master-data.test",
                Duration.ofNanos(1),
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");

        assertThatThrownBy(() -> new HttpTaxMasterDataAdapter(
                RestClient.builder(),
                "http://master-data.test",
                Duration.ofSeconds(1),
                Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-timeout");
    }
}
