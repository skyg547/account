package com.ho.account.masterdata.api.web;

import com.ho.account.basic.domain.Product;
import com.ho.account.masterdata.api.dto.ProductDto;
import com.ho.account.masterdata.api.dto.ProductRequestDto;
import com.ho.account.masterdata.core.application.usecase.ProductUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/basic/products")
public class ProductController {

    private final ProductUseCase productUseCase;

    public ProductController(ProductUseCase productUseCase) {
        this.productUseCase = productUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        try {
            Product createdProduct = productUseCase.createProduct(requestDto.toCommand());
            return ResponseEntity.ok(ProductDto.fromEntity(createdProduct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping
    public List<ProductDto> getActiveProducts() {
        return productUseCase.getAllActiveProducts().stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productUseCase.getProductById(id)
                .map(product -> ResponseEntity.ok(ProductDto.fromEntity(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto requestDto) {
        try {
            Product updatedProduct = productUseCase.updateProduct(id, requestDto.toCommand());
            return ResponseEntity.ok(ProductDto.fromEntity(updatedProduct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        try {
            productUseCase.deactivateProduct(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
