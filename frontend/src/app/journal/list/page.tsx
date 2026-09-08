"use client";

import React, { useState, useEffect } from 'react';
import { Search, Calendar, Filter, FileText, CheckCircle2, Clock } from 'lucide-react';
import styles from './JournalList.module.css';
import { journalService, JournalEntryDto, JournalDetailDto } from '@/services/journalService';

/**
 * [전표 목록 조회 화면]
 * 입력된 모든 전표를 조회하고 승인 상태를 확인하는 화면입니다.
 * 백엔드 API (/api/journals) 연동 완료.
 */
export default function JournalListPage() {
  const [journals, setJournals] = useState<JournalEntryDto[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchJournals = async () => {
    setLoading(true);
    try {
      const data = await journalService.getJournalEntries();
      setJournals(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {

    fetchJournals();
  }, []);

  const calculateTotalAmount = (details: JournalDetailDto[]) => {
    if (!Array.isArray(details)) return '₩0';
    // 차변 합계만 표시 (대차 평균이 맞다고 가정)
    const debitTotal = details
      .filter((d) => d.side === 'DEBIT')
      .reduce((sum: number, d) => sum + (Number(d.amount) || 0), 0);
    return `₩${debitTotal.toLocaleString()}`;
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>전표 조회</h2>
          <p>회계 장부에 기록된 모든 전표 내역을 조회합니다.</p>
        </div>
      </header>

      {/* 종합 검색 필터 */}
      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label><Calendar size={14} /> 기간 설정</label>
            <div className={styles.dateRange}>
              <input type="date" defaultValue="2026-04-01" />
              <span>~</span>
              <input type="date" defaultValue="2026-04-30" />
            </div>
          </div>
          <div className={styles.filterGroup}>
            <label><Filter size={14} /> 전표 상태</label>
            <select>
              <option>전체</option>
              <option>DRAFT</option>
              <option>APPROVED</option>
              <option>POSTED</option>
            </select>
          </div>
          <div className={styles.searchBox}>
            <Search size={18} className={styles.searchIcon} />
            <input type="text" placeholder="적요, 전표번호 검색" />
          </div>
          <button className={styles.queryBtn} onClick={fetchJournals}>조회하기</button>
        </div>
      </section>

      {/* 전표 리스트 */}
      <section className={`glass-card ${styles.gridSection}`}>
        {loading ? (
          <div className="flex justify-center items-center py-10">
             <span className="text-white">데이터를 불러오는 중입니다...</span>
          </div>
        ) : (
          <table className={styles.listTable}>
            <thead>
              <tr>
                <th>전표번호</th>
                <th>전표일자</th>
                <th>적요</th>
                <th>차변 합계 금액</th>
                <th>상태</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {journals.length > 0 ? (
                journals.map((j, idx) => (
                  <tr key={idx}>
                    <td><span className={styles.journalId}>{j.slipNo || '임시발급'}</span></td>
                    <td>{j.slipDate}</td>
                    <td className={styles.bold}>{j.description}</td>
                    <td className={styles.amount}>{calculateTotalAmount(j.details)}</td>
                    <td>
                      <span className={j.status === 'POSTED' || j.status === 'APPROVED' ? styles.statusSuccess : styles.statusPending}>
                        {j.status === 'POSTED' || j.status === 'APPROVED' ? <CheckCircle2 size={14} /> : <Clock size={14} />}
                        {j.status}
                      </span>
                    </td>
                    <td><button className={styles.viewBtn}><FileText size={16} /></button></td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="text-center py-6 text-slate-500">
                    조회된 전표가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}