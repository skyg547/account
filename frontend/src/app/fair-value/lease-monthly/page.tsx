"use client";

import React, { useState, useMemo } from 'react';
import {
  CalendarCheck2,
  Play,
  CheckCircle2,
  DollarSign,
  Percent,
  TrendingDown,
  Building2,
  Search,
  FileCheck
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import EmptyState from '@/components/ui/EmptyState';
import { mockLeaseMonthlies, LeaseMonthlyDto } from '@/mocks/fair-value';

export default function LeaseMonthlyClosingPage() {
  const [monthlies, setMonthlies] = useState<LeaseMonthlyDto[]>(mockLeaseMonthlies);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('2026-07');
  const [searchTerm, setSearchTerm] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Active items for selected period
  const activePeriodItems = useMemo(() => {
    return monthlies.filter(m => m.closingPeriod === selectedPeriod);
  }, [monthlies, selectedPeriod]);

  // KPI
  const totalPayment = useMemo(() => activePeriodItems.reduce((sum, item) => sum + item.monthlyPayment, 0), [activePeriodItems]);
  const totalInterest = useMemo(() => activePeriodItems.reduce((sum, item) => sum + item.interestExpense, 0), [activePeriodItems]);
  const totalPrincipal = useMemo(() => activePeriodItems.reduce((sum, item) => sum + item.principalRepayment, 0), [activePeriodItems]);
  const totalRouDep = useMemo(() => activePeriodItems.reduce((sum, item) => sum + item.rouDepreciation, 0), [activePeriodItems]);

  const handleRunBatchClosing = () => {
    if (activePeriodItems.length === 0) return;
    setIsProcessing(true);

    setTimeout(() => {
      setMonthlies(prev => prev.map(item => {
        if (item.closingPeriod === selectedPeriod) {
          return {
            ...item,
            closingStatus: 'COMPLETED',
            closingDate: new Date().toISOString().split('T')[0],
          };
        }
        return item;
      }));
      setIsProcessing(false);
      setToastMessage(`[${selectedPeriod}] IFRS 16 리스 월결산 (이자비용 인식 및 ROU상각 전표) 처리 완료!`);
      setTimeout(() => setToastMessage(null), 5000);
    }, 1200);
  };

  const getStatusVariant = (status: LeaseMonthlyDto['closingStatus']) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'PENDING': return 'warning';
      case 'SKIPPED': return 'neutral';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: LeaseMonthlyDto['closingStatus']) => {
    switch (status) {
      case 'COMPLETED': return '월결산 마감 완료';
      case 'PENDING': return '결산 대기';
      case 'SKIPPED': return '제외 처리';
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
        title="리스 월결산"
        description="IFRS 16 리스 계약의 월별 지급액(원금/이자 분리) 및 사용권자산 감가상각비를 자동 계상하고 결산 전표를 생성합니다."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: '리스 월결산' },
        ]}
        icon={CalendarCheck2}
        actions={
          <div className="flex items-center gap-3">
            <input
              type="month"
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value)}
              className="px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-xs font-mono text-white outline-none focus:border-blue-500"
            />
            <button
              onClick={handleRunBatchClosing}
              disabled={isProcessing}
              className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 disabled:opacity-50"
            >
              <Play size={16} />
              <span>{isProcessing ? '월결산 전표 집계 중...' : '당월 리스 결산 실행'}</span>
            </button>
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">당월 총 현금지급액</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <DollarSign size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalPayment} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">임대인 지급 리스료 합계</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">당월 이자비용 인식액</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Percent size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-amber-400">
            <AmountDisplay amount={totalInterest} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">유효이자율법 인식 이자비용</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">당월 리스부채 차감액</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <TrendingDown size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={totalPrincipal} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">부채 상각(원금) 반영액</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">당월 사용권자산 상각비</span>
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Building2 size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-purple-400">
            <AmountDisplay amount={totalRouDep} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">손익계산서 감가상각비 계상</p>
        </div>
      </div>

      {/* Main Table */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-white font-bold text-sm">
            <FileCheck size={18} className="text-blue-400" />
            <span>[{selectedPeriod}] 월결산 세부 리스 계약 명세</span>
          </div>

          <div className="relative min-w-[240px]">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="계약번호, 계약명..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          {activePeriodItems.length === 0 ? (
            <EmptyState
              icon={CalendarCheck2}
              title="해당 월결산 내역이 없습니다."
              description="선택한 회계연월의 결산 대상 데이터가 아직 생성되지 않았습니다."
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">계약 번호</th>
                  <th className="py-3 px-4">리스 계약명</th>
                  <th className="py-3 px-4 text-right">월 지급액</th>
                  <th className="py-3 px-4 text-right">이자 비용 (비용)</th>
                  <th className="py-3 px-4 text-right">부채 차감 (원금)</th>
                  <th className="py-3 px-4 text-right">사용권자산 상각비</th>
                  <th className="py-3 px-4">결산 마감일</th>
                  <th className="py-3 px-4">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs font-medium">
                {activePeriodItems.map((item) => (
                  <tr key={item.id} className="hover:bg-white/5 transition-all">
                    <td className="py-4 px-4 font-mono font-bold text-blue-400">{item.contractNo}</td>
                    <td className="py-4 px-4 font-bold text-white">{item.contractName}</td>
                    <td className="py-4 px-4 text-right font-bold text-white">
                      <AmountDisplay amount={item.monthlyPayment} />
                    </td>
                    <td className="py-4 px-4 text-right text-amber-400 font-bold">
                      <AmountDisplay amount={item.interestExpense} />
                    </td>
                    <td className="py-4 px-4 text-right text-emerald-400 font-bold">
                      <AmountDisplay amount={item.principalRepayment} />
                    </td>
                    <td className="py-4 px-4 text-right text-purple-400 font-bold">
                      <AmountDisplay amount={item.rouDepreciation} />
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-300">
                      {item.closingDate || '-'}
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge
                        status={getStatusLabel(item.closingStatus)}
                        variant={getStatusVariant(item.closingStatus)}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
