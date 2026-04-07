package com.ho.account.report.repository;

import com.ho.account.report.domain.DisclosureMart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisclosureMartRepository extends JpaRepository<DisclosureMart, Long> {
    List<DisclosureMart> findByMartTypeAndBaseDate(String martType, LocalDate baseDate);
}
