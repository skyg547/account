package com.ho.account.asset.repository;

import com.ho.account.asset.domain.LeaseContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaseContractRepository extends JpaRepository<LeaseContract, Long> {
    List<LeaseContract> findByStatus(String status);
    List<LeaseContract> findByStatusAndEndDateBefore(String status, LocalDate date);
}
