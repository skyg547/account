package com.ho.account.expenditure.resolution.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpExpenditureAssetAdapterTest {

    @Test
    void registersAcquiredAssetSuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://asset.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureAssetAdapter adapter = new HttpExpenditureAssetAdapter(builder.build());

        server.expect(requestTo("http://asset.test/api/fixed-assets"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "system"))
                .andExpect(content().json("""
                        {
                          "assetCode": "AST-2026-001",
                          "assetName": "Laptop",
                          "accountSubjectCode": "12000",
                          "acquisitionCost": 2000000,
                          "departmentCode": "DEPT-IT"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));

        AssetAcquisitionCommand command = new AssetAcquisitionCommand(
                "AST-2026-001",
                "Laptop",
                "12000",
                LocalDate.of(2026, 9, 1),
                BigDecimal.valueOf(2000000),
                "DEPT-IT",
                5,
                "STRAIGHT_LINE",
                "ACQUIRED"
        );

        adapter.registerAcquiredAsset(command);
        server.verify();
    }

    @Test
    void activatesLeaseContractSuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://asset.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureAssetAdapter adapter = new HttpExpenditureAssetAdapter(builder.build());

        server.expect(requestTo("http://asset.test/api/ifrs16/leases/901/activate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "system"))
                .andRespond(withSuccess());

        assertThatCode(() -> adapter.activateLeaseContract(901L)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void handlesNotFoundGracefullyWhenActivatingLease() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://asset.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureAssetAdapter adapter = new HttpExpenditureAssetAdapter(builder.build());

        server.expect(requestTo("http://asset.test/api/ifrs16/leases/902/activate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When endpoint not yet deployed, 404 should be caught safely without throwing
        assertThatCode(() -> adapter.activateLeaseContract(902L)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void failsOnServerErrorsWhenActivatingLease() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://asset.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureAssetAdapter adapter = new HttpExpenditureAssetAdapter(builder.build());

        server.expect(requestTo("http://asset.test/api/ifrs16/leases/903/activate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.activateLeaseContract(903L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lease contract activation returned a non-success status");
        server.verify();
    }
}
