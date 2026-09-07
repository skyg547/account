package com.ho.account.expenditure.payable.infrastructure.adapter;

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

class HttpPayableJournalAdapterTest {

    @Test
    void createsDraftEntrySuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpPayableJournalAdapter adapter = new HttpPayableJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/journals/posting"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"journalEntryId":201,"slipNo":"JE-AP-201","status":"DRAFT"}
                        """, MediaType.APPLICATION_JSON));

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                "AP Journal",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "PAYABLE_USER",
                "PAYABLE_USER",
                "PAYABLE",
                "2001",
                List.of(
                        new JournalLineCommand("DEBIT", "51000", BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), "D-1", "VEN-1", "Debit expense"),
                        new JournalLineCommand("CREDIT", "21000", BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), "D-1", "VEN-1", "Credit AP")
                )
        );

        JournalPostingResult result = adapter.createDraftEntry(command);

        assertThat(result.journalEntryId()).isEqualTo(201L);
        assertThat(result.slipNo()).isEqualTo("JE-AP-201");
        assertThat(result.status()).isEqualTo("DRAFT");
        server.verify();
    }

    @Test
    void approvesAndPostsJournalEntry() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpPayableJournalAdapter adapter = new HttpPayableJournalAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/journals/201/approve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "ap_officer"))
                .andRespond(withSuccess());

        server.expect(requestTo("http://journal-ledger.test/api/journals/201/post"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-ID", "ap_officer"))
                .andRespond(withSuccess());

        adapter.approveAndPost(201L, "ap_officer");
        server.verify();
    }

    @Test
    void failsDraftEntryOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpPayableJournalAdapter adapter = new HttpPayableJournalAdapter(builder.build());

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
                "PAYABLE",
                "1",
                List.of(new JournalLineCommand("DEBIT", "51000", BigDecimal.valueOf(100), BigDecimal.valueOf(100), "D", "B", "desc"))
        );

        assertThatThrownBy(() -> adapter.createDraftEntry(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Journal ledger posting failed");
        server.verify();
    }
}
