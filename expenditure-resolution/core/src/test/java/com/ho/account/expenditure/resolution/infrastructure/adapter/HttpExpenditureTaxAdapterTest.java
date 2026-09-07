package com.ho.account.expenditure.resolution.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ho.account.contracts.tax.TaxInvoiceRef;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpExpenditureTaxAdapterTest {

    @Test
    void findsTaxInvoiceByIdAndMapsResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://tax.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureTaxAdapter adapter = new HttpExpenditureTaxAdapter(builder.build());

        server.expect(requestTo("http://tax.test/api/ap/invoices/5001"))
                .andRespond(withSuccess("""
                        {"id":5001,"issueId":"TAX-2026-001","type":"PURCHASE","status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));

        Optional<TaxInvoiceRef> result = adapter.findById(5001L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(5001L);
        assertThat(result.get().issueId()).isEqualTo("TAX-2026-001");
        assertThat(result.get().purchase()).isTrue();
        assertThat(result.get().active()).isTrue();
        assertThat(result.get().usableForPurchaseSettlement()).isTrue();
        server.verify();
    }

    @Test
    void returnsEmptyWhenTaxInvoiceNotFound() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://tax.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureTaxAdapter adapter = new HttpExpenditureTaxAdapter(builder.build());

        server.expect(requestTo("http://tax.test/api/ap/invoices/9999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.findById(9999L)).isEmpty();
        server.verify();
    }

    @Test
    void failsOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://tax.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureTaxAdapter adapter = new HttpExpenditureTaxAdapter(builder.build());

        server.expect(requestTo("http://tax.test/api/ap/invoices/5002"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.findById(5002L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tax invoice lookup returned a non-success status");
        server.verify();
    }

    @Test
    void handlesNullIdGracefully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://tax.test");
        HttpExpenditureTaxAdapter adapter = new HttpExpenditureTaxAdapter(builder.build());

        assertThat(adapter.findById(null)).isEmpty();
    }
}
