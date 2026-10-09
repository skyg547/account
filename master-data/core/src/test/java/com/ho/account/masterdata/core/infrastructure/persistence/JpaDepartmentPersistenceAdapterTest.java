package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.DepartmentEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.DepartmentMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = JpaDepartmentPersistenceAdapterTest.TestApplication.class)
@Import({JpaDepartmentPersistenceAdapter.class, DepartmentMapper.class})
class JpaDepartmentPersistenceAdapterTest {

    @Autowired private JpaDepartmentPersistenceAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication { }

    @Test
    void existenceIncludesFutureAndExpiredDepartmentRows() {
        LocalDate today = LocalDate.now();
        persist("DEPT-FUTURE-EXISTS", today.plusDays(21), today.plusDays(30));
        persist("DEPT-HISTORY-EXISTS", today.minusDays(60), today.minusDays(30));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.existsByCode("DEPT-FUTURE-EXISTS")).isTrue();
        assertThat(adapter.existsByCode("DEPT-HISTORY-EXISTS")).isTrue();
        assertThat(adapter.existsByCode("DEPT-NEW-KEY")).isFalse();
    }

    private void persist(String code, LocalDate from, LocalDate to) {
        DepartmentEntity row = new DepartmentEntity();
        row.setCode(code);
        row.setName("테스트 부서");
        row.setValidFrom(from);
        row.setValidTo(to);
        entityManager.persist(row);
    }
}
