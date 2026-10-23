package com.ho.account.masterdata.api.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodStatusUpdateRequestDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
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
                .addFilters(new MasterDataIngressAuthenticationFilter(
                        USER_SECRET, "auth-service", "account-api", SERVICE_SECRET))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void acceptsStatusUpdateWhenAuthorizedServiceIdentityProvided() throws Exception {
        FiscalPeriodRef updatedRef = new FiscalPeriodRef(
                1L, "2026", "03", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "CLOSED");

        when(fiscalPeriodControlPort.updateClosingStatus(eq(1L), eq("CLOSED"), eq("closing:ADMIN")))
                .thenReturn(updatedRef);

        FiscalPeriodStatusUpdateRequestDto requestDto = new FiscalPeriodStatusUpdateRequestDto("CLOSED", "ADMIN");

        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .header("X-Service-Identity", "closing")
                        .header("X-Service-Assertion", assertion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.closingStatus").value("CLOSED"));

        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "CLOSED", "closing:ADMIN");
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
