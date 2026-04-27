"use client";

import React from 'react';
import { 
  FileText, 
  Plus, 
  Search, 
  Landmark, 
  Calendar, 
  Calculator, 
  ArrowUpRight,
  ShieldCheck,
  Zap,
  ChevronRight
} from 'lucide-react';

/**
 * [리스 회계 관리 화면 (IFRS 16)]
 * 리스 계약을 등록하고 사용권자산 및 리스부채의 상각/이자비용을 자동 산출합니다.
 */
export default function LeaseAccountingPage() {
  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-slate-500 mb-2">
            <Landmark size={20} className="text-blue-500" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">IFRS 16 Lease Management</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            리스 회계 통합 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            IFRS 16 국제 표준에 따른 사용권자산(ROU) 및 리스부채를 산출하고 월별 상각/이자 비용을 자동 관리합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <Calculator size={18} /> 현재가치 산출기
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Plus size={18} /> 신규 리스 계약
          </button>
        </div>
      </div>

      {/* Lease Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
         {[
           { label: 'Right-of-Use Assets', value: '₩1,840M', color: 'blue', desc: 'Net Book Value' },
           { label: 'Lease Liabilities', value: '₩1,920M', color: 'emerald', desc: 'Current + Non-current' },
           { label: 'Next Payment Due', value: 'Apr 30', color: 'amber', desc: 'Settlement: ₩45M' },
         ].map((stat, i) => (
           <div key={i} className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{stat.label}</span>
              <div className="text-3xl font-black text-white mt-1 italic tracking-tighter">{stat.value}</div>
              <p className="text-[10px] text-slate-700 font-bold mt-2 uppercase tracking-tight">{stat.desc}</p>
           </div>
         ))}
      </div>

      {/* Lease Registry List */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Lease Contract Registry</h3>
            <div className="relative group max-w-sm w-full">
               <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700" size={18} />
               <input type="text" placeholder="Search by contract name, lessor..." className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3.5 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/30 transition-all font-bold" />
            </div>
         </div>

         <div className="space-y-4">
            {[
              { id: 'LSE-001', name: 'HQ Office (15F)', lessor: '(주)강남부동산', rent: '45,000,000', end: '2028-12-31' },
              { id: 'LSE-002', name: 'Data Center Alpha', lessor: 'Global Cloud Inc.', rent: '12,500,000', end: '2027-06-30' },
              { id: 'LSE-003', name: 'Fleet Vehicles (10 Units)', lessor: 'Star Lease Corp.', rent: '18,200,000', end: '2026-09-15' },
            ].map((lease, idx) => (
              <div key={idx} className="flex items-center justify-between p-6 rounded-3xl bg-white/[0.01] border border-white/[0.03] hover:border-white/10 hover:bg-white/[0.02] transition-all group/item">
                 <div className="flex items-center gap-6">
                    <div className="w-12 h-12 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-500">
                       <FileText size={20} />
                    </div>
                    <div className="flex flex-col">
                       <span className="text-base font-black text-white tracking-tight">{lease.name}</span>
                       <span className="text-[10px] text-slate-700 font-bold uppercase tracking-widest leading-none mt-1">{lease.lessor}</span>
                    </div>
                 </div>

                 <div className="flex items-center gap-12">
                    <div className="flex flex-col items-end">
                       <span className="text-xs font-black text-slate-500 uppercase tracking-widest">{lease.end}</span>
                       <span className="text-[10px] text-slate-700 font-bold mt-1">Contract End Date</span>
                    </div>
                    <div className="flex flex-col items-end w-32">
                       <span className="text-sm font-mono font-black text-white tracking-tighter">₩{lease.rent}</span>
                       <span className="text-[10px] text-slate-700 font-bold mt-1 uppercase tracking-widest">Monthly Rent</span>
                    </div>
                    <button className="p-3 text-slate-700 group-hover/item:text-white transition-colors hover:bg-white/5 rounded-xl"><ChevronRight size={18} /></button>
                 </div>
              </div>
            ))}
         </div>
      </div>
    </div>
  );
}
