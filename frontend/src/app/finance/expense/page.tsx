"use client";

import React from 'react';
import { 
  Receipt, 
  Plus, 
  Search, 
  Clock, 
  CheckCircle2, 
  XCircle, 
  Image as ImageIcon,
  CreditCard,
  Building2,
  ChevronRight,
  MoreVertical,
  History,
  FileSearch
} from 'lucide-react';

/**
 * [지출결의 및 경비 신청 포털 화면]
 * 임직원이 법인카드/개인경비를 신청하고 재무팀이 이를 승인/전표화하는 통합 접점입니다.
 */
export default function ExpensePortalPage() {
  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-amber-500 mb-2">
            <Receipt size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Expense Management Portal</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            지출결의 및 경비 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            법인카드 사용 내역 및 개인 지출에 대한 정산/결의를 수행합니다. 승인 완료 시 회계 전표로 자동 전환됩니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <History size={18} /> 과거 상세 이력
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Plus size={18} /> 신규 지출결의
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
         {[
           { label: 'Unsettled Card Usage', value: '12', sub: '₩2,450,000', color: 'blue' },
           { label: 'Pending Approval', value: '5', sub: '₩1,800,000', color: 'amber' },
           { label: 'Settled (This Month)', value: '48', sub: '₩15,500,000', color: 'emerald' },
         ].map((stat, i) => (
           <div key={i} className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] flex flex-col items-center text-center">
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{stat.label}</span>
              <div className="text-4xl font-black text-white mt-3 italic tracking-tighter">{stat.value}</div>
              <div className="text-sm font-bold text-slate-400 mt-2">{stat.sub}</div>
              <div className={`mt-6 w-full h-[2px] bg-${stat.color}-600/20 rounded-full overflow-hidden`}>
                 <div className={`h-full bg-${stat.color}-500 w-1/2`} />
              </div>
           </div>
         ))}
      </div>

      {/* Recent Expense Requests List */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Recent Expense Requests</h3>
            <div className="relative group max-w-sm w-full">
               <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700 font-bold" size={18} />
               <input type="text" placeholder="Search by description, date..." className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3.5 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/30 transition-all font-bold" />
            </div>
         </div>

         <div className="space-y-4">
            {[
              { type: 'Corporate Card', desc: 'AWS Cloud Services - Monthly', amount: '₩1,240,000', date: '2026-04-24', status: 'PENDING', user: 'Kim J.M.' },
              { type: 'Personal Cash', desc: 'Taxi for Client Meeting', amount: '₩18,500', date: '2026-04-23', status: 'APPROVED', user: 'Lee S.Y.' },
              { type: 'Corporate Card', desc: 'Strategic Partner Lunch', amount: '₩85,000', date: '2026-04-23', status: 'APPROVED', user: 'Park D.W.' },
              { type: 'Expense Request', desc: 'Hardware Upgrade (Server)', amount: '₩5,600,000', date: '2026-04-22', status: 'REJECTED', user: 'Choi A.R.' },
            ].map((item, idx) => (
              <div key={idx} className="group/row flex items-center justify-between p-6 rounded-3xl bg-white/[0.01] border border-white/[0.03] hover:border-white/10 hover:bg-white/[0.02] transition-all cursor-pointer">
                 <div className="flex items-center gap-6">
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border ${
                      item.status === 'PENDING' ? 'bg-amber-500/10 border-amber-500/20 text-amber-500' :
                      item.status === 'APPROVED' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-500' :
                      'bg-rose-500/10 border-rose-500/20 text-rose-500'
                    }`}>
                       {item.type === 'Corporate Card' ? <CreditCard size={20} /> : <Receipt size={20} />}
                    </div>
                    <div className="flex flex-col">
                       <span className="text-base font-black text-white tracking-tight">{item.desc}</span>
                       <div className="flex items-center gap-3 mt-1">
                          <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">{item.user}</span>
                          <div className="w-1 h-1 bg-slate-800 rounded-full" />
                          <span className="text-[10px] font-bold text-slate-700 italic tracking-tighter">{item.date}</span>
                       </div>
                    </div>
                 </div>
                 
                 <div className="flex items-center gap-10">
                    <span className="text-sm font-mono font-black text-white w-24 text-right">{item.amount}</span>
                    <div className="flex items-center gap-3 w-32 justify-end">
                       {item.status === 'PENDING' ? (
                         <div className="flex items-center gap-1.5 text-amber-500 font-black text-[10px] tracking-widest uppercase"><Clock size={14} /> Pending</div>
                       ) : item.status === 'APPROVED' ? (
                         <div className="flex items-center gap-1.5 text-emerald-500 font-black text-[10px] tracking-widest uppercase"><CheckCircle2 size={14} /> Approved</div>
                       ) : (
                         <div className="flex items-center gap-1.5 text-rose-500 font-black text-[10px] tracking-widest uppercase"><XCircle size={14} /> Rejected</div>
                       )}
                       <button className="p-2 text-white/5 group-hover/row:text-slate-500 hover:text-white transition-colors"><ChevronRight size={18} /></button>
                    </div>
                 </div>
              </div>
            ))}
         </div>

         <button className="w-full mt-10 py-4 bg-white/5 hover:bg-white/10 rounded-2xl text-[10px] font-black text-slate-600 uppercase tracking-[0.3em] transition-all flex items-center justify-center gap-3">
            Load More Requests <FileSearch size={16} />
         </button>
      </div>
    </div>
  );
}
