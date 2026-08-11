package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.ProductEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.ProductMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaProductPersistenceAdapter implements ProductPersistencePort {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public JpaProductPersistenceAdapter(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return productRepository.existsByProductCode(productCode);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id).map(productMapper::toDomain);
    }

    @Override
    public Optional<Product> findByProductCode(String productCode) {
        return productRepository.findByProductCodeOrderByValidFromDesc(productCode).stream()
                .findFirst()
                .map(productMapper::toDomain);
    }

    @Override
    public Optional<Product> findActiveByProductCode(String productCode) {
        return productRepository.findActiveByProductCode(productCode, java.time.LocalDate.now())
                .map(productMapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findAllActive(LocalDate asOfDate) {
        return productRepository.findActiveVersions(asOfDate).stream()
                .map(productMapper::toDomain)
                .toList();
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = productMapper.toEntity(product);
        ProductEntity saved = productRepository.save(entity);
        return productMapper.toDomain(saved);
    }
}


