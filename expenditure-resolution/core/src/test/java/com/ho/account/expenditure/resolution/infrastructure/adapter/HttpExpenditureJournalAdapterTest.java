package com.ho.account.expenditure.resolution.infrastructure.adapter;

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

class HttpExpenditureJournalAdapterTest {

    @Test
    void createsDraftEntrySuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureJournalAdapter adapter = new HttpExpenditureJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/journals/posting"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"journalEntryId":101,"slipNo":"JE-2026-101","status":"DRAFT"}
                        """, MediaType.APPLICATION_JSON));

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                "Expenditure Resolution Journal",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "SYSTEM",
                "SYSTEM",
                "EXP_RESOLUTION",
                "1001",
                List.of(
                        new JournalLineCommand("DEBIT", "41000", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), "D-1", "BP-1", "Debit cash"),
                        new JournalLineCommand("CREDIT", "11100", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), "D-1", "BP-1", "Credit cash")
                )
        );

        JournalPostingResult result = adapter.createDraftEntry(command);

        assertThat(result.journalEntryId()).isEqualTo(101L);
        assertThat(result.slipNo()).isEqualTo("JE-2026-101");
        assertThat(result.status()).isEqualTo("DRAFT");
        server.verify();
    }

    @Test
    void approvesAndPostsJournalEntry() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureJournalAdapter adapter = new HttpExpenditureJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals/101/approve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "tester"))
                .andRespond(withSuccess());

        server.expect(requestTo("http://journal-ledger.test/api/journals/101/post"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "tester"))
                .andRespond(withSuccess());

        adapter.approveAndPost(101L, "tester");
        server.verify();
    }

    @Test
    void failsDraftEntryOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExpenditureJournalAdapter adapter = new HttpExpenditureJournalAdapter(builder.build());

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
                "EXP",
                "1",
                List.of(new JournalLineCommand("DEBIT", "11100", BigDecimal.valueOf(100), BigDecimal.valueOf(100), "D", "B", "desc"))
        );

        assertThatThrownBy(() -> adapter.createDraftEntry(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Journal ledger posting failed");
        server.verify();
    }
}
