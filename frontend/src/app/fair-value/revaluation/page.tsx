"use client";

import React, { useState, useMemo } from 'react';
import { 
  Scale, 
  Plus, 
  Search, 
  CheckCircle2, 
  XCircle, 
  TrendingUp, 
  TrendingDown, 
  Building, 
  FileCheck2,
  ShieldCheck,
  AlertOctagon
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockAssets, mockRevaluations, AssetRevaluationDto } from '@/mocks/fair-value';

export default function AssetRevaluationPage() {
  const [revaluations, setRevaluations] = useState<AssetRevaluationDto[]>(mockRevaluations);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Form state
  const [selectedAssetId, setSelectedAssetId] = useState<string>(mockAssets[0]?.id || '');
  const [valuationType, setValuationType] = useState<'REVALUATION' | 'IMPAIRMENT' | 'REVERSAL'>('REVALUATION');
  const [valuationDate, setValuationDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [fairMarketValue, setFairMarketValue] = useState<number>(0);
  const [appraiser, setAppraiser] = useState<string>('삼일회계법인');
  const [reason, setReason] = useState<string>('');

  const activeTargetAsset = useMemo(() => {
    return mockAssets.find(a => a.id === selectedAssetId) || mockAssets[0];
  }, [selectedAssetId]);

  // Auto calculate valuation gain / loss
  const calculatedValuationGainLoss = useMemo(() => {
    if (!activeTargetAsset) return 0;
    return fairMarketValue - activeTargetAsset.bookValue;
  }, [activeTargetAsset, fairMarketValue]);

  const typeTabs: TabItem[] = [
    { id: 'ALL', label: '전체 평가건' },
    { id: 'REVALUATION', label: '공정가치 재평가' },
    { id: 'IMPAIRMENT', label: '손상 차손 (Impairment)' },
    { id: 'REVERSAL', label: '손상차손 환입 (Reversal)' },
  ];

  const filteredRevaluations = useMemo(() => {
    return revaluations.filter(r => {
      const matchSearch = 
        r.assetName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.assetCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.revaluationNo.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.appraiser.toLowerCase().includes(searchTerm.toLowerCase());
      const matchType = selectedType === 'ALL' || r.type === selectedType;
      return matchSearch && matchType;
    });
  }, [revaluations, searchTerm, selectedType]);

  // KPI
  const totalRevaluationGain = useMemo(() => {
    return revaluations.filter(r => r.type === 'REVALUATION' && r.approvalStatus === 'APPROVED')
      .reduce((sum, r) => sum + r.valuationGainLoss, 0);
  }, [revaluations]);

  const totalImpairmentLoss = useMemo(() => {
    return revaluations.filter(r => r.type === 'IMPAIRMENT' && r.approvalStatus === 'APPROVED')
      .reduce((sum, r) => sum + r.valuationGainLoss, 0);
  }, [revaluations]);

  const handleRegisterRevaluation = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTargetAsset || !reason.trim() || fairMarketValue <= 0) return;

    const newRev: AssetRevaluationDto = {
      id: `reval-${Date.now()}`,
      revaluationNo: `${valuationType === 'REVALUATION' ? 'REV' : 'IMP'}-2026-${String(revaluations.length + 1).padStart(3, '0')}`,
      assetId: activeTargetAsset.id,
      assetCode: activeTargetAsset.assetCode,
      assetName: activeTargetAsset.assetName,
      valuationDate,
      type: valuationType,
      preValuationBookValue: activeTargetAsset.bookValue,
      fairMarketValue: Number(fairMarketValue),
      valuationGainLoss: calculatedValuationGainLoss,
      appraiser,
      approvalStatus: 'PENDING',
      reason,
    };

    setRevaluations(prev => [newRev, ...prev]);
    setIsModalOpen(false);
    setReason('');
    setFairMarketValue(0);
    setToastMessage(`[${newRev.revaluationNo}] 자산 공정가치 재평가/손상 평가건이 등록되었습니다.`);
    setTimeout(() => setToastMessage(null), 4000);
  };

  const handleApprove = (id: string) => {
    setRevaluations(prev => prev.map(r => {
      if (r.id === id) {
        return {
          ...r,
          approvalStatus: 'APPROVED',
        };
      }
      return r;
    }));
    setToastMessage('선택된 자산 재평가/손상건이 승인 완료되어 기타포괄손익/기타비용에 반영됩니다.');
    setTimeout(() => setToastMessage(null), 3500);
  };

  const getTypeLabel = (type: AssetRevaluationDto['type']) => {
    switch (type) {
      case 'REVALUATION': return '자산 재평가';
      case 'IMPAIRMENT': return '손상 차손';
      case 'REVERSAL': return '손상 환입';
      default: return type;
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
        title="자산 재평가/손상"
        description="K-IFRS/일반회계기준에 의거한 유형·무형 자산의 공정가치 감정평가, 손상차손 검토 및 손상환입을 관리합니다."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: '자산 재평가/손상' },
        ]}
        icon={Scale}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2"
          >
            <Plus size={16} />
            <span>재평가 / 손상 평가 등록</span>
          </button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">누적 재평가 잉여금 (OCI)</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <TrendingUp size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={totalRevaluationGain} showSign />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">자목별 공정가치 평가 차익</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">인식된 손상차손 (당기손익)</span>
            <div className="w-9 h-9 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
              <AlertOctagon size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-rose-400">
            <AmountDisplay amount={totalImpairmentLoss} showSign />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">자산가치 하락에 따른 비용 반영</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">평가 완료 자산</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <FileCheck2 size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white font-mono">{revaluations.length} <span className="text-xs text-slate-500">건</span></div>
          <p className="text-[11px] text-slate-500 font-medium">감정평가서 등재 완료 이력</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">제휴 감정평가기관</span>
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Building size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white font-mono">3 <span className="text-xs text-slate-500">개 기관</span></div>
          <p className="text-[11px] text-slate-500 font-medium">한국감정평가, 삼일회계 등</p>
        </div>
      </div>

      {/* Main Table Section */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={typeTabs} activeTab={selectedType} onChange={setSelectedType} />

          <div className="relative min-w-[240px]">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="평가번호, 자산명, 감정평가법인..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          {filteredRevaluations.length === 0 ? (
            <EmptyState
              icon={Scale}
              title="평가 내역이 없습니다."
              description="선택한 유형 또는 검색어에 해당하는 재평가/손상 이력이 없습니다."
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">평가 번호</th>
                  <th className="py-3 px-4">자산코드 / 자산명</th>
                  <th className="py-3 px-4">평가 구분</th>
                  <th className="py-3 px-4">평가 일자</th>
                  <th className="py-3 px-4 text-right">평가전 장부가</th>
                  <th className="py-3 px-4 text-right">공정가치 (평가액)</th>
                  <th className="py-3 px-4 text-right">평가 차손익</th>
                  <th className="py-3 px-4">감정 평가 기관</th>
                  <th className="py-3 px-4">상태</th>
                  <th className="py-3 px-4 text-center">액션</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs font-medium">
                {filteredRevaluations.map((rev) => (
                  <tr key={rev.id} className="hover:bg-white/5 transition-all">
                    <td className="py-4 px-4 font-mono font-bold text-blue-400">{rev.revaluationNo}</td>
                    <td className="py-4 px-4">
                      <p className="font-bold text-white">{rev.assetName}</p>
                      <p className="text-[11px] font-mono text-slate-500">{rev.assetCode}</p>
                    </td>
                    <td className="py-4 px-4">
                      <span className={`px-2.5 py-1 rounded-lg font-bold border ${
                        rev.type === 'REVALUATION' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      }`}>
                        {getTypeLabel(rev.type)}
                      </span>
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-300">{rev.valuationDate}</td>
                    <td className="py-4 px-4 text-right"><AmountDisplay amount={rev.preValuationBookValue} /></td>
                    <td className="py-4 px-4 text-right"><AmountDisplay amount={rev.fairMarketValue} className="text-white font-bold" /></td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay 
                        amount={rev.valuationGainLoss} 
                        showSign 
                        className={rev.valuationGainLoss >= 0 ? 'text-emerald-400' : 'text-rose-400'} 
                      />
                    </td>
                    <td className="py-4 px-4 text-slate-300">{rev.appraiser}</td>
                    <td className="py-4 px-4">
                      <StatusBadge
                        status={rev.approvalStatus === 'APPROVED' ? '승인 확정' : '검토 중'}
                        variant={rev.approvalStatus === 'APPROVED' ? 'success' : 'warning'}
                      />
                    </td>
                    <td className="py-4 px-4 text-center">
                      {rev.approvalStatus === 'PENDING' ? (
                        <button
                          onClick={() => handleApprove(rev.id)}
                          className="px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600 border border-emerald-500/30 text-emerald-300 hover:text-white font-bold transition-all text-xs"
                        >
                          승인
                        </button>
                      ) : (
                        <span className="text-[11px] text-slate-500 italic">완료</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-lg font-black text-white italic">자산 공정가치 재평가 / 손상 등록</h3>
              <button onClick={() => setIsModalOpen(false)} className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10">
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleRegisterRevaluation} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="font-bold text-slate-400">평가 대상 자산 선택 *</label>
                <select
                  value={selectedAssetId}
                  onChange={(e) => setSelectedAssetId(e.target.value)}
                  className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                >
                  {mockAssets.map(asset => (
                    <option key={asset.id} value={asset.id}>
                      [{asset.assetCode}] {asset.assetName} (현재 장부가: ₩{asset.bookValue.toLocaleString()})
                    </option>
                  ))}
                </select>
              </div>

              {activeTargetAsset && (
                <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between">
                  <span className="text-slate-400">평가 전 장부가액:</span>
                  <span className="font-bold text-white text-sm">₩{activeTargetAsset.bookValue.toLocaleString()}</span>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">평가 구분</label>
                  <select
                    value={valuationType}
                    onChange={(e) => setValuationType(e.target.value as any)}
                    className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                  >
                    <option value="REVALUATION">공정가치 재평가 (Revaluation)</option>
                    <option value="IMPAIRMENT">손상 차손 (Impairment)</option>
                    <option value="REVERSAL">손상차손 환입 (Reversal)</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">평가 일자</label>
                  <input
                    type="date"
                    value={valuationDate}
                    onChange={(e) => setValuationDate(e.target.value)}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">공정가치 / 감정평가액 (원) *</label>
                  <input
                    type="number"
                    min={1}
                    required
                    value={fairMarketValue || ''}
                    onChange={(e) => setFairMarketValue(Number(e.target.value))}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">감정평가기관 / 법인</label>
                  <input
                    type="text"
                    value={appraiser}
                    onChange={(e) => setAppraiser(e.target.value)}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-slate-950 border border-white/10 flex items-center justify-between">
                <span className="font-bold text-slate-400">예상 평가 차손익:</span>
                <span className={`text-base font-black font-mono ${calculatedValuationGainLoss >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {calculatedValuationGainLoss >= 0 ? '+' : ''}₩{calculatedValuationGainLoss.toLocaleString()}
                </span>
              </div>

              <div className="space-y-1">
                <label className="font-bold text-slate-400">평가 사유 및 근거 첨부 *</label>
                <textarea
                  rows={3}
                  required
                  placeholder="K-IFRS 평가모형 적용 또는 외부 감정평가 보고서 주요내용 기재"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 resize-none"
                />
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
                  평가 등록 제출
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
