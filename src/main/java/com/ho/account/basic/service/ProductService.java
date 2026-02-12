package com.ho.account.basic.service;

import com.ho.account.basic.domain.Product;
import com.ho.account.basic.dto.ProductRequestDto;
import com.ho.account.basic.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 새로운 상품을 생성합니다.
     *
     * @param requestDto 생성할 상품 정보가 담긴 DTO
     * @return 저장된 상품 엔티티
     */
    public Product createProduct(ProductRequestDto requestDto) {
        if (productRepository.existsByProductCode(requestDto.getProductCode())) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다: " + requestDto.getProductCode());
        }

        Product product = new Product();
        product.setProductCode(requestDto.getProductCode());
        product.setName(requestDto.getName());
        product.setDescription(requestDto.getDescription());
        product.setUnitOfMeasure(requestDto.getUnitOfMeasure());
        product.setPrice(requestDto.getPrice());
        product.setProductType(requestDto.getProductType());
        product.setValidFrom(requestDto.getValidFrom());
        product.setValidTo(requestDto.getValidTo());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser("system"); // TODO: 실제 사용자 정보로 대체

        // SCD2 원칙에 따라 초기 유효기간 설정 (requestDto에서 받아오므로 필요 없을 수 있음, 유효성 검증)
        if (product.getValidFrom() == null) {
            product.setValidFrom(LocalDate.now());
        }
        if (product.getValidTo() == null) {
            product.setValidTo(LocalDate.of(9999, 12, 31));
        }

        return productRepository.save(product);
    }

    /**
     * ID로 특정 상품을 조회합니다.
     *
     * @param id 조회할 상품 ID
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * 상품 코드로 특정 상품을 조회합니다.
     *
     * @param productCode 조회할 상품 코드
     * @return Optional<Product>
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductByProductCode(String productCode) {
        return productRepository.findByProductCode(productCode);
    }

    /**
     * 현재 시점(today)에 유효한 모든 상품을 조회합니다.
     *
     * @return 유효한 상품 리스트
     */
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        LocalDate today = LocalDate.now();
        return productRepository.findAll().stream()
                .filter(product -> !today.isBefore(product.getValidFrom()) && !today.isAfter(product.getValidTo()))
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
    public Product updateProduct(Long id, ProductRequestDto requestDto) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        // 상품 코드 변경 시 중복 확인
        if (!existingProduct.getProductCode().equals(requestDto.getProductCode()) && productRepository.existsByProductCode(requestDto.getProductCode())) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다: " + requestDto.getProductCode());
        }

        // SCD2 처리: validFrom 또는 validTo가 변경되면 새로운 버전을 생성하는 로직이 필요할 수 있으나,
        // 현재는 단순히 기존 엔티티를 업데이트하는 방식으로 진행.
        // 실제 SCD2 구현에서는 기존 엔티티의 validTo를 LocalDate.now()로 설정하고,
        // 새로운 엔티티를 requestDto의 validFrom, validTo로 생성하는 방식이 일반적.
        // 여기서는 유효기간 자체는 업데이트 가능하도록 구현.

        existingProduct.setProductCode(requestDto.getProductCode());
        existingProduct.setName(requestDto.getName());
        existingProduct.setDescription(requestDto.getDescription());
        existingProduct.setUnitOfMeasure(requestDto.getUnitOfMeasure());
        existingProduct.setPrice(requestDto.getPrice());
        existingProduct.setProductType(requestDto.getProductType());
        existingProduct.setValidFrom(requestDto.getValidFrom());
        existingProduct.setValidTo(requestDto.getValidTo());
        existingProduct.setUpdatedAt(LocalDateTime.now());
        existingProduct.setAuditUser("system"); // TODO: 실제 사용자 정보로 대체

        return productRepository.save(existingProduct);
    }

    /**
     * 특정 상품을 비활성화합니다. (논리적 삭제 - SCD2 유효 기간 종료)
     *
     * @param id 비활성화할 상품 ID
     */
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        // 오늘 날짜로 유효 종료일을 설정하여 상품을 비활성화 (SCD2)
        if (product.getValidTo().isAfter(LocalDate.now())) {
            product.setValidTo(LocalDate.now());
            product.setUpdatedAt(LocalDateTime.now());
            product.setAuditUser("system"); // TODO: 실제 사용자 정보로 대체
            productRepository.save(product);
        }
    }
}
