package com.ho.account.reconciliation.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalSummary;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpReconciliationJournalAdapterTest {

    @Test
    void createDraftEntryPostsCommandSuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/journals/posting"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"journalEntryId":101,"slipNo":"SLIP-101","status":"DRAFT"}
                        """, MediaType.APPLICATION_JSON));

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                "Test slip",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "recon-user",
                "recon-user",
                "RECON",
                "REC-01",
                List.of(new JournalLineCommand(
                        "DEBIT",
                        "1100",
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        null,
                        null,
                        "line 1"
                ))
        );

        JournalPostingResult result = adapter.createDraftEntry(command);

        assertThat(result.journalEntryId()).isEqualTo(101L);
        assertThat(result.slipNo()).isEqualTo("SLIP-101");
        assertThat(result.status()).isEqualTo("DRAFT");
        server.verify();
    }

    @Test
    void createDraftEntryFailsClosedOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/journals/posting"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                "Test slip",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "recon-user",
                "recon-user",
                "RECON",
                "REC-01",
                List.of(new JournalLineCommand(
                        "DEBIT",
                        "1100",
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        null,
                        null,
                        "line 1"
                ))
        );

        assertThatThrownBy(() -> adapter.createDraftEntry(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Remote journal posting failed");
        server.verify();
    }

    @Test
    void approveAndPostExecutesBothStepsWithActorHeader() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals/101/approve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "recon-user"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://journal-ledger.test/api/journals/101/post"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "recon-user"))
                .andRespond(withSuccess());

        adapter.approveAndPost(101L, "recon-user");
        server.verify();
    }

    @Test
    void approveAndPostDefaultsBlankActorToReconciliation() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals/101/approve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "reconciliation"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://journal-ledger.test/api/journals/101/post"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "reconciliation"))
                .andRespond(withSuccess());

        adapter.approveAndPost(101L, "   ");
        server.verify();
    }

    @Test
    void getJournalSummariesRetrievesAndMapsList() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals?startDate=2026-09-01&endDate=2026-09-07"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 101,
                            "slipNo": "SLIP-101",
                            "slipDate": "2026-09-01",
                            "accountingDate": "2026-09-01",
                            "description": "Payment",
                            "status": "POSTED",
                            "entryType": "GENERAL",
                            "currencyCode": "KRW",
                            "lineageSourceType": "RECON",
                            "lineageSourceId": "REC-01",
                            "lines": []
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<JournalSummary> summaries = adapter.getJournalSummaries(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7)
        );

        assertThat(summaries).hasSize(1);
        JournalSummary summary = summaries.get(0);
        assertThat(summary.getId()).isEqualTo(101L);
        assertThat(summary.getSlipNo()).isEqualTo("SLIP-101");
        assertThat(summary.getStatus()).isEqualTo("POSTED");
        server.verify();
    }

    @Test
    void getJournalSummariesReturnsEmptyOn404OrNullDates() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals?startDate=2026-09-01&endDate=2026-09-07"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        List<JournalSummary> summaries = adapter.getJournalSummaries(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7)
        );
        assertThat(summaries).isEmpty();
        assertThat(adapter.getJournalSummaries(null, LocalDate.now())).isEmpty();
        server.verify();
    }

    @Test
    void getJournalSummaryAndFindBySlipNoReturnExpectedOrEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals/101"))
                .andRespond(withSuccess("""
                        {"id":101,"slipNo":"SLIP-101","status":"POSTED"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://journal-ledger.test/api/journals/999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://journal-ledger.test/api/journals/SLIP-101"))
                .andRespond(withSuccess("""
                        {"id":101,"slipNo":"SLIP-101","status":"POSTED"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://journal-ledger.test/api/journals/SLIP-UNKNOWN"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        JournalSummary found = adapter.getJournalSummary(101L);
        assertThat(found).isNotNull();
        assertThat(found.getSlipNo()).isEqualTo("SLIP-101");

        JournalSummary notFound = adapter.getJournalSummary(999L);
        assertThat(notFound).isNull();
        assertThat(adapter.getJournalSummary(null)).isNull();

        Optional<JournalSummary> slipFound = adapter.findBySlipNo("SLIP-101");
        assertThat(slipFound).isPresent();
        assertThat(slipFound.get().getId()).isEqualTo(101L);

        Optional<JournalSummary> slipNotFound = adapter.findBySlipNo("SLIP-UNKNOWN");
        assertThat(slipNotFound).isEmpty();
        assertThat(adapter.findBySlipNo("   ")).isEmpty();

        server.verify();
    }

    @Test
    void failsClosedWithinConfiguredReadTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/journals/101", exchange -> {
            try {
                Thread.sleep(500);
                byte[] body = "{\"id\":101}".getBytes(StandardCharsets.UTF_8);
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
            HttpReconciliationJournalAdapter adapter = new HttpReconciliationJournalAdapter(
                    RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofMillis(100),
                    Duration.ofMillis(100)
            );
            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> adapter.getJournalSummary(101L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Remote journal lookup failed");
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                    .isLessThan(Duration.ofSeconds(2));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsInvalidTimeout() {
        assertThatThrownBy(() -> new HttpReconciliationJournalAdapter(
                RestClient.builder(),
                "http://journal-ledger.test",
                Duration.ofNanos(1),
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");

        assertThatThrownBy(() -> new HttpReconciliationJournalAdapter(
                RestClient.builder(),
                "http://journal-ledger.test",
                Duration.ofSeconds(1),
                Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-timeout");
    }
}
