"use client";

import React, { useState } from 'react';
import { 
  FileSpreadsheet, 
  Plus, 
  Trash2, 
  CheckCircle2, 
  AlertCircle, 
  ArrowRight, 
  Send, 
  Calculator, 
  BookOpen, 
  FileText,
  Search,
  Filter
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import EmptyState from '@/components/ui/EmptyState';

interface AdjustmentLine {
  id: string;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  description: string;
}

interface AdjustmentEntry {
  id: string;
  entryDate: string;
  type: '감가상각' | '선급비용' | '미지급비용' | '대손충당금' | '법인세추산';
  title: string;
  status: 'POSTED' | 'DRAFT' | 'APPROVED' | 'PENDING';
  totalAmount: number;
  createdBy: string;
  lines: AdjustmentLine[];
}

export default function ClosingAdjustmentPage() {
  const [entries, setEntries] = useState<AdjustmentEntry[]>([
    {
      id: 'ADJ-202607-001',
      entryDate: '2026-07-27',
      type: '감가상각',
      title: '2026년 7월 유형자산 감가상각비 계상 전표',
      status: 'POSTED',
      totalAmount: 18500000,
      createdBy: '박자산 대리',
      lines: [
        { id: 'L1', accountCode: '5200100', accountName: '감가상각비 (판관비)', debit: 18500000, credit: 0, description: '당월 컴퓨터 및 기계장치 감가상각비' },
        { id: 'L2', accountCode: '1420000', accountName: '감가상각누계액 (비품)', debit: 0, credit: 18500000, description: '당월 감가상각누계액 계상' },
      ],
    },
    {
      id: 'ADJ-202607-002',
      entryDate: '2026-07-28',
      type: '선급비용',
      title: '서버 클라우드 연간 구독료 기간 안분 조정',
      status: 'APPROVED',
      totalAmount: 3200000,
      createdBy: '최비용 사원',
      lines: [
        { id: 'L3', accountCode: '5300200', accountName: '지급수수료 (IT구독료)', debit: 3200000, credit: 0, description: 'AWS 7월분 사용 비용 계상' },
        { id: 'L4', accountCode: '1310000', accountName: '선급비용', debit: 0, credit: 3200000, description: '선급비용 차감 정산' },
      ],
    },
    {
      id: 'ADJ-202607-003',
      entryDate: '2026-07-28',
      type: '대손충당금',
      title: '기말 미수금 대손충당금 설정 전표',
      status: 'DRAFT',
      totalAmount: 5000000,
      createdBy: '김회계 과장',
      lines: [
        { id: 'L5', accountCode: '5400100', accountName: '대손상각비', debit: 5000000, credit: 0, description: '부실채권 1% 대손 충당' },
        { id: 'L6', accountCode: '1130000', accountName: '대손충당금', debit: 0, credit: 5000000, description: '대손충당금 설정' },
      ],
    },
  ]);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [entryTitle, setEntryTitle] = useState('');
  const [entryType, setEntryType] = useState<AdjustmentEntry['type']>('미지급비용');
  const [lines, setLines] = useState<AdjustmentLine[]>([
    { id: '1', accountCode: '5100100', accountName: '임차료 (본사)', debit: 12000000, credit: 0, description: '7월분 본사 사무실 임차료 계상' },
    { id: '2', accountCode: '2120000', accountName: '미지급비용', debit: 0, credit: 12000000, description: '7월분 임차료 미지급금 계상' },
  ]);

  const totalDebit = lines.reduce((acc, l) => acc + (Number(l.debit) || 0), 0);
  const totalCredit = lines.reduce((acc, l) => acc + (Number(l.credit) || 0), 0);
  const isBalanced = totalDebit === totalCredit && totalDebit > 0;

  const handleAddLine = () => {
    const newId = String(lines.length + 1);
    setLines([...lines, { id: newId, accountCode: '', accountName: '', debit: 0, credit: 0, description: '' }]);
  };

  const handleRemoveLine = (id: string) => {
    if (lines.length <= 2) return;
    setLines(lines.filter(l => l.id !== id));
  };

  const handleLineChange = (id: string, field: keyof AdjustmentLine, val: any) => {
    setLines(lines.map(l => l.id === id ? { ...l, [field]: val } : l));
  };

  const handleCreateEntry = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isBalanced || !entryTitle) return;

    const newEntry: AdjustmentEntry = {
      id: `ADJ-202607-${String(entries.length + 1).padStart(3, '0')}`,
      entryDate: new Date().toISOString().slice(0, 10),
      type: entryType,
      title: entryTitle,
      status: 'APPROVED',
      totalAmount: totalDebit,
      createdBy: '김회계 과장',
      lines: [...lines],
    };

    setEntries([newEntry, ...entries]);
    setIsModalOpen(false);
    setEntryTitle('');
  };

  const getStatusVariant = (status: AdjustmentEntry['status']) => {
    switch (status) {
      case 'POSTED': return 'success';
      case 'APPROVED': return 'info';
      case 'DRAFT': return 'warning';
      default: return 'neutral';
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="결산 조정 전표"
        description="월말/기말 결산 시 수익/비용 기간안분, 감가상각, 충당금 설정 등 결산 수정 전표를 생성 및 승인 관리합니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '결산 조정 전표' },
        ]}
        icon={FileSpreadsheet}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/20 transition-all"
          >
            <Plus size={18} />
            <span>신규 조정 전표 작성</span>
          </button>
        }
      />

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">당월 총 조정 전표</span>
          <p className="text-3xl font-black text-white">{entries.length} <span className="text-xs font-normal text-slate-500">건</span></p>
          <p className="text-xs text-blue-400 font-medium">자동/수동 결산 수정 합계</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">총 조정 금액 합계</span>
          <AmountDisplay amount={entries.reduce((a, b) => a + b.totalAmount, 0)} className="text-3xl" />
          <p className="text-xs text-slate-400">차변/대변 완전 균형 상태</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">포스팅 (원장 반영) 완료</span>
          <p className="text-3xl font-black text-emerald-400">
            {entries.filter(e => e.status === 'POSTED').length} <span className="text-xs font-normal text-slate-500">건</span>
          </p>
          <p className="text-xs text-emerald-400/80 font-medium">총계정원장 자동 전기됨</p>
        </div>
      </div>

      {/* Adjustment Journal Entries List */}
      <div className="space-y-4">
        <h3 className="text-xl font-black text-white italic">당월 결산 조정 전표 목록</h3>
        
        {entries.map((entry) => (
          <div key={entry.id} className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4 hover:border-white/10 transition-all">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-white/5">
              <div className="space-y-1">
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold text-blue-400 bg-blue-500/10 px-2.5 py-1 rounded-lg border border-blue-500/20">
                    {entry.id}
                  </span>
                  <span className="text-xs font-bold text-slate-300 bg-white/5 px-2.5 py-0.5 rounded-lg border border-white/5">
                    {entry.type}
                  </span>
                  <StatusBadge status={entry.status} variant={getStatusVariant(entry.status)} />
                </div>
                <h4 className="text-lg font-bold text-white mt-1">{entry.title}</h4>
              </div>

              <div className="flex items-center gap-6">
                <div className="text-right">
                  <span className="text-[10px] text-slate-500 font-bold block">전표 총액</span>
                  <AmountDisplay amount={entry.totalAmount} className="text-lg" />
                </div>
                <div className="text-right text-xs text-slate-400">
                  <span className="block font-medium">{entry.createdBy}</span>
                  <span className="font-mono text-[11px] text-slate-500">{entry.entryDate}</span>
                </div>
              </div>
            </div>

            {/* Lines Sub-table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="text-slate-500 text-[10px] font-black uppercase border-b border-white/5">
                    <th className="py-2 px-3">계정코드</th>
                    <th className="py-2 px-3">계정과목명</th>
                    <th className="py-2 px-3">적요</th>
                    <th className="py-2 px-3 text-right">차변 (Debit)</th>
                    <th className="py-2 px-3 text-right">대변 (Credit)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 font-medium">
                  {entry.lines.map((line) => (
                    <tr key={line.id}>
                      <td className="py-2.5 px-3 font-mono font-bold text-blue-400">{line.accountCode}</td>
                      <td className="py-2.5 px-3 text-white font-bold">{line.accountName}</td>
                      <td className="py-2.5 px-3 text-slate-400">{line.description}</td>
                      <td className="py-2.5 px-3 text-right font-mono">
                        {line.debit > 0 ? <AmountDisplay amount={line.debit} /> : <span className="text-slate-600">-</span>}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono">
                        {line.credit > 0 ? <AmountDisplay amount={line.credit} /> : <span className="text-slate-600">-</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))}
      </div>

      {/* New Adjustment Entry Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 w-full max-w-3xl rounded-3xl p-8 space-y-6 max-h-[90vh] overflow-y-auto animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-xl font-black text-white italic">신규 결산 조정 전표 작성</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">✕</button>
            </div>

            <form onSubmit={handleCreateEntry} className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400">조정 유형</label>
                  <select
                    value={entryType}
                    onChange={(e) => setEntryType(e.target.value as any)}
                    className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white outline-none"
                  >
                    <option value="감가상각" className="bg-slate-900">감가상각비 조정</option>
                    <option value="선급비용" className="bg-slate-900">선급비용 기간 안분</option>
                    <option value="미지급비용" className="bg-slate-900">미지급비용 계상</option>
                    <option value="대손충당금" className="bg-slate-900">대손충당금 설정</option>
                    <option value="법인세추산" className="bg-slate-900">법인세 추산전표</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400">전표 제목 / 적요 *</label>
                  <input
                    type="text"
                    required
                    placeholder="예: 7월분 미지급 임차료 결산 반영"
                    value={entryTitle}
                    onChange={(e) => setEntryTitle(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Lines table input */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-400">전표 분개 라인 (Journal Lines)</label>
                  <button
                    type="button"
                    onClick={handleAddLine}
                    className="text-xs font-bold text-blue-400 hover:text-blue-300 flex items-center gap-1"
                  >
                    <Plus size={14} /> 라인 추가
                  </button>
                </div>

                <div className="space-y-2">
                  {lines.map((l) => (
                    <div key={l.id} className="grid grid-cols-12 gap-2 items-center p-3 rounded-2xl bg-white/5 border border-white/5">
                      <div className="col-span-2">
                        <input
                          type="text"
                          placeholder="코드"
                          value={l.accountCode}
                          onChange={(e) => handleLineChange(l.id, 'accountCode', e.target.value)}
                          className="w-full p-2 rounded-lg bg-slate-950/50 border border-white/5 text-xs text-blue-400 font-mono"
                        />
                      </div>
                      <div className="col-span-3">
                        <input
                          type="text"
                          placeholder="계정 과목명"
                          value={l.accountName}
                          onChange={(e) => handleLineChange(l.id, 'accountName', e.target.value)}
                          className="w-full p-2 rounded-lg bg-slate-950/50 border border-white/5 text-xs text-white"
                        />
                      </div>
                      <div className="col-span-3">
                        <input
                          type="number"
                          placeholder="차변 (Debit)"
                          value={l.debit || ''}
                          onChange={(e) => handleLineChange(l.id, 'debit', Number(e.target.value))}
                          className="w-full p-2 rounded-lg bg-slate-950/50 border border-white/5 text-xs text-emerald-400 font-mono text-right"
                        />
                      </div>
                      <div className="col-span-3">
                        <input
                          type="number"
                          placeholder="대변 (Credit)"
                          value={l.credit || ''}
                          onChange={(e) => handleLineChange(l.id, 'credit', Number(e.target.value))}
                          className="w-full p-2 rounded-lg bg-slate-950/50 border border-white/5 text-xs text-indigo-400 font-mono text-right"
                        />
                      </div>
                      <div className="col-span-1 text-center">
                        <button
                          type="button"
                          onClick={() => handleRemoveLine(l.id)}
                          className="text-slate-500 hover:text-rose-400"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Balance validation bar */}
                <div className={`p-4 rounded-2xl border flex items-center justify-between text-xs font-bold ${
                  isBalanced ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-300' : 'bg-rose-500/10 border-rose-500/20 text-rose-300'
                }`}>
                  <div className="flex items-center gap-2">
                    <Calculator size={18} />
                    <span>차변 합계: ₩{totalDebit.toLocaleString()} | 대변 합계: ₩{totalCredit.toLocaleString()}</span>
                  </div>
                  <span>{isBalanced ? '✓ 차대변 일치 (Balanced)' : `⚠ 차액: ₩${Math.abs(totalDebit - totalCredit).toLocaleString()}원`}</span>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-white/5 text-slate-300 text-xs font-bold hover:bg-white/10"
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={!isBalanced || !entryTitle}
                  className="px-6 py-2.5 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-500 shadow-lg disabled:opacity-50"
                >
                  조정 전표 저장 및 발행
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
