package com.ho.account.basic.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.service.BusinessPartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic/businesspartners")
public class BusinessPartnerController {

    private final BusinessPartnerService businessPartnerService;

    @Autowired
    public BusinessPartnerController(BusinessPartnerService businessPartnerService) {
        this.businessPartnerService = businessPartnerService;
    }

    // 거래처 생성
    @PostMapping
    public ResponseEntity<BusinessPartner> createBusinessPartner(@RequestBody BusinessPartner businessPartner) {
        try {
            BusinessPartner createdBusinessPartner = businessPartnerService.createBusinessPartner(businessPartner);
            return ResponseEntity.ok(createdBusinessPartner);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 전체 거래처 조회
    @GetMapping
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerService.getAllBusinessPartners();
    }

    // 사용 중인 거래처만 조회
    @GetMapping("/active")
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerService.getActiveBusinessPartners();
    }

    // 거래처 상세 조회 (코드)
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartner> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerService.getBusinessPartnerByCode(businessPartnerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 거래처 검색 (이름)
    @GetMapping("/search")
    public List<Customer> searchCustomers(@RequestParam String name) {
        return customerService.searchCustomersByName(name);
    }

    // 거래처 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customerDetails) {
        try {
            Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
            return ResponseEntity.ok(updatedCustomer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 거래처 삭제 (논리적 삭제)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
