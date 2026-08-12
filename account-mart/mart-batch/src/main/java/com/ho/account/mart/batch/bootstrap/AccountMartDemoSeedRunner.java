package com.ho.account.mart.batch.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * [금융 데이터 마트 배치 데모 시드 데이터 로더 (AccountMartDemoSeedRunner)]
 *
 * 💡 [교육용 가이드 & 스프링 부트 배치 모듈 실행 메커니즘]
 * 1. 데모 시드 조건부 실행 (Conditional Seeding):
 *    'mart.batch.demo-seed.enabled=true' 플래그가 활성화된 데모/로컬 환경에서만 동작합니다.
 *    운영(Prod) 환경이나 일반 통합 테스트(Test) 환경에서 의도치 않게 시드 데이터가 적재되는 것을
 *    방지하기 위해 ConditionalOnProperty 어노테이션을 사용하여 빈 생성을 제어합니다.
 *
 * 2. 멱등성 및 정합성 보장 (Deterministic Fixture Seeding):
 *    AccountMartDemoFixtureService를 주입받아 시계열 분석 및 대손충당금 결산에 필요한
 *    기초 데이터 셋(ODS 원천, KAP 평가등급, 매매기준 환율 등)을 결정론적으로 적재합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mart.batch.demo-seed", name = "enabled", havingValue = "true")
public class AccountMartDemoSeedRunner implements CommandLineRunner {

    private final AccountMartDemoFixtureService demoFixtureService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 [Demo Seed Runner] mart.batch.demo-seed.enabled=true 설정 감지: 데모 시드 데이터 시딩을 시작합니다.");
        demoFixtureService.seedDemoFixtureData();
    }
}
