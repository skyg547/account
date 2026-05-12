package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
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
     */
    public Product createProduct(ProductCommand command) {
        // SCD2에서는 동일 코드가 존재할 수 있으나, 보통 신규 생성 시 중복 체크 로직은 정책에 따라 다름.
        // 여기서는 단순함을 유지.
        Product product = command.toEntity();
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        
        if (product.getValidFrom() == null || product.getValidTo() == null) {
            MasterDataValidityPolicy.applyDefaultWindow(product::getValidFrom, product::setValidFrom,
                    product::getValidTo, product::setValidTo);
        }

        return productPersistencePort.save(product);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productPersistencePort.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductByProductCode(String productCode) {
        return productPersistencePort.findByProductCode(productCode);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(Product::getValidFrom, Product::getValidTo))
                .toList();
    }

    /**
     * 상품 정보를 수정합니다. (SCD2 정책에 따라 기존 활성 이력을 종료하고 신규 버전을 생성합니다.)
     */
    public Product updateProduct(Long id, ProductCommand command) {
        Product currentActive = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        // SCD2: 기존 활성 버전 종료 (새로운 버전 시작일의 전날로 종료)
        java.time.LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : java.time.LocalDate.now();
        java.time.LocalDate oldValidTo = newValidFrom.minusDays(1);

        if (oldValidTo.isBefore(currentActive.getValidFrom())) {
            throw new IllegalArgumentException("새로운 유효 시작일이 기존 시작일보다 빠를 수 없습니다.");
        }

        currentActive.terminate(oldValidTo);
        productPersistencePort.save(currentActive);

        // SCD2: 새로운 버전 생성
        Product newVersion = new Product();
        newVersion.setProductCode(command.productCode() != null ? command.productCode() : currentActive.getProductCode());
        newVersion.setName(command.name());
        newVersion.setDescription(command.description());
        newVersion.setUnitOfMeasure(command.unitOfMeasure());
        newVersion.setPrice(command.price());
        newVersion.setProductType(command.productType());
        newVersion.setValidFrom(newValidFrom);
        newVersion.setValidTo(command.validTo() != null ? command.validTo() : java.time.LocalDate.of(9999, 12, 31));
        newVersion.setAuditUser("system");

        return productPersistencePort.save(newVersion);
    }

    /**
     * 상품을 비활성화(종료) 처리합니다.
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
