'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { 
  ShieldAlert, Activity, BarChart3, PieChart, TrendingUp, 
  AlertTriangle, RefreshCcw, Download,
  ArrowRight, Info, Scale
} from 'lucide-react';
import { riskService, RiskWeightedAsset } from '@/services/riskService';

/**
 * [리스크/RWA 대시보드]
 * Basel III 규제 준수를 위한 RWA(위험가중자산) 산출 현황 및 BIS 비율을 시각화합니다.
 */
export default function RiskDashboardPage() {
  const [rwaData, setRwaData] = useState<RiskWeightedAsset[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().split('T')[0];
  });

  const fetchRwaData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await riskService.getRwaResults(startDate, endDate);
      setRwaData(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '데이터 로드 실패');
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void fetchRwaData();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchRwaData]);

  // 집계 데이터
  const totalEad = rwaData.reduce((sum, item) => sum + item.eadAmount, 0);
  const totalRwa = rwaData.reduce((sum, item) => sum + item.rwaAmount, 0);
  const avgRw = totalEad > 0 ? (totalRwa / totalEad) * 100 : 0;
  
  // 자기자본 (임시값)
  const capital = 5000000000; // 50억
  const bisRatio = totalRwa > 0 ? (capital / totalRwa) * 100 : 0;

  return (
    <div className="flex flex-col gap-8 pb-20">
      <header className="flex justify-between items-center">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 bg-rose-500/10 rounded-lg">
              <ShieldAlert className="text-rose-400" size={24} />
            </div>
            <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">Risk & Capital Adequacy</h2>
          </div>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">Basel III 규제 준수 RWA 산출 및 BIS 비율 대시보드</p>
        </div>
        
        <div className="flex gap-2">
           <div className="flex bg-slate-900 border border-white/5 rounded-xl px-2 items-center mr-2">
              <input 
                type="date" 
                value={startDate} 
                onChange={(e) => setStartDate(e.target.value)}
                className="bg-transparent text-slate-300 text-xs font-bold p-2 focus:outline-none"
              />
              <span className="text-slate-600 font-black">~</span>
              <input 
                type="date" 
                value={endDate} 
                onChange={(e) => setEndDate(e.target.value)}
                className="bg-transparent text-slate-300 text-xs font-bold p-2 focus:outline-none"
              />
           </div>
           <button 
              onClick={fetchRwaData}
              className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg">
              <RefreshCcw size={18} className={loading ? 'animate-spin' : ''} /> 갱신
           </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-5 py-3 text-sm font-bold text-rose-300">
          {error}
        </div>
      )}

      {/* 핵심 지표 섹션 */}
      <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-slate-900/40 border border-white/10 rounded-3xl p-6 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <TrendingUp size={64} />
          </div>
          <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-2">Total Exposure (EAD)</span>
          <div className="text-3xl font-black text-white italic tracking-tighter">
            ₩{(totalEad / 100000000).toFixed(2)}<span className="text-sm ml-1 not-italic font-bold text-slate-500">억</span>
          </div>
          <div className="mt-4 flex items-center gap-2">
            <span className="text-[10px] font-bold text-emerald-500 bg-emerald-500/10 px-2 py-0.5 rounded">+2.4%</span>
            <span className="text-[10px] text-slate-600 font-bold uppercase">vs Prev Period</span>
          </div>
        </div>

        <div className="bg-slate-900/40 border border-white/10 rounded-3xl p-6 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <Activity size={64} />
          </div>
          <span className="text-[10px] font-black text-rose-500 uppercase tracking-widest block mb-2">Total RWA</span>
          <div className="text-3xl font-black text-white italic tracking-tighter">
            ₩{(totalRwa / 100000000).toFixed(2)}<span className="text-sm ml-1 not-italic font-bold text-slate-500">억</span>
          </div>
          <div className="mt-4 flex items-center gap-2 text-[10px] font-bold text-slate-400">
             Avg. Risk Weight: <span className="text-white ml-1">{avgRw.toFixed(2)}%</span>
          </div>
        </div>

        <div className="bg-gradient-to-br from-indigo-900/40 to-slate-900/40 border border-indigo-500/20 rounded-3xl p-6 backdrop-blur-xl relative overflow-hidden group shadow-2xl shadow-indigo-500/10">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <PieChart size={64} />
          </div>
          <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block mb-2">BIS Ratio</span>
          <div className={`text-4xl font-black italic tracking-tighter ${bisRatio >= 8 ? 'text-white' : 'text-rose-400'}`}>
            {bisRatio.toFixed(2)}%
          </div>
          <div className="mt-4 flex items-center gap-2">
            <div className="flex-grow h-1 bg-slate-800 rounded-full overflow-hidden">
              <div className="h-full bg-indigo-500 shadow-[0_0_10px_rgba(99,102,241,0.5)]" style={{width: `${Math.min(bisRatio, 100)}%`}} />
            </div>
            <span className="text-[10px] font-bold text-slate-500">Target 8.0%</span>
          </div>
        </div>

        <div className="bg-slate-900/40 border border-white/10 rounded-3xl p-6 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <Scale size={64} />
          </div>
          <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-2">Total Capital</span>
          <div className="text-3xl font-black text-white italic tracking-tighter">
            ₩{(capital / 100000000).toFixed(2)}<span className="text-sm ml-1 not-italic font-bold text-slate-500">억</span>
          </div>
          <div className="mt-4 flex items-center gap-2">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Tier 1 Capital</span>
          </div>
        </div>
      </section>

      {/* 차트 및 세부 분석 섹션 */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-8 bg-slate-900/40 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl shadow-2xl relative overflow-hidden group">
          <div className="flex justify-between items-center mb-10">
             <h3 className="text-lg font-black text-white italic uppercase flex items-center gap-3">
               <BarChart3 size={20} className="text-indigo-400" /> RWA Breakdown by Asset Class
             </h3>
             <button className="text-[10px] font-black text-slate-500 hover:text-white transition-colors uppercase tracking-[0.2em]">View Details</button>
          </div>
          
          <div className="h-[300px] flex items-end gap-4 pb-4">
             {/* 시각화 목업 (데이터 바) */}
             {[60, 45, 80, 30, 55].map((h, i) => (
               <div key={i} className="flex-grow group/bar relative">
                 <div 
                   className="w-full bg-gradient-to-t from-indigo-600/40 to-indigo-400 rounded-t-xl transition-all hover:scale-x-105 hover:brightness-110 shadow-lg shadow-indigo-500/10" 
                   style={{height: `${h}%`}} 
                 />
                 <div className="absolute -top-8 left-1/2 -translate-x-1/2 text-[10px] font-black text-white opacity-0 group-hover/bar:opacity-100 transition-opacity">₩{h}억</div>
                 <div className="mt-4 text-[10px] font-bold text-slate-500 text-center uppercase tracking-widest truncate">
                   {['Corp', 'Retail', 'Mort', 'Bank', 'Sov'][i]}
                 </div>
               </div>
             ))}
          </div>
        </div>

        <div className="lg:col-span-4 flex flex-col gap-6">
          <div className="bg-rose-600/10 border border-rose-500/20 rounded-[32px] p-8 group overflow-hidden relative">
            <div className="absolute -right-4 -bottom-4 opacity-5 group-hover:scale-110 transition-transform duration-700">
               <AlertTriangle size={150} className="text-rose-500" />
            </div>
            <h4 className="text-sm font-black text-rose-500 uppercase tracking-[0.2em] mb-4 flex items-center gap-2">
              <AlertTriangle size={16} /> Risk Alerts
            </h4>
            <div className="space-y-4 relative z-10">
              <div className="p-4 bg-slate-900/60 rounded-2xl border border-white/5">
                <p className="text-xs font-bold text-white mb-1">Concentration Risk Detected</p>
                <p className="text-[10px] text-slate-500 italic">Corporates 부문 익스포저가 한도를 초과했습니다.</p>
              </div>
              <div className="p-4 bg-slate-900/60 rounded-2xl border border-white/5">
                <p className="text-xs font-bold text-white mb-1">PD Migration Warning</p>
                <p className="text-[10px] text-slate-500 italic">RETAIL 부문 부도확률이 0.5%p 상승했습니다.</p>
              </div>
            </div>
          </div>
          
          <div className="bg-slate-900/60 border border-white/10 rounded-[32px] p-8 flex-grow flex flex-col justify-between group relative overflow-hidden">
            <div>
              <h4 className="text-sm font-black text-white uppercase tracking-[0.2em] mb-6 italic">Calculation Engine</h4>
              <p className="text-slate-500 text-xs font-medium leading-relaxed mb-8">
                Basel III 표준방법(SA) 기준 엔진이 활성화되어 있습니다. 최종 산출일: <span className="text-indigo-400 font-bold">2026.04.30</span>
              </p>
            </div>
            <div className="flex flex-col gap-2">
              <button className="w-full bg-white text-slate-950 font-black text-xs py-4 rounded-2xl transition-all hover:bg-indigo-400 hover:text-white shadow-xl active:scale-95 uppercase tracking-widest flex items-center justify-center gap-2">
                <RefreshCcw size={16} /> 신규 RWA 산출 실행
              </button>
              <button className="w-full bg-slate-800 text-slate-400 font-black text-xs py-4 rounded-2xl transition-all hover:bg-slate-700 active:scale-95 uppercase tracking-widest border border-white/5 flex items-center justify-center gap-2">
                <Download size={16} /> 규제 보고서 생성 (XBRL)
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* RWA 상세 리스트 */}
      <section className="bg-slate-900/40 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl shadow-2xl">
        <div className="p-8 border-b border-white/5 bg-white/[0.02] flex justify-between items-center">
            <h3 className="text-lg font-black text-white italic tracking-tight uppercase flex items-center gap-3 leading-none">
               <Info size={22} className="text-indigo-400" /> RWA Calculation Details
            </h3>
            <span className="text-[10px] font-bold text-slate-500 bg-slate-500/10 px-3 py-1 rounded-full border border-slate-500/20 uppercase tracking-widest leading-none">
              SA Method Applied
            </span>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-[0.15em] bg-white/[0.03]">
                <th className="px-8 py-6">Ref ID</th>
                <th className="px-6 py-6">Counterparty</th>
                <th className="px-6 py-6">Exposure (EAD)</th>
                <th className="px-6 py-6 text-center">RW (%)</th>
                <th className="px-6 py-6 text-right">RWA Amount</th>
                <th className="px-8 py-6 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-8 py-24 text-center text-slate-500 font-bold italic">
                    <div className="flex flex-col items-center gap-4">
                      <div className="w-6 h-6 border-2 border-rose-500 border-t-transparent rounded-full animate-spin" />
                      <span className="text-[10px] uppercase tracking-widest">분석 엔진 가동 중...</span>
                    </div>
                  </td>
                </tr>
              ) : rwaData.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-8 py-24 text-center text-slate-500 font-bold italic text-xs uppercase tracking-widest opacity-30">
                    조회된 리스크 데이터가 없습니다.
                  </td>
                </tr>
              ) : (
                rwaData.map((item) => (
                  <tr key={item.id} className="hover:bg-white/[0.03] transition-colors group cursor-pointer">
                    <td className="px-8 py-6">
                      <span className="text-xs font-mono text-slate-500 uppercase font-bold">{item.sourceReferenceId}</span>
                    </td>
                    <td className="px-6 py-6">
                      <span className="text-sm font-bold text-white tracking-tight">{item.businessPartnerName}</span>
                    </td>
                    <td className="px-6 py-6 text-sm font-mono text-slate-400 font-bold">
                      ₩{item.eadAmount.toLocaleString()}
                    </td>
                    <td className="px-6 py-6 text-center">
                      <span className="text-xs font-black text-rose-400 bg-rose-400/10 px-2 py-1 rounded">
                        {(item.riskWeight * 100).toFixed(0)}%
                      </span>
                    </td>
                    <td className="px-6 py-6 text-right">
                      <span className="text-sm font-black text-white italic tracking-tighter">
                        ₩{item.rwaAmount.toLocaleString()}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-right">
                       <button className="text-slate-600 hover:text-white transition-colors">
                          <ArrowRight size={18} />
                       </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
