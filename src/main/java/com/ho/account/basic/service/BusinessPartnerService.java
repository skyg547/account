package com.ho.account.basic.service;

import com.ho.account.basic.domain.Customer;
import com.ho.account.basic.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // 거래처 생성
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsByCustomerCode(customer.getCustomerCode())) {
            throw new IllegalArgumentException("이미 존재하는 거래처 코드입니다: " + customer.getCustomerCode());
        }
        return customerRepository.save(customer);
    }

    // 전체 거래처 조회
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // 사용 중인 거래처만 조회
    @Transactional(readOnly = true)
    public List<Customer> getActiveCustomers() {
        return customerRepository.findByUseYnTrue();
    }

    // 거래처 상세 조회 (코드)
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByCode(String customerCode) {
        return customerRepository.findByCustomerCode(customerCode);
    }

    // 거래처 검색 (이름)
    @Transactional(readOnly = true)
    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.findByCustomerNameContaining(name);
    }

    // 거래처 정보 수정
    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        customer.setCustomerName(customerDetails.getCustomerName());
        customer.setRegistrationNumber(customerDetails.getRegistrationNumber());
        customer.setCeoName(customerDetails.getCeoName());
        customer.setBusinessType(customerDetails.getBusinessType());
        customer.setBusinessItem(customerDetails.getBusinessItem());
        customer.setUseYn(customerDetails.getUseYn());
        
        return customerRepository.save(customer);
    }

    // 거래처 삭제 (논리적 삭제)
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));
        customer.setUseYn(false);
        customerRepository.save(customer);
    }
}
