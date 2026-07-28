package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaProductPersistenceAdapter implements ProductPersistencePort {

    private final ProductRepository productRepository;

    public JpaProductPersistenceAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return productRepository.existsByProductCode(productCode);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Optional<Product> findByProductCode(String productCode) {
        return productRepository.findByProductCodeOrderByValidFromDesc(productCode).stream().findFirst();
    }

    @Override
    public Optional<Product> findActiveByProductCode(String productCode) {
        return productRepository.findActiveByProductCode(productCode, java.time.LocalDate.now());
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findAllActive(LocalDate asOfDate) {
        return productRepository.findActiveVersions(asOfDate);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }
}

