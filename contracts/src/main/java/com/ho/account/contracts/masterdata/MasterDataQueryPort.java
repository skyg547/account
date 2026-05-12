package com.ho.account.contracts.masterdata;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 타 모듈에서 마스터 데이터를 조회하기 위한 인바운드 포트(Query Port).
 * 마스터 데이터 모듈의 도메인 엔티티를 직접 노출하지 않고, Ref(DTO) 형태로 제공하여 모듈 간 결합도를 낮춥니다.
 */
public interface MasterDataQueryPort {

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 계정과목을 조회합니다.
     */
    Optional<AccountSubjectRef> findAccountSubject(String accountCode);

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 거래처를 조회합니다.
     */
    Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode);

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 부서를 조회합니다.
     */
    Optional<DepartmentRef> findDepartment(String departmentCode);

    /**
     * 특정 일자(기준일)에 유효한 계정과목을 조회합니다. (SCD2 이력 지원용)
     */
    default Optional<AccountSubjectRef> findAccountSubjectAt(String accountCode, LocalDate effectiveDate) {
        return findAccountSubject(accountCode);
    }

    /**
     * 특정 일자(기준일)에 유효한 거래처를 조회합니다. (SCD2 이력 지원용)
     */
    default Optional<BusinessPartnerRef> findBusinessPartnerAt(String businessPartnerCode, LocalDate effectiveDate) {
        return findBusinessPartner(businessPartnerCode);
    }

    /**
     * 특정 일자(기준일)에 유효한 부서를 조회합니다. (SCD2 이력 지원용)
     */
    default Optional<DepartmentRef> findDepartmentAt(String departmentCode, LocalDate effectiveDate) {
        return findDepartment(departmentCode);
    }
}
