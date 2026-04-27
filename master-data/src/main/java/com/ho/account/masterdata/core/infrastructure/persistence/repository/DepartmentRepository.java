package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    Optional<Department> findByCode(String code);

    // useYn 필드가 삭제되었으므로 유효 기간 기반으로 활성 부서 조회
    @Query("SELECT d FROM Department d WHERE d.validFrom <= CURRENT_DATE AND d.validTo >= CURRENT_DATE")
    List<Department> findByUseYnTrue();
}
