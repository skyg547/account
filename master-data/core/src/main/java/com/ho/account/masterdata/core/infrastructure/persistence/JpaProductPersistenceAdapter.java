package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.ProductEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.ProductMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaProductPersistenceAdapter implements ProductPersistencePort {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Product> findByIdForUpdate(Long id) {
        return productRepository.findById(id).map(this::toFreshDomain);
    }

    @Override
    public Optional<String> findBusinessKeyById(Long id) {
        return productRepository.findBusinessKeyById(id);
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
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Product> findActiveByProductCodeForUpdate(String productCode) {
        return productRepository.findActiveByProductCode(productCode, LocalDate.now())
                .map(this::toFreshDomain);
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
        // 기존 구간 종료를 먼저 flush해야 새 IDENTITY 행의 즉시 INSERT가 중복 기간으로 거부되지 않습니다.
        ProductEntity saved = entity.getId() == null
                ? productRepository.save(entity)
                : productRepository.saveAndFlush(entity);
        return productMapper.toDomain(saved);
    }

    private Product toFreshDomain(ProductEntity entity) {
        // 상위 트랜잭션이 미리 읽은 객체도 키 잠금 대기 후 DB의 최신 구간으로 다시 검증합니다.
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return productMapper.toDomain(entity);
    }
}
