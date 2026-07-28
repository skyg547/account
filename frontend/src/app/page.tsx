'use client';

import React, { useSyncExternalStore } from 'react';
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
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';

const emptySubscribe = () => () => {};

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
        description="AI 실시간 재무 데이터 및 실시간 전표 유입 처리 현황 모니터링"
        breadcrumbs={[
          { label: '대시보드' },
          { label: '통합 재무 현황' }
        ]}
        icon={PieChart}
        actions={
          <div className="flex items-center gap-3 bg-slate-900/50 backdrop-blur-md px-4 py-2 rounded-2xl border border-white/5 text-xs text-slate-400">
            <Calendar size={14} className="text-blue-400" />
            <span>기준일: {new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
          </div>
        }
      />

      {/* 3 KPI Glassmorphism Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Card 1: Total Assets */}
        <div className="relative overflow-hidden rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 hover:border-blue-500/30 transition-all duration-300 group shadow-xl">
          <div className="flex items-center justify-between mb-5">
            <div className="w-13 h-13 rounded-2xl bg-blue-500/10 flex items-center justify-center text-blue-400 border border-blue-500/20 group-hover:scale-110 transition-transform">
              <Wallet size={26} />
            </div>
            <span className="flex items-center gap-1 text-xs font-black text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
              <TrendingUp size={14} /> +12.4%
            </span>
          </div>
          <p className="text-xs font-black uppercase tracking-widest text-slate-400 mb-1">Total Assets (총 자산)</p>
          <div className="text-3xl font-black text-white italic tracking-tight mb-4">
            <AmountDisplay amount={mockKpis.totalAssets} />
          </div>
          <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-blue-600 to-indigo-400 w-[78%] rounded-full" />
          </div>
          <div className="mt-3 flex justify-between text-[11px] font-medium text-slate-500">
            <span>목표 달성률 88%</span>
            <span>전월 대비 상승</span>
          </div>
        </div>

        {/* Card 2: Total Liabilities */}
        <div className="relative overflow-hidden rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 hover:border-rose-500/30 transition-all duration-300 group shadow-xl">
          <div className="flex items-center justify-between mb-5">
            <div className="w-13 h-13 rounded-2xl bg-rose-500/10 flex items-center justify-center text-rose-400 border border-rose-500/20 group-hover:scale-110 transition-transform">
              <CreditCard size={26} />
            </div>
            <span className="flex items-center gap-1 text-xs font-black text-rose-400 bg-rose-500/10 px-3 py-1 rounded-full border border-rose-500/20">
              <TrendingDown size={14} /> -2.1%
            </span>
          </div>
          <p className="text-xs font-black uppercase tracking-widest text-slate-400 mb-1">Total Liabilities (총 부채)</p>
          <div className="text-3xl font-black text-white italic tracking-tight mb-4">
            <AmountDisplay amount={mockKpis.totalLiabilities} />
          </div>
          <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-rose-600 to-amber-400 w-[36%] rounded-full" />
          </div>
          <div className="mt-3 flex justify-between text-[11px] font-medium text-slate-500">
            <span>부채 비율 36.3%</span>
            <span>안정 수준 유지</span>
          </div>
        </div>

        {/* Card 3: Net Income */}
        <div className="relative overflow-hidden rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 hover:border-emerald-500/30 transition-all duration-300 group shadow-xl">
          <div className="flex items-center justify-between mb-5">
            <div className="w-13 h-13 rounded-2xl bg-emerald-500/10 flex items-center justify-center text-emerald-400 border border-emerald-500/20 group-hover:scale-110 transition-transform">
              <ArrowUpRight size={26} />
            </div>
            <span className="flex items-center gap-1 text-xs font-black text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
              <TrendingUp size={14} /> +18.5%
            </span>
          </div>
          <p className="text-xs font-black uppercase tracking-widest text-slate-400 mb-1">Net Income (당기순이익)</p>
          <div className="text-3xl font-black text-white italic tracking-tight mb-4">
            <AmountDisplay amount={mockKpis.netIncome} />
          </div>
          <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-emerald-600 to-teal-400 w-[64%] rounded-full" />
          </div>
          <div className="mt-3 flex justify-between text-[11px] font-medium text-slate-500">
            <span>순이익률 63.7%</span>
            <span>목표 대비 초과</span>
          </div>
        </div>
      </div>

      {/* Monthly Trends Area Chart */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-xl font-black text-white tracking-tight flex items-center gap-2">
              <Layers className="text-blue-400" size={20} />
              월별 재무 추이 분석
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              최근 6개월 자산 및 부채 변화 트렌드 (단위: 원)
            </p>
          </div>
          <div className="flex items-center gap-4 text-xs font-bold">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-blue-500 inline-block" />
              <span className="text-slate-300">총 자산</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-rose-500 inline-block" />
              <span className="text-slate-300">총 부채</span>
            </div>
          </div>
        </div>

        <div className="h-72 w-full pt-4">
          {mounted ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={mockKpis.monthlyTrends} margin={{ top: 10, right: 10, left: 20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorAssets" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorLiabilities" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#f43f5e" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.3} />
                <XAxis dataKey="month" stroke="#64748b" tick={{ fontSize: 12 }} />
                <YAxis 
                  stroke="#64748b" 
                  tick={{ fontSize: 12 }} 
                  tickFormatter={(val) => `${(val / 100000000).toFixed(1)}억`}
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: '#0f172a', 
                    borderColor: 'rgba(255,255,255,0.1)', 
                    borderRadius: '16px', 
                    color: '#fff',
                    boxShadow: '0 20px 25px -5px rgba(0,0,0,0.5)'
                  }} 
                  formatter={(value) => [`₩${Number(value || 0).toLocaleString()}`, '']}
                />
                <Area type="monotone" dataKey="assets" name="총 자산" stroke="#3b82f6" strokeWidth={3} fillOpacity={1} fill="url(#colorAssets)" />
                <Area type="monotone" dataKey="liabilities" name="총 부채" stroke="#f43f5e" strokeWidth={3} fillOpacity={1} fill="url(#colorLiabilities)" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
          )}
        </div>
      </div>

      {/* Recent Journals Section */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h3 className="text-xl font-black text-white tracking-tight flex items-center gap-2">
              <FileText className="text-blue-400" size={20} />
              최근 발생 전표 내역
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              최근에 등록 및 검토 처리된 주요 전표 리스트
            </p>
          </div>
          <a href="/ledger/list" className="text-xs font-black text-blue-400 hover:text-blue-300 uppercase tracking-widest bg-blue-500/10 hover:bg-blue-500/20 px-4 py-2 rounded-xl transition-all border border-blue-500/20">
            전표 전체보기 →
          </a>
        </div>

        <div className="space-y-3">
          {mockJournals.slice(0, 5).map((journal) => {
            const statusInfo = getStatusVariant(journal.status);
            const totalDebit = journal.lines.reduce((sum, line) => sum + line.debit, 0);
            const primaryDesc = journal.lines[0]?.desc || '전표 내역';

            return (
              <div 
                key={journal.id}
                className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-2xl bg-white/[0.02] hover:bg-white/[0.06] border border-white/5 hover:border-white/10 transition-all duration-200 gap-4"
              >
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center font-mono text-xs font-bold border border-blue-500/20 flex-shrink-0">
                    <FileText size={18} />
                  </div>
                  <div>
                    <div className="flex items-center gap-3">
                      <span className="font-mono text-xs font-black text-blue-400">{journal.id}</span>
                      <StatusBadge status={statusInfo.label} variant={statusInfo.variant} />
                    </div>
                    <p className="text-sm font-bold text-white mt-1">{primaryDesc}</p>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-6 pt-2 sm:pt-0 border-t sm:border-t-0 border-white/5">
                  <div className="text-left sm:text-right">
                    <div className="text-xs text-slate-500 flex items-center gap-1 font-medium">
                      <Clock size={12} /> {journal.date}
                    </div>
                    <div className="text-base font-black text-white italic tracking-tight mt-0.5">
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
