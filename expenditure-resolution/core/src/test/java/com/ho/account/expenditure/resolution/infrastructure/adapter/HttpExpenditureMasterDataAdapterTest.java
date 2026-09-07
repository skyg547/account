package com.ho.account.expenditure.resolution.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpExpenditureMasterDataAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsMasterDataResponsesAndHandlesNotFound() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureMasterDataAdapter adapter = new HttpExpenditureMasterDataAdapter(builder.build());

        server.expect(requestTo("http://master-data.test/api/basic/account-subjects/11100"))
                .andRespond(withSuccess("""
                        {"code":"11100","name":"Cash","unsettled":false,"fixedAsset":false,"balanceType":"DEBIT","category":"ASSETS"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/departments/UNKNOWN"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(requestTo("http://master-data.test/api/basic/references/account-subjects/11100?effectiveDate=2026-01-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new AccountSubjectRef(
                        "11100", "Historical Cash", false, false, "DEBIT", "ASSETS")), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/BP-001"))
                .andRespond(withSuccess("""
                        {"businessPartnerCode":"BP-001","businessPartnerName":"Vendor 1","partnerType":"VENDOR","useYn":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/business-partners/BP-001?effectiveDate=2026-01-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new BusinessPartnerRef(
                        "BP-001", "Vendor 1 Hist", "VENDOR", true)), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/departments/DEPT-10"))
                .andRespond(withSuccess("""
                        {"code":"DEPT-10","name":"IT Dept","type":"COST_CENTER"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/departments/DEPT-10?effectiveDate=2026-01-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new DepartmentRef(
                        "DEPT-10", "IT Dept Hist", "COST_CENTER")), MediaType.APPLICATION_JSON));

        assertThat(adapter.findAccountSubject("11100"))
                .get()
                .satisfies(subject -> {
                    assertThat(subject.name()).isEqualTo("Cash");
                    assertThat(subject.accountCategory()).isEqualTo("ASSETS");
                });

        assertThat(adapter.findDepartment("UNKNOWN")).isEmpty();

        assertThat(adapter.findAccountSubjectAt("11100", LocalDate.of(2026, 1, 1)))
                .get()
                .extracting(AccountSubjectRef::name)
                .isEqualTo("Historical Cash");

        assertThat(adapter.findBusinessPartner("BP-001"))
                .get()
                .extracting(BusinessPartnerRef::name)
                .isEqualTo("Vendor 1");

        assertThat(adapter.findBusinessPartnerAt("BP-001", LocalDate.of(2026, 1, 1)))
                .get()
                .extracting(BusinessPartnerRef::name)
                .isEqualTo("Vendor 1 Hist");

        assertThat(adapter.findDepartment("DEPT-10"))
                .get()
                .extracting(DepartmentRef::name)
                .isEqualTo("IT Dept");

        assertThat(adapter.findDepartmentAt("DEPT-10", LocalDate.of(2026, 1, 1)))
                .get()
                .extracting(DepartmentRef::name)
                .isEqualTo("IT Dept Hist");

        server.verify();
    }

    @Test
    void failsOnServerErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureMasterDataAdapter adapter = new HttpExpenditureMasterDataAdapter(builder.build());

        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/BP-ERR"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.findBusinessPartner("BP-ERR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("master-data lookup returned a non-success status");

        server.verify();
    }

    @Test
    void validatesTimeouts() {
        assertThatThrownBy(() -> new HttpExpenditureMasterDataAdapter(
                RestClient.builder(),
                "http://master-data.test",
                Duration.ofNanos(1),
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");
    }
}
