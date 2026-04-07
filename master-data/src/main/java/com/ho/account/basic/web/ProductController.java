package com.ho.account.basic.web;

import com.ho.account.basic.domain.Product;
import com.ho.account.basic.dto.ProductDto;
import com.ho.account.basic.dto.ProductRequestDto;
import com.ho.account.basic.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/basic/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        try {
            Product createdProduct = productService.createProduct(requestDto);
            return ResponseEntity.ok(ProductDto.fromEntity(createdProduct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or a custom error response DTO
        }
    }

    @GetMapping
    public List<ProductDto> getActiveProducts() {
        return productService.getAllActiveProducts().stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(ProductDto.fromEntity(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto requestDto) {
        try {
            Product updatedProduct = productService.updateProduct(id, requestDto);
            return ResponseEntity.ok(ProductDto.fromEntity(updatedProduct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or a custom error response DTO
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        try {
            productService.deactivateProduct(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
