package com.ho.account.masterdata.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Product;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.usecase.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.usecase.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.usecase.DepartmentUseCase;
import com.ho.account.masterdata.core.application.usecase.ProductUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import org.springframework.stereotype.Component;

/**
 * 변경요청 payload를 실제 마스터 변경 유스케이스로 연결합니다.
 *
 * <p>이 클래스는 "승인 이력"과 "실제 마스터 row 변경" 사이의 접착제입니다. 변경요청 도메인은
 * 상태 전이만 알고, 대상별 생성/수정/비활성화 규칙은 기존 마스터 유스케이스가 계속 담당합니다.</p>
 */
@Component
public class DefaultMasterDataChangeApplier implements MasterDataChangeApplier {

    private final ObjectMapper objectMapper;
    private final AccountSubjectUseCase accountSubjectUseCase;
    private final BusinessPartnerUseCase businessPartnerUseCase;
    private final DepartmentUseCase departmentUseCase;
    private final ProductUseCase productUseCase;

    public DefaultMasterDataChangeApplier(ObjectMapper objectMapper, AccountSubjectUseCase accountSubjectUseCase,
            BusinessPartnerUseCase businessPartnerUseCase, DepartmentUseCase departmentUseCase,
            ProductUseCase productUseCase) {
        this.objectMapper = objectMapper;
        this.accountSubjectUseCase = accountSubjectUseCase;
        this.businessPartnerUseCase = businessPartnerUseCase;
        this.departmentUseCase = departmentUseCase;
        this.productUseCase = productUseCase;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getTargetType()) {
            case ACCOUNT_SUBJECT -> applyAccountSubject(request);
            case BUSINESS_PARTNER -> applyBusinessPartner(request);
            case DEPARTMENT -> applyDepartment(request);
            case PRODUCT -> applyProduct(request);
            default -> throw new IllegalArgumentException("아직 자동 적용을 지원하지 않는 마스터 유형입니다: "
                    + request.getTargetType());
        }
    }

    private void applyAccountSubject(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> accountSubjectUseCase.createAccountSubject(readPayload(request, AccountSubjectCommand.class));
            case UPDATE -> accountSubjectUseCase.updateAccountSubject(request.getTargetKey(),
                    readPayload(request, AccountSubjectCommand.class));
            case DEACTIVATE -> accountSubjectUseCase.deactivateAccountSubject(request.getTargetKey());
        }
    }

    private void applyBusinessPartner(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> businessPartnerUseCase.createBusinessPartner(readPayload(request, BusinessPartnerCommand.class));
            case UPDATE -> {
                BusinessPartner partner = businessPartnerUseCase.getBusinessPartnerByCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. 코드: "
                                + request.getTargetKey()));
                businessPartnerUseCase.updateBusinessPartner(partner.getId(),
                        readPayload(request, BusinessPartnerCommand.class));
            }
            case DEACTIVATE -> {
                BusinessPartner partner = businessPartnerUseCase.getBusinessPartnerByCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. 코드: "
                                + request.getTargetKey()));
                businessPartnerUseCase.deleteBusinessPartner(partner.getId());
            }
        }
    }

    private void applyDepartment(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> departmentUseCase.createDepartment(readPayload(request, DepartmentCommand.class));
            case UPDATE -> departmentUseCase.updateDepartment(request.getTargetKey(),
                    readPayload(request, DepartmentCommand.class));
            case DEACTIVATE -> departmentUseCase.deactivateDepartment(request.getTargetKey());
        }
    }

    private void applyProduct(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> productUseCase.createProduct(readPayload(request, ProductCommand.class));
            case UPDATE -> {
                Product product = productUseCase.getProductByProductCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. 코드: "
                                + request.getTargetKey()));
                productUseCase.updateProduct(product.getId(), readPayload(request, ProductCommand.class));
            }
            case DEACTIVATE -> {
                Product product = productUseCase.getProductByProductCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. 코드: "
                                + request.getTargetKey()));
                productUseCase.deactivateProduct(product.getId());
            }
        }
    }

    private <T> T readPayload(MasterDataChangeRequest request, Class<T> payloadType) {
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("생성/수정 변경요청에는 payloadJson이 필요합니다.");
        }
        try {
            return objectMapper.readValue(request.getPayloadJson(), payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payloadJson을 " + payloadType.getSimpleName() + "로 변환할 수 없습니다.", e);
        }
    }
}
