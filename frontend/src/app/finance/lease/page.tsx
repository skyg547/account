import React from 'react';
import { FileText, Calculator, Landmark, ShieldCheck } from 'lucide-react';
import styles from './Lease.module.css';

export default function LeasePage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>IFRS 16 리스 회계 관리</h2>
          <p>사용권 자산과 리스 부채를 인식하고 기간별 이자 및 상각을 관리합니다.</p>
        </div>
        <div className={styles.actions}>
          <button className={styles.addBtn}>신규 리스계약 등록</button>
        </div>
      </header>

      <div className={styles.summary}>
        <div className={styles.glassCard + " glass-card"}>
          <div className={styles.label}>총 사용권 자산 (ROU Asset)</div>
          <div className={styles.value}>₩12,450,000,000</div>
        </div>
        <div className={styles.glassCard + " glass-card"}>
          <div className={styles.label}>총 리스 부채 (Lease Liability)</div>
          <div className={styles.value}>₩11,820,500,000</div>
        </div>
      </div>

      <section className={styles.contractSection + " glass-card"}>
        <div className={styles.sectionHeader}>
          <h3>활성 리스 계약 목록</h3>
        </div>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>계약번호</th>
              <th>계약명</th>
              <th>종료일</th>
              <th>연금리</th>
              <th>월리스료</th>
              <th>부채잔액</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>LS-2024-001</td>
              <td className={styles.bold}>본사 사옥 임차 계약</td>
              <td>2034-12-31</td>
              <td>4.5%</td>
              <td>₩120,000,000</td>
              <td>₩9.5B</td>
              <td><span className={styles.activeTag}>계약중</span></td>
            </tr>
            <tr>
              <td>LS-2025-012</td>
              <td className={styles.bold}>데이터센터 서버 리스</td>
              <td>2028-06-30</td>
              <td>5.2%</td>
              <td>₩45,000,000</td>
              <td>₩1.2B</td>
              <td><span className={styles.activeTag}>계약중</span></td>
            </tr>
          </tbody>
        </table>
      </section>

      <section className={styles.details + " glass-card"}>
        <h3>당월 리스 전표 생성 현황</h3>
        <div className={styles.statusList}>
          <div className={styles.statusItem}>
            <ShieldCheck color="#22c55e" size={18} />
            <span>4월 감가상각 전표 생성 완료</span>
          </div>
          <div className={styles.statusItem}>
            <ShieldCheck color="#22c55e" size={18} />
            <span>4월 리스 이자비용 계상 완료</span>
          </div>
        </div>
      </section>
    </div>
  );
}
