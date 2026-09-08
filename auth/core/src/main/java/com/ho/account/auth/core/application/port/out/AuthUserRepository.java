package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.Optional;

/**
 * 사용자 조회 및 검증을 위한 저장소 포트 인터페이스입니다.
 * 헥사고날 아키텍처의 AuthUserQueryPort 계약을 확장합니다.
 */
public interface AuthUserRepository extends AuthUserQueryPort {

    @Override
    Optional<AuthUser> findByUsername(String username);
}
