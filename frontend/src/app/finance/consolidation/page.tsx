"use client";

import React from 'react';
import { 
  Network, 
  Plus, 
  Search, 
  Activity, 
  Layers, 
  RefreshCcw, 
  ArrowRight,
  Database,
  ShieldCheck,
  Zap,
  Globe
} from 'lucide-react';

/**
 * [연결 회계 및 지분법 기초 데이터 화면]
 * 종속회사 및 관계회사의 재무 데이터를 취합하고 투자 자본 상계 등 연결 조정의 기초 데이터를 관리합니다.
 */
export default function ConsolidationPage() {
  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-white mb-2">
            <Network size={20} className="text-blue-500" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Consolidation Dashboard</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            연결 회계 및 지분법 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            지배 기업 및 종속회사의 개별 재무제표를 통합하고, 내부거래 제거 및 투자자본 상계를 위한 고수준 인터페이스를 제공합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <RefreshCcw size={18} /> 실시간 데이터 취합
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Zap size={18} /> 연결 조정 분개 생성
          </button>
        </div>
      </div>

      {/* Group Entity Map View Placeholder */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Group Entity Structure</h3>
            <span className="text-[10px] text-blue-500 font-black tracking-widest bg-blue-500/10 px-3 py-1 rounded-full border border-blue-500/10">3 SUBSIDIARIES ACTIVE</span>
         </div>
         
         <div className="relative h-64 rounded-[3rem] bg-slate-950 border border-white/5 flex items-center justify-center border-dashed overflow-hidden group">
            <div className="absolute inset-0 bg-gradient-to-t from-blue-600/[0.03] to-transparent" />
            <div className="relative z-10 flex flex-col items-center">
               <div className="w-20 h-20 rounded-3xl bg-blue-600 flex items-center justify-center text-white mb-6 animate-float shadow-2xl shadow-blue-600/30 ring-4 ring-blue-600/20">
                  <Globe size={32} />
               </div>
               <span className="text-xs font-black text-slate-500 italic uppercase">Visual Entity Mapping Layer</span>
            </div>
         </div>
      </div>

      {/* Subsidiary Performance List */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Subsidiary Data Ingestion</h3>
            <div className="relative group max-w-sm w-full">
               <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700" size={18} />
               <input type="text" placeholder="Search entity by code, country..." className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3.5 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/30 transition-all font-bold" />
            </div>
         </div>

         <div className="space-y-4">
            {[
              { name: 'Antigrav Systems Japan', share: '100%', status: 'CONFIRMED', currency: 'JPY', lastIn: '2026-04-20' },
              { name: 'Agate Logistics Inc.', share: '85%', status: 'PROCESSING', currency: 'USD', lastIn: '2026-04-21' },
              { name: 'Sky Bridge Fintech', share: '30%', status: 'WAITING', currency: 'KRW', lastIn: 'Pending' },
            ].map((sub, idx) => (
              <div key={idx} className="flex items-center justify-between p-6 rounded-3xl bg-white/[0.01] border border-white/[0.03] hover:border-white/10 hover:bg-white/[0.02] transition-all cursor-pointer group/item">
                 <div className="flex items-center gap-6">
                    <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-white/10 flex items-center justify-center text-slate-500 group-hover/item:text-blue-400 transition-colors">
                       <Database size={20} />
                    </div>
                    <div className="flex flex-col">
                       <span className="text-base font-black text-white tracking-tight">{sub.name}</span>
                       <span className="text-[10px] text-slate-700 font-bold uppercase tracking-widest leading-none mt-1">Ownership: {sub.share}</span>
                    </div>
                 </div>

                 <div className="flex items-center gap-12 text-right">
                    <div className="flex flex-col">
                       <span className="text-xs font-black text-slate-500 uppercase tracking-widest">{sub.currency}</span>
                       <span className="text-[10px] text-slate-800 font-bold mt-1">Local Currency</span>
                    </div>
                    <div className="w-32 flex flex-col items-end">
                       <span className={`text-[10px] font-black px-2.5 py-1 rounded-lg border ${
                         sub.status === 'CONFIRMED' ? 'text-emerald-500 border-emerald-500/20 bg-emerald-500/5' :
                         sub.status === 'PROCESSING' ? 'text-blue-500 border-blue-500/20 bg-blue-500/5 pulse' :
                         'text-slate-600 border-white/5 bg-white/5'
                       }`}>
                          {sub.status}
                       </span>
                       <span className="text-[10px] text-slate-700 font-bold mt-2 italic">{sub.lastIn}</span>
                    </div>
                    <ArrowRight size={18} className="text-slate-800 group-hover/item:text-white transition-colors" />
                 </div>
              </div>
            ))}
         </div>
      </div>
    </div>
  );
}
