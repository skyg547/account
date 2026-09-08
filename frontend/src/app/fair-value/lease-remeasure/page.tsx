"use client";

import React, { useState, useMemo } from 'react';
import {
  GitBranch,
  Plus,
  Search,
  CheckCircle2,
  XCircle,
  TrendingUp,
  TrendingDown,
  FileText,
  Calculator
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockLeases, mockLeaseRemeasures, LeaseRemeasureDto } from '@/mocks/fair-value';

export default function LeaseRemeasurementPage() {
  const [remeasures, setRemeasures] = useState<LeaseRemeasureDto[]>(mockLeaseRemeasures);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedReason, setSelectedReason] = useState<string>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Form State
  const [selectedContractId, setSelectedContractId] = useState<string>(mockLeases[0]?.id || '');
  const [changeReason, setChangeReason] = useState<'OPTION_EXERCISE' | 'RATE_CHANGE' | 'PAYMENT_MODIFICATION' | 'TERM_EXTENSION'>('TERM_EXTENSION');
  const [remeasureDate, setRemeasureDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [newDiscountRate, setNewDiscountRate] = useState<number>(4.8);
  const [postLiability, setPostLiability] = useState<number>(0);

  const activeContract = useMemo(() => {
    return mockLeases.find(l => l.id === selectedContractId) || mockLeases[0];
  }, [selectedContractId]);

  // Calculated adjustment amount
  const adjustmentAmount = useMemo(() => {
    if (!activeContract || postLiability <= 0) return 0;
    return postLiability - activeContract.leaseLiability;
  }, [activeContract, postLiability]);

  const reasonTabs: TabItem[] = [
    { id: 'ALL', label: '전체 변동사유' },
    { id: 'TERM_EXTENSION', label: '기간 연장 (Term)' },
    { id: 'PAYMENT_MODIFICATION', label: '리스료 변경 (Payment)' },
    { id: 'RATE_CHANGE', label: '할인율 변동 (Rate)' },
    { id: 'OPTION_EXERCISE', label: '옵션 행사 (Option)' },
  ];

  const filteredRemeasures = useMemo(() => {
    return remeasures.filter(r => {
      const matchSearch = 
        r.contractName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.contractNo.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.remeasureNo.toLowerCase().includes(searchTerm.toLowerCase());
      const matchReason = selectedReason === 'ALL' || r.changeReason === selectedReason;
      return matchSearch && matchReason;
    });
  }, [remeasures, searchTerm, selectedReason]);

  // KPI
  const totalAdjustments = useMemo(() => remeasures.reduce((sum, r) => sum + r.adjustmentAmount, 0), [remeasures]);
  const pendingCount = useMemo(() => remeasures.filter(r => r.status === 'PENDING').length, [remeasures]);

  const handleRegisterRemeasure = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeContract || postLiability <= 0) return;

    const newRem: LeaseRemeasureDto = {
      id: `lsr-${Date.now()}`,
      remeasureNo: `REM-2026-${String(remeasures.length + 1).padStart(3, '0')}`,
      contractId: activeContract.id,
      contractNo: activeContract.contractNo,
      contractName: activeContract.contractName,
      remeasureDate,
      changeReason,
      preLiability: activeContract.leaseLiability,
      postLiability: Number(postLiability),
      adjustmentAmount,
      preRouAsset: activeContract.rouAssetValue,
      postRouAsset: activeContract.rouAssetValue + adjustmentAmount,
      newDiscountRate: Number(newDiscountRate),
      status: 'PENDING',
    };

    setRemeasures(prev => [newRem, ...prev]);
    setIsModalOpen(false);
    setPostLiability(0);
    setToastMessage(`[${newRem.remeasureNo}] 리스 재측정 조정 신청이 등록되었습니다 (부채 조정 ₩${adjustmentAmount.toLocaleString()}).`);
    setTimeout(() => setToastMessage(null), 5000);
  };

  const handleApprove = (id: string) => {
    setRemeasures(prev => prev.map(r => {
      if (r.id === id) {
        return {
          ...r,
          status: 'APPROVED',
          approvalDate: new Date().toISOString().split('T')[0],
        };
      }
      return r;
    }));
    setToastMessage('리스 재측정이 최종 승인되어 사용권자산 및 리스부채가 수정 계상되었습니다.');
    setTimeout(() => setToastMessage(null), 4000);
  };

  const getReasonLabel = (reason: LeaseRemeasureDto['changeReason']) => {
    switch (reason) {
      case 'TERM_EXTENSION': return '리스기간 연장';
      case 'PAYMENT_MODIFICATION': return '리스료 변경';
      case 'RATE_CHANGE': return '할인율 변동';
      case 'OPTION_EXERCISE': return '옵션 행사';
      default: return reason;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm font-bold flex items-center gap-3 animate-in fade-in slide-in-from-top-2">
          <CheckCircle2 size={20} className="text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Page Header */}
      <PageHeader
        title="리스 재측정"
        description="IFRS 16 규정에 따른 계약 변동(기간 연장, 리스료 조정, 할인율 변경)에 따른 리스부채 및 사용권자산 재측정."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: '리스 재측정' },
        ]}
        icon={GitBranch}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2"
          >
            <Plus size={16} />
            <span>신규 리스 재측정 신청</span>
          </button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">누적 리스 재측정 건수</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <FileText size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white font-mono">{remeasures.length} <span className="text-xs text-slate-500">건</span></div>
          <p className="text-[11px] text-slate-500 font-medium">계약 변경 및 조건 수정 처리</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">리스부채 조정 순변동액</span>
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              {totalAdjustments >= 0 ? <TrendingUp size={18} /> : <TrendingDown size={18} />}
            </div>
          </div>
          <div className="text-2xl font-black">
            <AmountDisplay amount={totalAdjustments} showSign />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">재측정 증액(+) 및 감액(-)</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">승인 대기 재측정</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Calculator size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-amber-400 font-mono">{pendingCount} <span className="text-xs text-slate-500">건</span></div>
          <p className="text-[11px] text-slate-500 font-medium">회계검토 및 최종 승인 대기</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">주요 변동 사유</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <GitBranch size={18} />
            </div>
          </div>
          <div className="text-xl font-black text-white italic">기간 연장 & 리스료</div>
          <p className="text-[11px] text-slate-500 font-medium">임대차 계약 갱신 위주</p>
        </div>
      </div>

      {/* Main Filter & Table */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={reasonTabs} activeTab={selectedReason} onChange={setSelectedReason} />

          <div className="relative min-w-[240px]">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="재측정번호, 계약명, 계약번호..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          {filteredRemeasures.length === 0 ? (
            <EmptyState
              icon={GitBranch}
              title="재측정 이력이 존재하지 않습니다."
              description="선택한 변동사유 또는 검색어에 해당하는 리스 재측정 내역이 없습니다."
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">재측정 번호</th>
                  <th className="py-3 px-4">리스 계약명</th>
                  <th className="py-3 px-4">변동 사유</th>
                  <th className="py-3 px-4">재측정 일자</th>
                  <th className="py-3 px-4 text-right">기존 리스부채</th>
                  <th className="py-3 px-4 text-right">변경후 리스부채</th>
                  <th className="py-3 px-4 text-right">조정 금액</th>
                  <th className="py-3 px-4 text-center">신규 할인율</th>
                  <th className="py-3 px-4">상태</th>
                  <th className="py-3 px-4 text-center">액션</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs font-medium">
                {filteredRemeasures.map((rem) => (
                  <tr key={rem.id} className="hover:bg-white/5 transition-all">
                    <td className="py-4 px-4 font-mono font-bold text-blue-400">{rem.remeasureNo}</td>
                    <td className="py-4 px-4">
                      <p className="font-bold text-white">{rem.contractName}</p>
                      <p className="text-[11px] font-mono text-slate-500">{rem.contractNo}</p>
                    </td>
                    <td className="py-4 px-4">
                      <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-slate-300 font-bold">
                        {getReasonLabel(rem.changeReason)}
                      </span>
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-300">{rem.remeasureDate}</td>
                    <td className="py-4 px-4 text-right"><AmountDisplay amount={rem.preLiability} /></td>
                    <td className="py-4 px-4 text-right"><AmountDisplay amount={rem.postLiability} className="text-white font-bold" /></td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay 
                        amount={rem.adjustmentAmount} 
                        showSign 
                        className={rem.adjustmentAmount >= 0 ? 'text-emerald-400' : 'text-rose-400'} 
                      />
                    </td>
                    <td className="py-4 px-4 text-center font-mono font-bold text-purple-400">
                      {rem.newDiscountRate}%
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge
                        status={rem.status === 'APPROVED' ? '승인 완료' : '결재 대기'}
                        variant={rem.status === 'APPROVED' ? 'success' : 'warning'}
                      />
                    </td>
                    <td className="py-4 px-4 text-center">
                      {rem.status === 'PENDING' ? (
                        <button
                          onClick={() => handleApprove(rem.id)}
                          className="px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600 border border-emerald-500/30 text-emerald-300 hover:text-white font-bold transition-all text-xs"
                        >
                          승인
                        </button>
                      ) : (
                        <span className="text-[11px] text-slate-500 italic">{rem.approvalDate || '완료'}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Remeasurement Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-lg font-black text-white italic">IFRS 16 리스 계약 재측정 신청</h3>
              <button onClick={() => setIsModalOpen(false)} className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10">
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleRegisterRemeasure} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="font-bold text-slate-400">변동 대상 리스계약 선택 *</label>
                <select
                  value={selectedContractId}
                  onChange={(e) => setSelectedContractId(e.target.value)}
                  className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                >
                  {mockLeases.map(lease => (
                    <option key={lease.id} value={lease.id}>
                      [{lease.contractNo}] {lease.contractName} (현재 부채: ₩{lease.leaseLiability.toLocaleString()})
                    </option>
                  ))}
                </select>
              </div>

              {activeContract && (
                <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 grid grid-cols-2 gap-2">
                  <div>
                    <span className="text-[10px] text-slate-500 block">기존 리스부채</span>
                    <span className="font-bold text-amber-400">₩{activeContract.leaseLiability.toLocaleString()}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block">기존 사용권자산</span>
                    <span className="font-bold text-white">₩{activeContract.rouAssetValue.toLocaleString()}</span>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">계약 변동 사유</label>
                  <select
                    value={changeReason}
                    onChange={(e) => setChangeReason(e.target.value as LeaseRemeasureDto['changeReason'])}
                    className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                  >
                    <option value="TERM_EXTENSION">리스기간 연장 (Term Extension)</option>
                    <option value="PAYMENT_MODIFICATION">리스료 변경 (Payment Change)</option>
                    <option value="RATE_CHANGE">할인율 변동 (Rate Change)</option>
                    <option value="OPTION_EXERCISE">매수/연장 옵션 행사</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">재측정 기준일자</label>
                  <input
                    type="date"
                    value={remeasureDate}
                    onChange={(e) => setRemeasureDate(e.target.value)}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">재측정 후 리스부채 (현재가치) *</label>
                  <input
                    type="number"
                    min={1}
                    required
                    value={postLiability || ''}
                    onChange={(e) => setPostLiability(Number(e.target.value))}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">변경 적용 할인율 (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={newDiscountRate}
                    onChange={(e) => setNewDiscountRate(Number(e.target.value))}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-slate-950 border border-white/10 flex items-center justify-between">
                <span className="font-bold text-slate-400">리스부채/사용권자산 조정액:</span>
                <span className={`text-base font-black font-mono ${adjustmentAmount >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {adjustmentAmount >= 0 ? '+' : ''}₩{adjustmentAmount.toLocaleString()}
                </span>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 font-bold"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold shadow-lg shadow-blue-600/20"
                >
                  재측정 신청 제출
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
