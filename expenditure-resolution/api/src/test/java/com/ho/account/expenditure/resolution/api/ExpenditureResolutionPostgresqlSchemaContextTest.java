package com.ho.account.expenditure.resolution.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * [Expenditure Resolution PostgreSQL Schema Context Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent (교육적 설명 및 설계 의도):</strong></p>
 * <ul>
 *   <li><strong>Flyway Schema Migration & Validation (마이그레이션 스크립트 검증)</strong>:
 *       local 프로파일의 Flyway 마이그레이션이 정상 실행되어 {@code flyway_schema_history}에
 *       성공 기록이 생성되고, 비즈니스 핵심 테이블들이 PostgreSQL 호환 모드에서 정상 생성되는지 검증합니다.</li>
 *   <li><strong>JPA Repository & DDL Alignment (레포지토리 및 DDL 정합성)</strong>:
 *       {@link ExpenditureResolutionRepository} 빈이 정상 주입되고 H2 인메모리 DB상에서
 *       ddl-auto validate 기준을 만족하는지 확인합니다.</li>
 * </ul>
 */
@SpringBootTest(
        classes = ExpenditureResolutionApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:expenditure-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        })
@ActiveProfiles("local")
class ExpenditureResolutionPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExpenditureResolutionRepository repository;

    @Test
    @DisplayName("local 프로파일 환경에서 Flyway 마이그레이션이 실행되고 비즈니스 테이블 및 레포지토리가 정상적으로 동작한다")
    void cleanBaselineMigratesValidatesAndSupportsRepository() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '1' AND success = TRUE
                """, Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('expenditure_resolutions', 'expenditure_details',
                                     'budgets', 'ap_invoices', 'ap_payments')
                """, Integer.class)).isEqualTo(5);
        assertThat(repository.count()).isZero();
    }
}

