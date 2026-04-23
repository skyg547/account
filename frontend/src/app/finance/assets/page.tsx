import React from 'react';
import { Box, BarChart2, Plus, ArrowRight } from 'lucide-react';
import styles from './AssetManagement.module.css';

/**
 * [고정자산 및 감가상각 관리 화면]
 * 비유동 자산의 취득, 상각, 처분 이력을 관리하고 상각 스케줄을 확인합니다.
 * 설계서 파트 6-⑯ 기반.
 */
export default function AssetManagementPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>고정자산 및 감가상각 관리</h2>
          <p>전사 유무형 자산 대장을 관리하고 상각 누계액 추이를 분석합니다.</p>
        </div>
        <button className={styles.addBtn}><Plus size={18} /> 신규 자산 등록</button>
      </header>

      {/* 자산 현황 요약 */}
      <section className={styles.assetSummary}>
        <div className={`glass-card ${styles.summaryCard}`}>
          <h4>총 자산 취득가액</h4>
          <h3>₩1,240,500,000</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <h4>상각 누계액</h4>
          <h3 className={styles.blue}>₩450,200,000</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <h4>미상각 잔액 (장부가액)</h4>
          <h3>₩790,300,000</h3>
        </div>
      </section>

      {/* 자산 리스트 및 상각 그래프 (컨셉) */}
      <div className={styles.layout}>
        <section className={`glass-card ${styles.listSection}`}>
          <h3>자산 대장 목록</h3>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>자산코드</th>
                <th>자산명</th>
                <th>취득일</th>
                <th>상각방법</th>
                <th>내용연수</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {[
                { code: 'AST-2024-001', name: '업무용 서버 저장장치', date: '2024-01-10', method: '정액법', years: '5년' },
                { code: 'AST-2025-015', name: '개발팀 MacBook Pro', date: '2025-03-05', method: '정액법', years: '3년' },
                { code: 'AST-2026-002', name: '본사 사무실 가구', date: '2026-02-15', method: '정률법', years: '5년' },
              ].map((row, idx) => (
                <tr key={idx}>
                  <td className={styles.code}>{row.code}</td>
                  <td className={styles.bold}>{row.name}</td>
                  <td>{row.date}</td>
                  <td>{row.method}</td>
                  <td>{row.years}</td>
                  <td><button className={styles.viewBtn}><ArrowRight size={16} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <aside className={`glass-card ${styles.graphSection}`}>
          <h3><BarChart2 size={18} /> 감가상각 추이 (예상)</h3>
          <div className={styles.chartPlaceholder}>
            <div className={styles.chartLine}></div>
            <p>2026년 말 예상 잔액: ₩650,000,000</p>
          </div>
        </aside>
      </div>
    </div>
  );
}
