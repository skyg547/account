"use client";

import React, { useState } from 'react';
import { 
  Plus, 
  Search, 
  FileText, 
  Calculator, 
  History, 
  ArrowUpRight,
  ShieldCheck,
  Landmark,
  TrendingUp,
  X
} from 'lucide-react';

// [Mock Data] 리스 계약 목록
const mockLeases = [
  { id: 1, no: 'L-2026-001', name: '영등포 지점 본동 임대차', lessor: '서울빌딩매니지먼트', startDate: '2026-01-01', endDate: '2028-12-31', payment: 4500000, ifrs16: true },
  { id: 2, no: 'L-2026-002', name: '업무용 복합기 리스 (HP)', lessor: '한국리스금융', startDate: '2026-03-01', endDate: '2029-02-28', payment: 1200000, ifrs16: false },
  { id: 3, no: 'L-2025-084', name: '판교 R&D 센터 데이터룸', lessor: '판교테크노홀딩스', startDate: '2025-06-01', endDate: '2030-05-31', payment: 8500000, ifrs16: true },
];

export default function LeaseManagementPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">리스 계약 관리 (IFRS 16)</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">금융리스 및 운용리스 계약 정보를 기반으로 사용권자산 상각 및 이자비용 자동 산출</p>
        </div>
        <div className="flex gap-3">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-6 py-2.5 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest">
            <History size={16} /> 재측정 이력
          </button>
          <button 
            className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-2.5 rounded-xl border border-blue-500/20 transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest shadow-lg shadow-blue-500/20 active:scale-95"
            onClick={() => setIsModalOpen(true)}
          >
            <Plus size={18} /> 신규 리스 등록
          </button>
        </div>
      </header>

      {/* 요약 지표 */}
      <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { label: '총 사용권자산 (ROU)', value: '4,520,000,000', color: 'text-blue-400', icon: <Landmark size={20} /> },
          { label: '리스부채 총액', value: '3,842,000,000', color: 'text-rose-400', icon: <FileText size={20} /> },
          { label: '평균 유효이자율', value: '4.20 %', color: 'text-emerald-400', icon: <Calculator size={20} /> },
          { label: '당월 리스 비용', value: '125,400,000', color: 'text-amber-400', icon: <TrendingUp size={20} /> },
        ].map((item, i) => (
          <div key={i} className="bg-white/5 border border-white/10 rounded-[28px] p-8 hover:bg-white/[0.07] transition-all group overflow-hidden relative">
            <div className="flex justify-between items-start mb-4">
               <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest leading-none">{item.label}</span>
               <div className={`${item.color} opacity-30 group-hover:opacity-100 transition-opacity`}>{item.icon}</div>
            </div>
            <div className={`text-2xl font-black italic tracking-tighter ${item.color}`}>
              {item.value.includes('%') ? item.value : `₩${item.value}`}
            </div>
            <div className="absolute -right-2 -bottom-2 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity">
               {React.cloneElement(item.icon, { size: 100 })}
            </div>
          </div>
        ))}
      </section>

      {/* 3. 계약 목록 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl shrink-0">
        <div className="p-8 border-b border-white/5 bg-white/[0.01] flex justify-between items-center">
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight leading-none uppercase">
               <Landmark size={22} className="text-blue-500" /> 리스 계약 원장 (Contract Ledger)
            </h3>
            <div className="relative">
              <Search className="absolute left-3 top-2.5 text-slate-500" size={16} />
              <input 
                type="text" 
                placeholder="Search contract..." 
                className="bg-slate-950 border border-white/5 rounded-xl py-2 pl-10 pr-4 text-xs text-slate-400 outline-none w-64 focus:border-blue-500/50 transition-all" 
              />
            </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-5">No</th>
                <th className="px-6 py-5">Contract Name</th>
                <th className="px-6 py-5">Lessor</th>
                <th className="px-6 py-5">Period</th>
                <th className="px-6 py-5">Monthly Paymnt</th>
                <th className="px-6 py-5">IFRS 16</th>
                <th className="px-8 py-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {mockLeases.map(lease => (
                <tr key={lease.id} className="hover:bg-white/[0.03] transition-colors group">
                  <td className="px-8 py-6 text-sm font-mono font-black text-blue-400 italic">{lease.no}</td>
                  <td className="px-6 py-6 text-sm font-bold text-white tracking-tight leading-tight">{lease.name}</td>
                  <td className="px-6 py-6 text-sm font-medium text-slate-400">{lease.lessor}</td>
                  <td className="px-6 py-6 text-[11px] font-mono font-bold text-slate-500">{lease.startDate} ~ {lease.endDate}</td>
                  <td className="px-6 py-6 text-sm font-mono font-black text-slate-200">₩{lease.payment.toLocaleString()}</td>
                  <td className="px-6 py-6">
                    {lease.ifrs16 ? (
                      <span className="text-[10px] font-black bg-blue-500/10 text-blue-400 border border-blue-500/20 px-2 py-1 rounded uppercase tracking-widest">Active</span>
                    ) : (
                      <span className="text-[10px] text-slate-600 font-bold uppercase tracking-widest">-</span>
                    )}
                  </td>
                  <td className="px-8 py-6 text-right">
                    <div className="flex justify-end gap-1 opacity-20 group-hover:opacity-100 transition-opacity">
                      <button className="p-2 hover:bg-white/5 rounded-lg text-slate-400 hover:text-blue-400 transition-all"><FileText size={16} /></button>
                      <button className="p-2 hover:bg-white/5 rounded-lg text-slate-400 hover:text-amber-400 transition-all"><Calculator size={16} /></button>
                      <button className="p-2 hover:bg-white/5 rounded-lg text-slate-400 hover:text-emerald-400 transition-all"><ArrowUpRight size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* 4. 신규 등록 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xl z-[100] flex items-center justify-center p-8 animate-in fade-in duration-300">
          <div className="bg-[#0f172a] border border-white/10 rounded-[40px] w-full max-w-4xl max-h-[90vh] overflow-hidden shadow-2xl flex flex-col animate-in zoom-in-95 duration-500">
            <div className="p-8 border-b border-white/5 flex justify-between items-center bg-white/[0.01]">
               <div>
                  <h3 className="text-2xl font-black text-white italic tracking-tighter uppercase">리스 계약 신규 등록</h3>
                  <p className="text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] mt-1 italic">New Lease Contract Intake Form</p>
               </div>
               <button 
                  onClick={() => setIsModalOpen(false)}
                  className="w-12 h-12 rounded-2xl bg-white/5 hover:bg-rose-500/20 text-slate-400 hover:text-rose-500 transition-all flex items-center justify-center"
               >
                 <X size={24} />
               </button>
            </div>

            <div className="flex-1 overflow-y-auto p-10">
              <form className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
                <div className="lg:col-span-3">
                   <h5 className="text-[10px] font-black text-blue-500 uppercase tracking-widest border-b border-blue-500/20 pb-2 mb-6 italic">Basic Information</h5>
                </div>
                
                <div className="flex flex-col gap-2 group">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">계약번호 (Ref No)</label>
                  <input type="text" placeholder="L-2026-XXX" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono font-bold focus:border-blue-500 outline-none transition-all shadow-inner" />
                </div>
                
                <div className="md:col-span-2 flex flex-col gap-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">계약명 (Description)</label>
                  <input type="text" placeholder="리스 계약 본문 명칭 입력" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-bold focus:border-blue-500 outline-none transition-all shadow-inner" />
                </div>
                
                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">리스제공자 (Lessor)</label>
                  <select className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-bold focus:border-blue-500 outline-none transition-all cursor-pointer">
                    <option>선택하세요...</option>
                    <option>한국리스금융</option>
                    <option>판교테크노홀딩스</option>
                  </select>
                </div>
                
                <div className="flex flex-col gap-2 text-slate-400 group">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">계약시작일 (Start)</label>
                  <input type="date" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono focus:border-blue-500 outline-none" />
                </div>
                
                <div className="flex flex-col gap-2 text-slate-400">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">계약종료일 (End)</label>
                  <input type="date" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono focus:border-blue-500 outline-none" />
                </div>

                <div className="lg:col-span-3 mt-4">
                   <h5 className="text-[10px] font-black text-emerald-500 uppercase tracking-widest border-b border-emerald-500/20 pb-2 mb-6 italic">Financial Conditions (IFRS 16)</h5>
                </div>
                
                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">월 리스료 (VAT 제외)</label>
                  <input type="number" placeholder="0" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono font-black italic text-lg focus:border-emerald-500 outline-none transition-all" />
                </div>
                
                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">할인율 (Discount %)</label>
                  <input type="number" placeholder="4.5" step="0.1" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono font-black italic text-lg focus:border-emerald-500 outline-none transition-all" />
                </div>
                
                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">지급일 (Payment Day)</label>
                  <input type="number" placeholder="25" min="1" max="31" className="bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono font-black italic text-lg focus:border-emerald-500 outline-none transition-all" />
                </div>

                <div className="lg:col-span-3 bg-emerald-500/5 border border-emerald-500/10 rounded-2xl p-6 flex flex-col gap-4">
                   <div className="flex items-center gap-3 text-emerald-500 font-black text-[10px] uppercase tracking-widest">
                      <Calculator size={16} /> Valuation Results
                   </div>
                   <div className="grid grid-cols-2 gap-10">
                      <div className="flex flex-col">
                         <span className="text-[10px] font-black text-slate-600 uppercase mb-1">Estimated ROU Asset</span>
                         <span className="text-2xl font-black italic text-slate-400 tracking-tighter">Calculating...</span>
                      </div>
                      <div className="flex flex-col">
                         <span className="text-[10px] font-black text-slate-600 uppercase mb-1">Initial Lease Liability</span>
                         <span className="text-2xl font-black italic text-slate-400 tracking-tighter">Calculating...</span>
                      </div>
                   </div>
                </div>
              </form>
            </div>

            <div className="p-10 border-t border-white/5 flex justify-end gap-4 bg-white/[0.01]">
               <button 
                  className="px-8 py-4 rounded-2xl text-slate-400 font-black text-sm uppercase tracking-widest hover:text-white transition-all"
                  onClick={() => setIsModalOpen(false)}
               >
                 Cancel
               </button>
               <button className="bg-emerald-600 hover:bg-emerald-500 text-white px-10 py-4 rounded-2xl transition-all flex items-center gap-3 text-sm font-black uppercase tracking-widest shadow-xl shadow-emerald-500/20 active:scale-95">
                 <ShieldCheck size={20} /> Register & Evaluate
               </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
