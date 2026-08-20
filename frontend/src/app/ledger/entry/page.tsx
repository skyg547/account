'use client';

import React, { useState } from 'react';
import { 
  FilePlus, 
  Plus, 
  Trash2, 
  Save, 
  Send, 
  CheckCircle2, 
  RotateCcw,
  Calculator
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockAccounts, mockPartners } from '@/mocks/master';

interface JournalLineItem {
  id: string;
  accountCode: string;
  partnerId: string;
  desc: string;
  debit: number;
  credit: number;
}

export default function JournalEntryPage() {
  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [journalType, setJournalType] = useState<string>('GENERAL');
  const [memo, setMemo] = useState<string>('');

  const [lines, setLines] = useState<JournalLineItem[]>([
    { id: 'line-1', accountCode: '1110200', partnerId: 'PT-001', desc: '제품 매출 대금 입금', debit: 10000000, credit: 0 },
    { id: 'line-2', accountCode: '4000000', partnerId: 'PT-001', desc: '제품 매출 계상', debit: 0, credit: 10000000 },
  ]);

  const [submittedMessage, setSubmittedMessage] = useState<string | null>(null);

  const totalDebit = lines.reduce((sum, line) => sum + Number(line.debit || 0), 0);
  const totalCredit = lines.reduce((sum, line) => sum + Number(line.credit || 0), 0);
  const isBalanced = totalDebit === totalCredit && totalDebit > 0;

  const handleAddLine = () => {
    setLines((prev) => [
      ...prev,
      {
        id: `line-${Date.now()}`,
        accountCode: mockAccounts[0]?.code || '1110200',
        partnerId: '',
        desc: memo || '',
        debit: 0,
        credit: 0,
      },
    ]);
  };

  const handleRemoveLine = (id: string) => {
    if (lines.length <= 2) {
      alert('최소 2개의 분개 라인이 필요합니다.');
      return;
    }
    setLines((prev) => prev.filter((line) => line.id !== id));
  };

  const handleLineChange = (id: string, field: keyof JournalLineItem, value: string | number) => {
    setLines((prev) =>
      prev.map((line) => {
        if (line.id === id) {
          const updated = { ...line, [field]: value };
          // If editing debit, reset credit to 0 if updated > 0 to simplify single-side line
          if (field === 'debit' && Number(value) > 0) {
            updated.credit = 0;
          } else if (field === 'credit' && Number(value) > 0) {
            updated.debit = 0;
          }
          return updated;
        }
        return line;
      })
    );
  };

  const handleReset = () => {
    setLines([
      { id: 'line-1', accountCode: '1110200', partnerId: '', desc: '', debit: 0, credit: 0 },
      { id: 'line-2', accountCode: '4000000', partnerId: '', desc: '', debit: 0, credit: 0 },
    ]);
    setMemo('');
    setSubmittedMessage(null);
  };

  const handleSubmit = (actionType: 'SAVE' | 'APPROVE') => {
    if (!isBalanced) {
      alert('차변 금액과 대변 금액의 합계가 일치해야 합니다.');
      return;
    }
    const msg = actionType === 'SAVE' ? '전표가 임시 저장되었습니다.' : '전표가 결재 요청 되었습니다.';
    setSubmittedMessage(msg);
    setTimeout(() => setSubmittedMessage(null), 4000);
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="전표 입력"
        description="신규 일반전표 및 결산전표를 작성하고 차변/대변 라인을 등록합니다."
        breadcrumbs={[
          { label: '재무/회계' },
          { label: '전표관리' },
          { label: '전표 입력' }
        ]}
        icon={FilePlus}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleReset}
              className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white/5 hover:bg-white/10 text-slate-300 font-black text-xs transition-all border border-white/5"
            >
              <RotateCcw size={14} /> 초기화
            </button>
            <button
              onClick={() => handleSubmit('SAVE')}
              className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 text-white font-black text-xs transition-all border border-white/10 shadow-lg"
            >
              <Save size={14} /> 전표 임시저장
            </button>
            <button
              onClick={() => handleSubmit('APPROVE')}
              disabled={!isBalanced}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-2xl text-xs font-black transition-all shadow-lg ${
                isBalanced
                  ? 'bg-blue-600 hover:bg-blue-500 text-white shadow-blue-600/30'
                  : 'bg-slate-800 text-slate-500 cursor-not-allowed border border-white/5'
              }`}
            >
              <Send size={14} /> 결재 요청
            </button>
          </div>
        }
      />

      {submittedMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm font-bold flex items-center gap-3 animate-in fade-in">
          <CheckCircle2 size={18} />
          <span>{submittedMessage}</span>
        </div>
      )}

      {/* Journal Header Form */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl">
        <h3 className="text-lg font-black text-white tracking-tight mb-5 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-blue-500" />
          전표 헤더 정보
        </h3>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
              전표 일자
            </label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-2xl px-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors"
            />
          </div>

          <div>
            <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
              전표 유형
            </label>
            <select
              value={journalType}
              onChange={(e) => setJournalType(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-2xl px-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors"
            >
              <option value="GENERAL">일반 전표</option>
              <option value="CLOSING">결산 전표</option>
              <option value="ADJUSTMENT">수정/대체 전표</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
              전표 통합 적요
            </label>
            <input
              type="text"
              placeholder="예: 2025년 6월 1차 매출 및 부대 비용 전표"
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-2xl px-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors placeholder:text-slate-600"
            />
          </div>
        </div>
      </div>

      {/* Line Item Grid Header & Actions */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <Calculator size={20} className="text-blue-400" />
              분개 상세 라인 (Debit & Credit Lines)
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              차변(Debit)과 대변(Credit) 합계가 반드시 일치해야 전표 등록이 가능합니다.
            </p>
          </div>
          <button
            onClick={handleAddLine}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/30 font-black text-xs transition-all w-max"
          >
            <Plus size={16} /> 분개 라인 추가
          </button>
        </div>

        {/* Lines Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-3 w-12 text-center">No.</th>
                <th className="py-3 px-4 min-w-[200px]">계정과목</th>
                <th className="py-3 px-4 min-w-[180px]">거래처 (선택)</th>
                <th className="py-3 px-4 min-w-[220px]">적요</th>
                <th className="py-3 px-4 min-w-[160px] text-right">차변 금액 (Debit)</th>
                <th className="py-3 px-4 min-w-[160px] text-right">대변 금액 (Credit)</th>
                <th className="py-3 px-3 w-12 text-center">삭제</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {lines.map((line, index) => (
                <tr key={line.id} className="group hover:bg-white/[0.02] transition-colors">
                  <td className="py-3 px-3 text-center text-xs font-mono font-bold text-slate-500">
                    {index + 1}
                  </td>
                  <td className="py-3 px-4">
                    <select
                      value={line.accountCode}
                      onChange={(e) => handleLineChange(line.id, 'accountCode', e.target.value)}
                      className="w-full bg-slate-950/60 border border-white/10 rounded-xl px-3 py-2 text-white text-xs font-medium focus:outline-none focus:border-blue-500"
                    >
                      {mockAccounts.map((acc) => (
                        <option key={acc.code} value={acc.code}>
                          [{acc.code}] {acc.name} ({acc.balanceType === 'DEBIT' ? '차변' : '대변'})
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="py-3 px-4">
                    <select
                      value={line.partnerId}
                      onChange={(e) => handleLineChange(line.id, 'partnerId', e.target.value)}
                      className="w-full bg-slate-950/60 border border-white/10 rounded-xl px-3 py-2 text-white text-xs font-medium focus:outline-none focus:border-blue-500"
                    >
                      <option value="">(거래처 없음)</option>
                      {mockPartners.map((pt) => (
                        <option key={pt.id} value={pt.id}>
                          {pt.name} ({pt.id})
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="py-3 px-4">
                    <input
                      type="text"
                      placeholder="적요 입력"
                      value={line.desc}
                      onChange={(e) => handleLineChange(line.id, 'desc', e.target.value)}
                      className="w-full bg-slate-950/60 border border-white/10 rounded-xl px-3 py-2 text-white text-xs font-medium focus:outline-none focus:border-blue-500"
                    />
                  </td>
                  <td className="py-3 px-4 text-right">
                    <input
                      type="number"
                      min="0"
                      value={line.debit || ''}
                      onChange={(e) => handleLineChange(line.id, 'debit', Math.max(0, Number(e.target.value)))}
                      className="w-full bg-slate-950/60 border border-white/10 rounded-xl px-3 py-2 text-emerald-400 text-xs font-mono font-bold text-right focus:outline-none focus:border-blue-500"
                      placeholder="0"
                    />
                  </td>
                  <td className="py-3 px-4 text-right">
                    <input
                      type="number"
                      min="0"
                      value={line.credit || ''}
                      onChange={(e) => handleLineChange(line.id, 'credit', Math.max(0, Number(e.target.value)))}
                      className="w-full bg-slate-950/60 border border-white/10 rounded-xl px-3 py-2 text-blue-400 text-xs font-mono font-bold text-right focus:outline-none focus:border-blue-500"
                      placeholder="0"
                    />
                  </td>
                  <td className="py-3 px-3 text-center">
                    <button
                      onClick={() => handleRemoveLine(line.id)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="라인 삭제"
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Balance Status Footer */}
        <div className="pt-4 border-t border-white/10 flex flex-col md:flex-row items-center justify-between gap-6 bg-slate-950/40 p-6 rounded-2xl border border-white/5">
          <div className="flex items-center gap-3">
            {isBalanced ? (
              <StatusBadge status="대차 평형 (균형)" variant="success" className="py-1 px-3" />
            ) : (
              <StatusBadge status="대차 불일치 (차액 발생)" variant="error" className="py-1 px-3" />
            )}
            <span className="text-xs text-slate-400">
              {isBalanced ? '차변 금액과 대변 금액이 정확히 일치합니다.' : `차액: ₩${Math.abs(totalDebit - totalCredit).toLocaleString()}`}
            </span>
          </div>

          <div className="flex items-center gap-8 text-sm">
            <div>
              <span className="text-xs font-bold text-slate-400 mr-3 uppercase">Total Debit:</span>
              <AmountDisplay amount={totalDebit} className="text-emerald-400 text-lg" />
            </div>
            <div className="text-slate-600">|</div>
            <div>
              <span className="text-xs font-bold text-slate-400 mr-3 uppercase">Total Credit:</span>
              <AmountDisplay amount={totalCredit} className="text-blue-400 text-lg" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
