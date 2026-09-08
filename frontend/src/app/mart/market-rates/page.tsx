'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  Percent,
  Search,
  RefreshCw,
  Plus,
  ArrowUpRight,
  ArrowDownRight
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockMarketRates, MarketRateDto } from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function MarketRatesPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [rates] = useState<MarketRateDto[]>(mockMarketRates);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isSyncing, setIsSyncing] = useState(false);

  const categoryTabs: TabItem[] = [
    { id: 'ALL', label: '전체 금리' },
    { id: '단기지표금리', label: '단기 지표 금리' },
    { id: '국채/회사채', label: '국채 / 회사채' },
    { id: '해외지표금리', label: '해외 지표 금리' },
    { id: '코픽스/기타', label: 'COFIX / 기타' },
  ];

  const filteredRates = rates.filter(r => {
    const matchesCat = selectedCategory === 'ALL' || r.category === selectedCategory;
    const matchesSearch = 
      r.rateName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.rateCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.provider.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  const handleSyncMarketRates = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
      alert('금융투자협회 및 인포맥스 시장금리 수신 완료되었습니다.');
    }, 900);
  };

  const bokRate = rates.find(r => r.rateCode === 'BOK_BASE');
  const cd91Rate = rates.find(r => r.rateCode === 'CD91');
  const koriborRate = rates.find(r => r.rateCode === 'KORIBOR3M');
  const sofrRate = rates.find(r => r.rateCode === 'SOFR_1M');

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="시장금리 관리"
        description="기준금리, CD 91일물, KORIBOR, SOFR 등 단기 및 국채 시장금리 고시 현황 관리"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '기준정보 / 마트' },
          { label: '시장금리 관리' },
        ]}
        icon={Percent}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleSyncMarketRates}
              disabled={isSyncing}
              className="px-4 py-2.5 rounded-xl text-xs font-black bg-white/5 text-slate-300 hover:text-white border border-white/10 hover:bg-white/10 transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <RefreshCw size={14} className={isSyncing ? 'animate-spin text-blue-400' : 'text-blue-400'} />
              {isSyncing ? '동기화 중...' : '시장금리 수신'}
            </button>
            <button
              onClick={() => alert('신규 시장금리 등록 창을 시작합니다.')}
              className="px-5 py-2.5 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Plus size={14} />
              신규 금리 등록
            </button>
          </div>
        }
      />

      {/* Top 4 Key Rate Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {[bokRate, cd91Rate, koriborRate, sofrRate].map((r) => {
          if (!r) return null;
          const isUp = r.changeBp > 0;
          const isZero = r.changeBp === 0;
          return (
            <div key={r.id} className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black text-slate-400 uppercase tracking-widest">{r.rateName}</span>
                <span className="text-[10px] text-slate-500 font-mono">{r.currency}</span>
              </div>
              <div className="flex items-baseline justify-between">
                <div className="text-3xl font-black text-white font-mono">
                  {r.rateValue.toFixed(2)}<span className="text-base text-slate-400">%</span>
                </div>
                <div className={`flex items-center text-xs font-bold ${
                  isZero ? 'text-slate-400' : isUp ? 'text-rose-400' : 'text-emerald-400'
                }`}>
                  {!isZero && (isUp ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />)}
                  {isZero ? '보합' : `${isUp ? '+' : ''}${r.changeBp} bp`}
                </div>
              </div>
              <div className="flex items-center justify-between text-[11px] pt-2 border-t border-white/5 font-mono text-slate-400">
                <span>전일: {r.prevRateValue.toFixed(2)}%</span>
                <span className="text-slate-500">{r.provider}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Market Rate Table Section */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <Tabs tabs={categoryTabs} activeTab={selectedCategory} onChange={setSelectedCategory} />

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="금리명 / 금리코드 / 제공기관..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        {filteredRates.length === 0 ? (
          <EmptyState
            icon={Percent}
            title="조회된 시장금리가 없습니다"
            description="선택한 카테고리 또는 검색 조건과 매칭되는 금리 항목이 없습니다."
          />
        ) : (
          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">금리 코드</th>
                  <th className="py-3.5 px-4">금리명</th>
                  <th className="py-3.5 px-4">카테고리</th>
                  <th className="py-3.5 px-4 text-right">고시 금리 (%)</th>
                  <th className="py-3.5 px-4 text-right">전일 금리 (%)</th>
                  <th className="py-3.5 px-4 text-right">변동 (bp)</th>
                  <th className="py-3.5 px-4">통화</th>
                  <th className="py-3.5 px-4">고시 일자</th>
                  <th className="py-3.5 px-4">제공 기관</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {filteredRates.map((r) => {
                  const isUp = r.changeBp > 0;
                  const isZero = r.changeBp === 0;
                  return (
                    <tr key={r.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3.5 px-4 font-mono font-bold text-blue-400">{r.rateCode}</td>
                      <td className="py-3.5 px-4 font-bold text-white">{r.rateName}</td>
                      <td className="py-3.5 px-4">
                        <span className="px-2 py-0.5 rounded bg-white/5 text-slate-300 font-medium">
                          {r.category}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-black text-white text-sm">
                        {r.rateValue.toFixed(2)} %
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono text-slate-400">
                        {r.prevRateValue.toFixed(2)} %
                      </td>
                      <td className={`py-3.5 px-4 text-right font-mono font-bold ${
                        isZero ? 'text-slate-500' : isUp ? 'text-rose-400' : 'text-emerald-400'
                      }`}>
                        {isZero ? '0.0 bp' : `${isUp ? '+' : ''}${r.changeBp} bp`}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-400">{r.currency}</td>
                      <td className="py-3.5 px-4 font-mono text-slate-400">{r.baseDate}</td>
                      <td className="py-3.5 px-4 text-slate-300">{r.provider}</td>
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
