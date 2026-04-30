import React from 'react';
import { AlertCircle } from 'lucide-react';
import styles from './InterBranch.module.css';

export default function InterBranchPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>지점간 자금 정산 관리</h2>
          <p>본점-지점 및 지점간 상호 거래에 대한 장부 대조 및 정산을 수행합니다.</p>
        </div>
        <div className={styles.stats}>
          <div className={styles.statCard}>
            <span>미정산 항목</span>
            <h3>12건</h3>
          </div>
          <div className={styles.statCard}>
            <span>총 차액</span>
            <h3 className={styles.danger}>₩45,200,000</h3>
          </div>
        </div>
      </header>

      <div className={styles.layout}>
        <section className={styles.listSection + " glass-card"}>
          <div className={styles.sectionHeader}>
            <h3>미정산 내역 대조</h3>
            <button className={styles.autoMatchBtn}>자동 매칭 실행</button>
          </div>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>발생지점</th>
                <th>상대지점</th>
                <th>거래내용</th>
                <th>금액</th>
                <th>상태</th>
                <th>조치</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>강남금융센터</td>
                <td>본점영업부</td>
                <td>본지점 자금 이체</td>
                <td className={styles.bold}>₩10,000,000</td>
                <td><span className={styles.tagWarning}>차액발생</span></td>
                <td><button className={styles.viewBtn}>상세보기</button></td>
              </tr>
              <tr className={styles.matchRow}>
                <td>여의도지점</td>
                <td>삼성역지점</td>
                <td>타점권 추심</td>
                <td className={styles.bold}>₩5,500,000</td>
                <td><span className={styles.tagSuccess}>정상</span></td>
                <td><button className={styles.viewBtn}>확인</button></td>
              </tr>
            </tbody>
          </table>
        </section>

        <aside className={styles.infoSection + " glass-card"}>
          <h3>정산 요약</h3>
          <div className={styles.summaryItem}>
            <p>자동 매칭율</p>
            <div className={styles.progress}>
              <div className={styles.bar} style={{width: '85%'}}></div>
            </div>
          </div>
          <div className={styles.alerts}>
            <div className={styles.alertItem}>
              <AlertCircle size={16} />
              <p>원인 미상 입금 3건 발생</p>
            </div>
          </div>
          <button className={styles.reportBtn}>정산 결과 보고서 출력</button>
        </aside>
      </div>
    </div>
  );
}
