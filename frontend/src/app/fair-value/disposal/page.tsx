"use client";

import React, { useState, useMemo } from 'react';
import { 
  Trash2, 
  Plus, 
  Search, 
  CheckCircle2, 
  XCircle, 
  TrendingUp, 
  TrendingDown, 
  DollarSign,
  AlertTriangle,
  FileCheck
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockAssets, mockDisposals, AssetDisposalDto } from '@/mocks/fair-value';

export default function AssetDisposalPage() {
  const [disposals, setDisposals] = useState<AssetDisposalDto[]>(mockDisposals);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Form State
  const [selectedAssetId, setSelectedAssetId] = useState<string>(mockAssets[0]?.id || '');
  const [disposalType, setDisposalType] = useState<'SALE' | 'SCRAP' | 'DONATION' | 'LOSS'>('SALE');
  const [disposalDate, setDisposalDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [saleAmount, setSaleAmount] = useState<number>(0);
  const [reason, setReason] = useState<string>('');

  const activeTargetAsset = useMemo(() => {
    return mockAssets.find(a => a.id === selectedAssetId) || mockAssets[0];
  }, [selectedAssetId]);

  // Auto calculate gain / loss
  const calculatedGainLoss = useMemo(() => {
    if (!activeTargetAsset) return 0;
    if (disposalType === 'SCRAP' || disposalType === 'LOSS') {
      return -activeTargetAsset.bookValue;
    }
    return saleAmount - activeTargetAsset.bookValue;
  }, [activeTargetAsset, disposalType, saleAmount]);

  const typeTabs: TabItem[] = [
    { id: 'ALL', label: '전체 처분건' },
    { id: 'SALE', label: '매각 (Sale)' },
    { id: 'SCRAP', label: '폐기 (Scrap)' },
    { id: 'DONATION', label: '증여 (Donation)' },
    { id: 'LOSS', label: '손실 (Loss)' },
  ];

  const filteredDisposals = useMemo(() => {
    return disposals.filter(d => {
      const matchSearch = 
        d.assetName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        d.assetCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
        d.disposalNo.toLowerCase().includes(searchTerm.toLowerCase());
      const matchType = selectedType === 'ALL' || d.disposalType === selectedType;
      return matchSearch && matchType;
    });
  }, [disposals, searchTerm, selectedType]);

  // KPI
  const totalSaleAmount = useMemo(() => disposals.reduce((sum, d) => sum + d.saleAmount, 0), [disposals]);
  const totalGainLoss = useMemo(() => disposals.reduce((sum, d) => sum + d.gainLossAmount, 0), [disposals]);
  const pendingCount = useMemo(() => disposals.filter(d => d.approvalStatus === 'PENDING').length, [disposals]);

  const handleRegisterDisposal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTargetAsset || !reason.trim()) return;

    const newDisposal: AssetDisposalDto = {
      id: `disp-${Date.now()}`,
      disposalNo: `DISP-2026-${String(disposals.length + 1).padStart(3, '0')}`,
      assetId: activeTargetAsset.id,
      assetCode: activeTargetAsset.assetCode,
      assetName: activeTargetAsset.assetName,
      disposalDate,
      disposalType,
      acquisitionCost: activeTargetAsset.acquisitionCost,
      accumulatedDepreciation: activeTargetAsset.accumulatedDepreciation,
      bookValue: activeTargetAsset.bookValue,
      saleAmount: disposalType === 'SALE' ? Number(saleAmount) : 0,
      gainLossAmount: calculatedGainLoss,
      approvalStatus: 'PENDING',
      reason,
    };

    setDisposals(prev => [newDisposal, ...prev]);
    setIsModalOpen(false);
    setReason('');
    setSaleAmount(0);
    setToastMessage(`[${newDisposal.disposalNo}] ${newDisposal.assetName} 자산 처분 신청이 등록되었습니다 (결재 대기).`);
    setTimeout(() => setToastMessage(null), 4000);
  };

  const handleApprove = (id: string) => {
    setDisposals(prev => prev.map(d => {
      if (d.id === id) {
        return {
          ...d,
          approvalStatus: 'APPROVED',
          approvedBy: '강남구 본부장',
        };
      }
      return d;
    }));
    setToastMessage('선택한 자산 처분건이 최종 승인 처리되었습니다.');
    setTimeout(() => setToastMessage(null), 3000);
  };

  const getTypeLabel = (type: AssetDisposalDto['disposalType']) => {
    switch (type) {
      case 'SALE': return '매각';
      case 'SCRAP': return '폐기';
      case 'DONATION': return '증여';
      case 'LOSS': return '손실';
      default: return type;
    }
  };

  const getApprovalVariant = (status: AssetDisposalDto['approvalStatus']) => {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'PENDING': return 'warning';
      case 'REJECTED': return 'error';
      default: return 'neutral';
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
        title="자산 처분"
        description="불용, 고장, 매각 대상 고정자산의 처분 신청, 처분가액 산정, 처분손익 인식 및 승인 이력을 관리합니다."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: '자산 처분' },
        ]}
        icon={Trash2}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2"
          >
            <Plus size={16} />
            <span>신규 자산 처분 신청</span>
          </button>
        }
      />

      {/* KPI Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">총 처분 신청 건수</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <FileCheck size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white font-mono">{disposals.length} <span className="text-xs text-slate-500">건</span></div>
          <p className="text-[11px] text-slate-500 font-medium">올해 매각/폐기 처리 전체</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">총 처분(매각)가액</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <DollarSign size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={totalSaleAmount} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">자산 매각에 따른 수취 금액</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">처분손익 합계</span>
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              {totalGainLoss >= 0 ? <TrendingUp size={18} /> : <TrendingDown size={18} />}
            </div>
          </div>
          <div className="text-2xl font-black">
            <AmountDisplay amount={totalGainLoss} showSign />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">자산처분이익(+) 및 자산처분손실(-)</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">결재 승인 대기</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <AlertTriangle size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-amber-400 font-mono">{pendingCount} <span className="text-xs text-slate-500">건</span></div>
          <p className="text-[11px] text-slate-500 font-medium">임원 결재 대기 중인 처분건</p>
        </div>
      </div>

      {/* Main Filter & Table */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={typeTabs} activeTab={selectedType} onChange={setSelectedType} />

          <div className="relative min-w-[240px]">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="처분번호, 자산명, 자산코드..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          {filteredDisposals.length === 0 ? (
            <EmptyState
              icon={Trash2}
              title="처분 내역이 없습니다."
              description="선택한 구분 또는 검색어에 해당하는 처분 기록이 존재하지 않습니다."
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">처분 번호</th>
                  <th className="py-3 px-4">자산코드 / 자산명</th>
                  <th className="py-3 px-4">처분 구분</th>
                  <th className="py-3 px-4">처분 일자</th>
                  <th className="py-3 px-4 text-right">장부가액</th>
                  <th className="py-3 px-4 text-right">매각 금액</th>
                  <th className="py-3 px-4 text-right">처분 손익</th>
                  <th className="py-3 px-4">승인 상태</th>
                  <th className="py-3 px-4 text-center">액션</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs font-medium">
                {filteredDisposals.map((disp) => (
                  <tr key={disp.id} className="hover:bg-white/5 transition-all">
                    <td className="py-4 px-4 font-mono font-bold text-blue-400">{disp.disposalNo}</td>
                    <td className="py-4 px-4">
                      <p className="font-bold text-white">{disp.assetName}</p>
                      <p className="text-[11px] font-mono text-slate-500">{disp.assetCode}</p>
                    </td>
                    <td className="py-4 px-4">
                      <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-slate-300 font-bold">
                        {getTypeLabel(disp.disposalType)}
                      </span>
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-300">{disp.disposalDate}</td>
                    <td className="py-4 px-4 text-right"><AmountDisplay amount={disp.bookValue} /></td>
                    <td className="py-4 px-4 text-right"><AmountDisplay amount={disp.saleAmount} className="text-emerald-400" /></td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay amount={disp.gainLossAmount} showSign className={disp.gainLossAmount >= 0 ? 'text-emerald-400' : 'text-rose-400'} />
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge
                        status={disp.approvalStatus === 'APPROVED' ? '승인 완료' : disp.approvalStatus === 'PENDING' ? '결재 대기' : '반려'}
                        variant={getApprovalVariant(disp.approvalStatus)}
                      />
                    </td>
                    <td className="py-4 px-4 text-center">
                      {disp.approvalStatus === 'PENDING' ? (
                        <button
                          onClick={() => handleApprove(disp.id)}
                          className="px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600 border border-emerald-500/30 text-emerald-300 hover:text-white font-bold transition-all text-xs"
                        >
                          승인
                        </button>
                      ) : (
                        <span className="text-[11px] text-slate-500 italic">{disp.approvedBy || '처리 완료'}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Disposal Request Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-lg font-black text-white italic">신규 고정자산 처분 신청</h3>
              <button onClick={() => setIsModalOpen(false)} className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10">
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleRegisterDisposal} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="font-bold text-slate-400">처분 대상 자산 선택 *</label>
                <select
                  value={selectedAssetId}
                  onChange={(e) => setSelectedAssetId(e.target.value)}
                  className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                >
                  {mockAssets.map(asset => (
                    <option key={asset.id} value={asset.id}>
                      [{asset.assetCode}] {asset.assetName} (장부가: ₩{asset.bookValue.toLocaleString()})
                    </option>
                  ))}
                </select>
              </div>

              {activeTargetAsset && (
                <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 grid grid-cols-3 gap-2 text-center">
                  <div>
                    <span className="text-[10px] text-slate-500 block">취득원가</span>
                    <span className="font-bold text-white">₩{activeTargetAsset.acquisitionCost.toLocaleString()}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block">상각누계액</span>
                    <span className="font-bold text-rose-400">₩{activeTargetAsset.accumulatedDepreciation.toLocaleString()}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block">현재 장부가액</span>
                    <span className="font-bold text-emerald-400">₩{activeTargetAsset.bookValue.toLocaleString()}</span>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">처분 구분</label>
                  <select
                    value={disposalType}
                    onChange={(e) => setDisposalType(e.target.value as any)}
                    className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                  >
                    <option value="SALE">매각 (Sale)</option>
                    <option value="SCRAP">폐기 (Scrap)</option>
                    <option value="DONATION">증여 (Donation)</option>
                    <option value="LOSS">손실/화재 (Loss)</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">처분 예정일자</label>
                  <input
                    type="date"
                    value={disposalDate}
                    onChange={(e) => setDisposalDate(e.target.value)}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {disposalType === 'SALE' && (
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">실 매각 금액 (원)</label>
                  <input
                    type="number"
                    min={0}
                    value={saleAmount}
                    onChange={(e) => setSaleAmount(Number(e.target.value))}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>
              )}

              <div className="p-3.5 rounded-2xl bg-slate-950 border border-white/10 flex items-center justify-between">
                <span className="font-bold text-slate-400">자동 예상 처분 손익:</span>
                <span className={`text-base font-black font-mono ${calculatedGainLoss >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {calculatedGainLoss >= 0 ? '+' : ''}₩{calculatedGainLoss.toLocaleString()}
                </span>
              </div>

              <div className="space-y-1">
                <label className="font-bold text-slate-400">처분 사유 및 상세 설명 *</label>
                <textarea
                  rows={3}
                  required
                  placeholder="처분 사유를 입력하세요 (예: 노후화에 따른 신규 교체 매각)"
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
                  처분 결재 신청
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
