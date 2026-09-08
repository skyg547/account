'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  FileCheck,
  AlertTriangle,
  CheckCircle2,
  Search,
  Edit3,
  X,
  MessageSquare,
  Send
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockDiffs, ReconciliationDiffDto } from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function ReconciliationDiffPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [diffs, setDiffs] = useState<ReconciliationDiffDto[]>(mockDiffs);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Resolution Modal State
  const [selectedDiff, setSelectedDiff] = useState<ReconciliationDiffDto | null>(null);
  const [memoReason, setMemoReason] = useState<'시차 반영' | '환율 차이' | '수수료 미계상' | '시스템 오류' | '단수 차이'>('시차 반영');
  const [memoDetail, setMemoDetail] = useState('');

  const statusTabs: TabItem[] = [
    { id: 'ALL', label: '전체 차이 건' },
    { id: 'UNRESOLVED', label: '미해소 (UNRESOLVED)' },
    { id: 'IN_PROGRESS', label: '처리 중 (IN_PROGRESS)' },
    { id: 'RESOLVED', label: '해소 완료 (RESOLVED)' },
  ];

  const filteredDiffs = diffs.filter(d => {
    const matchesStatus = 
      selectedStatus === 'ALL' || 
      (selectedStatus === 'RESOLVED' ? (d.status === 'RESOLVED' || d.status === 'EXPLAINED') : d.status === selectedStatus);

    const matchesSearch = 
      d.ruleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.accountCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.accountName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.id.toLowerCase().includes(searchQuery.toLowerCase());

    return matchesStatus && matchesSearch;
  });

  const unresolvedCount = diffs.filter(d => d.status === 'UNRESOLVED').length;
  const criticalCount = diffs.filter(d => d.severity === 'CRITICAL' && d.status !== 'RESOLVED').length;
  const totalDiffAmount = diffs.reduce((sum, d) => sum + Math.abs(d.diffAmount), 0);
  const resolutionRate = ((diffs.filter(d => d.status === 'RESOLVED' || d.status === 'EXPLAINED').length / diffs.length) * 100).toFixed(0);

  const openResolutionModal = (diff: ReconciliationDiffDto) => {
    setSelectedDiff(diff);
    setMemoReason(diff.reasonCategory || '시차 반영');
    setMemoDetail(diff.resolutionMemo || '');
  };

  const handleSaveResolution = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDiff) return;

    setDiffs(diffs.map(d => {
      if (d.id === selectedDiff.id) {
        return {
          ...d,
          status: 'RESOLVED',
          reasonCategory: memoReason,
          resolutionMemo: memoDetail,
          resolvedBy: '강팀장 (회계팀)',
          resolvedAt: '2026-07-28 09:50:00',
        };
      }
      return d;
    }));

    alert(`[${selectedDiff.id}] 차이 항목에 대한 해소 승인 메모가 저장 및 해소 처리되었습니다.`);
    setSelectedDiff(null);
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'RESOLVED':
      case 'EXPLAINED': return 'success';
      case 'IN_PROGRESS': return 'warning';
      case 'UNRESOLVED': return 'error';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'RESOLVED': return '해소 완료';
      case 'EXPLAINED': return '사유 설명완료';
      case 'IN_PROGRESS': return '처리 진행중';
      case 'UNRESOLVED': return '미해소 (조치필요)';
      default: return status;
    }
  };

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return <span className="px-2 py-0.5 rounded text-[10px] font-black bg-rose-500/20 text-rose-400 border border-rose-500/30">CRITICAL</span>;
      case 'HIGH': return <span className="px-2 py-0.5 rounded text-[10px] font-black bg-amber-500/20 text-amber-400 border border-amber-500/30">HIGH</span>;
      case 'MEDIUM': return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-500/20 text-blue-400 border border-blue-500/30">MEDIUM</span>;
      default: return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-500/20 text-slate-400 border border-slate-500/30">LOW</span>;
    }
  };

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="차이 해소"
        description="원장/마트 대사 차액(Diff) 상세 내역 조회 및 사유 작성/해소 승인 (Resolution Memo)"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '대사 관리' },
          { label: '차이 해소' },
        ]}
        icon={FileCheck}
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">미해소 대사 차액 건수</div>
          <div className="text-3xl font-black text-rose-400 font-mono">{unresolvedCount} <span className="text-sm font-normal text-slate-400">건</span></div>
          <div className="text-xs text-rose-400 flex items-center gap-1">
            <AlertTriangle size={12} /> 조속한 결산 조정전표 반영 필요
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">긴급 (Critical) 심각도</div>
          <div className="text-3xl font-black text-white font-mono">{criticalCount} <span className="text-sm font-normal text-slate-400">건</span></div>
          <div className="text-xs text-amber-400">파생상품 공정가치 2.5억원 차액 건 포함</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">총 차액 절대값 금액</div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalDiffAmount} />
          </div>
          <div className="text-xs text-slate-500">전체 대사 규칙 합계</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">차이 해소 완료율</div>
          <div className="text-3xl font-black text-white font-mono">{resolutionRate}%</div>
          <div className="text-xs text-emerald-400 flex items-center gap-1">
            <CheckCircle2 size={12} /> 당월 결산 목표 (100%)
          </div>
        </div>
      </div>

      {/* Main Diff Table */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <Tabs tabs={statusTabs} activeTab={selectedStatus} onChange={setSelectedStatus} />

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="대사규칙 / 계정 / Diff ID 검색..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        {filteredDiffs.length === 0 ? (
          <EmptyState
            icon={CheckCircle2}
            title="조건에 해당하는 대사 차액이 없습니다"
            description="모든 대사 규칙이 정상 일치하거나 해소 처리 완료되었습니다."
          />
        ) : (
          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">Diff ID</th>
                  <th className="py-3.5 px-4">대사 규칙명</th>
                  <th className="py-3.5 px-4">계정과목</th>
                  <th className="py-3.5 px-4 text-right">소스 금액 ({filteredDiffs[0]?.sourceSystem})</th>
                  <th className="py-3.5 px-4 text-right">타겟 금액 ({filteredDiffs[0]?.targetSystem})</th>
                  <th className="py-3.5 px-4 text-right">차액 (Diff)</th>
                  <th className="py-3.5 px-4 text-center">심각도</th>
                  <th className="py-3.5 px-4 text-center">상태</th>
                  <th className="py-3.5 px-4 text-center">해소 사유 작성</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {filteredDiffs.map((diff) => (
                  <tr key={diff.id} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3.5 px-4 font-mono font-bold text-blue-400">{diff.id}</td>
                    <td className="py-3.5 px-4 font-bold text-white max-w-xs">{diff.ruleName}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-mono text-slate-300">{diff.accountCode}</div>
                      <div className="text-[11px] text-slate-500">{diff.accountName}</div>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">
                      <AmountDisplay amount={diff.sourceAmount} />
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">
                      <AmountDisplay amount={diff.targetAmount} />
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold">
                      <AmountDisplay amount={diff.diffAmount} showSign />
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      {getSeverityBadge(diff.severity)}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <StatusBadge 
                        status={getStatusLabel(diff.status)} 
                        variant={getStatusVariant(diff.status)} 
                      />
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <button
                        onClick={() => openResolutionModal(diff)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1.5 mx-auto ${
                          diff.status === 'RESOLVED' || diff.status === 'EXPLAINED'
                            ? 'bg-white/5 text-slate-400 hover:text-white border border-white/10'
                            : 'bg-blue-600 hover:bg-blue-500 text-white shadow'
                        }`}
                      >
                        <Edit3 size={12} />
                        {diff.status === 'RESOLVED' || diff.status === 'EXPLAINED' ? '메모 조회' : '해소 작성'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Resolution Memo Drawer / Modal */}
      {selectedDiff && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="w-full max-w-xl rounded-2xl bg-slate-900 border border-white/10 p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/5 pb-4">
              <div>
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <MessageSquare className="text-blue-400" size={20} />
                  차이 해소 승인 메모 (Resolution Memo)
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">[{selectedDiff.id}] {selectedDiff.ruleName}</p>
              </div>
              <button onClick={() => setSelectedDiff(null)} className="text-slate-400 hover:text-white">
                <X size={18} />
              </button>
            </div>

            {/* Comparison Box */}
            <div className="p-4 rounded-xl bg-slate-950/80 border border-white/5 space-y-3 font-mono text-xs">
              <div className="grid grid-cols-2 gap-4 border-b border-white/5 pb-3">
                <div>
                  <div className="text-slate-500 text-[10px] uppercase">{selectedDiff.sourceSystem} 원장 금액</div>
                  <div className="text-base font-black text-white mt-1">
                    <AmountDisplay amount={selectedDiff.sourceAmount} />
                  </div>
                </div>
                <div>
                  <div className="text-slate-500 text-[10px] uppercase">{selectedDiff.targetSystem} 마트 금액</div>
                  <div className="text-base font-black text-white mt-1">
                    <AmountDisplay amount={selectedDiff.targetAmount} />
                  </div>
                </div>
              </div>
              <div className="flex items-center justify-between pt-1">
                <span className="text-slate-400 font-sans font-bold">검출된 차액 (Difference):</span>
                <span className="text-base font-black text-rose-400">
                  <AmountDisplay amount={selectedDiff.diffAmount} showSign />
                </span>
              </div>
            </div>

            <form onSubmit={handleSaveResolution} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-400 font-bold mb-1.5">차이 발생 원인 분류 (Reason Category) *</label>
                <select
                  value={memoReason}
                  onChange={e => setMemoReason(e.target.value as '시차 반영' | '환율 차이' | '수수료 미계상' | '시스템 오류' | '단수 차이')}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-white font-medium focus:outline-none focus:border-blue-500"
                >
                  <option value="시차 반영" className="bg-slate-900">시차 반영 (Timing Difference)</option>
                  <option value="환율 차이" className="bg-slate-900">환율 평가 차이 (FX Difference)</option>
                  <option value="수수료 미계상" className="bg-slate-900">수수료 미계상 (Fee Unaccrued)</option>
                  <option value="시스템 오류" className="bg-slate-900">시스템 연동 오류 (System Bug)</option>
                  <option value="단수 차이" className="bg-slate-900">원화 소수점 단수 차이 (Rounding Diff)</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 font-bold mb-1.5">해소 상세 메모 및 조정 전표 참조 *</label>
                <textarea
                  required
                  rows={4}
                  placeholder="차이 원인 분석 결과 및 해소 조치 내역(조정 전표 번호 등)을 기술하세요..."
                  value={memoDetail}
                  onChange={e => setMemoDetail(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl p-3.5 text-white placeholder:text-slate-600 focus:outline-none focus:border-blue-500"
                />
              </div>

              {selectedDiff.resolvedBy && (
                <div className="text-[11px] text-slate-500 border-t border-white/5 pt-2">
                  최종 승인자: <span className="text-slate-300 font-bold">{selectedDiff.resolvedBy}</span> ({selectedDiff.resolvedAt})
                </div>
              )}

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/5">
                <button
                  type="button"
                  onClick={() => setSelectedDiff(null)}
                  className="px-4 py-2 rounded-xl bg-white/5 text-slate-400 hover:text-white font-bold"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold shadow-lg shadow-blue-600/20 flex items-center gap-1.5"
                >
                  <Send size={13} />
                  해소 승인 처리 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
