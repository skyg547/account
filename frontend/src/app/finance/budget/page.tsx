import React from 'react';
import { Wallet, TrendingUp, AlertCircle, CheckCircle2 } from 'lucide-react';
import styles from './Budget.module.css';

export default function BudgetPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>예산 편성 및 실적 관리</h2>
          <p>부서별 연간/월간 예산을 수립하고 집행 실적을 실시간으로 통제합니다.</p>
        </div>
      </header>

      <div className={styles.stats}>
        <div className={styles.card + " glass-card"}>
          <span>총 예산 대비 집행률</span>
          <h2>68.5%</h2>
          <div className={styles.progress}><div className={styles.bar} style={{width: '68.5%'}}></div></div>
        </div>
        <div className={styles.card + " glass-card"}>
          <span>임박한 예산 소진 (3건)</span>
          <h2 className={styles.warning}>₩12,400,000</h2>
          <p>마케팅본부 광고비 외 2건</p>
        </div>
      </div>

      <section className={styles.gridSection + " glass-card"}>
        <div className={styles.sectionHeader}>
          <h3>부서별 예산 현황 (2026년)</h3>
          <button className={styles.editBtn}>상세 편성</button>
        </div>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>부서명</th>
              <th>편성 예산</th>
              <th>실행 실적</th>
              <th>집행 잔액</th>
              <th>집행률</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>재무본부</td>
              <td>₩500,000,000</td>
              <td>₩320,000,000</td>
              <td>₩180,000,000</td>
              <td>64%</td>
              <td><span className={styles.statusOk}>정상</span></td>
            </tr>
            <tr>
              <td>영업본부</td>
              <td>₩1,200,000,000</td>
              <td>₩1,140,000,000</td>
              <td>₩60,000,000</td>
              <td>95%</td>
              <td><span className={styles.statusWarn}>위험</span></td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}
