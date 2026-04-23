package com.ho.account.masterdata.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Product;
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
 * 蹂寃쎌슂泥?payload瑜??ㅼ젣 留덉뒪??蹂寃??좎뒪耳?댁뒪濡??곌껐?⑸땲??
 *
 * <p>???대옒?ㅻ뒗 "?뱀씤 ?대젰"怨?"?ㅼ젣 留덉뒪??row 蹂寃? ?ъ씠???묒갑?쒖엯?덈떎. 蹂寃쎌슂泥??꾨찓?몄?
 * ?곹깭 ?꾩씠留??뚭퀬, ??곷퀎 ?앹꽦/?섏젙/鍮꾪솢?깊솕 洹쒖튃? 湲곗〈 留덉뒪???좎뒪耳?댁뒪媛 怨꾩냽 ?대떦?⑸땲??</p>
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
            default -> throw new IllegalArgumentException("?꾩쭅 ?먮룞 ?곸슜??吏?먰븯吏 ?딅뒗 留덉뒪???좏삎?낅땲?? "
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
                        .orElseThrow(() -> new IllegalArgumentException("嫄곕옒泥섎? 李얠쓣 ???놁뒿?덈떎. 肄붾뱶: "
                                + request.getTargetKey()));
                businessPartnerUseCase.updateBusinessPartner(partner.getId(),
                        readPayload(request, BusinessPartnerCommand.class));
            }
            case DEACTIVATE -> {
                BusinessPartner partner = businessPartnerUseCase.getBusinessPartnerByCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("嫄곕옒泥섎? 李얠쓣 ???놁뒿?덈떎. 肄붾뱶: "
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
                        .orElseThrow(() -> new IllegalArgumentException("?곹뭹??李얠쓣 ???놁뒿?덈떎. 肄붾뱶: "
                                + request.getTargetKey()));
                productUseCase.updateProduct(product.getId(), readPayload(request, ProductCommand.class));
            }
            case DEACTIVATE -> {
                Product product = productUseCase.getProductByProductCode(request.getTargetKey())
                        .orElseThrow(() -> new IllegalArgumentException("?곹뭹??李얠쓣 ???놁뒿?덈떎. 肄붾뱶: "
                                + request.getTargetKey()));
                productUseCase.deactivateProduct(product.getId());
            }
        }
    }

    private <T> T readPayload(MasterDataChangeRequest request, Class<T> payloadType) {
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("?앹꽦/?섏젙 蹂寃쎌슂泥?뿉??payloadJson???꾩슂?⑸땲??");
        }
        try {
            return objectMapper.readValue(request.getPayloadJson(), payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payloadJson??" + payloadType.getSimpleName() + "濡?蹂?섑븷 ???놁뒿?덈떎.", e);
        }
    }
}
