package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.*;
import com.ho.account.journalledger.application.service.journal.JournalCreationConflictException;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.infrastructure.adapter.JournalPostingAdapter;
import com.ho.account.loan.application.port.out.LoanJournalPort.*;
import com.ho.account.loan.support.DurableLoanJournalTestConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.*;
import org.springframework.web.client.RestClient;

/** JSON transport into the actual durable provider. Gateway authorization is outside this fixture. */
@SpringBootTest(classes = DurableLoanJournalTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.config.name=loan-durable-test", "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.mapping-resources=META-INF/journal-ledger-orm.xml", "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false", "spring.sql.init.mode=never", "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false", "spring.cloud.vault.enabled=false", "eureka.client.enabled=false"})
class LoanJournalCreationHttpIntegrationTest {
    @Autowired LoanJournalAdapter local;
    @Autowired JournalEntryService journals;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @BeforeEach void clear() {
        jdbc.update("DELETE FROM journal_creation_operations"); jdbc.update("DELETE FROM journal_details");
        jdbc.update("DELETE FROM journal_entries");
    }

    @Test void localAndHttpRetryShareDurableIdentityAndRejectChangedSourceOnlyContent() {
        var command = command("fee-1", "100", "2026-09-18");
        var original = local.post(command);
        var builder = RestClient.builder().baseUrl("http://synthetic-journal");
        var server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        server.expect(ExpectedCount.manyTimes(), requestTo("http://synthetic-journal/api/v1/journals/posting"))
                .andExpect(method(HttpMethod.POST)).andRespond(request -> {
                    var received = json.readValue(((MockClientHttpRequest) request).getBodyAsString(), JournalEntryCommand.class);
                    assertThat(received.operationKey()).isEqualTo(command.operationKey());
                    assertThat(received.sourceRequestFingerprint()).isNotNull();
                    try {
                        var result = new JournalPostingAdapter(journals).createDraftEntry(received);
                        return withSuccess(json.writeValueAsString(result), MediaType.APPLICATION_JSON).createResponse(request);
                    } catch (JournalCreationConflictException conflict) {
                        return withStatus(HttpStatus.CONFLICT).createResponse(request);
                    }
                });
        server.expect(ExpectedCount.manyTimes(), requestTo("http://synthetic-journal/api/journals/" + original.slipNo()))
                .andExpect(method(HttpMethod.GET)).andRespond(request -> withSuccess(
                        json.writeValueAsString(journalBody(original.journalEntryId())), MediaType.APPLICATION_JSON)
                        .createResponse(request));
        var remote = new HttpLoanJournalAdapter(builder.build());
        assertThat(remote.post(command)).isEqualTo(original);
        // A new client/provider mapping instance still consults the committed operation receipt.
        assertThat(new HttpLoanJournalAdapter(builder.build()).post(command)).isEqualTo(original);
        assertThatThrownBy(() -> remote.post(command("fee-1", "101", "2026-09-18")))
                .hasMessageContaining("HTTP 409");
        assertThatThrownBy(() -> remote.post(command("fee-1", "100", "2026-10-18")))
                .hasMessageContaining("HTTP 409");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
        server.verify();
    }

    @Test void explicitEventKeysPermitSeveralJournalsOnSameLoanAndDay() {
        var first = local.post(command("fee-1", "100", "2026-09-18"));
        var second = local.post(command("fee-2", "100", "2026-09-18"));
        assertThat(second.journalEntryId()).isNotEqualTo(first.journalEntryId());
        assertThat(local.post(command("fee-1", "100.0", "2026-09-18"))).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isEqualTo(2);
    }

    private LoanJournalCommand command(String key, String value, String end) {
        var amount = new BigDecimal(value);
        return new LoanJournalCommand(LocalDate.of(2026, 5, 18), "Synthetic loan deferred item", "loan-maker",
                "LOAN_DEFERRED_ITEM", "955", "KRW", "DEFERRED_ITEM_V1:" + key,
                SourceRequestFingerprint.calculate("LOAN_DEFERRED_REQUEST_V1", 955L, 1L, amount,
                        LocalDate.of(2026, 5, 18), LocalDate.parse(end)), List.of(
                        new LoanJournalLine("DEBIT", "171900", amount, "Deferred"),
                        new LoanJournalLine("CREDIT", "401900", amount, "Fee")));
    }

    private Map<String, Object> journalBody(Long id) {
        var entry = journals.getJournalEntryWithDetails(id).orElseThrow();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id); body.put("slipNo", entry.getSlipNo()); body.put("status", entry.getStatus());
        body.put("slipDate", entry.getSlipDate()); body.put("accountingDate", entry.getAccountingDate());
        body.put("description", entry.getDescription()); body.put("entryType", entry.getEntryType());
        body.put("currencyCode", entry.getCurrencyCode()); body.put("exchangeRate", entry.getExchangeRate());
        body.put("createdBy", entry.getCreatedBy()); body.put("auditUser", entry.getAuditUser());
        body.put("lineageSourceType", entry.getLineageSourceType()); body.put("lineageSourceId", entry.getLineageSourceId());
        body.put("lines", entry.getDetails().stream().map(line -> Map.of("side", line.getSide(),
                "accountCode", line.getAccountCode(), "amount", line.getAmount(), "baseAmount", line.getBaseAmount(),
                "description", line.getDetailDescription())).toList());
        return body;
    }
}
