package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MasterDataDepartmentValidationAdapter implements DepartmentValidationPort {

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
            masterDataRestClient.get()
                    .uri("/api/basic/departments/{departmentCode}", departmentCode)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new IllegalStateException("master-data lookup failed for departmentCode=" + departmentCode, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("master-data lookup failed for departmentCode=" + departmentCode, ex);
        }
    }
}

