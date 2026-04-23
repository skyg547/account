package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.Product;
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
     * ?덈줈???곹뭹???앹꽦?⑸땲??
     *
     * @param requestDto ?앹꽦???곹뭹 ?뺣낫媛 ?닿릿 DTO
     * @return ??λ맂 ?곹뭹 ?뷀떚??
     */
    public Product createProduct(ProductCommand command) {
        if (productPersistencePort.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("?대? 議댁옱?섎뒗 ?곹뭹 肄붾뱶?낅땲?? " + command.productCode());
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
     * ID濡??뱀젙 ?곹뭹??議고쉶?⑸땲??
     *
     * @param id 議고쉶???곹뭹 ID
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productPersistencePort.findById(id);
    }

    /**
     * ?곹뭹 肄붾뱶濡??뱀젙 ?곹뭹??議고쉶?⑸땲??
     *
     * @param productCode 議고쉶???곹뭹 肄붾뱶
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductByProductCode(String productCode) {
        return productPersistencePort.findByProductCode(productCode);
    }

    /**
     * ?꾩옱 ?쒖젏(today)???좏슚??紐⑤뱺 ?곹뭹??議고쉶?⑸땲??
     *
     * @return ?좏슚???곹뭹 由ъ뒪??
     */
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(Product::getValidFrom, Product::getValidTo))
                .toList();
    }

    /**
     * ?곹뭹 ?뺣낫瑜??섏젙?⑸땲??
     * (SCD2 ?먯튃???곕씪 湲곗〈 ?곹뭹??鍮꾪솢?깊솕?섍퀬 ?덈줈???좏슚 湲곌컙?쇰줈 ?앹꽦???섎룄 ?덉뒿?덈떎. ?ш린?쒕뒗 ?⑥닚???꾩옱 ?좏슚 ?곹뭹???뺣낫瑜??낅뜲?댄듃?⑸땲??)
     *
     * @param id         ?섏젙???곹뭹 ID
     * @param requestDto ?섏젙???댁슜???닿릿 DTO
     * @return ?섏젙???곹뭹 ?뷀떚??
     */
    public Product updateProduct(Long id, ProductCommand command) {
        Product existingProduct = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("?곹뭹??李얠쓣 ???놁뒿?덈떎. ID: " + id));

        if (!existingProduct.getProductCode().equals(command.productCode())
                && productPersistencePort.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("?대? 議댁옱?섎뒗 ?곹뭹 肄붾뱶?낅땲?? " + command.productCode());
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
     * ?뱀젙 ?곹뭹??鍮꾪솢?깊솕?⑸땲?? (?쇰━????젣 - SCD2 ?좏슚 湲곌컙 醫낅즺)
     *
     * @param id 鍮꾪솢?깊솕???곹뭹 ID
     */
    public void deactivateProduct(Long id) {
        Product product = productPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("?곹뭹??李얠쓣 ???놁뒿?덈떎. ID: " + id));

        MasterDataValidityPolicy.closeIfActive(product::getValidTo, product::setValidTo);
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system");
        productPersistencePort.save(product);
    }
}
