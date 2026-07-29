package com.ho.account.auth.core.application.port.out;

/**
 * 로그인 실패 횟수와 잠금 상태를 관리하는 출력 포트입니다.
 *
 * <p>애플리케이션 서비스는 "잠금 정책을 적용한다"는 업무 흐름만 알고,
 * 실제 저장 방식과 감사 로그 기록 방식은 인프라 어댑터에 맡깁니다.</p>
 */
public interface LoginAttemptPort {

    boolean isLocked(String username);

    void recordFailure(String username, String reason);

    void recordSuccess(String username);
}
