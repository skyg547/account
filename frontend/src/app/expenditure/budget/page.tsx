'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  Calculator,
  Search,
  CheckCircle2,
  TrendingUp,
  Plus,
  ArrowRightLeft,
  BarChart3,
  PieChart
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  BarChart, 
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
import { mockBudget, BudgetItem } from '@/mocks/expenditure';

const emptySubscribe = () => () => {};

export default function BudgetPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [budgetList] = useState<BudgetItem[]>(mockBudget);
  const [selectedDept, setSelectedDept] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const totalAnnual = budgetList.reduce((sum, b) => sum + b.annualBudget, 0);
  const totalAllocated = budgetList.reduce((sum, b) => sum + b.allocatedBudget, 0);
  const totalUsed = budgetList.reduce((sum, b) => sum + b.usedAmount, 0);
  const totalEncumbered = budgetList.reduce((sum, b) => sum + b.encumberedAmount, 0);
  const totalRate = totalAllocated > 0 ? (((totalUsed + totalEncumbered) / totalAllocated) * 100).toFixed(1) : '0';

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 부서' },
    { id: 'IT개발실', label: 'IT개발실' },
    { id: '마케팅본부', label: '마케팅본부' },
    { id: '경영지원실', label: '경영지원실' },
    { id: '영업본부', label: '영업본부' },
  ];

  const filteredBudget = budgetList.filter(item => {
    const matchesDept = selectedDept === 'ALL' || item.department === selectedDept;
    const matchesSearch = 
      item.accountName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.accountCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.costCenter.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesDept && matchesSearch;
  });

  const chartData = filteredBudget.map(item => ({
    name: item.accountName.substring(0, 10),
    allocated: item.allocatedBudget / 1000000,
    used: item.usedAmount / 1000000,
    encumbered: item.encumberedAmount / 1000000,
  }));

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'NORMAL': return 'success';
      case 'WARNING': return 'warning';
      case 'EXCEEDED': return 'error';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'NORMAL': return '정상';
      case 'WARNING': return '임계경고 (90%+)';
      case 'EXCEEDED': return '예산초과';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="예산 편성 및 통제"
        description="부서 및 코스트센터별 예산 편성 금액 대비 실시간 집행실적 통제 모니터링을 수행합니다."
        breadcrumbs={[
          { label: '지출관리' },
          { label: '예산 편성/통제' }
        ]}
        icon={Calculator}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-slate-300 text-xs font-black transition-all flex items-center gap-2">
              <ArrowRightLeft size={14} /> 예산 전용/조정
            </button>
            <button className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
              <Plus size={14} /> 신규 예산 편성
            </button>
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>배정 예산 총액</span>
            <Calculator size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white italic tracking-tight font-mono">
            <AmountDisplay amount={totalAllocated} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            연간 예산 총액: ₩{totalAnnual.toLocaleString()}
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>실 집행 누적액</span>
            <CheckCircle2 size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalUsed} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            전표 확정 집행 완료금
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>집행 예정액 (결의중)</span>
            <TrendingUp size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-black text-amber-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalEncumbered} />
          </div>
          <div className="text-xs text-amber-400/80 font-medium">
            상신 승인 대기중 결의금
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>통합 예산 소진율</span>
            <PieChart size={18} className="text-indigo-400" />
          </div>
          <div className="text-3xl font-black text-indigo-400 italic tracking-tight font-mono">
            {totalRate}%
          </div>
          <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden mt-1">
            <div className="bg-indigo-500 h-full rounded-full" style={{ width: `${Math.min(Number(totalRate), 100)}%` }} />
          </div>
        </div>
      </div>

      {/* Budget vs Actual Comparison Recharts */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
        <div className="flex items-center justify-between border-b border-white/5 pb-4">
          <div>
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <BarChart3 className="text-blue-400" size={18} />
              계정과목별 예산 배정 vs 집행실적 비교 (단위: 백만원)
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              배정 예산 대비 확정 집행액 및 승인 대기 집행 예정액 현황
            </p>
          </div>
        </div>

        <div className="h-64 w-full pt-4">
          {mounted ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.3} />
                <XAxis dataKey="name" stroke="#94a3b8" tick={{ fontSize: 12 }} />
                <YAxis stroke="#94a3b8" tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: 'rgba(255,255,255,0.1)',
                    borderRadius: '16px',
                    color: '#fff',
                    boxShadow: '0 20px 25px -5px rgba(0,0,0,0.5)'
                  }}
                  formatter={(value) => [`${Number(value || 0).toLocaleString()} 백만원`, '']}
                />
                <Legend />
                <Bar dataKey="allocated" name="배정 예산" fill="#3b82f6" radius={[6, 6, 0, 0]} />
                <Bar dataKey="used" name="실 집행액" fill="#10b981" radius={[6, 6, 0, 0]} />
                <Bar dataKey="encumbered" name="집행 예정액" fill="#f59e0b" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
          )}
        </div>
      </div>

      {/* Budget List Table & Filter */}
      <div className="space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={filterTabs} activeTab={selectedDept} onChange={setSelectedDept} />
          
          <div className="relative w-full md:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="계정명, 코드, 코스트센터 검색"
              className="w-full bg-slate-900/50 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-all font-medium"
            />
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                <th className="py-3.5 px-4">부서 / 코스트센터</th>
                <th className="py-3.5 px-4">계정과목</th>
                <th className="py-3.5 px-4 text-right">배정 예산</th>
                <th className="py-3.5 px-4 text-right">실 집행액</th>
                <th className="py-3.5 px-4 text-right">집행 예정액</th>
                <th className="py-3.5 px-4 text-right">잔여 예산</th>
                <th className="py-3.5 px-4 text-center">소진율</th>
                <th className="py-3.5 px-4 text-center">통제 상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {filteredBudget.map((item) => (
                <tr key={item.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-4 px-4">
                    <div className="font-black text-white">{item.department}</div>
                    <div className="text-[11px] text-slate-500 font-mono">{item.costCenter}</div>
                  </td>
                  <td className="py-4 px-4">
                    <div className="font-bold text-white">{item.accountName}</div>
                    <div className="text-[11px] text-blue-400 font-mono">{item.accountCode}</div>
                  </td>
                  <td className="py-4 px-4 text-right font-mono font-bold text-white"><AmountDisplay amount={item.allocatedBudget} /></td>
                  <td className="py-4 px-4 text-right font-mono text-emerald-400"><AmountDisplay amount={item.usedAmount} /></td>
                  <td className="py-4 px-4 text-right font-mono text-amber-400"><AmountDisplay amount={item.encumberedAmount} /></td>
                  <td className="py-4 px-4 text-right font-mono font-black italic">
                    <AmountDisplay amount={item.remainingAmount} />
                  </td>
                  <td className="py-4 px-4 text-center font-mono font-black text-slate-200">
                    {item.utilizationRate}%
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
