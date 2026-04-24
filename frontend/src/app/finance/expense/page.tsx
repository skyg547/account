import React from 'react';
import { Camera, ClipboardCheck, Wallet, History, FileText } from 'lucide-react';
import styles from './ExpenseResolution.module.css';

/**
 * [지출결의 및 경비 포털 화면]
 * 임직원이 사용한 경비를 청구하고 승인 워크플로우를 관리합니다.
 * 설계서 파트 6-⑰ 기반.
 */
export default function ExpenseResolutionPage() {
  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">지출결의 및 경비 포털</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">법인카드 및 개인 경비 청구 통합 관리 및 지능형 승인 워크플로우 현황</p>
        </div>
        <button className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-3 rounded-2xl border border-blue-500/20 transition-all flex items-center gap-3 text-sm font-black uppercase tracking-widest shadow-xl shadow-blue-500/20 active:scale-95">
          <FileText size={18} /> 신규 경비 청구
        </button>
      </header>

      {/* 개인 경비 요약 위젯 */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          { label: '이번 달 사용 금액', value: '1,240,500', color: 'text-white', icon: <Wallet size={20} className="text-blue-400" /> },
          { label: '부서 예산 잔액', value: '5,800,000', color: 'text-slate-300', icon: <ClipboardCheck size={20} className="text-emerald-400" /> },
          { label: '진행 중인 청구', value: '2건', color: 'text-blue-400', icon: <Camera size={20} className="text-amber-400" /> },
        ].map((item, i) => (
          <div key={i} className="bg-white/5 border border-white/10 rounded-[28px] p-8 hover:bg-white/[0.08] transition-all group relative overflow-hidden">
            <div className="flex justify-between items-center mb-4">
              <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">{item.label}</span>
              <div className="opacity-40 group-hover:opacity-100 transition-opacity">{item.icon}</div>
            </div>
            <h3 className={`text-3xl font-black italic tracking-tighter ${item.color}`}>
              {item.value.includes('건') ? item.value : `₩${item.value}`}
            </h3>
            <div className="absolute -right-2 -bottom-2 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity">
              {React.cloneElement(item.icon, { size: 100 })}
            </div>
          </div>
        ))}
      </section>

      {/* 최근 청구 내역 및 카드 내역 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <main className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl shrink-0">
          <div className="p-8 border-b border-white/5 bg-white/[0.01] flex justify-between items-center">
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight leading-none uppercase italic">
              최근 지출결의 내역 (Recent Claims)
            </h3>
            <button className="text-[10px] font-black text-slate-500 hover:text-white uppercase tracking-widest flex items-center gap-2 transition-colors group">
              <History size={14} className="group-hover:rotate-[-45deg] transition-transform" /> VIEW FULL HISTORY
            </button>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                  <th className="px-8 py-5">DATE</th>
                  <th className="px-6 py-5">DESCRIPTION</th>
                  <th className="px-6 py-5 text-right">AMOUNT</th>
                  <th className="px-6 py-5 text-center">STATUS</th>
                  <th className="px-8 py-5 text-right">DETAIL</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {[
                  { date: '2026-04-20', desc: '점심 식대 (A식당)', amount: '₩12,000', status: '승인완료', type: 'success' },
                  { date: '2026-04-18', desc: '영업용 택시비', amount: '₩24,500', status: '검토중', type: 'warning' },
                  { date: '2026-04-15', desc: '도서 구입 (클린 코드)', amount: '₩35,000', status: '반려', type: 'danger' },
                ].map((row, idx) => (
                  <tr key={idx} className="hover:bg-white/[0.03] transition-colors group">
                    <td className="px-8 py-6 text-sm font-mono font-bold text-slate-500">{row.date}</td>
                    <td className="px-6 py-6 text-sm font-black text-white tracking-tight">{row.desc}</td>
                    <td className="px-6 py-6 text-sm font-mono font-black italic text-slate-300 text-right">{row.amount}</td>
                    <td className="px-6 py-6 text-center">
                      <span className={`text-[10px] font-black px-3 py-1 rounded-full border ${
                        row.type === 'success' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                        row.type === 'warning' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20 shadow-[0_0_15px_rgba(245,158,11,0.1)]' :
                        'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      }`}>
                        {row.status}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-right">
                      <button className="text-blue-500 hover:text-blue-400 text-xs font-black uppercase tracking-widest underline decoration-blue-500/30 underline-offset-4">Open</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </main>

        <aside className="lg:col-span-4 space-y-6">
          <section className="bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl relative overflow-hidden group">
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight mb-8 uppercase italic">
              <Wallet size={20} className="text-blue-400" /> 미청구 법인카드 (Feed)
            </h3>
            
            <div className="space-y-4 relative z-10">
              {[
                { date: '04.22 12:30', vendor: '무한갈비 정식', price: '₩45,000' },
                { date: '04.21 08:45', vendor: '스타벅스 강남역', price: '₩5,600' },
              ].map((item, i) => (
                <div key={i} className="flex flex-col gap-4 p-5 bg-slate-950/40 border border-white/5 rounded-2xl hover:border-blue-500/30 transition-all group/item">
                  <div className="flex justify-between items-center">
                    <span className="text-[10px] font-mono font-bold text-slate-500">{item.date}</span>
                    <button className="bg-blue-600/10 hover:bg-blue-600 text-blue-400 hover:text-white px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest border border-blue-500/20 transition-all">Quick Claim</button>
                  </div>
                  <div className="flex justify-between items-end">
                    <div className="text-sm font-black text-white tracking-tight">{item.vendor}</div>
                    <div className="text-lg font-black italic text-slate-200 group-hover/item:text-blue-400 transition-colors">{item.price}</div>
                  </div>
                </div>
              ))}
            </div>

            <div className="absolute -right-10 -bottom-10 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity duration-1000 pointer-events-none">
              <Wallet size={200} className="text-white" />
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
