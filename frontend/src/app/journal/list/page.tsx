"use client";

import React, { useCallback, useEffect, useState } from 'react';
import { Calendar, CheckCircle2, Clock, Filter, Loader2 } from 'lucide-react';
import styles from './JournalList.module.css';
import {
  journalService,
  JournalDetailDto,
  JournalEntryDto,
  JournalQueryParams,
  JournalStatus
} from '@/services/journalService';
import { useToast } from '@/context/ToastContext';

const INITIAL_FILTERS = {
  startDate: '2026-04-01',
  endDate: '2026-04-30',
  status: 'ALL'
};

type JournalAction = 'approve' | 'post';

/**
 * [전표 목록 조회 및 승인 화면]
 * 기간/상태로 전표를 조회하고 DRAFT -> APPROVED -> POSTED 상태 전이를 처리합니다.
 */
export default function JournalListPage() {
  const { success: showSuccessToast, error: showErrorToast, warning: showWarningToast } = useToast();
  const [journals, setJournals] = useState<JournalEntryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState(INITIAL_FILTERS.startDate);
  const [endDate, setEndDate] = useState(INITIAL_FILTERS.endDate);
  const [selectedStatus, setSelectedStatus] = useState(INITIAL_FILTERS.status);
  const [usingMockFallback, setUsingMockFallback] = useState(false);
  const [processingJournalId, setProcessingJournalId] = useState<number | null>(null);

  const loadJournals = useCallback(async (params: JournalQueryParams) => {
    setLoading(true);
    try {
      const data = await journalService.getJournalEntries(params);
      setJournals(data);
      setUsingMockFallback(journalService.isUsingMockFallback());
    } catch (error) {
      console.error(error);
      showErrorToast('전표 목록을 불러오지 못했습니다.', { title: '조회 실패' });
    } finally {
      setLoading(false);
    }
  }, [showErrorToast]);

  const fetchJournals = () => {
    if (startDate && endDate && startDate > endDate) {
      showWarningToast('시작일은 종료일보다 늦을 수 없습니다.', { title: '조회 기간 확인' });
      return;
    }

    void loadJournals({
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      status: selectedStatus === 'ALL' ? undefined : selectedStatus
    });
  };

  useEffect(() => {
    void loadJournals({
      startDate: INITIAL_FILTERS.startDate,
      endDate: INITIAL_FILTERS.endDate
    });
  }, [loadJournals]);

  const calculateTotalAmount = (details: JournalDetailDto[]) => {
    const debitTotal = details
      .filter((detail) => detail.side === 'DEBIT')
      .reduce((sum, detail) => sum + (Number(detail.amount) || 0), 0);
    return `₩${debitTotal.toLocaleString()}`;
  };

  const handleJournalAction = async (journal: JournalEntryDto, action: JournalAction) => {
    if (!journal.id || processingJournalId !== null) return;

    const nextStatus: JournalStatus = action === 'approve' ? 'APPROVED' : 'POSTED';
    const actionLabel = action === 'approve' ? '승인' : '전기';
    setProcessingJournalId(journal.id);

    try {
      if (action === 'approve') {
        await journalService.approveJournalEntry(journal.id);
      } else {
        await journalService.postJournalEntry(journal.id);
      }

      // Mock 조회 결과는 서버에 존재하지 않으므로 상태 전이를 화면에만 반영합니다.
      setJournals((current) => current.map((item) => (
        item.id === journal.id ? { ...item, status: nextStatus } : item
      )));
      showSuccessToast(
        usingMockFallback
          ? `Mock 전표의 ${actionLabel} 상태를 화면에 반영했습니다.`
          : `전표가 성공적으로 ${actionLabel} 처리되었습니다.`,
        { title: `${actionLabel} 완료` }
      );
    } catch (error) {
      showErrorToast(
        error instanceof Error ? error.message : `전표 ${actionLabel} 처리 중 오류가 발생했습니다.`,
        { title: `${actionLabel} 실패` }
      );
    } finally {
      setProcessingJournalId(null);
    }
  };

  const renderAction = (journal: JournalEntryDto) => {
    const isProcessing = processingJournalId === journal.id;
    const actionsDisabled = loading || processingJournalId !== null || !journal.id;

    if (journal.status === 'DRAFT') {
      return (
        <button
          type="button"
          className={styles.actionBtn}
          onClick={() => void handleJournalAction(journal, 'approve')}
          disabled={actionsDisabled}
          aria-busy={isProcessing}
        >
          {isProcessing ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
          {isProcessing ? '처리 중...' : '승인'}
        </button>
      );
    }

    if (journal.status === 'APPROVED') {
      return (
        <button
          type="button"
          className={styles.actionBtn}
          onClick={() => void handleJournalAction(journal, 'post')}
          disabled={actionsDisabled}
          aria-busy={isProcessing}
        >
          {isProcessing ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
          {isProcessing ? '처리 중...' : '전기'}
        </button>
      );
    }

    if (journal.status === 'POSTED') {
      return <span className={styles.completedBadge}><CheckCircle2 size={14} /> 완료</span>;
    }

    return <span className={styles.noAction}>-</span>;
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>전표 조회</h2>
          <p>회계 장부에 기록된 전표를 조회하고 승인·전기합니다.</p>
        </div>
      </header>

      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label><Calendar size={14} /> 기간 설정</label>
            <div className={styles.dateRange}>
              <input
                type="date"
                value={startDate}
                onChange={(event) => setStartDate(event.target.value)}
                disabled={loading}
                aria-label="조회 시작일"
              />
              <span>~</span>
              <input
                type="date"
                value={endDate}
                onChange={(event) => setEndDate(event.target.value)}
                disabled={loading}
                aria-label="조회 종료일"
              />
            </div>
          </div>
          <div className={styles.filterGroup}>
            <label><Filter size={14} /> 전표 상태</label>
            <select
              value={selectedStatus}
              onChange={(event) => setSelectedStatus(event.target.value)}
              disabled={loading}
              aria-label="전표 상태"
            >
              <option value="ALL">전체</option>
              <option value="DRAFT">DRAFT</option>
              <option value="APPROVED">APPROVED</option>
              <option value="POSTED">POSTED</option>
            </select>
          </div>
          <button
            type="button"
            className={styles.queryBtn}
            onClick={fetchJournals}
            disabled={loading || processingJournalId !== null}
          >
            {loading ? '조회 중...' : '조회하기'}
          </button>
        </div>
        {usingMockFallback && (
          <p className={styles.mockNotice}>백엔드에 연결할 수 없어 Mock 데이터로 표시 중입니다.</p>
        )}
      </section>

      <section className={`glass-card ${styles.gridSection}`}>
        {loading ? (
          <div className={styles.loadingState}>
            <Loader2 size={20} className="animate-spin" />
            <span>데이터를 불러오는 중입니다...</span>
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
                <th>처리</th>
              </tr>
            </thead>
            <tbody>
              {journals.length > 0 ? (
                journals.map((journal, index) => (
                  <tr key={journal.id ?? journal.slipNo ?? `${journal.slipDate}-${index}`}>
                    <td><span className={styles.journalId}>{journal.slipNo || '임시발급'}</span></td>
                    <td>{journal.slipDate}</td>
                    <td className={styles.bold}>{journal.description}</td>
                    <td className={styles.amount}>{calculateTotalAmount(journal.details)}</td>
                    <td>
                      <span className={journal.status === 'POSTED' || journal.status === 'APPROVED' ? styles.statusSuccess : styles.statusPending}>
                        {journal.status === 'POSTED' || journal.status === 'APPROVED'
                          ? <CheckCircle2 size={14} />
                          : <Clock size={14} />}
                        {journal.status ?? 'UNKNOWN'}
                      </span>
                    </td>
                    <td>{renderAction(journal)}</td>
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
