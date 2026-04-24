import React from 'react';
import { FileSearch, Calculator, Download, AlertCircle } from 'lucide-react';
import styles from './TaxVatSupport.module.css';

/**
 * [세무/부가세 신고 지원 화면]
 * 부가가치세 신고를 위해 매입/매출 증빙 데이터를 집계하고 국세청 데이터와 대조합니다.
 * 설계서 파트 6-⑮ 기반.
 */
export default function TaxVatSupportPage() {
  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">세무/부가세 신고 지원</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">분기별 매입/매출 집계 및 국세청(Hometax) 데이터 교차 검증</p>
        </div>
        <div className="flex gap-2">
           <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg">
              <Download size={18} /> 집계내역 다운로드
           </button>
        </div>
      </header>

      {/* 부가세 요약 현황 */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[32px] p-10 backdrop-blur-xl relative overflow-hidden group">
          <h4 className="text-xs font-black text-blue-500 uppercase tracking-[0.2em] mb-10">2026년 1기 확정 부가세 실시간 현황</h4>
          
          <div className="space-y-6 relative z-10">
            <div className="flex justify-between items-end pb-4 border-b border-white/5 group/row hover:border-white/20 transition-colors">
              <div className="flex flex-col">
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest leading-none mb-2">OUTPUT (A)</span>
                <span className="text-lg font-bold text-slate-300 group-hover/row:text-white">매출 부가가치세</span>
              </div>
              <div className="text-3xl font-black italic text-slate-200 group-hover/row:text-blue-400 transition-colors tracking-tighter">₩84,200,000</div>
            </div>
            
            <div className="flex justify-between items-end pb-4 border-b border-white/5 group/row hover:border-white/20 transition-colors">
              <div className="flex flex-col">
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest leading-none mb-2">INPUT (B)</span>
                <span className="text-lg font-bold text-slate-300 group-hover/row:text-white">매입 부가가치세 (공제 대상)</span>
              </div>
              <div className="text-3xl font-black italic text-slate-200 group-hover/row:text-rose-400 transition-colors tracking-tighter">- ₩52,500,000</div>
            </div>

            <div className="pt-8 flex justify-between items-baseline">
               <span className="text-xl font-black text-white italic tracking-tight">최종 납부/환급 예상 세액</span>
               <div className="flex flex-col items-end">
                  <span className="text-5xl font-black italic text-white tracking-tighter shadow-blue-500/50">₩31,700,000</span>
                  <span className="text-[10px] font-bold text-emerald-500 mt-2 flex items-center gap-1 uppercase tracking-widest leading-none">
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" /> Final Estimate
                  </span>
               </div>
            </div>
          </div>
          
          <div className="absolute -right-10 -bottom-10 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity duration-1000 pointer-events-none">
             <Calculator size={300} className="text-white" />
          </div>
        </div>

        <div className="lg:col-span-4 bg-gradient-to-br from-indigo-900/20 to-slate-900/20 border border-indigo-500/20 rounded-[32px] p-8 shadow-2xl flex flex-col justify-between group">
          <div>
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight mb-4">
               <AlertCircle size={22} className="text-indigo-400" /> 신고 안내 및 통제
            </h3>
            <p className="text-slate-400 text-sm font-medium leading-relaxed italic tracking-tighter">
              1기 확정 신고 마감일까지 <strong>D-22</strong> 남았습니다.<br/>
              모든 매입 세금계산서의 국세청 대조 작업을 완료해 주시기 바랍니다.
            </p>
            <div className="mt-8 space-y-3">
              <div className="flex justify-between text-xs font-bold px-1">
                 <span className="text-slate-500 uppercase">신고 기간</span>
                 <span className="text-slate-300">07.01 ~ 07.25</span>
              </div>
              <div className="w-full h-1 bg-slate-800 rounded-full overflow-hidden">
                 <div className="w-2/3 h-full bg-indigo-500 shadow-[0_0_10px_rgba(99,102,241,0.5)]" />
              </div>
            </div>
          </div>
          
          <button className="bg-white text-slate-950 font-black text-sm py-4 rounded-2xl mt-12 transition-all hover:bg-indigo-400 hover:text-white shadow-xl active:scale-95 uppercase tracking-widest">
            신고 기초자료 생성
          </button>
        </div>
      </section>

      {/* 불일치 대조 그리드 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl">
        <div className="p-8 border-b border-white/5 bg-white/[0.01] flex justify-between items-center">
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight leading-none uppercase">
               <FileSearch size={22} className="text-amber-400" /> Hometax vs System 교차 검증 (Mismatch)
            </h3>
            <span className="text-[10px] font-bold text-rose-500 bg-rose-500/10 px-3 py-1 rounded-full border border-rose-500/20 animate-pulse">2 INCONSISTENCIES FOUND</span>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-5">TYPE</th>
                <th className="px-6 py-5">DATE</th>
                <th className="px-6 py-5">PARTNER</th>
                <th className="px-6 py-5">SUPPLY AMT</th>
                <th className="px-6 py-5">SYSTEM TAX</th>
                <th className="px-6 py-5">HOMETAX AMT</th>
                <th className="px-8 py-5 text-right">VARIANCE</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              <tr className="bg-rose-500/5 hover:bg-rose-500/10 transition-colors group">
                <td className="px-8 py-6 text-sm font-black text-rose-400 uppercase italic">매입</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-400 font-bold">2026-04-12</td>
                <td className="px-6 py-6 text-sm font-bold text-white tracking-tight">삼선기술(주)</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-400 font-bold">₩1,000,000</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-300 font-black">₩100,000</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-500 font-bold italic">₩0</td>
                <td className="px-8 py-6 text-right">
                  <span className="text-sm font-black text-rose-500 italic shadow-rose-950 shadow-lg tracking-tighter">₩100,000</span>
                </td>
              </tr>
              <tr className="hover:bg-white/[0.03] transition-colors group">
                <td className="px-8 py-6 text-sm font-black text-blue-400 uppercase italic">매출</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-500 font-bold">2026-04-15</td>
                <td className="px-6 py-6 text-sm font-bold text-slate-300 group-hover:text-white">글로벌샵</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-500 font-bold">₩500,000</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-500 font-bold">₩50,000</td>
                <td className="px-6 py-6 text-sm font-mono text-slate-500 font-bold">₩50,000</td>
                <td className="px-8 py-6 text-right text-sm font-mono text-slate-600 font-bold">₩0</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
