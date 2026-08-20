'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  Wallet, 
  Calendar, 
  ArrowUpRight, 
  ArrowDownRight, 
  Clock, 
  CheckCircle2, 
  Layers, 
  Plus, 
  Search,
  SlidersHorizontal
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  ComposedChart, 
  Area, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  Legend, 
  CartesianGrid 
} from 'recharts';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockCashflow, CashflowItem } from '@/mocks/expenditure';

const emptySubscribe = () => () => {};

export default function CashflowPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [cashflowItems, setCashflowItems] = useState<CashflowItem[]>(mockCashflow.items);
  const [activeFilter, setActiveFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 수지 항목' },
    { id: 'INFLOW', label: '자금 유입 (+)' },
    { id: 'OUTFLOW', label: '자금 유출 (-)' },
    { id: 'EXECUTED', label: '집행 완료' },
    { id: 'PLANNED', label: '예정/계획' },
  ];

  const filteredItems = cashflowItems.filter(item => {
    const matchesTab = 
      activeFilter === 'ALL' || 
      item.type === activeFilter || 
      item.status === activeFilter;
    const matchesSearch = 
      item.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.counterparty.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'EXECUTED': return 'success';
      case 'CONFIRMED': return 'info';
      case 'PLANNED': return 'warning';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'EXECUTED': return '집행완료';
      case 'CONFIRMED': return '확정';
      case 'PLANNED': return '계획';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="자금 수지 계획"
        description="기간별 자금 유입/유출 수지를 예측하고 주차별 자금 수지 트렌드 및 유동성 리스크를 관리합니다."
        breadcrumbs={[
          { label: '지출관리' },
          { label: '자금 수지 계획' }
        ]}
        icon={TrendingUp}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-slate-300 text-xs font-black transition-all flex items-center gap-2">
              <SlidersHorizontal size={14} /> 수지 시뮬레이션
            </button>
            <button className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
              <Plus size={14} /> 수지 계획 등록
            </button>
          </div>
        }
      />

      {/* KPI Cards: Inflow, Outflow, Net, Ending Balance */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>기초 자금 잔액</span>
            <Wallet size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white italic tracking-tight font-mono">
            <AmountDisplay amount={mockCashflow.summary.beginningBalance} />
          </div>
          <div className="text-xs text-slate-400 font-medium">당월 이월 현금성 자산</div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>예상 자금 유입 (+)</span>
            <ArrowUpRight size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400 italic tracking-tight font-mono">
            <AmountDisplay amount={mockCashflow.summary.expectedInflow} />
          </div>
          <div className="text-xs text-emerald-400/80 font-medium">AR 수금 및 기타 유입액</div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>예상 자금 유출 (-)</span>
            <ArrowDownRight size={18} className="text-rose-400" />
          </div>
          <div className="text-2xl font-black text-rose-400 italic tracking-tight font-mono">
            <AmountDisplay amount={mockCashflow.summary.expectedOutflow} />
          </div>
          <div className="text-xs text-rose-400/80 font-medium">AP 지급, 인건비, 운영비</div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>기말 잔액 예측</span>
            <TrendingUp size={18} className="text-indigo-400" />
          </div>
          <div className="text-2xl font-black text-indigo-400 italic tracking-tight font-mono">
            <AmountDisplay amount={mockCashflow.summary.endingBalance} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            순수지 흐름: <span className="text-emerald-400 font-black">+₩{mockCashflow.summary.netCashflow.toLocaleString()}</span>
          </div>
        </div>
      </div>

      {/* Weekly Cashflow Trends Recharts */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
        <div className="flex items-center justify-between border-b border-white/5 pb-4">
          <div>
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <Layers className="text-blue-400" size={18} />
              주차별 자금 수지 동향 및 예측 트렌드 (단위: 원)
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              주차별 자금 유입 vs 유출 vs 순자금흐름(Net Cashflow) 그래프
            </p>
          </div>
        </div>

        <div className="h-64 w-full pt-4">
          {mounted ? (
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={mockCashflow.weeklyTrends} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                <defs>
                  <linearGradient id="colorNet" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.3} />
                <XAxis dataKey="week" stroke="#94a3b8" tick={{ fontSize: 12 }} />
                <YAxis 
                  stroke="#94a3b8" 
                  tick={{ fontSize: 12 }}
                  tickFormatter={(val) => `${(val / 10000000).toFixed(0)}천만`}
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
                <Legend />
                <Bar dataKey="inflow" name="자금 유입" fill="#10b981" radius={[4, 4, 0, 0]} />
                <Bar dataKey="outflow" name="자금 유출" fill="#f43f5e" radius={[4, 4, 0, 0]} />
                <Area type="monotone" dataKey="net" name="순자금 흐름" stroke="#6366f1" strokeWidth={3} fill="url(#colorNet)" />
              </ComposedChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
          )}
        </div>
      </div>

      {/* Cashflow Schedule Table */}
      <div className="space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={filterTabs} activeTab={activeFilter} onChange={setActiveFilter} />

          <div className="relative w-full md:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="내용, 거래처, 분류 검색"
              className="w-full bg-slate-900/50 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-all font-medium"
            />
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                <th className="py-3.5 px-4">예정/집행일</th>
                <th className="py-3.5 px-4">구분</th>
                <th className="py-3.5 px-4">수지 분류</th>
                <th className="py-3.5 px-4">적요 및 상세내용</th>
                <th className="py-3.5 px-4">거래상대방</th>
                <th className="py-3.5 px-4 text-right">금액</th>
                <th className="py-3.5 px-4 text-center">출처 모듈</th>
                <th className="py-3.5 px-4 text-center">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {filteredItems.map((item) => (
                <tr key={item.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-4 px-4 font-mono text-white font-bold">{item.date}</td>
                  <td className="py-4 px-4">
                    {item.type === 'INFLOW' ? (
                      <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-black">
                        유입 (+)
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20 font-black">
                        유출 (-)
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-4 font-bold text-slate-300">{item.category}</td>
                  <td className="py-4 px-4 text-white font-medium">{item.description}</td>
                  <td className="py-4 px-4 text-slate-400 font-bold">{item.counterparty}</td>
                  <td className="py-4 px-4 text-right font-mono font-black italic text-sm">
                    <AmountDisplay 
                      amount={item.type === 'OUTFLOW' ? -item.amount : item.amount} 
                      showSign={true} 
                    />
                  </td>
                  <td className="py-4 px-4 text-center font-mono text-slate-400 font-bold">
                    {item.sourceModule}
                  </td>
                  <td className="py-4 px-4 text-center">
                    <StatusBadge 
                      status={getStatusLabel(item.status)} 
                      variant={getStatusVariant(item.status)} 
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
