package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

/**
 * Product Domain POJO <-> ProductEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * Product 도메인 객체와 JPA 영속성 엔티티(ProductEntity) 간 양방향 데이터 매핑을 책임집니다.
 */
@Component
public class ProductMapper {

    public Product toDomain(ProductEntity entity) {
        if (entity == null) return null;
        Product domain = new Product();
        domain.setId(entity.getId());
        domain.setProductCode(entity.getProductCode());
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setUnitOfMeasure(entity.getUnitOfMeasure());
        domain.setPrice(entity.getPrice());
        domain.setProductType(entity.getProductType());
        domain.setValidFrom(entity.getValidFrom());
        domain.setValidTo(entity.getValidTo());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    public ProductEntity toEntity(Product domain) {
        if (domain == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setId(domain.getId());
        entity.setProductCode(domain.getProductCode());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setUnitOfMeasure(domain.getUnitOfMeasure());
        entity.setPrice(domain.getPrice());
        entity.setProductType(domain.getProductType());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidTo(domain.getValidTo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }
}
