'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  DollarSign,
  TrendingUp,
  RefreshCw,
  Plus,
  Search,
  Calendar,
  ArrowUpRight,
  ArrowDownRight,
  Globe
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  Tooltip, 
  Legend, 
  CartesianGrid 
} from 'recharts';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { 
  mockExchangeRates, 
  mockExchangeRateHistory, 
  ExchangeRateDto 
} from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function ExchangeRatesPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [rates] = useState<ExchangeRateDto[]>(mockExchangeRates);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isSyncing, setIsSyncing] = useState(false);

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 고시 환율' },
    { id: 'CONFIRMED', label: '확정 고시' },
    { id: 'PENDING', label: '검토 대기' },
  ];

  const filteredRates = rates.filter(r => {
    const matchesStatus = selectedStatus === 'ALL' || r.status === selectedStatus;
    const matchesSearch = 
      r.currencyPair.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.baseCurrency.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.source.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const handleSyncRates = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
      alert('서울외환중개 및 한국은행 실시간 고시환율 수신이 완료되었습니다.');
    }, 1000);
  };

  const usdRate = rates.find(r => r.baseCurrency === 'USD');
  const eurRate = rates.find(r => r.baseCurrency === 'EUR');
  const jpyRate = rates.find(r => r.baseCurrency === 'JPY');
  const cnyRate = rates.find(r => r.baseCurrency === 'CNY');

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="환율 관리"
        description="주요 통화별 최초/최종 고시 환율 조회, 기말 평가 환율 수신 및 추이 시각화"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '기준정보 / 마트' },
          { label: '환율 관리' },
        ]}
        icon={DollarSign}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleSyncRates}
              disabled={isSyncing}
              className="px-4 py-2.5 rounded-xl text-xs font-black bg-white/5 text-slate-300 hover:text-white border border-white/10 hover:bg-white/10 transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <RefreshCw size={14} className={isSyncing ? 'animate-spin text-blue-400' : 'text-blue-400'} />
              {isSyncing ? '환율 수신 중...' : '고시 환율 재수신'}
            </button>
            <button
              onClick={() => alert('수동 환율 등록 창을 활성화합니다.')}
              className="px-5 py-2.5 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Plus size={14} />
              수동 환율 등록
            </button>
          </div>
        }
      />

      {/* Key Exchange Rates Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {[usdRate, eurRate, jpyRate, cnyRate].map((r) => {
          if (!r) return null;
          const isUp = r.changeAmount >= 0;
          return (
            <div key={r.id} className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black text-slate-400 uppercase tracking-widest">{r.currencyPair}</span>
                <span className="text-[10px] text-slate-500 font-mono">{r.source}</span>
              </div>
              <div className="flex items-baseline justify-between">
                <div className="text-3xl font-black text-white font-mono">
                  {r.baseRate.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </div>
                <div className={`flex items-center text-xs font-bold ${isUp ? 'text-rose-400' : 'text-emerald-400'}`}>
                  {isUp ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
                  {isUp ? '+' : ''}{r.changeAmount} ({r.changeRate}%)
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2 text-[11px] pt-2 border-t border-white/5 font-mono text-slate-400">
                <div>현찰 매도: <span className="text-white">{r.ttSelling}</span></div>
                <div>현찰 매수: <span className="text-white">{r.ttBuying}</span></div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Recharts Line Chart for Exchange Rate Trend */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-black text-white flex items-center gap-2">
              <TrendingUp className="text-blue-400" size={18} />
              최근 7일 주요 통화 매매기준율 추이
            </h3>
            <p className="text-xs text-slate-400 mt-1">USD/KRW, EUR/KRW 환율 변동 모니터링</p>
          </div>
          <div className="flex items-center gap-2 text-xs font-mono text-slate-500">
            <Calendar size={14} /> 2026-07-21 ~ 2026-07-28
          </div>
        </div>

        <div className="h-72 w-full pt-4">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={mockExchangeRateHistory} margin={{ top: 10, right: 30, left: 10, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickLine={false} />
              <YAxis domain={['auto', 'auto']} stroke="#64748b" fontSize={11} tickLine={false} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#0f172a', 
                  borderColor: 'rgba(255,255,255,0.1)', 
                  borderRadius: '12px',
                  color: '#fff',
                  fontSize: '12px'
                }} 
              />
              <Legend wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }} />
              <Line type="monotone" dataKey="USD" name="USD/KRW" stroke="#3b82f6" strokeWidth={2.5} dot={{ r: 3 }} />
              <Line type="monotone" dataKey="EUR" name="EUR/KRW" stroke="#10b981" strokeWidth={2.5} dot={{ r: 3 }} />
              <Line type="monotone" dataKey="JPY" name="JPY(100)/KRW" stroke="#8b5cf6" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Full Exchange Rate Table */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <Tabs tabs={filterTabs} activeTab={selectedStatus} onChange={setSelectedStatus} />

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="통화쌍 / 고시기관 검색..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        {filteredRates.length === 0 ? (
          <EmptyState
            icon={Globe}
            title="고시 환율 내역이 없습니다"
            description="검색 조건과 일치하는 통화 고시 내역이 존재하지 않습니다."
          />
        ) : (
          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">고시 코드</th>
                  <th className="py-3.5 px-4">통화쌍</th>
                  <th className="py-3.5 px-4">기초/대상 통화</th>
                  <th className="py-3.5 px-4 text-right">매매기준율</th>
                  <th className="py-3.5 px-4 text-right">전일대비 변동</th>
                  <th className="py-3.5 px-4 text-right">현찰 매도율</th>
                  <th className="py-3.5 px-4 text-right">현찰 매수율</th>
                  <th className="py-3.5 px-4">고시 일자</th>
                  <th className="py-3.5 px-4">고시 기관</th>
                  <th className="py-3.5 px-4 text-center">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {filteredRates.map((r) => {
                  const isUp = r.changeAmount >= 0;
                  return (
                    <tr key={r.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3.5 px-4 font-mono text-slate-500">{r.id}</td>
                      <td className="py-3.5 px-4 font-bold text-blue-400 font-mono">{r.currencyPair}</td>
                      <td className="py-3.5 px-4 text-slate-400 font-mono">{r.baseCurrency} / {r.targetCurrency}</td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold text-white text-sm">
                        {r.baseRate.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td className={`py-3.5 px-4 text-right font-mono font-bold ${isUp ? 'text-rose-400' : 'text-emerald-400'}`}>
                        {isUp ? '+' : ''}{r.changeAmount} ({r.changeRate}%)
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono text-slate-300">{r.ttSelling.toLocaleString()}</td>
                      <td className="py-3.5 px-4 text-right font-mono text-slate-300">{r.ttBuying.toLocaleString()}</td>
                      <td className="py-3.5 px-4 font-mono text-slate-400">{r.officialRateDate}</td>
                      <td className="py-3.5 px-4 text-slate-300">{r.source}</td>
                      <td className="py-3.5 px-4 text-center">
                        <StatusBadge 
                          status={r.status === 'CONFIRMED' ? '확정 고시' : '검토 대기'} 
                          variant={r.status === 'CONFIRMED' ? 'success' : 'warning'} 
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
