import React from 'react';
import { Globe, RefreshCcw, TrendingUp, TrendingDown, DollarSign } from 'lucide-react';
import styles from './FxPosition.module.css';

export default function FxPositionPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>외환(FX) 포지션 관리</h2>
          <p>통화별 보유 자원 및 리스크 포지션을 실시간으로 모니터링합니다.</p>
        </div>
        <div className={styles.exchangeRate}>
          <div className={styles.rateItem}>
            <span>USD/KRW</span>
            <strong>1,385.40</strong>
            <span className={styles.up}>+2.4 (0.17%)</span>
          </div>
          <div className={styles.rateItem}>
            <span>JPY/KRW</span>
            <strong>894.20</strong>
            <span className={styles.down}>-1.1 (0.12%)</span>
          </div>
        </div>
      </header>

      <div className={styles.positionSummary}>
        <div className={styles.card + " glass-card"}>
          <h4>전체 외화 순포지션</h4>
          <h3>$4,250,000</h3>
          <p className={styles.subText}>환산금액 ₩5,888,250,000</p>
        </div>
        <div className={styles.card + " glass-card"}>
          <h4>당일 환평가 손익</h4>
          <h3 className={styles.up}>+₩12,450,000</h3>
          <p className={styles.subText}>전일 대비 15% 상승</p>
        </div>
      </div>

      <section className={styles.gridSection + " glass-card"}>
        <h3>통화별 상세 포지션</h3>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>통화</th>
              <th>장부금액 (외화)</th>
              <th>평균단가</th>
              <th>평가금액 (KRW)</th>
              <th>평가손익</th>
              <th>리밋 준수</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><span className={styles.currencyCode}>USD</span> 미국 달러</td>
              <td className={styles.bold}>$2,500,000</td>
              <td>1,375.00</td>
              <td>₩3,463,500,000</td>
              <td className={styles.up}>+₩26,000,000</td>
              <td><span className={styles.statusOk}>SAFE</span></td>
            </tr>
            <tr>
              <td><span className={styles.currencyCode}>JPY</span> 일본 엔</td>
              <td className={styles.bold}>¥150,000,000</td>
              <td>905.00</td>
              <td>₩1,341,300,000</td>
              <td className={styles.down}>-₩8,500,000</td>
              <td><span className={styles.statusOk}>SAFE</span></td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}
