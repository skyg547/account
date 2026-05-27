package com.ho.account.ecl.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("postgres")
public class CreditRiskDataExporterTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void exportCreditRiskDataFlow() throws IOException {
        String filePath = "../../CREDIT_DATA_REPORT.md";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("# 📊 원격 PostgreSQL 대손충당금(IFRS9) 데이터 흐름 리포트");
            writer.println("\n> **조회 시점**: " + java.time.LocalDateTime.now());
            writer.println("> **원격 서버**: 192.168.0.104:5432 (credit_risk_db)");

            // 1. 차주 및 계좌 현황
            writer.println("\n## 1단계: 산출 대상 차주 및 계좌 정보");
            writer.println("| 고객명 | 계좌번호 | 잔액 | 등급 |");
            writer.println("| :--- | :--- | :--- | :--- |");
            List<Map<String, Object>> step1 = jdbcTemplate.queryForList(
                "SELECT c.customer_name, a.account_no, a.outstanding_amt, c.internal_rating " +
                "FROM cr_customers c JOIN cr_accounts a ON c.id = a.customer_id " +
                "WHERE a.is_active = true LIMIT 10");
            for (Map<String, Object> row : step1) {
                writer.printf("| %s | %s | %s | %s |\n", 
                    row.get("customer_name"), row.get("account_no"), row.get("outstanding_amt"), row.get("internal_rating"));
            }

            // 2. 최종 대손충당금(IFRS9) 산출 결과
            writer.println("\n## 2단계: 최종 대손충당금(IFRS9) 산출 결과 (RWA/ECL)");
            writer.println("| 기준일 | 계좌ID | 스테이징 | RWA(표준) | RWA(내부) | 기대손실 |");
            writer.println("| :--- | :--- | :--- | :--- | :--- | :--- |");
            List<Map<String, Object>> step2 = jdbcTemplate.queryForList(
                "SELECT base_date, account_id, staging, rwa_sa, rwa_irb, expected_loss " +
                "FROM cr_risk_results ORDER BY base_date DESC LIMIT 10");
            for (Map<String, Object> row : step2) {
                writer.printf("| %s | %s | %s | %s | %s | %s |\n", 
                    row.get("base_date"), row.get("account_id"), row.get("staging"), row.get("rwa_sa"), row.get("rwa_irb"), row.get("expected_loss"));
            }

            // 3. 배치 수행 이력
            writer.println("\n## 3단계: 배치 수행 이력 (Audit)");
            writer.println("| 작업명 | 상태 | 총 건수 | 성공 | 시작시간 |");
            writer.println("| :--- | :--- | :--- | :--- | :--- |");
            List<Map<String, Object>> step3 = jdbcTemplate.queryForList(
                "SELECT job_name, status, total_count, success_count, start_at FROM cr_batch_audits ORDER BY start_at DESC LIMIT 5");
            for (Map<String, Object> row : step3) {
                writer.printf("| %s | %s | %s | %s | %s |\n", 
                    row.get("job_name"), row.get("status"), row.get("total_count"), row.get("success_count"), row.get("start_at"));
            }
        }
        System.out.println("✅ 대손충당금(IFRS9) 리포트 생성 완료: " + filePath);
    }
}
