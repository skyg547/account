package com.ho.account.auth.core.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.ho.account.auth.core.application.exception.DepartmentValidationUnavailableException;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "auth.master-data.enabled", havingValue = "true")
public class MasterDataDepartmentValidationAdapter implements DepartmentValidationPort {
    // Prove one complete object: default conversion can ignore trailing tokens and overwrite duplicate identities.
    private static final ObjectReader DEPARTMENT_RESPONSE_READER = new ObjectMapper().readerFor(Map.class)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private final RestClient masterDataRestClient;

    public MasterDataDepartmentValidationAdapter(RestClient masterDataRestClient) {
        this.masterDataRestClient = masterDataRestClient;
    }

    @Override
    public boolean existsDepartmentCode(String departmentCode) {
        if (departmentCode == null || departmentCode.isBlank()) {
            return false;
        }

        try {
            var response = masterDataRestClient.get()
                    .uri("/api/basic/departments/{departmentCode}", departmentCode)
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw unavailable();
            }
            Map<?, ?> body = DEPARTMENT_RESPONSE_READER.readValue(response.getBody());
            // A successful status alone proves nothing: require the requested identity without JSON type coercion.
            if (body != null && body.get("code") instanceof String code && code.equals(departmentCode)) {
                return true;
            }
            throw unavailable();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw unavailable();
        } catch (RestClientException | JsonProcessingException ex) {
            throw unavailable();
        }
    }

    private static DepartmentValidationUnavailableException unavailable() {
        // Keep remote details out of both the public response and any exception logging.
        return new DepartmentValidationUnavailableException();
    }
}
