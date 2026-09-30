package com.ho.account.masterdata.core.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JSON payload를 Jackson으로 역직렬화하는 기술 어댑터입니다.
 */
@Component
@RequiredArgsConstructor
public class JacksonMasterDataChangePayloadDecoder implements MasterDataChangePayloadDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public <T> T decode(MasterDataChangeRequest request, Class<T> payloadType) {
        String payloadJson = requiredPayloadJson(request);
        try {
            return objectMapper.readValue(payloadJson, payloadType);
        } catch (JsonProcessingException exception) {
            throw invalidPayload(request, exception);
        }
    }

    @Override
    public boolean hasExplicitNullField(MasterDataChangeRequest request, String fieldName) {
        try {
            JsonNode payload = objectMapper.readTree(requiredPayloadJson(request));
            JsonNode field = payload == null ? null : payload.get(fieldName);
            return field != null && field.isNull();
        } catch (JsonProcessingException exception) {
            throw invalidPayload(request, exception);
        }
    }

    private String requiredPayloadJson(MasterDataChangeRequest request) {
        String payloadJson = request.getPayloadJson();
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException(
                    "Payload is required for " + request.getTargetType()
                            + " " + request.getChangeType() + " request " + request.getId());
        }
        return payloadJson;
    }

    private IllegalArgumentException invalidPayload(MasterDataChangeRequest request,
            JsonProcessingException exception) {
        return new IllegalArgumentException(
                "Invalid " + request.getTargetType() + " change payload for request " + request.getId(),
                exception);
    }
}
