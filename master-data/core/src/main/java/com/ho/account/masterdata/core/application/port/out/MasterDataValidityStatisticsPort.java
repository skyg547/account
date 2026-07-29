package com.ho.account.masterdata.core.application.port.out;

import java.time.LocalDate;

/**
 * 기준일 현재 유효한 기준정보 건수를 조회하는 출력 포트입니다.
 *
 * <p>초보자 설명: 일일 점검 배치는 모든 행을 Java 메모리로 가져오지 않습니다.
 * 애플리케이션 코어는 필요한 집계만 이 포트에 요청하고, JPA/JDBC 어댑터가
 * 데이터베이스의 {@code COUNT} 연산으로 처리합니다.</p>
 */
public interface MasterDataValidityStatisticsPort {

    long countActiveAccountSubjects(LocalDate asOfDate);

    long countActiveDepartments(LocalDate asOfDate);

    long countActiveProducts(LocalDate asOfDate);

    long countActiveBusinessPartners(LocalDate asOfDate);
}