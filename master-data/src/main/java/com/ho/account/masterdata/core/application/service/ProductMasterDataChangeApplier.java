package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 승인된 상품 변경 요청을 실제 SCD2 유즈케이스에 연결합니다.
 */
@Service
@RequiredArgsConstructor
public class ProductMasterDataChangeApplier implements MasterDataChangeApplier {

    private final ProductUseCase productUseCase;
    private final MasterDataChangePayloadDecoder payloadDecoder;

    @Override
    public boolean supports(MasterDataType targetType) {
        return targetType == MasterDataType.PRODUCT;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> productUseCase.createProduct(command(request));
            case UPDATE -> productUseCase.updateProduct(currentId(request), command(request));
            case DEACTIVATE -> productUseCase.deactivateProduct(currentId(request), request.getEffectiveDate());
        }
    }

    private ProductCommand command(MasterDataChangeRequest request) {
        ProductCommand payload = MasterDataChangeApplierSupport.decode(
                request, payloadDecoder, ProductCommand.class);
        String code = MasterDataChangeApplierSupport.targetKey(request, payload.productCode());
        return new ProductCommand(
                code,
                payload.name(),
                payload.description(),
                payload.unitOfMeasure(),
                payload.price(),
                payload.productType(),
                request.getEffectiveDate(),
                payload.validTo());
    }

    private Long currentId(MasterDataChangeRequest request) {
        Product current = productUseCase.getProductByProductCode(request.getTargetKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active product not found. Code: " + request.getTargetKey()));
        return MasterDataChangeApplierSupport.requirePersistentId(current.getId(), request);
    }
}