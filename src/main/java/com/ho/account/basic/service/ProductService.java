package com.ho.account.basic.service;

import com.ho.account.basic.domain.Product;
import com.ho.account.basic.repository.ProductRepository;
import com.ho.account.basic.dto.ProductRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(ProductRequestDto requestDto, String auditUser) {
        // Check for existing product with the same code that is currently valid
        Optional<Product> existingValidProduct = productRepository.findByCode(requestDto.getCode())
                .filter(p -> p.getValidTo().isAfter(LocalDate.now()));

        if (existingValidProduct.isPresent()) {
            throw new IllegalArgumentException("Product with code " + requestDto.getCode() + " already exists and is currently valid.");
        }

        Product product = new Product();
        product.setCode(requestDto.getCode());
        product.setName(requestDto.getName());
        product.setDescription(requestDto.getDescription());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setAuditUser(auditUser);
        product.setValidFrom(requestDto.getValidFrom() != null ? requestDto.getValidFrom() : LocalDate.now());
        product.setValidTo(requestDto.getValidTo() != null ? requestDto.getValidTo() : LocalDate.MAX);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequestDto requestDto, String auditUser) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        // Inactivate the old record (SCD2)
        existingProduct.setValidTo(LocalDate.now().minusDays(1));
        existingProduct.setUpdatedAt(LocalDateTime.now());
        existingProduct.setAuditUser(auditUser);
        productRepository.save(existingProduct); // Save the inactivated old record

        // Create a new record with updated details
        Product newProduct = new Product();
        newProduct.setCode(existingProduct.getCode()); // Code remains the same for the same logical product
        newProduct.setName(requestDto.getName());
        newProduct.setDescription(requestDto.getDescription());
        newProduct.setCreatedAt(LocalDateTime.now()); // New record, so new creation timestamp
        newProduct.setUpdatedAt(LocalDateTime.now());
        newProduct.setAuditUser(auditUser);
        newProduct.setValidFrom(LocalDate.now()); // New record starts validity from today
        newProduct.setValidTo(requestDto.getValidTo() != null ? requestDto.getValidTo() : LocalDate.MAX);

        return productRepository.save(newProduct);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getValidProductByCode(String code) {
        return productRepository.findByCode(code)
                .filter(product -> product.getValidFrom().isBefore(LocalDate.now().plusDays(1)) &&
                        product.getValidTo().isAfter(LocalDate.now().minusDays(1)));
    }

    // You might also want methods to get all historical versions of a product by code
    @Transactional(readOnly = true)
    public List<Product> getAllProductVersionsByCode(String code) {
        return productRepository.findByCode(code)
                .stream()
                .toList(); // Assuming findByCode can return multiple versions if not filtered by validTo
    }

    @Transactional
    public void deleteProduct(Long id, String auditUser) {
        Product productToDelete = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        // For SCD2, "delete" means setting validTo to yesterday
        if (productToDelete.getValidTo().isAfter(LocalDate.now().minusDays(1))) { // Only if it's currently valid or future dated
            productToDelete.setValidTo(LocalDate.now().minusDays(1));
            productToDelete.setUpdatedAt(LocalDateTime.now());
            productToDelete.setAuditUser(auditUser);
            productRepository.save(productToDelete);
        } else {
            // If it's already expired, a hard delete might be considered depending on policy,
            // but for SCD2, we generally keep historical records.
            // For now, we'll just ensure it's marked as invalid.
        }
    }
}
