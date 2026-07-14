package com.ho.account.masterdata.core.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        String payloadJson = request.getPayloadJson();
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException(
                    "Payload is required for " + request.getTargetType()
                            + " " + request.getChangeType() + " request " + request.getId());
        }
        try {
            return objectMapper.readValue(payloadJson, payloadType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Invalid " + request.getTargetType() + " change payload for request " + request.getId(),
                    exception);
        }
    }
}