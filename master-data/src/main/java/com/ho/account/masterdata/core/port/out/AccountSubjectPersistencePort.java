package com.ho.account.masterdata.core.port.out;

import com.ho.account.basic.domain.AccountSubject;
import java.util.List;
import java.util.Optional;

/**
 * 계정과목 저장소로 나가는 출력 포트입니다.
 *
 * <p>헥사고날 아키텍처에서는 application service가 JPA Repository를 직접 알면 안 됩니다.
 * 서비스는 이 포트만 보고, 실제 DB 접근은 infrastructure adapter가 담당합니다.</p>
 */
public interface AccountSubjectPersistencePort {

    boolean existsByCode(String code);

    Optional<AccountSubject> findByCode(String code);

    List<AccountSubject> findAll();

    AccountSubject save(AccountSubject accountSubject);
}
