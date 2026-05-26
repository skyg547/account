package com.risk.credit.core.infrastructure.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Configuration] QueryDSL 전역 설정 클래스.
 * 
 * 💡 [초보자를 위한 가이드]
 * JPAQueryFactory를 빈으로 등록하여 서비스나 리포지토리에서 
 * 쿼리를 코드로 작성할 수 있게 해주는 '쿼리 공장' 설정입니다.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
