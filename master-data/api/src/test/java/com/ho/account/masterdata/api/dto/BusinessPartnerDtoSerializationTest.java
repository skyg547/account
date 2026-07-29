package com.ho.account.masterdata.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 거래처 DTO가 사업자번호 원문을 객체 내부에 유지하되 외부 JSON에서는 마스킹하는지 검증합니다.
 *
 * <p>마스킹을 도메인이나 DB 값에 적용하면 중복 확인 같은 내부 업무가 깨지고, 반대로 Controller
 * 한 곳에서만 처리하면 다른 응답 경로에서 원문이 유출될 수 있습니다. 따라서 실제 Jackson
 * 직렬화를 실행해 DTO 경계의 보호 정책을 회귀 테스트로 고정합니다.</p>
 */
class BusinessPartnerDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void masksRegistrationNumberOnlyInJsonRepresentation() throws Exception {
        BusinessPartnerDto dto = new BusinessPartnerDto(
                41L,
                "BP-041",
                "새봄상사",
                "123-45-67890",
                "김새봄",
                "도매업",
                "원자재",
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                LocalDate.of(2026, 1, 1),
                BusinessPartner.OPEN_ENDED_VALID_TO);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        // 구분자는 읽기 쉽게 남기되 앞 다섯 자리 이후의 민감 숫자는 외부 응답에서 가립니다.
        assertThat(json.get("registrationNumber").asText()).isEqualTo("123-45-*****");
        assertThat(json.get("businessPartnerCode").asText()).isEqualTo("BP-041");
        assertThat(dto.getRegistrationNumber()).isEqualTo("123-45-67890");
    }
}
