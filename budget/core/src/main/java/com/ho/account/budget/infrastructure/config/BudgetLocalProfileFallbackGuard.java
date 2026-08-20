package com.ho.account.budget.infrastructure.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * 운영 환경에서 프로파일 지정을 빠뜨려 local 설정으로 기동하는 것을 막는다.
 *
 * <p>{@code spring.profiles.default: local} 덕분에 개발자는 프로파일을 지정하지 않고도
 * 바로 실행할 수 있다. 그러나 운영 배포에서 {@code SPRING_PROFILES_ACTIVE}를 빠뜨리면
 * 같은 기본값이 발동해 인메모리 데이터베이스로 <strong>오류 없이</strong> 기동한다.
 * 서버는 정상으로 보이지만 실제 데이터베이스에는 연결되지 않는다. 이 가드는 그 조합을
 * 기동 실패로 바꾼다.</p>
 *
 * <p>같은 규칙이 {@code shared-kernel}의 {@code LocalProfileFallbackGuard}에 auto-configuration
 * 으로 들어 있으나, Budget은 {@code shared-kernel}에 의존하지 않는다. 이미
 * {@link BudgetProductionPostgresqlTlsGuard}가 같은 이유로 공용 가드를 복제해 두었으므로
 * 그 선례를 따른다. 규칙을 바꿀 때는 두 곳을 함께 고쳐야 한다.</p>
 *
 * @see BudgetProductionPostgresqlTlsGuard 운영 프로파일 쪽의 대응 가드
 */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class BudgetLocalProfileFallbackGuard {

    private static final List<String> PRODUCTION_MARKERS = List.of(
            "PROD_DB_URL",
            "PROD_KAFKA_BOOTSTRAP_SERVERS",
            "PROD_REDIS_HOST");

    public BudgetLocalProfileFallbackGuard(Environment environment) {
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
