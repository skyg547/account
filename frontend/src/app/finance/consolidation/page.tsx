import React from 'react';
import { Network, ArrowRightLeft, Globe, Database } from 'lucide-react';
import styles from './Consolidation.module.css';

export default function ConsolidationPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>연결 회계 기초 관리</h2>
          <p>모회사와 자회사 간의 지배구조를 정의하고 내부 거래 제거 기초 데이터를 관리합니다.</p>
        </div>
      </header>

      <div className={styles.hierarchy + " glass-card"}>
        <h3>그룹 지배구조 (Entity Tree)</h3>
        <div className={styles.entityMap}>
          <div className={styles.parent}>
            <Globe color="var(--primary)" />
            <strong>(주)어카운트 어드밴스드 (본사)</strong>
          </div>
          <div className={styles.children}>
            <div className={styles.line}></div>
            <div className={styles.childList}>
              <div className={styles.childItem}>
                <Database size={16} />
                <span>어카운트 테크 (지분 100%)</span>
              </div>
              <div className={styles.childItem}>
                <Database size={16} />
                <span>어카운트 금융투자 (지분 85%)</span>
              </div>
              <div className={styles.childItem}>
                <Database size={16} />
                <span>글로벌 로지스틱스 (지분 60%)</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <section className={styles.intercompany + " glass-card"}>
        <div className={styles.sectionHeader}>
          <h3>내부거래 대조 (Intercompany Elimination)</h3>
          <button className={styles.matchBtn}>내부거래 자동 매칭</button>
        </div>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>송신법인</th>
              <th>수신법인</th>
              <th>거래유형</th>
              <th>송신금액</th>
              <th>수신금액</th>
              <th>차액</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>본사</td>
              <td>어카운트 테크</td>
              <td>용역 매출</td>
              <td>₩120,000,000</td>
              <td>₩120,000,000</td>
              <td>0</td>
              <td><span className={styles.tagMatched}>매칭완료</span></td>
            </tr>
            <tr>
              <td>본사</td>
              <td>글로벌 로지스틱스</td>
              <td>이자 비용</td>
              <td>₩15,000,000</td>
              <td>₩14,800,000</td>
              <td className={styles.danger}>₩200,000</td>
              <td><span className={styles.tagError}>불일치</span></td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}
