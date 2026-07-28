package com.ho.account.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MaskingSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void masksBusinessNumberAccountAndEmailWithoutChangingSeparators() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(new SensitivePayload()));

        assertThat(json.get("registrationNumber").asText()).isEqualTo("123-45-*****");
        assertThat(json.get("accountNumber").asText()).isEqualTo("110-***-**6789");
        assertThat(json.get("email").asText()).isEqualTo("te****@example.com");
    }

    @Test
    void failsClosedForShortOrUnknownPatterns() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(new FailClosedPayload()));

        assertThat(json.get("shortAccount").asText()).isEqualTo("**********");
        assertThat(json.get("unknown").asText()).isEqualTo("**********");
    }

    private static final class SensitivePayload {

        @Masked(pattern = "REG_NO")
        public String getRegistrationNumber() {
            return "123-45-67890";
        }

        @Masked(pattern = "ACCOUNT")
        public String getAccountNumber() {
            return "110-123-456789";
        }

        @Masked(pattern = "EMAIL")
        public String getEmail() {
            return "tester@example.com";
        }
    }

    private static final class FailClosedPayload {

        @Masked(pattern = "ACCOUNT")
        public String getShortAccount() {
            return "12345";
        }

        @Masked(pattern = "UNSUPPORTED")
        public String getUnknown() {
            return "must-not-leak";
        }
    }
}