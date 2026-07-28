package com.ho.account.shared.finance.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 과거 분산락 적용 예정 지점을 표시하기 위해 만든 호환 애노테이션입니다.
 *
 * <p><strong>중요:</strong> 현재 저장소에는 이 애노테이션을 처리하는 AOP/Redis 구현이 없으므로
 * 붙이는 것만으로 락이 획득되지 않습니다.</p>
 *
 * <p>@todo ECL 인바운드 어댑터에 명시적인 DistributedLockPort와 Redis/JDBC 구현을 추가하고,
 * owner token 기반 안전 해제, lease 갱신, 동일 기준일 멱등 키를 검증한 뒤 이 표식 애노테이션을 제거한다.</p>
 *
 * @deprecated 실제 락 구현이 없는 표식이므로 업무 정합성 보장에 사용하면 안 됩니다.
 */
@Deprecated(forRemoval = true)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String key();

    long waitTime() default 0L;

    long leaseTime() default 60L;
}