"use client";

import React, { useState, useMemo } from 'react';
import { 
  Calculator, 
  Play, 
  CheckCircle2, 
  FileText, 
  History, 
  AlertCircle, 
  RefreshCw, 
  Layers, 
  TrendingDown,
  Calendar,
  Sparkles,
  Loader2
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockAssets, mockDepreciationBatches, DepreciationBatchDto } from '@/mocks/fair-value';

export default function DepreciationPage() {
  const [batches, setBatches] = useState<DepreciationBatchDto[]>(mockDepreciationBatches);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('2026-07');
  const [isSimulating, setIsSimulating] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [simulationResult, setSimulationResult] = useState<{
    assetCount: number;
    totalAmount: number;
    byCategory: Record<string, number>;
  } | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Active target assets for depreciation calculation
  const activeAssets = useMemo(() => {
    return mockAssets.filter(a => a.status === 'ACTIVE');
  }, []);

  // Calculate monthly depreciation per asset
  const calculateMonthlyDep = (asset: typeof mockAssets[0]) => {
    if (asset.depreciationMethod === 'STRAIGHT_LINE') {
      const netCost = asset.acquisitionCost - asset.salvageValue;
      return Math.round(netCost / (asset.usefulLifeYears * 12));
    } else {
      // Simplistic declining balance approximation for demo
      return Math.round((asset.bookValue * 0.206) / 12);
    }
  };

  const calculatedTotalMonthly = useMemo(() => {
    return activeAssets.reduce((sum, a) => sum + calculateMonthlyDep(a), 0);
  }, [activeAssets]);

  const handleRunSimulation = () => {
    setIsSimulating(true);
    setTimeout(() => {
      const byCategory: Record<string, number> = {};
      activeAssets.forEach(a => {
        const dep = calculateMonthlyDep(a);
        byCategory[a.category] = (byCategory[a.category] || 0) + dep;
      });

      setSimulationResult({
        assetCount: activeAssets.length,
        totalAmount: calculatedTotalMonthly,
        byCategory,
      });
      setIsSimulating(false);
      setToastMessage(`[${selectedPeriod}] 감가상각 시뮬레이션 계산이 완료되었습니다.`);
      setTimeout(() => setToastMessage(null), 4000);
    }, 800);
  };

  const handleExecuteBatch = () => {
    if (!confirm(`${selectedPeriod} 회계기간 감가상각 전표를 자동 생성 및 마감하시겠습니까?`)) return;

    setIsExecuting(true);
    setTimeout(() => {
      const newBatch: DepreciationBatchDto = {
        id: `dep-${selectedPeriod.replace('-', '')}`,
        periodYearMonth: selectedPeriod,
        executionDate: new Date().toISOString().slice(0, 16).replace('T', ' '),
        targetAssetCount: activeAssets.length,
        totalDepreciationAmount: calculatedTotalMonthly,
        status: 'COMPLETED',
        executor: '김해경 과장 (자산회계팀)',
        journalEntryNo: `JV-${selectedPeriod.replace('-', '')}31-0088`,
      };

      setBatches(prev => [newBatch, ...prev.filter(b => b.periodYearMonth !== selectedPeriod)]);
      setIsExecuting(false);
      setSimulationResult(null);
      setToastMessage(`[${selectedPeriod}] 감가상각 배치 및 자동 결산 전표 [${newBatch.journalEntryNo}]가 정상 확정되었습니다.`);
      setTimeout(() => setToastMessage(null), 5000);
    }, 1200);
  };

  const getStatusVariant = (status: DepreciationBatchDto['status']) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'IN_PROGRESS': return 'warning';
      case 'FAILED': return 'error';
      case 'DRAFT': return 'neutral';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: DepreciationBatchDto['status']) => {
    switch (status) {
      case 'COMPLETED': return '상각 및 전표 발행 완료';
      case 'IN_PROGRESS': return '처리 중';
      case 'FAILED': return '실패';
      case 'DRAFT': return '시뮬레이션 대기';
      default: return status;
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
        title="감가상각 실행"
        description="월별 고정자산 감가상각비를 자동 계산하고 결산 차변·대변 자동 전표를 시뮬레이션 및 확정합니다."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: '감가상각 실행' },
        ]}
        icon={Calculator}
      />

      {/* Main Execution Panel & Quick Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Control Box (1 col) */}
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
          <div className="flex items-center gap-3 pb-4 border-b border-white/5">
            <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Sparkles size={20} />
            </div>
            <div>
              <h3 className="text-base font-black text-white italic">월 감가상각 배치 실행</h3>
              <p className="text-xs text-slate-400">당월 대상자산 일괄 자동 상각</p>
            </div>
          </div>

          <div className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label className="font-bold text-slate-400 flex items-center gap-1.5">
                <Calendar size={14} className="text-blue-400" />
                <span>상각 대상 회계 연월</span>
              </label>
              <input
                type="month"
                value={selectedPeriod}
                onChange={(e) => setSelectedPeriod(e.target.value)}
                className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white font-mono outline-none focus:border-blue-500"
              />
            </div>

            <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
              <div className="flex justify-between text-slate-400">
                <span>상각 대상 유/무형자산</span>
                <span className="font-bold text-white font-mono">{activeAssets.length} 건</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>예상 당월 총 상각비액</span>
                <span className="font-bold text-rose-400 font-mono">
                  <AmountDisplay amount={calculatedTotalMonthly} />
                </span>
              </div>
            </div>

            <div className="space-y-2 pt-2">
              <button
                type="button"
                onClick={handleRunSimulation}
                disabled={isSimulating || isExecuting}
                className="w-full py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-slate-200 hover:text-white font-bold transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                <RefreshCw size={16} className={isSimulating ? 'animate-spin text-blue-400' : ''} />
                <span>{isSimulating ? '상각액 시뮬레이션 계산 중...' : '상각비 사전 검증 (시뮬레이션)'}</span>
              </button>

              <button
                type="button"
                onClick={handleExecuteBatch}
                disabled={isSimulating || isExecuting}
                aria-busy={isExecuting}
                className="w-full py-3.5 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-black shadow-lg shadow-blue-600/20 transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed disabled:pointer-events-none cursor-pointer"
              >
                {isExecuting ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    <span>전표 발행 및 마감 처리 중...</span>
                  </>
                ) : (
                  <>
                    <Play size={16} />
                    <span>감가상각 확정 및 전표 생성</span>
                  </>
                )}
              </button>
            </div>

            <div className="p-3 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-300 text-[11px] font-medium flex items-start gap-2">
              <AlertCircle size={16} className="shrink-0 mt-0.5" />
              <span>확정 실행 시 각 자산별 장부가액이 자동 차감되고 회계 결산 전표(JV)가 발행됩니다.</span>
            </div>
          </div>
        </div>

        {/* Breakdown & Target Preview (2 cols) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Simulation Details Banner */}
          {simulationResult && (
            <div className="p-6 rounded-3xl bg-blue-950/40 border border-blue-500/30 space-y-4 animate-in fade-in">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-blue-400 font-bold text-sm">
                  <CheckCircle2 size={18} />
                  <span>{selectedPeriod} 시뮬레이션 계산 결과</span>
                </div>
                <span className="text-xs font-mono text-slate-400">총 {simulationResult.assetCount}개 자산</span>
              </div>

              <div className="grid grid-cols-3 gap-4">
                {Object.entries(simulationResult.byCategory).map(([cat, amount]) => (
                  <div key={cat} className="p-3 rounded-xl bg-white/5 border border-white/5 space-y-1">
                    <span className="text-[10px] text-slate-400 font-bold">{cat} 상각 합계</span>
                    <p className="text-sm font-black text-white">
                      <AmountDisplay amount={amount} />
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Asset List Preview */}
          <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-black text-white italic">당월 감가상각 대상 자산 명세</h3>
                <p className="text-xs text-slate-400">정상 사용 상태인 자산별 당월 계산 금액</p>
              </div>
              <span className="text-xs font-mono font-bold text-slate-400 bg-white/5 px-3 py-1 rounded-xl border border-white/5">
                당월 총 {calculatedTotalMonthly.toLocaleString()} 원
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                    <th className="py-3 px-4">자산코드</th>
                    <th className="py-3 px-4">자산명</th>
                    <th className="py-3 px-4">취득가액</th>
                    <th className="py-3 px-4">현재 장부가액</th>
                    <th className="py-3 px-4">상각방법</th>
                    <th className="py-3 px-4 text-right">당월 상각예상액</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-xs font-medium">
                  {activeAssets.map(asset => {
                    const monthlyDep = calculateMonthlyDep(asset);
                    return (
                      <tr key={asset.id} className="hover:bg-white/5 transition-all">
                        <td className="py-3.5 px-4 font-mono font-bold text-blue-400">{asset.assetCode}</td>
                        <td className="py-3.5 px-4 font-bold text-white">{asset.assetName}</td>
                        <td className="py-3.5 px-4"><AmountDisplay amount={asset.acquisitionCost} /></td>
                        <td className="py-3.5 px-4"><AmountDisplay amount={asset.bookValue} className="text-emerald-400" /></td>
                        <td className="py-3.5 px-4 text-slate-400">
                          {asset.depreciationMethod === 'STRAIGHT_LINE' ? '정액법' : '정률법'}
                        </td>
                        <td className="py-3.5 px-4 text-right font-bold text-rose-400">
                          <AmountDisplay amount={monthlyDep} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Execution History Table */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <History size={18} />
            </div>
            <div>
              <h3 className="text-base font-black text-white italic">감가상각 배치 수행 이력</h3>
              <p className="text-xs text-slate-400">최근 회계 연월별 감가상각비 집계 및 자동 전표 생성 기록</p>
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">회계연월</th>
                <th className="py-3 px-4">실행 일시</th>
                <th className="py-3 px-4">대상 자산 수</th>
                <th className="py-3 px-4 text-right">총 감가상각비</th>
                <th className="py-3 px-4">발행 전표 번호</th>
                <th className="py-3 px-4">실무 담당자</th>
                <th className="py-3 px-4">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-xs font-medium">
              {batches.map((batch) => (
                <tr key={batch.id} className="hover:bg-white/5 transition-all">
                  <td className="py-4 px-4 font-mono font-bold text-white">{batch.periodYearMonth}</td>
                  <td className="py-4 px-4 font-mono text-slate-300">{batch.executionDate}</td>
                  <td className="py-4 px-4 font-mono text-slate-300">{batch.targetAssetCount} 건</td>
                  <td className="py-4 px-4 text-right font-bold text-rose-400">
                    <AmountDisplay amount={batch.totalDepreciationAmount} />
                  </td>
                  <td className="py-4 px-4 font-mono text-blue-400">{batch.journalEntryNo || '-'}</td>
                  <td className="py-4 px-4 text-slate-300">{batch.executor}</td>
                  <td className="py-4 px-4">
                    <StatusBadge
                      status={getStatusLabel(batch.status)}
                      variant={getStatusVariant(batch.status)}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
