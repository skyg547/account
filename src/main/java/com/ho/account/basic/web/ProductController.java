package com.ho.account.basic.web;

import com.ho.account.basic.domain.Product;
import com.ho.account.basic.dto.ProductDto;
import com.ho.account.basic.dto.ProductRequestDto;
import com.ho.account.basic.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        // TODO: Get auditUser from security context
        String auditUser = "system";
        Product product = productService.createProduct(requestDto, auditUser);
        return new ResponseEntity<>(new ProductDto(product), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto requestDto) {
        // TODO: Get auditUser from security context
        String auditUser = "system";
        Product updatedProduct = productService.updateProduct(id, requestDto, auditUser);
        return ResponseEntity.ok(new ProductDto(updatedProduct));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(new ProductDto(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts().stream()
                .map(ProductDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ProductDto> getValidProductByCode(@PathVariable String code) {
        return productService.getValidProductByCode(code)
                .map(product -> ResponseEntity.ok(new ProductDto(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        // TODO: Get auditUser from security context
        String auditUser = "system";
        productService.deleteProduct(id, auditUser);
        return ResponseEntity.noContent().build();
    }
}
