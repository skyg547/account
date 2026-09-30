package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService implements ProductUseCase {

    private final ProductPersistencePort productPersistencePort;
    private final MasterDataBusinessKeyLockPort businessKeyLockPort;

    public ProductService(ProductPersistencePort productPersistencePort,
            MasterDataBusinessKeyLockPort businessKeyLockPort) {
        this.productPersistencePort = productPersistencePort;
        this.businessKeyLockPort = businessKeyLockPort;
    }

    /**
     * 새로운 상품을 생성합니다.
     */
    public Product createProduct(ProductCommand command) {
        businessKeyLockPort.lock(MasterDataType.PRODUCT, command.productCode());
        if (productPersistencePort.existsByProductCode(command.productCode())) {
            throw new MasterDataVersionConflictException("이미 이력이 존재하는 상품 코드입니다: " + command.productCode());
        }
        Product product = command.toEntity();
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        
        MasterDataValidityPolicy.applyDefaultWindow(product::getValidFrom, product::setValidFrom,
                product::getValidTo, product::setValidTo);

        return productPersistencePort.save(product);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productPersistencePort.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductByProductCode(String productCode) {
        return productPersistencePort.findActiveByProductCode(productCode);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productPersistencePort.findAllActive(LocalDate.now());
    }

    /**
     * 상품 정보를 수정합니다. (SCD2 정책에 따라 기존 활성 이력을 종료하고 신규 버전을 생성합니다.)
     */
    public Product updateProduct(Long id, ProductCommand command) {
        lockBusinessKeyById(id);
        Product currentActive = productPersistencePort.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
        if (!MasterDataValidityPolicy.isActiveAt(LocalDate.now(), currentActive.getValidFrom(), currentActive.getValidTo())) {
            throw new IllegalArgumentException("활성 상품 버전만 수정할 수 있습니다. ID: " + id);
        }
        if (command.productCode() != null && !command.productCode().equals(currentActive.getProductCode())) {
            throw new IllegalArgumentException("상품 코드는 SCD2 버전 수정에서 변경할 수 없습니다: " + currentActive.getProductCode());
        }

        // SCD2: 기존 활성 버전 종료 (새로운 버전 시작일의 전날로 종료)
        LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : LocalDate.now();
        LocalDate newValidTo = command.validTo() != null
                ? command.validTo()
                : LocalDate.of(9999, 12, 31);
        MasterDataValidityPolicy.requireVersionSplit(
                currentActive.getValidFrom(), currentActive.getValidTo(), newValidFrom, newValidTo);
        LocalDate oldValidTo = newValidFrom.minusDays(1);

        currentActive.terminate(oldValidTo);
        productPersistencePort.save(currentActive);

        // SCD2: 새로운 버전 생성
        Product newVersion = new Product();
        newVersion.setProductCode(currentActive.getProductCode());
        newVersion.setName(command.name() != null ? command.name() : currentActive.getName());
        newVersion.setDescription(command.description() != null ? command.description() : currentActive.getDescription());
        newVersion.setUnitOfMeasure(command.unitOfMeasure() != null ? command.unitOfMeasure() : currentActive.getUnitOfMeasure());
        newVersion.setPrice(command.price() != null ? command.price() : currentActive.getPrice());
        newVersion.setProductType(command.productType() != null ? command.productType() : currentActive.getProductType());
        newVersion.setValidFrom(newValidFrom);
        newVersion.setValidTo(newValidTo);
        newVersion.setAuditUser("system");

        return productPersistencePort.save(newVersion);
    }

    /**
     * 상품을 비활성화(종료) 처리합니다.
     */
    public void deactivateProduct(Long id) {
        deactivateProduct(id, LocalDate.now());
    }

    @Override
    public void deactivateProduct(Long id, LocalDate effectiveDate) {
        lockBusinessKeyById(id);
        Product product = productPersistencePort.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        LocalDate terminationDate = MasterDataValidityPolicy.requireNewTerminationDate(
                effectiveDate, product.getValidFrom(), product.getValidTo());
        product.terminate(terminationDate);
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        productPersistencePort.save(product);
    }

    private void lockBusinessKeyById(Long id) {
        // 변경 가능한 유효기간은 잠금 후에만 읽고, 잠금 전에는 불변 업무 키만 조회합니다.
        String businessKey = productPersistencePort.findBusinessKeyById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
        businessKeyLockPort.lock(MasterDataType.PRODUCT, businessKey);
    }
}
