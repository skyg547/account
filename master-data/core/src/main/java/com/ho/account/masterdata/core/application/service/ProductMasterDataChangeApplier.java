package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
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
    private final ProductPersistencePort productPersistencePort;

    @Override
    public MasterDataType targetType() {
        return MasterDataType.PRODUCT;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() != MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            validatedCommand(request);
        }
    }

    private ProductCommand validatedCommand(MasterDataChangeRequest request) {
        ProductCommand command = command(request);
        if (command.price() != null) {
            Product.requireValidPrice(command.price());
        }
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
        Product candidate;
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE) {
            candidate = command.toEntity();
        } else {
            Product current = productUseCase.getProductByProductCode(request.getTargetKey())
                    .orElseThrow(() -> new MasterDataVersionConflictException("Active product no longer exists."));
            candidate = new Product();
            candidate.setProductCode(current.getProductCode());
            candidate.setName(command.name() != null ? command.name() : current.getName());
            candidate.setDescription(command.description() != null ? command.description() : current.getDescription());
            candidate.setUnitOfMeasure(command.unitOfMeasure() != null ? command.unitOfMeasure() : current.getUnitOfMeasure());
            if (command.price() != null) {
                candidate.setPrice(command.price());
            } else if (current.getPrice() != null) {
                candidate.setPrice(current.getPrice());
            }
            candidate.setProductType(command.productType() != null ? command.productType() : current.getProductType());
        }
        candidate.validateBusinessFields();
        return command;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        validate(request);
        switch (request.getChangeType()) {
            case CREATE -> productUseCase.createProduct(validatedCommand(request));
            case UPDATE -> {
                ProductCommand command = validatedCommand(request);
                Product current = current(request);
                MasterDataChangeApplierSupport.requireCurrentUpdateWindow(
                        current.getValidTo(), command.validFrom(), command.validTo());
                productUseCase.updateProduct(
                        MasterDataChangeApplierSupport.requirePersistentId(current.getId(), request), command);
            }
            case DEACTIVATE -> productUseCase.deactivateProduct(
                    MasterDataChangeApplierSupport.requirePersistentId(current(request).getId(), request),
                    request.getEffectiveDate());
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

    private Product current(MasterDataChangeRequest request) {
        return productPersistencePort.findActiveByProductCodeForUpdate(request.getTargetKey())
                .orElseThrow(() -> new MasterDataVersionConflictException(
                        "Active product not found. Code: " + request.getTargetKey()));
    }
}
