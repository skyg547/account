package com.ho.account.expenditure.payable.infrastructure.adapter;

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

class HttpPayableMasterDataAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsMasterDataResponsesAndFiscalPeriods() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://master-data.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpPayableMasterDataAdapter adapter = new HttpPayableMasterDataAdapter(builder.build());

        server.expect(requestTo("http://master-data.test/api/basic/account-subjects/21000"))
                .andRespond(withSuccess("""
                        {"code":"21000","name":"Accounts Payable","unsettled":true,"fixedAsset":false,"balanceType":"CREDIT","category":"LIABILITIES"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/departments/UNKNOWN"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(requestTo("http://master-data.test/api/basic/references/account-subjects/21000?effectiveDate=2026-05-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new AccountSubjectRef(
                        "21000", "Historical AP", true, false, "CREDIT", "LIABILITIES")), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/businesspartners/VEN-1"))
                .andRespond(withSuccess("""
                        {"businessPartnerCode":"VEN-1","businessPartnerName":"Supplier A","partnerType":"VENDOR","useYn":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/business-partners/VEN-1?effectiveDate=2026-05-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new BusinessPartnerRef(
                        "VEN-1", "Supplier A Hist", "VENDOR", true)), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/departments/D-PURCHASE"))
                .andRespond(withSuccess("""
                        {"code":"D-PURCHASE","name":"Purchasing","type":"DEPARTMENT"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/references/departments/D-PURCHASE?effectiveDate=2026-05-01"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(new DepartmentRef(
                        "D-PURCHASE", "Purchasing Hist", "DEPARTMENT")), MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/fiscal-periods/id/5"))
                .andRespond(withSuccess("""
                        {"id":5,"fiscalYear":"2026","fiscalPeriod":"05","startDate":"2026-05-01","endDate":"2026-05-31","closingStatus":"OPEN"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://master-data.test/api/basic/fiscal-periods/2026/05"))
                .andRespond(withSuccess("""
                        {"id":5,"fiscalYear":"2026","fiscalPeriod":"05","startDate":"2026-05-01","endDate":"2026-05-31","closingStatus":"OPEN"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findAccountSubject("21000"))
                .get()
                .satisfies(subject -> {
                    assertThat(subject.name()).isEqualTo("Accounts Payable");
                    assertThat(subject.accountCategory()).isEqualTo("LIABILITIES");
                });

        assertThat(adapter.findDepartment("UNKNOWN")).isEmpty();

        assertThat(adapter.findAccountSubjectAt("21000", LocalDate.of(2026, 5, 1)))
                .get()
                .extracting(AccountSubjectRef::name)
                .isEqualTo("Historical AP");

        assertThat(adapter.findBusinessPartner("VEN-1"))
                .get()
                .extracting(BusinessPartnerRef::name)
                .isEqualTo("Supplier A");

        assertThat(adapter.findBusinessPartnerAt("VEN-1", LocalDate.of(2026, 5, 1)))
                .get()
                .extracting(BusinessPartnerRef::name)
                .isEqualTo("Supplier A Hist");

        assertThat(adapter.findDepartment("D-PURCHASE"))
                .get()
                .extracting(DepartmentRef::name)
                .isEqualTo("Purchasing");

        assertThat(adapter.findDepartmentAt("D-PURCHASE", LocalDate.of(2026, 5, 1)))
                .get()
                .extracting(DepartmentRef::name)
                .isEqualTo("Purchasing Hist");

        assertThat(adapter.findFiscalPeriodById(5L))
                .get()
                .extracting(period -> period.closingStatus())
                .isEqualTo("OPEN");

        assertThat(adapter.findFiscalPeriod("2026", "05"))
                .get()
                .extracting(period -> period.closingStatus())
                .isEqualTo("OPEN");

        server.verify();
    }

    @Test
    void rejectsUpdateClosingStatusAsReadOnly() {
        HttpPayableMasterDataAdapter adapter = new HttpPayableMasterDataAdapter(RestClient.builder().build());
        assertThatThrownBy(() -> adapter.updateClosingStatus(1L, "CLOSED", "admin"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void validatesTimeouts() {
        assertThatThrownBy(() -> new HttpPayableMasterDataAdapter(
                RestClient.builder(),
                "http://master-data.test",
                Duration.ofNanos(1),
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");
    }
}
