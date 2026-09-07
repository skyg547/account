package com.ho.account.receivable.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpReceivableJournalAdapterTest {

    @Test
    void createsDraftEntrySuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReceivableJournalAdapter adapter = new HttpReceivableJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/journals/posting"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"journalEntryId":301,"slipNo":"JE-AR-301","status":"DRAFT"}
                        """, MediaType.APPLICATION_JSON));

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                "AR Journal",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "AR_USER",
                "AR_USER",
                "RECEIVABLE",
                "3001",
                List.of(
                        new JournalLineCommand("DEBIT", "11200", BigDecimal.valueOf(8000), BigDecimal.valueOf(8000), "D-1", "CUST-1", "Debit AR"),
                        new JournalLineCommand("CREDIT", "41000", BigDecimal.valueOf(8000), BigDecimal.valueOf(8000), "D-1", "CUST-1", "Credit Sales")
                )
        );

        JournalPostingResult result = adapter.createDraftEntry(command);

        assertThat(result.journalEntryId()).isEqualTo(301L);
        assertThat(result.slipNo()).isEqualTo("JE-AR-301");
        assertThat(result.status()).isEqualTo("DRAFT");
        server.verify();
    }

    @Test
    void approvesAndPostsJournalEntry() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReceivableJournalAdapter adapter = new HttpReceivableJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals/301/approve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "ar_officer"))
                .andRespond(withSuccess());

        server.expect(requestTo("http://journal-ledger.test/api/journals/301/post"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "ar_officer"))
                .andRespond(withSuccess());

        adapter.approveAndPost(301L, "ar_officer");
        server.verify();
    }

    @Test
    void failsDraftEntryOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReceivableJournalAdapter adapter = new HttpReceivableJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/journals/posting"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Test",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "USR",
                "USR",
                "RECEIVABLE",
                "1",
                List.of(new JournalLineCommand("DEBIT", "11200", BigDecimal.valueOf(100), BigDecimal.valueOf(100), "D", "B", "desc"))
        );

        assertThatThrownBy(() -> adapter.createDraftEntry(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Journal ledger posting failed");
        server.verify();
    }
}
