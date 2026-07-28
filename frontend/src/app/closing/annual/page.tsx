"use client";

import React, { useState } from 'react';
import { 
  Award, 
  Play, 
  CheckCircle2, 
  AlertCircle, 
  PieChart, 
  Scale, 
  TrendingUp, 
  Lock, 
  FileCheck2,
  RefreshCw,
  ArrowRight,
  Sparkles,
  ChevronRight
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';

interface ClosingStep {
  step: number;
  title: string;
  description: string;
  status: 'COMPLETED' | 'READY' | 'LOCKED';
  completedAt?: string;
  summary: string;
}

export default function AnnualClosingPage() {
  const [fiscalYear, setFiscalYear] = useState<number>(2025);
  const [steps, setSteps] = useState<ClosingStep[]>([
    {
      step: 1,
      title: 'Step 1: 손익 계정 영식화 (Revenue/Expense Zero-out)',
      description: '당기 모든 수익계정 및 비용계정 잔액을 집계손익(Profit & Loss) 계정으로 이전하여 0원 마감',
      status: 'COMPLETED',
      completedAt: '2026-03-15 14:20',
      summary: '총 수익 ₩4,520,000,000 / 총 비용 ₩3,729,700,000 대체 완료',
    },
    {
      step: 2,
      title: 'Step 2: 당기순이익 산출 및 미처분이익잉여금 대체',
      description: '집계손익 잔액 (당기순이익)을 자본 항목인 미처분이익잉여금 계정으로 이전 전표 발행',
      status: 'COMPLETED',
      completedAt: '2026-03-15 15:00',
      summary: '당기순이익 ₩790,300,000 이탁 대체 완료',
    },
    {
      step: 3,
      title: 'Step 3: 이익잉여금 처분안 계산서 반영',
      description: '주주총회 승인 이익준비금(법정적립금) 및 현금배당, 차기이월이익잉여금 확정 계상',
      status: 'READY',
      summary: '법정적립금 ₩79,030,000 / 현금배당 ₩200,000,000 확정 대기',
    },
    {
      step: 4,
      title: 'Step 4: 연차 장부 마감 및 2026 회계연도 이월',
      description: '자산/부채/자본 이월 시산표 생성 및 2025 회계연도 장부 최종 영구 잠금(Permanent Lock)',
      status: 'LOCKED',
      summary: 'Step 3 완료 후 실행 가능',
    },
  ]);

  const [isExecutingStep, setIsExecutingStep] = useState(false);

  // Financial Summary Data for Annual Closing
  const netRevenue = 4520000000;
  const netExpense = 3729700000;
  const netIncome = netRevenue - netExpense; // 790,300,000
  const legalReserve = Math.round(netIncome * 0.1); // 79,030,000
  const cashDividends = 200000000;
  const carriedForwardEarnings = netIncome - legalReserve - cashDividends; // 511,270,000

  const handleExecuteNextStep = () => {
    const readyStep = steps.find(s => s.status === 'READY');
    if (!readyStep) return;

    setIsExecutingStep(true);
    setTimeout(() => {
      setSteps(prev =>
        prev.map(s => {
          if (s.step === readyStep.step) {
            return {
              ...s,
              status: 'COMPLETED',
              completedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
              summary: '처분안 최종 반영 및 전표 생성 완료',
            };
          }
          if (s.step === readyStep.step + 1) {
            return {
              ...s,
              status: 'READY',
              summary: '최종 연차 장부 잠금 준비 완료',
            };
          }
          return s;
        })
      );
      setIsExecutingStep(false);
    }, 1200);
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="연차 결산"
        description="회계연도(Fiscal Year) 마감, 손익계정 영식화, 당기순이익 대체 및 이익잉여금 처분 실행 패널입니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '연차 결산' },
        ]}
        icon={Award}
        actions={
          <div className="flex items-center gap-3">
            <select
              value={fiscalYear}
              onChange={(e) => setFiscalYear(Number(e.target.value))}
              className="px-4 py-2.5 rounded-2xl bg-slate-900/50 border border-white/10 text-xs font-bold text-white outline-none focus:border-blue-500"
            >
              <option value={2025} className="bg-slate-900">2025 회계연도 (연차 결산)</option>
              <option value={2024} className="bg-slate-900">2024 회계연도 (마감 완료)</option>
            </select>
          </div>
        }
      />

      {/* Retained Earnings Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">당기순이익 (Net Income)</span>
          <AmountDisplay amount={netIncome} className="text-3xl font-black text-emerald-400" />
          <p className="text-xs text-emerald-400/80 font-medium">수익 ₩45.2억 - 비용 ₩37.3억</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">이익준비금 (법정적립금)</span>
          <AmountDisplay amount={legalReserve} className="text-2xl" />
          <p className="text-xs text-slate-400">당기순이익의 10% 적립</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">현금 배당 예정액</span>
          <AmountDisplay amount={cashDividends} className="text-2xl text-amber-400" />
          <p className="text-xs text-amber-400/80">주주총회 배당 결의안</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">차기 이월 이익잉여금</span>
          <AmountDisplay amount={carriedForwardEarnings} className="text-2xl text-blue-400" />
          <p className="text-xs text-blue-400/80">2026 회계연도 이월 잔액</p>
        </div>
      </div>

      {/* Retained Earnings Allocation Table */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white italic">{fiscalYear} 회계연도 이익잉여금 처분 계산서 명세</h3>
            <p className="text-xs text-slate-400">당기순이익 배분 및 차기이월액 계산</p>
          </div>
          <span className="text-xs font-mono font-bold text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-xl border border-emerald-500/20">
            상태: 처분안 가결산 확정
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">항목 (Line Item)</th>
                <th className="py-3 px-4">구분</th>
                <th className="py-3 px-4 text-right">금액 (KRW)</th>
                <th className="py-3 px-4">비고 / 관계법령</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-xs font-medium">
              <tr className="bg-white/[0.02]">
                <td className="py-3.5 px-4 font-bold text-white">Ⅰ. 미처분이익잉여금</td>
                <td className="py-3.5 px-4 text-slate-400">기초 + 당기순이익</td>
                <td className="py-3.5 px-4 text-right">
                  <AmountDisplay amount={netIncome} className="font-bold text-sm" />
                </td>
                <td className="py-3.5 px-4 text-slate-500 text-[11px]">2025년도 가결산 당기순손익전표</td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 pl-8 text-slate-300">1. 전기이월 미처분이익잉여금</td>
                <td className="py-3.5 px-4 text-slate-500">기초 이월액</td>
                <td className="py-3.5 px-4 text-right">
                  <AmountDisplay amount={0} />
                </td>
                <td className="py-3.5 px-4 text-slate-500 text-[11px]">-</td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 pl-8 text-slate-300">2. 당기순이익</td>
                <td className="py-3.5 px-4 text-emerald-400 font-bold">당기 손익전기</td>
                <td className="py-3.5 px-4 text-right">
                  <AmountDisplay amount={netIncome} className="text-emerald-400 font-bold" />
                </td>
                <td className="py-3.5 px-4 text-slate-500 text-[11px]">손익계산서 최종 순이익</td>
              </tr>
              <tr className="bg-white/[0.02]">
                <td className="py-3.5 px-4 font-bold text-white">Ⅱ. 이익잉여금 처분액</td>
                <td className="py-3.5 px-4 text-slate-400">적립금 및 배당금</td>
                <td className="py-3.5 px-4 text-right">
                  <AmountDisplay amount={legalReserve + cashDividends} className="font-bold text-amber-400" />
                </td>
                <td className="py-3.5 px-4 text-slate-500 text-[11px]">주주총회 승인 대상</td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 pl-8 text-slate-300">1. 이익준비금 (법정적립금)</td>
                <td className="py-3.5 px-4 text-slate-400">상법 제458조</td>
                <td className="py-3.5 px-4 text-right">
                  <AmountDisplay amount={legalReserve} />
                </td>
                <td className="py-3.5 px-4 text-slate-500 text-[11px]">현금배당액의 10% 이상 필수 적립</td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 pl-8 text-slate-300">2. 주주 현금배당금</td>
                <td className="py-3.5 px-4 text-slate-400">주식 배당 산출</td>
                <td className="py-3.5 px-4 text-right">
                  <AmountDisplay amount={cashDividends} className="text-amber-400 font-bold" />
                </td>
                <td className="py-3.5 px-4 text-slate-500 text-[11px]">보통주 주당 ₩1,000배당 예정</td>
              </tr>
              <tr className="bg-blue-600/10 font-bold">
                <td className="py-4 px-4 text-white">Ⅲ. 차기이월 미처분이익잉여금</td>
                <td className="py-4 px-4 text-blue-400">Ⅰ - Ⅱ 최종 기말 잔액</td>
                <td className="py-4 px-4 text-right">
                  <AmountDisplay amount={carriedForwardEarnings} className="text-base font-black text-blue-400" />
                </td>
                <td className="py-4 px-4 text-blue-300 text-[11px]">2026년 01월 01일자 기초잔액 이월</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Step-by-Step Annual Closing Workflow Execution Panel */}
      <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3 className="text-xl font-black text-white italic">연차 결산 실행 프로세스 워크플로우</h3>
            <p className="text-xs text-slate-400">순차적 단계별 연도 마감 실행 및 장부 영구 잠금</p>
          </div>

          <button
            onClick={handleExecuteNextStep}
            disabled={isExecutingStep || !steps.some(s => s.status === 'READY')}
            className="flex items-center gap-2 px-6 py-3 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/20 transition-all disabled:opacity-40"
          >
            <Play size={18} className={isExecutingStep ? 'animate-spin' : ''} />
            <span>{isExecutingStep ? '마감 전표 자동 생성 중...' : '다음 단계 마감 실행'}</span>
          </button>
        </div>

        {/* Steps List */}
        <div className="space-y-4">
          {steps.map((st) => {
            const isDone = st.status === 'COMPLETED';
            const isReady = st.status === 'READY';

            return (
              <div
                key={st.step}
                className={`p-6 rounded-2xl border transition-all flex flex-col md:flex-row md:items-center justify-between gap-4 ${
                  isDone
                    ? 'bg-white/[0.02] border-emerald-500/30'
                    : isReady
                    ? 'bg-blue-600/10 border-blue-500/40 ring-1 ring-blue-500/30'
                    : 'bg-white/[0.01] border-white/5 opacity-50'
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className={`w-10 h-10 rounded-2xl flex items-center justify-center font-black shrink-0 ${
                    isDone ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
                    isReady ? 'bg-blue-600 text-white shadow-lg' : 'bg-slate-800 text-slate-500'
                  }`}>
                    {isDone ? <CheckCircle2 size={20} /> : st.step}
                  </div>

                  <div className="space-y-1">
                    <div className="flex items-center gap-3">
                      <h4 className="text-base font-bold text-white">{st.title}</h4>
                      <StatusBadge
                        status={isDone ? '완료됨' : isReady ? '실행 대기' : '잠김'}
                        variant={isDone ? 'success' : isReady ? 'info' : 'neutral'}
                      />
                    </div>
                    <p className="text-xs text-slate-400">{st.description}</p>
                    <p className="text-xs font-mono font-bold text-slate-300 pt-1">
                      요약: {st.summary}
                    </p>
                  </div>
                </div>

                {st.completedAt && (
                  <div className="text-right text-[11px] font-mono text-emerald-400 shrink-0">
                    완료시각: {st.completedAt}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
