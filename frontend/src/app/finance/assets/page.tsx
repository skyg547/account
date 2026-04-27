"use client";

import React from 'react';
import { 
  Package, 
  Plus, 
  Search, 
  BarChart3, 
  Calculator, 
  ArrowDownCircle, 
  History,
  Building,
  Computer,
  Car,
  Table,
  FileText,
  RotateCw
} from 'lucide-react';

/**
 * [고정자산 관리 화면]
 * 회사의 유무형 자산을 등록하고 감가상각비를 자동 계산 및 전표화합니다.
 */
export default function AssetsPage() {
  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <Package size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Fixed Asset Management</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            고정자산 통합 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            유무형 자산의 취득, 감가상각, 이동 및 처분 프로세스를 디지털화하고 재무제표와 실시간 연동합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <RotateCw size={18} /> 감가상각 재계산
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Plus size={18} /> 자산 신규 취득
          </button>
        </div>
      </div>

      {/* Asset Category Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
         {[
           { icon: Building, label: '건물/구축물', count: '14', value: '₩12.4B' },
           { icon: Computer, label: '비품/IT기기', count: '450', value: '₩2.1B' },
           { icon: Car, label: '차량운반구', count: '8', value: '₩0.4B' },
           { icon: FileText, label: '무형자산', count: '25', value: '₩3.5B' },
         ].map((cat, i) => (
           <div key={i} className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] hover:bg-white/[0.03] transition-all group cursor-pointer">
              <div className="w-12 h-12 rounded-2xl bg-blue-600/10 flex items-center justify-center text-blue-500 mb-6 group-hover:scale-110 transition-transform">
                 <cat.icon size={24} />
              </div>
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{cat.label}</span>
              <div className="text-2xl font-black text-white mt-1 italic tracking-tighter">{cat.value}</div>
              <div className="text-xs font-bold text-slate-700 mt-2">{cat.count} Items Listed</div>
           </div>
         ))}
      </div>

      {/* Asset Inventory Table Placeholder */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <div className="flex flex-col gap-1">
               <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Asset Inventory Registry</h3>
               <span className="text-[10px] text-slate-700 font-bold uppercase tracking-widest leading-none">Last depreciation calculated: 2026-03-31</span>
            </div>
            <div className="relative group max-w-sm w-full">
               <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700" size={18} />
               <input type="text" placeholder="Asset code, name, serial..." className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3.5 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/30 transition-all font-bold" />
            </div>
         </div>

         <div className="overflow-x-auto">
            <table className="w-full text-left">
               <thead>
                  <tr className="border-b border-white/5 text-[10px] font-black text-slate-600 uppercase tracking-[0.2em]">
                     <th className="pb-6 px-4">Code / Asset Name</th>
                     <th className="pb-6 px-4">Acquisition Date</th>
                     <th className="pb-6 px-4">Cost (Book Value)</th>
                     <th className="pb-6 px-4">Depreciation</th>
                     <th className="pb-6 px-4 text-right">Action</th>
                  </tr>
               </thead>
               <tbody className="divide-y divide-white/[0.02]">
                  {[
                    { id: 'AST-26012', name: 'MacBook Pro 16" (M3 Max)', date: '2026-01-15', cost: '₩4,500,000', book: '₩3,800,000', method: '정액법 (5년)' },
                    { id: 'AST-25881', name: 'HQ Server Rack Alpha', date: '2025-11-20', cost: '₩45,000,000', book: '₩32,500,000', method: '정액법 (5년)' },
                    { id: 'AST-25102', name: 'Electric Truck Genesis', date: '2025-08-05', cost: '₩68,000,000', book: '₩55,000,000', method: '정액법 (5년)' },
                  ].map((asset, idx) => (
                    <tr key={idx} className="group/row hover:bg-white/[0.02] transition-colors">
                       <td className="py-6 px-4">
                          <div className="flex flex-col">
                             <span className="text-sm font-black text-white">{asset.name}</span>
                             <span className="text-[10px] text-slate-700 font-mono tracking-tighter uppercase">{asset.id}</span>
                          </div>
                       </td>
                       <td className="py-6 px-4">
                          <span className="text-xs font-bold text-slate-500">{asset.date}</span>
                       </td>
                       <td className="py-6 px-4">
                          <div className="flex flex-col">
                             <span className="text-sm font-black text-white">{asset.book}</span>
                             <span className="text-[10px] text-slate-700 italic">Cost: {asset.cost}</span>
                          </div>
                       </td>
                       <td className="py-6 px-4">
                          <div className="flex items-center gap-3">
                             <div className="w-1.5 h-1.5 rounded-full bg-blue-500" />
                             <span className="text-xs font-bold text-slate-400 capitalize">{asset.method}</span>
                          </div>
                       </td>
                       <td className="py-6 px-4 text-right">
                          <button className="p-3 bg-white/5 hover:bg-white/10 rounded-xl text-slate-500 hover:text-white transition-all"><History size={16} /></button>
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
