package com.ho.account.journalledger.adapter.in.web.journal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.service.journal.JournalCreationConflictException;
import com.ho.account.journalledger.application.service.journal.JournalCreationFingerprint;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalCreationIdempotencyMvcTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JournalUseCase useCase = org.mockito.Mockito.mock(JournalUseCase.class);
        Map<JournalCreationIdentity, StoredRequest> manualRequests = new HashMap<>();
        Map<JournalCreationIdentity, StoredRequest> eventRequests = new HashMap<>();
        AtomicLong sequence = new AtomicLong(7650L);
        when(useCase.createJournalEntry(any(JournalCreationIdentity.class), any(JournalEntry.class)))
                .thenAnswer(invocation -> {
                    JournalCreationIdentity identity = invocation.getArgument(0);
                    JournalEntry request = invocation.getArgument(1);
                    String fingerprint = JournalCreationFingerprint.calculate(request);
                    StoredRequest existing = manualRequests.get(identity);
                    if (existing != null) {
                        if (!existing.fingerprint().equals(fingerprint)) {
                            throw new JournalCreationConflictException();
                        }
                        return existing.entry();
                    }
                    request.setId(sequence.incrementAndGet());
                    request.setSlipNo("JE-HTTP-" + request.getId());
                    request.initializeDraft();
                    manualRequests.put(identity, new StoredRequest(fingerprint, request));
                    return request;
                });
        when(useCase.createJournalEntryFromEvent(
                any(JournalCreationIdentity.class), any(Map.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    JournalCreationIdentity identity = invocation.getArgument(0);
                    Map<String, Object> event = invocation.getArgument(1);
                    LocalDate date = invocation.getArgument(2);
                    String fingerprint = canonicalEvent(event, date);
                    StoredRequest existing = eventRequests.get(identity);
                    if (existing != null) {
                        if (!existing.fingerprint().equals(fingerprint)) {
                            throw new JournalCreationConflictException();
                        }
                        return Optional.of(existing.entry());
                    }
                    JournalEntry entry = new JournalEntry();
                    entry.setId(sequence.incrementAndGet());
                    entry.setSlipNo("JE-EVENT-" + entry.getId());
                    entry.setSlipDate(date);
                    entry.setAccountingDate(date);
                    entry.setDescription("event journal");
                    entry.setEntryType("NORMAL");
                    entry.setCurrencyCode("KRW");
                    entry.setCreatedBy(event.get("createdBy").toString());
                    entry.setAuditUser(event.get("auditUser").toString());
                    entry.setLineageSourceType(identity.sourceType());
                    entry.setLineageSourceId(identity.sourceId());
                    entry.initializeDraft();
                    eventRequests.put(identity, new StoredRequest(fingerprint, entry));
                    return Optional.of(entry);
                });

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mvc = MockMvcBuilders.standaloneSetup(new JournalController(useCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void manualEquivalentRetryReturnsSameIdAndChangedAmountReturns409() throws Exception {
        mvc.perform(manualRequest("MANUAL-765", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7651L));
        mvc.perform(manualRequest("MANUAL-765", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7651L));
        mvc.perform(manualRequest("MANUAL-765", "101.00"))
                .andExpect(status().isConflict());
    }

    @Test
    void eventEquivalentReplayReturnsSameIdAndChangedAmountReturns409() throws Exception {
        mvc.perform(eventRequest("EVENT-765", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7651L));
        mvc.perform(eventRequest("EVENT-765", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7651L));
        mvc.perform(eventRequest("EVENT-765", "101.00"))
                .andExpect(status().isConflict());
    }

    @Test
    void manualCreationRequiresAnExplicitIdempotencyHeader() throws Exception {
        mvc.perform(post("/api/journals")
                        .header("X-Auth-User", "manual-maker")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody("100.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eventCreationRequiresAnExplicitOperationKey() throws Exception {
        mvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", "2026-09-25")
                        .header("X-Auth-User", "event-maker")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventData":{"eventType":"PAYABLE","eventId":"INV-765","amount":100.00}}
                                """))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder manualRequest(
            String operationKey, String amount) {
        return post("/api/journals")
                .header("X-Auth-User", "manual-maker")
                .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                .header("Idempotency-Key", operationKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualBody(amount));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder eventRequest(
            String operationKey, String amount) {
        return post("/api/journals/from-event")
                .queryParam("accountingDate", "2026-09-25")
                .header("X-Auth-User", "event-maker")
                .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                .header("Idempotency-Key", operationKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"eventData":{"eventType":"PAYABLE","eventId":"INV-765","amount":%s}}
                        """.formatted(amount));
    }

    private static String manualBody(String amount) {
        return """
                {
                  "slipDate":"2026-09-25",
                  "accountingDate":"2026-09-25",
                  "description":"manual retry",
                  "entryType":"NORMAL",
                  "currencyCode":"KRW",
                  "lineageSourceType":"MANUAL_TEST",
                  "lineageSourceId":"SOURCE-765",
                  "lines":[
                    {"side":"DEBIT","accountCode":"10100","amount":%s,"baseAmount":%s},
                    {"side":"CREDIT","accountCode":"40100","amount":%s,"baseAmount":%s}
                  ]
                }
                """.formatted(amount, amount, amount, amount);
    }

    private static String canonicalEvent(Map<String, Object> event, LocalDate date) {
        BigDecimal amount = new BigDecimal(event.get("amount").toString()).stripTrailingZeros();
        return date + "|" + amount.toPlainString() + "|" + event.get("createdBy")
                + "|" + event.get("lineageSourceType") + "|" + event.get("lineageSourceId");
    }

    private record StoredRequest(String fingerprint, JournalEntry entry) { }
}
