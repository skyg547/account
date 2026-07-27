'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { 
  DollarSign, 
  Search, 
  AlertCircle, 
  CheckCircle2, 
  Clock, 
  TrendingUp, 
  Building, 
  Mail, 
  FileCheck,
  Calendar,
  BarChart3
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  CartesianGrid, 
  Cell 
} from 'recharts';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockReceivables, ReceivableDto } from '@/mocks/expenditure';

const emptySubscribe = () => () => {};

export default function ReceivablePage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [receivables, setReceivables] = useState<ReceivableDto[]>(mockReceivables);
  const [selectedAging, setSelectedAging] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Aging Summary Calculation
  const agingCategories = [
    { key: 'CURRENT', label: '정상 (Current)', color: '#10b981' },
    { key: '1-30', label: '1 ~ 30일', color: '#3b82f6' },
    { key: '31-60', label: '31 ~ 60일', color: '#f59e0b' },
    { key: '61-90', label: '61 ~ 90일', color: '#f97316' },
    { key: '90+', label: '90일 초과', color: '#ef4444' },
  ];

  const agingChartData = agingCategories.map(cat => {
    const items = receivables.filter(r => r.agingCategory === cat.key && r.balance > 0);
    const totalBalance = items.reduce((sum, item) => sum + item.balance, 0);
    return {
      category: cat.label,
      key: cat.key,
      amount: totalBalance,
      count: items.length,
      color: cat.color
    };
  });

  const totalOutstanding = receivables.reduce((sum, r) => sum + r.balance, 0);
  const totalOverdue = receivables.filter(r => r.status === 'OVERDUE').reduce((sum, r) => sum + r.balance, 0);
  const overdueRatio = totalOutstanding > 0 ? ((totalOverdue / totalOutstanding) * 100).toFixed(1) : '0';

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 채권' },
    { id: 'CURRENT', label: '정상 채권' },
    { id: '1-30', label: '1~30일 연체' },
    { id: '31-60', label: '31~60일 연체' },
    { id: '61-90', label: '61~90일 연체' },
    { id: '90+', label: '90일+ 부실채권' },
  ];

  const filteredReceivables = receivables.filter(item => {
    const matchesCategory = selectedAging === 'ALL' || item.agingCategory === selectedAging;
    const matchesSearch = 
      item.arNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.invoiceNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'PARTIAL': return 'info';
      case 'UNCOLLECTED': return 'warning';
      case 'OVERDUE': return 'error';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'COMPLETED': return '수금완료';
      case 'PARTIAL': return '부분수금';
      case 'UNCOLLECTED': return '미수금';
      case 'OVERDUE': return '연체발생';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="매출채권 AR 관리"
        description="고객사 매출채권(Accounts Receivable) 잔액 트래킹, 수금 기한 연체 독촉 및 회수 관리를 수행합니다."
        breadcrumbs={[
          { label: '지출/수금관리' },
          { label: '매출채권 AR 관리' }
        ]}
        icon={DollarSign}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-slate-300 text-xs font-black transition-all flex items-center gap-2">
              <Mail size={14} /> 연체 독촉장 일괄 발행
            </button>
            <button className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
              <FileCheck size={14} /> 수금 매칭 등록
            </button>
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>총 매출채권 잔액</span>
            <Building size={18} className="text-blue-400" />
          </div>
          <div className="text-3xl font-black text-white italic tracking-tight font-mono">
            <AmountDisplay amount={totalOutstanding} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            회수 대상 거래처: <span className="text-white font-black">{receivables.filter(r=>r.balance>0).length}개사</span>
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>연체 채권 금액</span>
            <AlertCircle size={18} className="text-rose-400" />
          </div>
          <div className="text-3xl font-black text-rose-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalOverdue} />
          </div>
          <div className="text-xs text-rose-400/80 font-medium">
            전체 채권 대비 연체율 <span className="font-black underline">{overdueRatio}%</span>
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>이번 달 수금 예상금</span>
            <TrendingUp size={18} className="text-emerald-400" />
          </div>
          <div className="text-3xl font-black text-emerald-400 italic tracking-tight font-mono">
            <AmountDisplay amount={155000000} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            주요 고객: 현대자동차(주), (주)엔씨소프트
          </div>
        </div>
      </div>

      {/* AR Aging Chart */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
        <div className="flex items-center justify-between border-b border-white/5 pb-4">
          <div>
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <BarChart3 className="text-emerald-400" size={18} />
              매출채권 연령 분석 그래프 (AR Aging Analysis)
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              수금 만기 경과일수에 따른 회수 리스크 구간별 채권 잔액 분포
            </p>
          </div>
        </div>

        <div className="h-64 w-full pt-4">
          {mounted ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={agingChartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.3} />
                <XAxis dataKey="category" stroke="#94a3b8" tick={{ fontSize: 12 }} />
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
                  formatter={(value) => [`₩${Number(value || 0).toLocaleString()}`, '채권 잔액']}
                />
                <Bar dataKey="amount" radius={[8, 8, 0, 0]}>
                  {agingChartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
          )}
        </div>
      </div>

      {/* AR List Table */}
      <div className="space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={filterTabs} activeTab={selectedAging} onChange={setSelectedAging} />
          
          <div className="relative w-full md:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="AR번호, 고객사명, 세금계산서 검색"
              className="w-full bg-slate-900/50 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-all font-medium"
            />
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                <th className="py-3.5 px-4">AR 번호</th>
                <th className="py-3.5 px-4">고객사명</th>
                <th className="py-3.5 px-4">세금계산서 번호</th>
                <th className="py-3.5 px-4">청구일 / 수금만기일</th>
                <th className="py-3.5 px-4 text-right">총 채권액</th>
                <th className="py-3.5 px-4 text-right">회수 완료액</th>
                <th className="py-3.5 px-4 text-right">미수 잔액</th>
                <th className="py-3.5 px-4 text-center">연체일수</th>
                <th className="py-3.5 px-4 text-center">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {filteredReceivables.map((item) => (
                <tr key={item.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-4 px-4 font-mono font-bold text-emerald-400">{item.arNumber}</td>
                  <td className="py-4 px-4 font-black text-white">{item.customerName}</td>
                  <td className="py-4 px-4 font-mono text-slate-400">{item.invoiceNumber}</td>
                  <td className="py-4 px-4 text-slate-300 font-medium">
                    <div>{item.issueDate}</div>
                    <div className="text-[11px] text-slate-500">만기: {item.dueDate}</div>
                  </td>
                  <td className="py-4 px-4 text-right font-mono font-bold text-white">
                    <AmountDisplay amount={item.amount} />
                  </td>
                  <td className="py-4 px-4 text-right font-mono text-slate-400">
                    <AmountDisplay amount={item.collectedAmount} />
                  </td>
                  <td className="py-4 px-4 text-right font-mono font-black text-blue-400 italic text-sm">
                    <AmountDisplay amount={item.balance} />
                  </td>
                  <td className="py-4 px-4 text-center">
                    {item.agingDays > 0 ? (
                      <span className="px-2.5 py-1 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20 font-mono font-black">
                        +{item.agingDays}일
                      </span>
                    ) : (
                      <span className="text-slate-500 font-mono">-</span>
                    )}
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
