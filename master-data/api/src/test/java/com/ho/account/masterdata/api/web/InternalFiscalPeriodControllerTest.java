package com.ho.account.masterdata.api.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionNotFoundException;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodStatusUpdateRequestDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalFiscalPeriodControllerTest {

    private static final String USER_SECRET = "synthetic-user-signing-key-long-enough-1";
    private static final String SERVICE_SECRET = "synthetic-service-signing-key-long-enough-2";

    private MockMvc mockMvc;
    private FiscalPeriodControlPort fiscalPeriodControlPort;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fiscalPeriodControlPort = mock(FiscalPeriodControlPort.class);
        InternalFiscalPeriodController controller = new InternalFiscalPeriodController(fiscalPeriodControlPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MasterDataExceptionHandler())
                .addFilters(new MasterDataIngressAuthenticationFilter(
                        USER_SECRET, "auth-service", "account-api", SERVICE_SECRET))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void legacyDirectStatusUpdateCannotBypassOperationProtocol() throws Exception {
        FiscalPeriodStatusUpdateRequestDto requestDto = new FiscalPeriodStatusUpdateRequestDto("CLOSED", "ADMIN");

        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .header("X-Service-Identity", "closing")
                        .header("X-Service-Assertion", assertion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isGone());
        org.mockito.Mockito.verifyNoInteractions(fiscalPeriodControlPort);
    }

    @Test
    void signedPrepareApplyAndReceiptBindOneOperation() throws Exception {
        String operationId = "close-773";
        String preparePath = "/api/internal/fiscal-periods/1/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        FiscalPeriodTransitionCommand command = new FiscalPeriodTransitionCommand(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN");
        FiscalPeriodTransitionReceipt prepared = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.PREPARED);
        FiscalPeriodTransitionReceipt applied = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.APPLIED);
        when(fiscalPeriodControlPort.prepareClosingTransition(command)).thenReturn(prepared);
        when(fiscalPeriodControlPort.applyClosingTransition(operationId)).thenReturn(applied);
        when(fiscalPeriodControlPort.findTransitionReceipt(operationId)).thenReturn(Optional.of(applied));

        mockMvc.perform(put(preparePath)
                        .header("X-Service-Assertion", transitionAssertion("PUT", preparePath,
                                operationId, "closing-fiscal-transition"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("PREPARED"));
        verify(fiscalPeriodControlPort).prepareClosingTransition(command);

        mockMvc.perform(post(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("POST", receiptPath,
                                operationId, "closing-fiscal-transition")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("APPLIED"));
        mockMvc.perform(get(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("GET", receiptPath,
                                operationId, "closing-fiscal-transition")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.operationId").value(operationId));
    }

    @Test
    void signedConfirmFollowsAppliedReopenOperation() throws Exception {
        String operationId = "reopen-773";
        String preparePath = "/api/internal/fiscal-periods/1/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        String confirmPath = receiptPath + "/confirm";
        FiscalPeriodTransitionCommand command = new FiscalPeriodTransitionCommand(operationId, 1L,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, "ADMIN");
        FiscalPeriodTransitionReceipt prepared = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.PREPARED);
        FiscalPeriodTransitionReceipt applied = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.APPLIED);
        FiscalPeriodTransitionReceipt confirmed = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
        when(fiscalPeriodControlPort.prepareClosingTransition(command)).thenReturn(prepared);
        when(fiscalPeriodControlPort.applyClosingTransition(operationId)).thenReturn(applied);
        when(fiscalPeriodControlPort.confirmClosingTransition(operationId)).thenReturn(confirmed);

        mockMvc.perform(put(preparePath)
                        .header("X-Service-Assertion", transitionAssertion("PUT", preparePath,
                                operationId, "closing-fiscal-transition", "CLOSED", "OPEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"CLOSED\",\"targetStatus\":\"OPEN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("PREPARED"));
        mockMvc.perform(post(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("POST", receiptPath,
                                operationId, "closing-fiscal-transition", "CLOSED", "OPEN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("APPLIED"));
        mockMvc.perform(post(confirmPath)
                        .header("X-Service-Assertion", transitionAssertion("POST", confirmPath,
                                operationId, "closing-fiscal-transition", "CLOSED", "OPEN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("CONFIRMED"));
    }

    @Test
    void signedCancelFollowsPreparedOperation() throws Exception {
        String operationId = "cancel-773";
        String preparePath = "/api/internal/fiscal-periods/1/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        FiscalPeriodTransitionCommand command = new FiscalPeriodTransitionCommand(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN");
        FiscalPeriodTransitionReceipt prepared = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.PREPARED);
        FiscalPeriodTransitionReceipt cancelled = new FiscalPeriodTransitionReceipt(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN",
                FiscalPeriodTransitionReceipt.Phase.CANCELLED);
        when(fiscalPeriodControlPort.prepareClosingTransition(command)).thenReturn(prepared);
        when(fiscalPeriodControlPort.cancelClosingTransition(operationId, "ADMIN")).thenReturn(cancelled);

        mockMvc.perform(put(preparePath)
                        .header("X-Service-Assertion", transitionAssertion("PUT", preparePath,
                                operationId, "closing-fiscal-transition"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("PREPARED"));
        mockMvc.perform(delete(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("DELETE", receiptPath,
                                operationId, "closing-fiscal-transition")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("CANCELLED"));
    }

    @Test
    void transitionRejectsWrongPayloadOperationAndInsufficientRole() throws Exception {
        String operationId = "close-773";
        String preparePath = "/api/internal/fiscal-periods/1/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        mockMvc.perform(put(preparePath)
                        .header("X-Service-Assertion", transitionAssertion("PUT", preparePath,
                                operationId, "closing-fiscal-transition"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"CLOSED\",\"targetStatus\":\"OPEN\"}"))
                .andExpect(status().isUnauthorized());
        for (String method : List.of("PUT", "POST", "GET", "DELETE")) {
            String path = "PUT".equals(method) ? preparePath : receiptPath;
            var builder = "PUT".equals(method) ? put(path)
                    : "POST".equals(method) ? post(path)
                    : "DELETE".equals(method) ? delete(path) : get(path);
            mockMvc.perform(builder.header("X-Service-Assertion", transitionAssertion(method, path,
                            operationId, "viewer"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("GET", receiptPath,
                                "different-op", "closing-fiscal-transition")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(receiptPath + "/confirm")
                        .header("X-Service-Assertion", transitionAssertion("POST", receiptPath,
                                operationId, "closing-fiscal-transition")))
                .andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(fiscalPeriodControlPort);
    }

    @Test
    void authenticatedTransitionConflictAndMissingReceiptHaveStableHttpStatuses() throws Exception {
        String operationId = "close-conflict";
        String preparePath = "/api/internal/fiscal-periods/1/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        FiscalPeriodTransitionCommand command = new FiscalPeriodTransitionCommand(operationId, 1L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "ADMIN");
        when(fiscalPeriodControlPort.prepareClosingTransition(command))
                .thenThrow(new FiscalPeriodTransitionConflictException("different payload"));
        when(fiscalPeriodControlPort.applyClosingTransition(operationId))
                .thenThrow(new FiscalPeriodTransitionNotFoundException());

        mockMvc.perform(put(preparePath)
                        .header("X-Service-Assertion", transitionAssertion("PUT", preparePath,
                                operationId, "closing-fiscal-transition"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("POST", receiptPath,
                                operationId, "closing-fiscal-transition")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(receiptPath)
                        .header("X-Service-Assertion", transitionAssertion("GET", receiptPath,
                                operationId, "closing-fiscal-transition")))
                .andExpect(status().isNotFound());
    }

    private static String transitionAssertion(String method, String path, String operationId, String authority) {
        return transitionAssertion(method, path, operationId, authority, "OPEN", "CLOSED");
    }

    private static String transitionAssertion(String method, String path, String operationId, String authority,
                                              String expectedStatus, String targetStatus) {
        Instant now = Instant.now();
        return Jwts.builder().setSubject("closing")
                .claim("actor", "ADMIN").claim("serviceAuthority", authority)
                .claim("fiscalPeriodId", 1L).claim("operationId", operationId)
                .claim("expectedStatus", expectedStatus).claim("targetStatus", targetStatus)
                .claim("method", method).claim("path", path)
                .setIssuer("closing-service").setAudience("master-data-internal")
                .setIssuedAt(Date.from(now.minusSeconds(1))).setExpiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SERVICE_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void rejectsStatusUpdateWhenServiceIdentityMissingOrInvalid() throws Exception {
        FiscalPeriodStatusUpdateRequestDto requestDto = new FiscalPeriodStatusUpdateRequestDto("CLOSED", "ADMIN");

        // No signed assertion is an authentication failure.
        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .header("X-Service-Identity", "spoofed-user")
                        .header("X-Service-Assertion", assertion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }

    private static String assertion() {
        Instant now = Instant.now();
        return Jwts.builder().setSubject("closing")
                .claim("actor", "ADMIN")
                .claim("actorRoles", List.of("ROLE_ACCOUNTING_ADMIN"))
                .claim("fiscalPeriodId", 1L)
                .claim("closingStatus", "CLOSED")
                .claim("method", "PUT")
                .claim("path", "/api/internal/fiscal-periods/1/closing-status")
                .setIssuer("closing-service")
                .setAudience("master-data-internal")
                .setIssuedAt(Date.from(now.minusSeconds(1)))
                .setExpiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SERVICE_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
