package com.ho.account.masterdata.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import org.springframework.stereotype.Component;

/**
 * ‚ÂƒìŒ?‚ï?payload????¼ì £ ë‰???‚Â??ë’ª?³Â??ë’ª??Œê»??¸ë•²??
 *
 * <p>???????»ë’— "?????????"??¼ì £ ë‰???row ‚Â? ??????’ê°‘??–ì—¯??ˆë–. ‚ÂƒìŒ?‚ï??°“??
 * ?¹ê¹­ ?” ????? ???·í???¹ê½¦/??ì ™/??¾ª??Šì†• ¹ì’–??? ²ê³—??ë‰????ë’ª?³Â??ë’ª›Â ?¾©???????¸ë•²??</p>
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
            default -> throw new IllegalArgumentException("?­… ??£ ?¸ìŠœ??Â?°ë¸¯Â ??…ë’— ë‰????ì‚??…ë•²?? "
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
                        .orElseThrow(() -> new IllegalArgumentException("„ê³•?’ï?? – ??????ë’¿??ˆë–. ?„ë¶¾? "
                                + request.getTargetKey()));
                businessPartnerUseCase.updateBusinessPartner(partner.getId(),
                        readPayload(request, BusinessPartnerCommand.class));
            }
            case DEACTIVATE -> {
                BusinessPartner partner = businessPartnerUseCase.getBusinessPartnerByCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("„ê³•?’ï?? – ??????ë’¿??ˆë–. ?„ë¶¾? "
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
                        .orElseThrow(() -> new IllegalArgumentException("?¹ë???– ??????ë’¿??ˆë–. ?„ë¶¾? "
                                + request.getTargetKey()));
                productUseCase.updateProduct(product.getId(), readPayload(request, ProductCommand.class));
            }
            case DEACTIVATE -> {
                Product product = productUseCase.getProductByProductCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("?¹ë???– ??????ë’¿??ˆë–. ?„ë¶¾? "
                                + request.getTargetKey()));
                productUseCase.deactivateProduct(product.getId());
            }
        }
    }

    private <T> T readPayload(MasterDataChangeRequest request, Class<T> payloadType) {
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("??¹ê½¦/??ì ™ ‚ÂƒìŒ?‚ï???payloadJson???Š‚??¸ë•²??");
        }
        try {
            return objectMapper.readValue(request.getPayloadJson(), payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payloadJson??" + payloadType.getSimpleName() + "?‚Â??‘ë¸· ????ë’¿??ˆë–.", e);
        }
    }
}

