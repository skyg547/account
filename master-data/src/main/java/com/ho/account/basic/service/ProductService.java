package com.ho.account.masterdata.core.application.service;

import com.ho.account.basic.domain.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.usecase.ProductUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.ProductPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService implements ProductUseCase {

    private final ProductPersistencePort productPersistencePort;

    public ProductService(ProductPersistencePort productPersistencePort) {
        this.productPersistencePort = productPersistencePort;
    }

    /**
     * 새로운 상품을 생성합니다.
     *
     * @param requestDto 생성할 상품 정보가 담긴 DTO
     * @return 저장된 상품 엔티티
     */
    public Product createProduct(ProductCommand command) {
        if (productPersistencePort.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다: " + command.productCode());
        }

        Product product = command.toEntity();
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        MasterDataValidityPolicy.applyDefaultWindow(product::getValidFrom, product::setValidFrom,
                product::getValidTo, product::setValidTo);

        return productPersistencePort.save(product);
    }

    /**
     * ID로 특정 상품을 조회합니다.
     *
     * @param id 조회할 상품 ID
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productPersistencePort.findById(id);
    }

    /**
     * 상품 코드로 특정 상품을 조회합니다.
     *
     * @param productCode 조회할 상품 코드
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductByProductCode(String productCode) {
        return productPersistencePort.findByProductCode(productCode);
    }

    /**
     * 현재 시점(today)에 유효한 모든 상품을 조회합니다.
     *
     * @return 유효한 상품 리스트
     */
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(Product::getValidFrom, Product::getValidTo))
                .toList();
    }

    /**
     * 상품 정보를 수정합니다.
     * (SCD2 원칙에 따라 기존 상품을 비활성화하고 새로운 유효 기간으로 생성할 수도 있습니다. 여기서는 단순히 현재 유효 상품의 정보를 업데이트합니다.)
     *
     * @param id         수정할 상품 ID
     * @param requestDto 수정할 내용이 담긴 DTO
     * @return 수정된 상품 엔티티
     */
    public Product updateProduct(Long id, ProductCommand command) {
        Product existingProduct = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        if (!existingProduct.getProductCode().equals(command.productCode())
                && productPersistencePort.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다: " + command.productCode());
        }

        existingProduct.setProductCode(command.productCode());
        existingProduct.setName(command.name());
        existingProduct.setDescription(command.description());
        existingProduct.setUnitOfMeasure(command.unitOfMeasure());
        existingProduct.setPrice(command.price());
        existingProduct.setProductType(command.productType());
        existingProduct.setValidFrom(command.validFrom());
        existingProduct.setValidTo(command.validTo());
        existingProduct.setUpdatedAt(LocalDateTime.now());
        existingProduct.setAuditUser("system");

        return productPersistencePort.save(existingProduct);
    }

    /**
     * 특정 상품을 비활성화합니다. (논리적 삭제 - SCD2 유효 기간 종료)
     *
     * @param id 비활성화할 상품 ID
     */
    public void deactivateProduct(Long id) {
        Product product = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        MasterDataValidityPolicy.closeIfActive(product::getValidTo, product::setValidTo);
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        productPersistencePort.save(product);
    }
}
