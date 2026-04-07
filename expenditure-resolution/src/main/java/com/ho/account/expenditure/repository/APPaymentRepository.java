package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.APPayment; // Import APPayment
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface APPaymentRepository extends JpaRepository<APPayment, Long> { // Updated interface name and generic type
}
