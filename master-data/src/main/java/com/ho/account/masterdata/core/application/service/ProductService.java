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
     * ??àÏ§à???πÎ?????πÍΩ¶??∏Îï≤??
     *
     * @param requestDto ??πÍΩ¶???πÎ? ?Ç´õ¬ ??øÎ DTO
     * @return ?????πÎ? ????
     */
    public Product createProduct(ProductCommand command) {
        if (productPersistencePort.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("??? ∞ÎåÅ???éÎíó ?πÎ? ?ÑÎ∂æ??ÖÎï≤?? " + command.productCode());
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
     * ID?????πÎ???∞Í≥†???∏Îï≤??
     *
     * @param id ∞Í≥†????πÎ? ID
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productPersistencePort.findById(id);
    }

    /**
     * ?πÎ? ?ÑÎ∂æ∂Êø°?????πÎ???∞Í≥†???∏Îï≤??
     *
     * @param productCode ∞Í≥†????πÎ? ?ÑÎ∂æ?
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductByProductCode(String productCode) {
        return productPersistencePort.findByProductCode(productCode);
    }

    /**
     * ?ò± ??ñÏ†è(today)???èÏäö??è‚ë§??πÎ???∞Í≥†???∏Îï≤??
     *
     * @return ?èÏäö???πÎ? ?±—äÎí™??
     */
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(Product::getValidFrom, Product::getValidTo))
                .toList();
    }

    /**
     * ?πÎ? ?Ç´????èÏ†ô??∏Îï≤??
     * (SCD2 ??äÉ???ïÏî™ ≤Í≥ó???πÎ?????æ™??äÏÜï??çÌ???àÏ§à???èÏäö ≤Í≥å??∞Ï§à ??πÍΩ¶????éÎ£Ñ ??âÎíø??àÎñé. ????ïÎíó ??ãö???ò± ?èÏäö ?πÎ????Ç´????ÖÎú≤??ÑÎìÉ??∏Îï≤??)
     *
     * @param id         ??èÏ†ô???πÎ? ID
     * @param requestDto ??èÏ†ô????ÅÏäú????øÎ DTO
     * @return ??èÏ†ô???πÎ? ????
     */
    public Product updateProduct(Long id, ProductCommand command) {
        Product existingProduct = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("?πÎ???ñ†??????ÅÎíø??àÎñé. ID: " + id));

        if (!existingProduct.getProductCode().equals(command.productCode())
                && productPersistencePort.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("??? ∞ÎåÅ???éÎíó ?πÎ? ?ÑÎ∂æ??ÖÎï≤?? " + command.productCode());
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
     * ????πÎ?????æ™??äÏÜï??∏Îï≤?? (??∞‚îÅ??????- SCD2 ?èÏäö ≤Í≥å??ÇÖ?
     *
     * @param id ??æ™??äÏÜï???πÎ? ID
     */
    public void deactivateProduct(Long id) {
        Product product = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("?πÎ???ñ†??????ÅÎíø??àÎñé. ID: " + id));

        MasterDataValidityPolicy.closeIfActive(product::getValidTo, product::setValidTo);
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        productPersistencePort.save(product);
    }
}

