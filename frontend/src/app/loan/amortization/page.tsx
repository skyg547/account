'use client';

import React, { useState, useMemo } from 'react';
import { 
  Table, 
  Calculator, 
  Calendar, 
  Percent, 
  TrendingUp, 
  Download, 
  RefreshCw, 
  ChevronRight, 
  Sparkles, 
  FileSpreadsheet,
  CheckCircle2,
  Clock,
  AlertTriangle
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import EmptyState from '@/components/ui/EmptyState';
import { mockSchedules, mockContracts, AmortizationScheduleDto, LoanContractDto } from '@/mocks/loan';

export default function LoanAmortizationPage() {
  const [selectedContractId, setSelectedContractId] = useState<string>(mockContracts[0].id);
  const [isRecalculating, setIsRecalculating] = useState(false);

  const selectedContract = useMemo(() => {
    return mockContracts.find(c => c.id === selectedContractId) || mockContracts[0];
  }, [selectedContractId]);

  const schedules = useMemo(() => {
    return mockSchedules.filter(s => s.contractId === selectedContractId);
  }, [selectedContractId]);

  const scheduleSummary = useMemo(() => {
    const totalScheduledPayment = schedules.reduce((acc, cur) => acc + cur.scheduledPayment, 0);
    const totalPrincipal = schedules.reduce((acc, cur) => acc + cur.principalComponent, 0);
    const totalNominalInterest = schedules.reduce((acc, cur) => acc + cur.interestComponent, 0);
    const totalEffectiveInterest = schedules.reduce((acc, cur) => acc + cur.effectiveInterest, 0);
    const totalFeeAmortization = schedules.reduce((acc, cur) => acc + cur.feeAmortization, 0);
    const totalCostAmortization = schedules.reduce((acc, cur) => acc + cur.costAmortization, 0);

    return {
      totalScheduledPayment,
      totalPrincipal,
      totalNominalInterest,
      totalEffectiveInterest,
      totalFeeAmortization,
      totalCostAmortization,
      interestDiff: totalEffectiveInterest - totalNominalInterest,
    };
  }, [schedules]);

  const handleRecalculateEir = () => {
    setIsRecalculating(true);
    setTimeout(() => {
      setIsRecalculating(false);
      alert(`${selectedContract.contractNo} 계약의 EIR(유효이자율) 상각 스케줄이 성공적으로 재계산되었습니다.`);
    }, 1000);
  };

  const getStatusBadgeProps = (status: AmortizationScheduleDto['status']) => {
    switch (status) {
      case 'PAID':
        return { status: '상환 완료', variant: 'success' as const };
      case 'SCHEDULED':
        return { status: '상환 예정', variant: 'info' as const };
      case 'OVERDUE':
        return { status: '연체 중', variant: 'error' as const };
      default:
        return { status, variant: 'neutral' as const };
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <PageHeader
        title="EIR 상각 스케줄"
        description="유효이자율법(Effective Interest Rate Method)에 따른 대출 회차별 장부가액, 유효이자수익 및 부대수수료/원가 상각 스케줄을 조회합니다."
        breadcrumbs={[
          { label: '여신/대출 관리' },
          { label: 'EIR 상각 스케줄' },
        ]}
        icon={Table}
        actions={
          <div className="flex items-center gap-3">
            <button 
              onClick={handleRecalculateEir}
              disabled={isRecalculating}
              className="px-5 py-2.5 bg-white/5 hover:bg-white/10 disabled:opacity-50 text-slate-300 rounded-xl text-sm font-bold border border-white/10 transition-all flex items-center gap-2"
            >
              <RefreshCw size={16} className={isRecalculating ? 'animate-spin text-blue-400' : ''} />
              EIR 스케줄 재계산
            </button>
            <button className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2">
              <FileSpreadsheet size={16} /> 스케줄 엑셀 다운로드
            </button>
          </div>
        }
      />

      {/* Contract Selector & Summary Bar */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Selector Card */}
        <div className="lg:col-span-1 p-6 rounded-3xl bg-slate-900/50 border border-white/10 backdrop-blur-md space-y-4">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
            <Calculator size={16} className="text-blue-400" />
            <span>대출 계약 선택</span>
          </div>

          <select
            value={selectedContractId}
            onChange={(e) => setSelectedContractId(e.target.value)}
            className="w-full px-4 py-3 bg-slate-950/80 border border-white/10 rounded-2xl text-sm text-white font-bold focus:outline-none focus:border-blue-500/50"
          >
            {mockContracts.map(c => (
              <option key={c.id} value={c.id}>
                [{c.contractNo}] {c.borrowerName}
              </option>
            ))}
          </select>

          <div className="space-y-3 pt-2 text-xs divide-y divide-white/5">
            <div className="flex justify-between items-center pt-2">
              <span className="text-slate-400">대출 상품</span>
              <span className="text-white font-bold">{selectedContract.productName}</span>
            </div>

            <div className="flex justify-between items-center pt-2">
              <span className="text-slate-400">약정 금액</span>
              <AmountDisplay amount={selectedContract.principalAmount} className="text-white font-bold" />
            </div>

            <div className="flex justify-between items-center pt-2">
              <span className="text-slate-400">명목 금리 / EIR</span>
              <span className="font-mono text-amber-400 font-bold">
                {selectedContract.nominalRate}% / {selectedContract.effectiveRate}%
              </span>
            </div>

            <div className="flex justify-between items-center pt-2">
              <span className="text-slate-400">이연 순수수료</span>
              <AmountDisplay amount={selectedContract.deferredFee - selectedContract.deferredCost} showSign className="text-emerald-400 font-bold" />
            </div>
          </div>
        </div>

        {/* Schedule Summary Stats */}
        <div className="lg:col-span-3 grid grid-cols-1 sm:grid-cols-3 gap-5">
          <div className="p-6 rounded-3xl bg-slate-900/50 border border-white/10 backdrop-blur-md flex flex-col justify-between">
            <div className="flex items-center justify-between text-xs font-bold text-slate-400 uppercase">
              <span>총 약정 이자수익</span>
              <Percent size={18} className="text-blue-400" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-black text-white">
                <AmountDisplay amount={scheduleSummary.totalNominalInterest} />
              </div>
              <span className="text-xs text-slate-500 font-medium">명목 표면이자 총합</span>
            </div>
            <div className="text-[11px] text-slate-400 bg-white/5 px-3 py-1 rounded-xl w-fit border border-white/5">
              원금 총액 ₩{scheduleSummary.totalPrincipal.toLocaleString()}
            </div>
          </div>

          <div className="p-6 rounded-3xl bg-slate-900/50 border border-white/10 backdrop-blur-md flex flex-col justify-between">
            <div className="flex items-center justify-between text-xs font-bold text-slate-400 uppercase">
              <span>EIR 유효 이자수익</span>
              <Sparkles size={18} className="text-amber-400" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-black text-amber-400">
                <AmountDisplay amount={scheduleSummary.totalEffectiveInterest} className="text-amber-400" />
              </div>
              <span className="text-xs text-slate-500 font-medium">부대수수료 상각 반영 총 손익</span>
            </div>
            <div className="text-[11px] text-amber-300/80 bg-amber-500/10 px-3 py-1 rounded-xl w-fit border border-amber-500/20 font-mono">
              손익 조정 차액: +₩{scheduleSummary.interestDiff.toLocaleString()}
            </div>
          </div>

          <div className="p-6 rounded-3xl bg-slate-900/50 border border-white/10 backdrop-blur-md flex flex-col justify-between">
            <div className="flex items-center justify-between text-xs font-bold text-slate-400 uppercase">
              <span>부대 수수료/원가 상각액</span>
              <TrendingUp size={18} className="text-emerald-400" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-black text-emerald-400">
                <AmountDisplay amount={scheduleSummary.totalFeeAmortization - scheduleSummary.totalCostAmortization} showSign className="text-emerald-400" />
              </div>
              <span className="text-xs text-slate-500 font-medium">누적 상각 손익 가산액</span>
            </div>
            <div className="text-[11px] text-emerald-300/80 bg-emerald-500/10 px-3 py-1 rounded-xl w-fit border border-emerald-500/20">
              상각 완료율 25.0%
            </div>
          </div>
        </div>
      </div>

      {/* Main Amortization Table */}
      {schedules.length === 0 ? (
        <EmptyState
          icon={Table}
          title="등록된 상각 스케줄이 없습니다"
          description="선택한 대출 계약의 상각 스케줄 데이터가 생성되지 않았습니다."
        />
      ) : (
        <div className="rounded-3xl border border-white/10 bg-slate-900/50 backdrop-blur-md overflow-hidden">
          <div className="p-6 border-b border-white/10 flex items-center justify-between">
            <h3 className="text-lg font-black text-white italic tracking-tight">
              [{selectedContract.contractNo}] 회차별 상각 세부 스케줄
            </h3>
            <span className="text-xs text-slate-400 font-mono font-semibold bg-white/5 px-3 py-1 rounded-full border border-white/5">
              총 {schedules.length}회차 스케줄
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-white/5 text-xs uppercase font-bold tracking-wider text-slate-400 border-b border-white/5">
                <tr>
                  <th className="py-4 px-5 text-center">회차</th>
                  <th className="py-4 px-5">상환 예정일</th>
                  <th className="py-4 px-5 text-right">기초 장부가액</th>
                  <th className="py-4 px-5 text-right">상환 예정액</th>
                  <th className="py-4 px-5 text-right">원금 상환액</th>
                  <th className="py-4 px-5 text-right">명목 이자</th>
                  <th className="py-4 px-5 text-right text-amber-400">EIR 유효이자</th>
                  <th className="py-4 px-5 text-right text-emerald-400">수수료 상각</th>
                  <th className="py-4 px-5 text-right text-rose-400">원가 상각</th>
                  <th className="py-4 px-5 text-right">기말 장부가액</th>
                  <th className="py-4 px-5 text-center">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 font-mono text-xs">
                {schedules.map((sch) => {
                  const badge = getStatusBadgeProps(sch.status);
                  return (
                    <tr key={sch.scheduleId} className="hover:bg-white/[0.03] transition-colors">
                      <td className="py-4 px-5 text-center font-bold text-blue-400">
                        {sch.period}회차
                      </td>
                      <td className="py-4 px-5 text-slate-300 font-sans font-medium">
                        {sch.dueDate}
                      </td>
                      <td className="py-4 px-5 text-right text-slate-300">
                        <AmountDisplay amount={sch.beginningBalance} className="text-slate-300" />
                      </td>
                      <td className="py-4 px-5 text-right font-bold text-white">
                        <AmountDisplay amount={sch.scheduledPayment} />
                      </td>
                      <td className="py-4 px-5 text-right text-slate-300">
                        <AmountDisplay amount={sch.principalComponent} className="text-slate-300" />
                      </td>
                      <td className="py-4 px-5 text-right text-slate-400">
                        <AmountDisplay amount={sch.interestComponent} className="text-slate-400" />
                      </td>
                      <td className="py-4 px-5 text-right font-bold text-amber-400">
                        <AmountDisplay amount={sch.effectiveInterest} className="text-amber-400" />
                      </td>
                      <td className="py-4 px-5 text-right text-emerald-400">
                        <AmountDisplay amount={sch.feeAmortization} className="text-emerald-400" />
                      </td>
                      <td className="py-4 px-5 text-right text-rose-400">
                        <AmountDisplay amount={sch.costAmortization} className="text-rose-400" />
                      </td>
                      <td className="py-4 px-5 text-right font-bold text-white">
                        <AmountDisplay amount={sch.endingBalance} />
                      </td>
                      <td className="py-4 px-5 text-center font-sans">
                        <StatusBadge status={badge.status} variant={badge.variant} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
