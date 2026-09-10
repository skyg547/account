package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import java.time.LocalDate;
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
    public MasterDataType targetType() {
        return MasterDataType.PRODUCT;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            return;
        }
        ProductCommand command = command(request);
        // currentId 조회는 apply에만 둡니다. 부분 UPDATE와 CREATE의 price 기본값을 검증으로 덮지 않습니다.
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE) {
            if (command.name() == null) {
                throw new IllegalArgumentException("Product name is required.");
            }
            if (command.productType() == null) {
                throw new IllegalArgumentException("Product productType is required.");
            }
        }
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
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
        if (payload == null) {
            throw new IllegalArgumentException("Product change payload must not be null.");
        }
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
