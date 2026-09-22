"use client";

import React, { useState, useMemo } from 'react';
import { Save, AlertCircle, Plus, Trash2, CheckCircle2, Loader2 } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import styles from './JournalEntry.module.css';
import { journalService, JournalEntryDto, JournalDetailDto, JournalSide } from '@/services/journalService';
import { useToast } from '@/context/ToastContext';

/**
 * [전표 입력 화면]
 * 수동으로 차/대 전표를 행 단위로 입력하는 핵심 화면입니다.
 * 백엔드 API 연동을 통해 실제 전표를 생성합니다.
 */
export default function JournalEntryPage() {
  const router = useRouter();
  const { success: showSuccessToast, error: showErrorToast, warning: showWarningToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // 전표 헤더 상태
  const [slipDate, setSlipDate] = useState(new Date().toISOString().split('T')[0]);
  const [departmentCode, setDepartmentCode] = useState('DEPT_FINANCE');
  const [entryType, setEntryType] = useState('MANUAL');
  const [description, setDescription] = useState('');

  // 전표 라인 상태
  const [details, setDetails] = useState<JournalDetailDto[]>([
    { side: 'DEBIT', accountCode: '', amount: 0, detailDescription: '' },
    { side: 'CREDIT', accountCode: '', amount: 0, detailDescription: '' }
  ]);

  // 금액을 원 단위 부동소수점이 아닌 2자리 소수 단위로 합산해 대차 오차를 방지합니다.
  const { debitTotal, creditTotal, isBalanced } = useMemo(() => {
    let debitMinorUnits = 0;
    let creditMinorUnits = 0;
    details.forEach((detail) => {
      const amountInMinorUnits = Math.round((Number(detail.amount) || 0) * 100);
      if (detail.side === 'DEBIT') debitMinorUnits += amountInMinorUnits;
      else creditMinorUnits += amountInMinorUnits;
    });

    return {
      debitTotal: debitMinorUnits / 100,
      creditTotal: creditMinorUnits / 100,
      isBalanced: debitMinorUnits === creditMinorUnits && debitMinorUnits > 0
    };
  }, [details]);

  const handleAddLine = () => {
    setDetails((current) => [
      ...current,
      { side: 'DEBIT', accountCode: '', amount: 0, detailDescription: '' }
    ]);
  };

  const handleRemoveLine = (index: number) => {
    if (details.length <= 2) {
      showWarningToast('최소 2개의 분개 라인이 필요합니다.');
      return;
    }
    setDetails((current) => current.filter((_, rowIndex) => rowIndex !== index));
  };

  const handleDetailChange = (index: number, changes: Partial<JournalDetailDto>) => {
    setDetails((current) => current.map((detail, rowIndex) => (
      rowIndex === index ? { ...detail, ...changes } : detail
    )));
  };

  const handleSave = async () => {
    if (isSubmitting) return;

    if (!description.trim()) {
      showWarningToast('전표 적요(설명)를 입력해주세요.', { title: '입력 확인' });
      return;
    }
    if (!slipDate) {
      showWarningToast('전표 일자를 입력해주세요.', { title: '입력 확인' });
      return;
    }
    if (!isBalanced) {
      showWarningToast('차대변 합계가 일치하지 않거나 0원입니다.', { title: '대차 평형 불일치' });
      return;
    }

    // 유효성 검사
    for (let i = 0; i < details.length; i++) {
      const d = details[i];
      if (!d.accountCode?.trim()) {
        showWarningToast(`${i + 1}번째 줄의 계정 과목을 입력해주세요.`, { title: '계정 과목 누락' });
        return;
      }
      if (!d.amount || d.amount <= 0) {
        showWarningToast(`${i + 1}번째 줄의 금액을 올바르게 입력해주세요.`, { title: '금액 오류' });
        return;
      }
      if (Math.abs(d.amount * 100 - Math.round(d.amount * 100)) > 0.000001) {
        showWarningToast(`${i + 1}번째 줄의 금액은 소수점 둘째 자리까지만 입력할 수 있습니다.`, { title: '금액 오류' });
        return;
      }
    }

    setIsSubmitting(true);
    try {
      const payload: JournalEntryDto = {
        slipDate,
        accountingDate: slipDate,
        description: description.trim(),
        entryType,
        currencyCode: 'KRW',
        status: 'DRAFT',
        details: details.map((detail) => ({
          ...detail,
          accountCode: detail.accountCode.trim(),
          departmentCode,
          detailDescription: detail.detailDescription?.trim() || undefined
        }))
      };

      await journalService.createJournalEntry(payload);
      showSuccessToast('전표가 성공적으로 생성되었습니다.', { title: '전표 저장 완료' });
      router.push('/journal/list');
    } catch (err: unknown) {
      showErrorToast('전표 생성 중 오류가 발생했습니다: ' + (err instanceof Error ? err.message : String(err)), { title: '저장 실패' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>전표 입력</h2>
          <p>새로운 분개 전표를 수동으로 생성합니다.</p>
        </div>
        <div className={styles.headerActions}>
          <Link href="/journal/list" className={styles.draftBtn}>목록으로</Link>
          <button 
            className={`${styles.saveBtn} ${(!isBalanced || isSubmitting) ? 'opacity-50 cursor-not-allowed pointer-events-none' : ''}`}
            onClick={handleSave}
            disabled={!isBalanced || isSubmitting}
            aria-busy={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                <span>저장 중...</span>
              </>
            ) : (
              <>
                <Save size={18} />
                <span>전표 확정</span>
              </>
            )}
          </button>
        </div>
      </header>

      {/* 전표 기본 정보 (Master) */}
      <section className={`glass-card ${styles.masterSection}`}>
        <div className={styles.formRow}>
          <div className={styles.inputGroup}>
            <label>전표 일자</label>
            <input 
              type="date" 
              value={slipDate} 
              onChange={e => setSlipDate(e.target.value)} 
            />
          </div>
          <div className={styles.inputGroup}>
            <label>기안 부서</label>
            <select value={departmentCode} onChange={e => setDepartmentCode(e.target.value)}>
              <option value="DEPT_FINANCE">재무팀</option>
              <option value="DEPT_HR">인사팀</option>
              <option value="DEPT_SALES">영업부</option>
            </select>
          </div>
          <div className={styles.inputGroup}>
            <label>전표 유형</label>
            <select value={entryType} onChange={e => setEntryType(e.target.value)}>
              <option value="MANUAL">일반 전표</option>
              <option value="CLOSING_ADJUSTMENT">결산 전표</option>
            </select>
          </div>
        </div>
        <div className={styles.formRow}>
          <div className={styles.inputFull}>
            <label>전표 적요 (설명)</label>
            <input 
              type="text" 
              placeholder="예: 4월 업무추진비 정산" 
              value={description}
              onChange={e => setDescription(e.target.value)}
            />
          </div>
        </div>
      </section>

      {/* 전표 행 입력 (Detail Grid) */}
      <section className={`glass-card ${styles.detailSection}`}>
        <div className={styles.cardHeader}>
          <h3>분개 상세 내역</h3>
          <button className={styles.addLineBtn} onClick={handleAddLine}><Plus size={16} /> 행 추가</button>
        </div>
        
        <table className={styles.detailTable}>
          <thead>
            <tr>
              <th>구분</th>
              <th>계정 과목</th>
              <th>차변 (Dr)</th>
              <th>대변 (Cr)</th>
              <th>적요</th>
              <th>삭제</th>
            </tr>
          </thead>
          <tbody>
            {details.map((row, idx) => (
              <tr key={idx}>
                <td>
                  <select 
                    value={row.side} 
                    onChange={e => handleDetailChange(idx, { side: e.target.value as JournalSide })}
                  >
                    <option value="DEBIT">차변</option>
                    <option value="CREDIT">대변</option>
                  </select>
                </td>
                <td>
                  <input 
                    type="text" 
                    placeholder="계정 코드 입력..." 
                    value={row.accountCode}
                    onChange={e => handleDetailChange(idx, { accountCode: e.target.value })}
                  />
                </td>
                <td>
                  <input 
                    type="number" 
                    placeholder="0" 
                    className={styles.numInput} 
                    disabled={row.side === 'CREDIT'}
                    value={row.side === 'DEBIT' ? (row.amount || '') : ''}
                    min="0.01"
                    step="0.01"
                    onChange={e => handleDetailChange(idx, { amount: Number(e.target.value) })}
                  />
                </td>
                <td>
                  <input 
                    type="number" 
                    placeholder="0" 
                    className={styles.numInput} 
                    disabled={row.side === 'DEBIT'}
                    value={row.side === 'CREDIT' ? (row.amount || '') : ''}
                    min="0.01"
                    step="0.01"
                    onChange={e => handleDetailChange(idx, { amount: Number(e.target.value) })}
                  />
                </td>
                <td>
                  <input 
                    type="text" 
                    placeholder="라인 적요..." 
                    value={row.detailDescription ?? ''}
                    onChange={e => handleDetailChange(idx, { detailDescription: e.target.value })}
                  />
                </td>
                <td>
                  <button className={styles.deleteBtn} onClick={() => handleRemoveLine(idx)}>
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* 대차 불일치 검증 바 */}
        <div className={styles.validationBar}>
          <div className={styles.valInfo}>
            <span>차변 합계: ₩{debitTotal.toLocaleString()}</span>
            <span>대변 합계: ₩{creditTotal.toLocaleString()}</span>
            <span className={isBalanced ? 'text-emerald-500 font-bold' : styles.diffText}>
              차액: ₩{Math.abs(debitTotal - creditTotal).toLocaleString()}
            </span>
          </div>
          {!isBalanced && (
            <div className={styles.valStatus}>
              <AlertCircle size={18} className={styles.warningIcon} />
              <span>대차가 일치하지 않습니다.</span>
            </div>
          )}
          {isBalanced && (
            <div className="flex items-center gap-2 text-emerald-500 font-bold bg-emerald-500/10 px-4 py-2 rounded-xl">
              <CheckCircle2 size={18} />
              <span>대차가 일치합니다. (검증 통과)</span>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
