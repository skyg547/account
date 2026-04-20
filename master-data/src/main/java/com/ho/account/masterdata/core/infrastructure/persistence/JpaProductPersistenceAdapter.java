package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.basic.domain.Product;
import com.ho.account.basic.repository.ProductRepository;
import com.ho.account.masterdata.core.port.out.ProductPersistencePort;
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
        return productRepository.findByProductCode(productCode);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }
}
