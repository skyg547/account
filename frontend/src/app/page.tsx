'use client';

import React, { useSyncExternalStore } from 'react';
import Link from 'next/link';
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  CreditCard,
  PieChart,
  ArrowUpRight,
  Clock,
  FileText,
  Calendar,
  Layers
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  Tooltip, 
  CartesianGrid 
} from 'recharts';
import { mockKpis, mockJournals } from '@/mocks/ledger';
import { mockStatements } from '@/mocks/reporting';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';

const emptySubscribe = () => () => {};

const statementQuickSummary = {
  totalAssets: mockStatements.find((item) => item.accountCode === '1000000')?.amountCurrent ?? 0,
  totalLiabilities: mockStatements.find((item) => item.accountCode === '2000000')?.amountCurrent ?? 0,
  totalEquity: mockStatements.find((item) => item.accountCode === '3000000')?.amountCurrent ?? 0,
  netIncome: mockStatements.find((item) => item.accountCode === '9900000')?.amountCurrent ?? 0,
};

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'POSTED':
      return { label: '확정', variant: 'success' as const };
    case 'APPROVED':
      return { label: '승인완료', variant: 'info' as const };
    case 'PENDING':
      return { label: '검토대기', variant: 'warning' as const };
    case 'DRAFT':
      return { label: '작성중', variant: 'neutral' as const };
    case 'REJECTED':
      return { label: '반려', variant: 'error' as const };
    default:
      return { label: status, variant: 'neutral' as const };
  }
};

export default function DashboardPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="통합 재무 현황"
        description="실시간 재무 지표 및 전표 유입 처리 현황을 한눈에 모니터링합니다."
        breadcrumbs={[
          { label: '대시보드' },
          { label: '통합 재무 현황' }
        ]}
        icon={PieChart}
        actions={
          <div className="flex items-center gap-2 bg-white dark:bg-[#131b2e] px-3.5 py-2 rounded-xl border border-[#eaedf4] dark:border-slate-800 text-xs font-semibold text-[#545b69] dark:text-slate-400 shadow-xs">
            <Calendar size={14} className="text-[#4262ff]" />
            <span suppressHydrationWarning>
              기준일: {mounted ? new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }) : ''}
            </span>
          </div>
        }
      />

      {/* 3 KPI K-Bank Style Clean Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Card 1: Total Assets */}
        <div className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-6 hover:border-[#4262ff]/40 hover:shadow-md transition-all duration-300 group shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/50 flex items-center justify-center text-[#4262ff] dark:text-blue-400 border border-blue-100 dark:border-blue-800 group-hover:scale-105 transition-transform">
              <Wallet size={24} />
            </div>
            <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
              <TrendingUp size={13} /> +12.4%
            </span>
          </div>
          <p className="text-xs font-bold uppercase tracking-wider text-[#8c94a4] dark:text-slate-400 mb-1">Total Assets (총 자산)</p>
          <div className="text-2xl sm:text-3xl font-black text-[#17191e] dark:text-slate-100 tracking-tight mb-4">
            <AmountDisplay amount={mockKpis.totalAssets} />
          </div>
          <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-[#4262ff] w-[78%] rounded-full" />
          </div>
          <div className="mt-3 flex justify-between text-[11px] font-semibold text-[#8c94a4] dark:text-slate-400">
            <span>목표 달성률 88%</span>
            <span className="text-[#545b69] dark:text-slate-300">전월 대비 상승</span>
          </div>
        </div>

        {/* Card 2: Total Liabilities */}
        <div className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-6 hover:border-rose-300 hover:shadow-md transition-all duration-300 group shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-950/50 flex items-center justify-center text-rose-500 dark:text-rose-400 border border-rose-100 dark:border-rose-800 group-hover:scale-105 transition-transform">
              <CreditCard size={24} />
            </div>
            <span className="flex items-center gap-1 text-xs font-bold text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 px-2.5 py-1 rounded-full border border-rose-200 dark:border-rose-800">
              <TrendingDown size={13} /> -2.1%
            </span>
          </div>
          <p className="text-xs font-bold uppercase tracking-wider text-[#8c94a4] dark:text-slate-400 mb-1">Total Liabilities (총 부채)</p>
          <div className="text-2xl sm:text-3xl font-black text-[#17191e] dark:text-slate-100 tracking-tight mb-4">
            <AmountDisplay amount={mockKpis.totalLiabilities} />
          </div>
          <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-rose-500 w-[36%] rounded-full" />
          </div>
          <div className="mt-3 flex justify-between text-[11px] font-semibold text-[#8c94a4] dark:text-slate-400">
            <span>부채 비율 36.3%</span>
            <span className="text-[#545b69] dark:text-slate-300">안정 수준 유지</span>
          </div>
        </div>

        {/* Card 3: Net Income */}
        <div className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-6 hover:border-emerald-300 hover:shadow-md transition-all duration-300 group shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 flex items-center justify-center text-emerald-600 dark:text-emerald-400 border border-emerald-100 dark:border-emerald-800 group-hover:scale-105 transition-transform">
              <ArrowUpRight size={24} />
            </div>
            <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
              <TrendingUp size={13} /> +18.5%
            </span>
          </div>
          <p className="text-xs font-bold uppercase tracking-wider text-[#8c94a4] dark:text-slate-400 mb-1">Net Income (당기순이익)</p>
          <div className="text-2xl sm:text-3xl font-black text-[#17191e] dark:text-slate-100 tracking-tight mb-4">
            <AmountDisplay amount={mockKpis.netIncome} />
          </div>
          <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-emerald-500 w-[64%] rounded-full" />
          </div>
          <div className="mt-3 flex justify-between text-[11px] font-semibold text-[#8c94a4] dark:text-slate-400">
            <span>순이익률 63.7%</span>
            <span className="text-[#545b69] dark:text-slate-300">목표 대비 초과</span>
          </div>
        </div>
      </div>

      {/* Balance Sheet & Income Statement quick summary */}
      <section className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-7 shadow-xs">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-6">
          <div>
            <h2 className="text-lg font-black text-[#17191e] dark:text-slate-100 tracking-tight">
              재무상태표 &amp; 손익계산서 요약
            </h2>
            <p className="text-xs text-[#545b69] dark:text-slate-400 font-medium mt-1">
              최근 결산 기준 핵심 재무제표 수치를 빠르게 확인합니다.
            </p>
          </div>
          <Link
            href="/reports/statements"
            className="text-xs font-bold text-[#4262ff] hover:text-[#3452e6] bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/50 px-3.5 py-2 rounded-lg transition-all border border-blue-200/50 dark:border-blue-800/50"
          >
            재무제표 상세 보기 &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          {[
            { label: '자산총계', amount: statementQuickSummary.totalAssets, color: 'text-blue-600 dark:text-blue-400' },
            { label: '부채총계', amount: statementQuickSummary.totalLiabilities, color: 'text-rose-600 dark:text-rose-400' },
            { label: '자본총계', amount: statementQuickSummary.totalEquity, color: 'text-indigo-600 dark:text-indigo-400' },
            { label: '당기순이익', amount: statementQuickSummary.netIncome, color: 'text-emerald-600 dark:text-emerald-400' },
          ].map((item) => (
            <div
              key={item.label}
              className="rounded-xl border border-[#eaedf4] dark:border-slate-800 bg-[#f7f8fb] dark:bg-slate-900/60 p-4"
            >
              <p className="text-[11px] font-bold text-[#8c94a4] dark:text-slate-400 mb-2">{item.label}</p>
              <AmountDisplay amount={item.amount} className={`text-base sm:text-lg font-black ${item.color}`} />
            </div>
          ))}
        </div>
      </section>

      {/* Monthly Trends Area Chart */}
      <div className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-7 shadow-xs">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-lg font-black text-[#17191e] dark:text-slate-100 tracking-tight flex items-center gap-2">
              <Layers className="text-[#4262ff]" size={18} />
              월별 재무 추이 분석
            </h3>
            <p className="text-xs text-[#545b69] dark:text-slate-400 font-medium mt-0.5">
              최근 6개월 자산 및 부채 변화 트렌드 (단위: 원)
            </p>
          </div>
          <div className="flex items-center gap-4 text-xs font-bold">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#4262ff] inline-block" />
              <span className="text-[#545b69] dark:text-slate-400">총 자산</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500 inline-block" />
              <span className="text-[#545b69] dark:text-slate-400">총 부채</span>
            </div>
          </div>
        </div>

        <div className="h-72 w-full pt-2">
          {mounted ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={mockKpis.monthlyTrends} margin={{ top: 10, right: 10, left: 20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorAssets" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#4262ff" stopOpacity={0.25}/>
                    <stop offset="95%" stopColor="#4262ff" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorLiabilities" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.25}/>
                    <stop offset="95%" stopColor="#f43f5e" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#eaedf4" className="dark:opacity-10" />
                <XAxis dataKey="month" stroke="#8c94a4" tick={{ fontSize: 12, fill: '#8c94a4' }} />
                <YAxis 
                  stroke="#8c94a4" 
                  tick={{ fontSize: 12, fill: '#8c94a4' }} 
                  tickFormatter={(val) => `${(val / 100000000).toFixed(1)}억`}
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'var(--card)', 
                    borderColor: 'var(--border)', 
                    borderRadius: '12px', 
                    color: 'var(--foreground)',
                    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.2)',
                    fontSize: '12px',
                    fontWeight: 'bold'
                  }} 
                  formatter={(value) => [`₩${Number(value || 0).toLocaleString()}`, '']}
                />
                <Area type="monotone" dataKey="assets" name="총 자산" stroke="#4262ff" strokeWidth={2.5} fillOpacity={1} fill="url(#colorAssets)" />
                <Area type="monotone" dataKey="liabilities" name="총 부채" stroke="#f43f5e" strokeWidth={2.5} fillOpacity={1} fill="url(#colorLiabilities)" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full w-full bg-slate-50 dark:bg-slate-800 animate-pulse rounded-xl" />
          )}
        </div>
      </div>

      {/* Recent Journals Section */}
      <div className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-7 shadow-xs">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h3 className="text-lg font-black text-[#17191e] dark:text-slate-100 tracking-tight flex items-center gap-2">
              <FileText className="text-[#4262ff]" size={18} />
              최근 발생 전표 내역
            </h3>
            <p className="text-xs text-[#545b69] dark:text-slate-400 font-medium mt-0.5">
              최근에 등록 및 검토 처리된 주요 전표 리스트
            </p>
          </div>
          <Link href="/ledger/list" className="text-xs font-bold text-[#4262ff] hover:text-[#3452e6] bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/50 px-3.5 py-1.5 rounded-lg transition-all border border-blue-200/50 dark:border-blue-800/50">
            전표 전체보기 →
          </Link>
        </div>

        <div className="space-y-2.5">
          {mockJournals.slice(0, 5).map((journal) => {
            const statusInfo = getStatusVariant(journal.status);
            const totalDebit = journal.lines.reduce((sum, line) => sum + line.debit, 0);
            const primaryDesc = journal.lines[0]?.desc || '전표 내역';

            return (
              <div 
                key={journal.id}
                className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl bg-[#f7f8fb] dark:bg-slate-900/60 hover:bg-blue-50/30 dark:hover:bg-slate-800/80 border border-[#eaedf4] dark:border-slate-800 hover:border-blue-200 dark:hover:border-slate-700 transition-all duration-200 gap-3"
              >
                <div className="flex items-center gap-3.5">
                  <div className="w-9 h-9 rounded-lg bg-white dark:bg-[#131b2e] text-[#4262ff] flex items-center justify-center font-mono text-xs font-bold border border-[#eaedf4] dark:border-slate-800 shadow-xs flex-shrink-0">
                    <FileText size={17} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2.5">
                      <span className="font-mono text-xs font-bold text-[#4262ff]">{journal.id}</span>
                      <StatusBadge status={statusInfo.label} variant={statusInfo.variant} />
                    </div>
                    <p className="text-xs sm:text-sm font-bold text-[#17191e] dark:text-slate-100 mt-0.5">{primaryDesc}</p>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-6 pt-2 sm:pt-0 border-t sm:border-t-0 border-[#eaedf4] dark:border-slate-800">
                  <div className="text-left sm:text-right">
                    <div className="text-[11px] text-[#8c94a4] dark:text-slate-400 flex items-center gap-1 font-medium">
                      <Clock size={11} /> {journal.date}
                    </div>
                    <div className="text-sm font-bold text-[#17191e] dark:text-slate-100 mt-0.5">
                      <AmountDisplay amount={totalDebit} />
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
