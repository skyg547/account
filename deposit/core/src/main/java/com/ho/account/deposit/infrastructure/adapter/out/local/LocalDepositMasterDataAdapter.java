package com.ho.account.deposit.infrastructure.adapter.out.local;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local-only adapter for starting the deposit API/BATCH apps without master-data.
 *
 * <p>운영에서는 master-data 모듈의 실제 조회 어댑터를 연결해야 합니다. 이 어댑터는
 * `account.deposit.local-adapters.enabled=true`를 켠 로컬 학습 환경에서만 계정과목 존재
 * 검증을 통과시키기 위한 최소 구현입니다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "account.deposit.local-adapters", name = "enabled", havingValue = "true")
public class LocalDepositMasterDataAdapter implements MasterDataQueryPort {

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            return Optional.empty();
        }
        String trimmedCode = accountCode.trim();
        return Optional.of(new AccountSubjectRef(trimmedCode, "Local account " + trimmedCode, false, false));
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        return Optional.empty();
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String departmentCode) {
        return Optional.empty();
    }
}
