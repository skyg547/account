package com.ho.account.shared.infrastructure;

import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * 운영 환경에서 프로파일 지정을 빠뜨려 local 설정으로 기동하는 것을 막는다.
 *
 * <p>모듈 대부분은 {@code spring.profiles.default: local}을 두어 개발자가
 * {@code --spring.profiles.active=local} 없이도 바로 실행할 수 있게 한다. 편의를 위한
 * 선택이지만, 운영 배포에서 {@code SPRING_PROFILES_ACTIVE}를 빠뜨리면 같은 기본값이
 * 발동해 인메모리 H2와 {@code ddl-auto: create-drop}으로 <strong>오류 없이</strong>
 * 기동한다. 서버는 올라오고 헬스체크도 통과하지만 실제 데이터베이스에는 연결되지
 * 않으며, 기록한 데이터는 재시작과 함께 사라진다.</p>
 *
 * <p>이 가드는 그 조합을 기동 실패로 바꾼다. local 프로파일이 활성인 상태에서 운영
 * 전용 환경 변수가 함께 발견되면 예외를 던진다. 두 조건이 동시에 성립하는 정상적인
 * 상황은 없다 — 개발자 PC에 {@code PROD_DB_URL}이 있을 이유가 없다.</p>
 *
 * <p>초보자 설명: "조용히 잘못 도는 것"보다 "시끄럽게 멈추는 것"이 낫다는 원칙이다.
 * 금융 데이터를 다루는 서비스에서 잘못된 DB에 연결된 채 정상처럼 보이는 상태가 가장
 * 위험하다.</p>
 *
 * <p>실행자가 의도적으로 {@code --spring.profiles.active=local}을 지정한 경우까지
 * 막는다. 운영 표식이 있는 곳에서 local을 명시하는 것 역시 사고에 가깝고, 규칙을
 * "local + 운영 표식 = 중단" 하나로 두는 편이 설명하기 쉽기 때문이다.</p>
 *
 * @see ProductionPostgresqlTlsGuard 운영 프로파일 쪽의 대응 가드
 */
@AutoConfiguration
@Profile("local")
public class LocalProfileFallbackGuard {

    /**
     * 운영 배포에서만 주입되는 환경 변수들. {@code compose.prod.yml}의 공통 환경 블록과
     * 서비스별 데이터소스 설정에서 온다. 개발자 PC에 존재할 이유가 없는 이름만 고른다.
     */
    private static final List<String> PRODUCTION_MARKERS = List.of(
            "PROD_DB_URL",
            "PROD_KAFKA_BOOTSTRAP_SERVERS",
            "PROD_REDIS_HOST");

    public LocalProfileFallbackGuard(Environment environment) {
        requireNoProductionMarkers(environment);
    }

    static void requireNoProductionMarkers(Environment environment) {
        List<String> present = PRODUCTION_MARKERS.stream()
                .filter(marker -> hasText(environment.getProperty(marker)))
                .toList();

        if (present.isEmpty()) {
            return;
        }

        throw new IllegalStateException(
                "Refusing to start on the local profile while production settings are present: "
                        + String.join(", ", present)
                        + ". The local profile uses an in-memory database, so starting here would"
                        + " look healthy while writing nowhere. Set SPRING_PROFILES_ACTIVE to the"
                        + " intended profile, or unset those variables to run locally.");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
