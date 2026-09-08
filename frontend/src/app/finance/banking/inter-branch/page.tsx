"use client";

import React, { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import styles from './InterBranch.module.css';
import { bankingService, InterBranchDashboardData } from '@/services/bankingService';

export default function InterBranchPage() {
  const [data, setData] = useState<InterBranchDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [isMatching, setIsMatching] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await bankingService.getInterBranchData();
      setData(res);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {

    fetchData();
  }, []);

  const handleAutoMatch = async () => {
    setIsMatching(true);
    try {
      const success = await bankingService.runAutoMatch();
      if (success) {
        alert('자동 매칭이 성공적으로 완료되었습니다.');
        fetchData(); // 재조회
      } else {
        alert('자동 매칭 실행 중 오류가 발생했습니다.');
      }
    } catch (e) {
      console.error(e);
      alert('자동 매칭 실행 실패');
    } finally {
      setIsMatching(false);
    }
  };

  const formatCurrency = (val: number) => `₩${new Intl.NumberFormat('ko-KR').format(val)}`;

  if (loading || !data) {
    return (
      <div className={styles.container}>
        <div className="flex justify-center items-center py-20 text-slate-500 font-bold">
          Loading Inter-Branch Data...
        </div>
      </div>
    );
  }

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
            <h3>{data.unmatchedCount}건</h3>
          </div>
          <div className={styles.statCard}>
            <span>총 차액</span>
            <h3 className={data.totalDiscrepancyAmount > 0 ? styles.danger : ''}>{formatCurrency(data.totalDiscrepancyAmount)}</h3>
          </div>
        </div>
      </header>

      <div className={styles.layout}>
        <section className={styles.listSection + " glass-card"}>
          <div className={styles.sectionHeader}>
            <h3>미정산 내역 대조</h3>
            <button
              className={styles.autoMatchBtn}
              onClick={handleAutoMatch}
              disabled={isMatching}
            >
              {isMatching ? '매칭 중...' : '자동 매칭 실행'}
            </button>
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
              {data.transactions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-500">조회된 데이터가 없습니다.</td>
                </tr>
              ) : (
                data.transactions.map((tx) => (
                  <tr key={tx.id} className={tx.status === 'MATCHED' ? styles.matchRow : ''}>
                    <td>{tx.sourceBranch}</td>
                    <td>{tx.targetBranch}</td>
                    <td>{tx.transactionType}</td>
                    <td className={styles.bold}>{formatCurrency(tx.amount)}</td>
                    <td>
                      <span className={tx.status === 'MATCHED' ? styles.tagSuccess : styles.tagWarning}>
                        {tx.status === 'MATCHED' ? '정상' : tx.status === 'DISCREPANCY' ? '차액발생' : '대기'}
                      </span>
                    </td>
                    <td><button className={styles.viewBtn}>{tx.status === 'MATCHED' ? '확인' : '상세보기'}</button></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </section>

        <aside className={styles.infoSection + " glass-card"}>
          <h3>정산 요약</h3>
          <div className={styles.summaryItem}>
            <p>자동 매칭율</p>
            <div className={styles.progress}>
              <div className={styles.bar} style={{width: `${data.autoMatchRate}%`}}></div>
            </div>
          </div>
          <div className={styles.alerts}>
            <div className={styles.alertItem}>
              <AlertCircle size={16} />
              <p>원인 미상 입금 {data.unexplainedDepositsCount}건 발생</p>
            </div>
          </div>
          <button className={styles.reportBtn}>정산 결과 보고서 출력</button>
        </aside>
      </div>
    </div>
  );
}
