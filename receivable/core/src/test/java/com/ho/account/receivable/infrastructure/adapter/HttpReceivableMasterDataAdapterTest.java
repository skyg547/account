package com.ho.account.receivable.infrastructure.adapter;

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

class HttpReceivableMasterDataAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsMasterDataResponsesAndFiscalPeriods() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReceivableMasterDataAdapter adapter = new HttpReceivableMasterDataAdapter(builder.build());

        server.expect(requestTo("http://master-data.test/api/basic/account-subjects/11200"))
                .andRespond(withSuccess("""
                        {"code":"11200","name":"Accounts Receivable","unsettled":true,"fixedAsset":false,"balanceType":"DEBIT","category":"ASSETS"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/departments/UNKNOWN"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(requestTo("http://master-data.test/api/basic/references/account-subjects/11200?effectiveDate=2026-06-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new AccountSubjectRef(
                        "11200", "Historical AR", true, false, "DEBIT", "ASSETS")), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/CUST-1"))
                .andRespond(withSuccess("""
                        {"businessPartnerCode":"CUST-1","businessPartnerName":"Customer A","partnerType":"CUSTOMER","useYn":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/business-partners/CUST-1?effectiveDate=2026-06-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new BusinessPartnerRef(
                        "CUST-1", "Customer A Hist", "CUSTOMER", true)), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/departments/D-SALES"))
                .andRespond(withSuccess("""
                        {"code":"D-SALES","name":"Sales Dept","type":"DEPARTMENT"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/departments/D-SALES?effectiveDate=2026-06-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new DepartmentRef(
                        "D-SALES", "Sales Dept Hist", "DEPARTMENT")), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/fiscal-periods/id/6"))
                .andRespond(withSuccess("""
                        {"id":6,"fiscalYear":"2026","fiscalPeriod":"06","startDate":"2026-06-01","endDate":"2026-06-30","closingStatus":"OPEN"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/fiscal-periods/2026/06"))
                .andRespond(withSuccess("""
                        {"id":6,"fiscalYear":"2026","fiscalPeriod":"06","startDate":"2026-06-01","endDate":"2026-06-30","closingStatus":"OPEN"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findAccountSubject("11200"))
                .get()
                .satisfies(subject -> {
                    assertThat(subject.name()).isEqualTo("Accounts Receivable");
                    assertThat(subject.accountCategory()).isEqualTo("ASSETS");
                });

        assertThat(adapter.findDepartment("UNKNOWN")).isEmpty();

        assertThat(adapter.findAccountSubjectAt("11200", LocalDate.of(2026, 6, 1)))
                .get()
                .extracting(AccountSubjectRef::name)
                .isEqualTo("Historical AR");

        assertThat(adapter.findBusinessPartner("CUST-1"))
                .get()
                .extracting(BusinessPartnerRef::name)
                .isEqualTo("Customer A");

        assertThat(adapter.findBusinessPartnerAt("CUST-1", LocalDate.of(2026, 6, 1)))
                .get()
                .extracting(BusinessPartnerRef::name)
                .isEqualTo("Customer A Hist");

        assertThat(adapter.findDepartment("D-SALES"))
                .get()
                .extracting(DepartmentRef::name)
                .isEqualTo("Sales Dept");

        assertThat(adapter.findDepartmentAt("D-SALES", LocalDate.of(2026, 6, 1)))
                .get()
                .extracting(DepartmentRef::name)
                .isEqualTo("Sales Dept Hist");

        assertThat(adapter.findFiscalPeriodById(6L))
                .get()
                .extracting(period -> period.closingStatus())
                .isEqualTo("OPEN");

        assertThat(adapter.findFiscalPeriod("2026", "06"))
                .get()
                .extracting(period -> period.closingStatus())
                .isEqualTo("OPEN");

        server.verify();
    }

    @Test
    void rejectsUpdateClosingStatusAsReadOnly() {
        HttpReceivableMasterDataAdapter adapter = new HttpReceivableMasterDataAdapter(RestClient.builder().build());
        assertThatThrownBy(() -> adapter.updateClosingStatus(1L, "CLOSED", "admin"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void validatesTimeouts() {
        assertThatThrownBy(() -> new HttpReceivableMasterDataAdapter(
                RestClient.builder(),
                "http://master-data.test",
                Duration.ofNanos(1),
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");
    }
}
