package com.ho.account.ecl.batch.support;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * [Support] Spring Batch 5.x용 QueryDSL Paging ItemReader 구현체.
 * 
 * 💡 [초보자를 위한 가이드]
 * Spring Batch의 기본 JpaPagingItemReader는 문자열(JPQL)만 지원하지만, 
 * 이 클래스는 QueryDSL을 사용하여 컴파일 타임에 검증된 쿼리를 기반으로 페이징을 수행합니다.
 */
public class QuerydslPagingItemReader<T> extends AbstractPagingItemReader<T> {

    private final EntityManagerFactory entityManagerFactory;
    private final Function<JPAQueryFactory, JPAQuery<T>> queryFunction;
    private EntityManager entityManager;

    public QuerydslPagingItemReader(EntityManagerFactory entityManagerFactory,
                                    int pageSize,
                                    Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        setPageSize(pageSize);
        setName(ClassUtils.getShortName(QuerydslPagingItemReader.class));
        this.entityManagerFactory = entityManagerFactory;
        this.queryFunction = queryFunction;
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
        entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    protected void doReadPage() {
        if (results == null) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        JPAQuery<T> query = queryFunction.apply(queryFactory)
                .offset((long) getPage() * getPageSize())
                .limit(getPageSize());

        List<T> fetchedResults = query.fetch();
        results.addAll(fetchedResults);
    }

    @Override
    protected void doClose() throws Exception {
        if (entityManager != null) {
            entityManager.close();
        }
        super.doClose();
    }
}
