import { FileBarChart, Layers, Download, ChevronRight, ChevronDown } from 'lucide-react';

/**
 * [재무제표 보고서 조회 화면]
 * 재무상태표(BS) 및 손익계산서(PL)를 계층적으로 조회하고 비교 분석합니다.
 * 설계서 파트 4-⑪ 기반.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 화면은 회사의 성적표(재무제표)를 보여주는 대시보드입니다.
 * 경영진이 가장 많이 보는 화면으로, '작년 이맘때 대비 자산이 얼마나 늘었는지(비교 재무제표)'를 한눈에 보여줍니다.
 * 헥사고날 백엔드(reporting 모듈)에서 생성된 데이터를 시계열로 뿌려주는 역할을 합니다.
 */
export default function FinancialStatementsPage() {
  return (
    <div className="flex flex-col gap-10">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">재무제표 보고서 (Financial Statements)</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">전사 재무상태표(BS) 및 손익계산서(PL) 통합 시계열 조회 및 비교 분석</p>
        </div>
        <div className="flex gap-3">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-5 py-2.5 rounded-2xl border border-white/5 transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest shadow-lg">
            <Download size={18} className="text-slate-500" /> PDF Export
          </button>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-2xl border border-blue-500/20 transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest shadow-xl shadow-blue-500/20 active:scale-95">
            <Layers size={18} /> Excel Engine
          </button>
        </div>
      </header>

      {/* 리포트 설정 바 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl group">
        <div className="flex flex-col lg:flex-row items-end gap-10">
          <div className="flex flex-col gap-3">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">보고서 종류 (Statement Type)</label>
            <div className="bg-white/5 p-1.5 rounded-2xl border border-white/5 flex gap-1">
              <button className="bg-blue-600 text-white px-6 py-2 rounded-xl text-xs font-black uppercase tracking-tight shadow-lg shadow-blue-600/20 transition-all">BS</button>
              <button className="text-slate-500 hover:text-slate-300 px-6 py-2 rounded-xl text-xs font-black uppercase tracking-tight transition-all">PL</button>
              <button className="text-slate-500 hover:text-slate-300 px-6 py-2 rounded-xl text-xs font-black uppercase tracking-tight transition-all">CF</button>
            </div>
          </div>
          
          <div className="flex flex-col gap-3">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Fiscal Period</label>
            <select className="bg-slate-950 border border-white/10 text-white text-xs rounded-2xl px-6 py-3 focus:border-blue-500 outline-none transition-all cursor-pointer font-black min-w-[240px]">
              <option>2026-04 (Current)</option>
              <option>2026-03 (Previous)</option>
              <option>2025 Annual (Audited)</option>
            </select>
          </div>

          <button className="bg-slate-100 hover:bg-white text-slate-950 px-10 py-3 rounded-2xl text-xs font-black uppercase tracking-widest transition-all shadow-xl active:scale-95 ml-auto">
            Run Analysis
          </button>
        </div>
      </section>

      {/* 리포트 본문 */}
      <section className="bg-white/5 border border-white/10 rounded-[40px] p-12 backdrop-blur-xl relative overflow-hidden">
        <div className="text-center mb-16 relative z-10">
          <h3 className="text-4xl font-black text-white italic tracking-tighter uppercase mb-4">재무상태표 (Balance Sheet)</h3>
          <p className="text-slate-500 text-sm font-bold tracking-widest uppercase flex items-center justify-center gap-3">
            <span className="w-8 h-px bg-slate-800" />
            As of April 22, 2026 (Unit: KRW)
            <span className="w-8 h-px bg-slate-800" />
          </p>
        </div>

        <div className="flex flex-col border-t-2 border-white/10 relative z-10">
          <div className="grid grid-cols-12 gap-4 px-8 py-6 border-b border-white/5 text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] bg-white/[0.01]">
            <div className="col-span-6">Account Item</div>
            <div className="col-span-2 text-right">Current Period</div>
            <div className="col-span-2 text-right">Previous Period</div>
            <div className="col-span-2 text-right text-blue-400">Variance (%)</div>
          </div>

          {/* Hierarchy Layers */}
          <div className="space-y-px">
            {/* 자산 섹션 */}
            <div className="grid grid-cols-12 gap-4 px-8 py-6 border-b border-white/5 bg-white/[0.03] group hover:bg-white/[0.05] transition-colors items-center">
              <div className="col-span-6 text-sm font-black text-white flex items-center gap-3">
                <ChevronDown size={14} className="text-blue-500" /> [ I ] 자산 (Total Assets)
              </div>
              <div className="col-span-2 text-sm font-mono font-black text-white text-right italic">1,540,200,000</div>
              <div className="col-span-2 text-sm font-mono font-bold text-slate-400 text-right">1,480,000,000</div>
              <div className="col-span-2 text-sm font-black text-emerald-400 text-right">+4.1%</div>
            </div>

            <div className="grid grid-cols-12 gap-4 px-8 py-5 border-b border-white/5 pl-14 group hover:bg-white/[0.02] transition-colors items-center">
              <div className="col-span-6 text-sm font-black text-slate-300">1. 유동자산</div>
              <div className="col-span-2 text-sm font-mono font-black text-slate-200 text-right italic">840,200,000</div>
              <div className="col-span-2 text-sm font-mono font-bold text-slate-500 text-right">780,000,000</div>
              <div className="col-span-2 text-sm font-black text-emerald-400 text-right">+7.7%</div>
            </div>

            <div className="grid grid-cols-12 gap-4 px-8 py-4 border-b border-white/5 pl-24 group hover:bg-white/[0.01] transition-colors items-center opacity-70 hover:opacity-100">
              <div className="col-span-6 text-xs font-bold text-slate-400">현금 및 현금성자산</div>
              <div className="col-span-2 text-xs font-mono font-bold text-slate-300 text-right italic">320,000,000</div>
              <div className="col-span-2 text-xs font-mono font-medium text-slate-600 text-right">210,000,000</div>
              <div className="col-span-2 text-xs font-black text-emerald-400 text-right">+52.4%</div>
            </div>

            <div className="grid grid-cols-12 gap-4 px-8 py-4 border-b border-white/5 pl-24 group hover:bg-white/[0.01] transition-colors items-center opacity-70 hover:opacity-100 font-mono font-bold">
              <div className="col-span-6 text-xs font-bold text-slate-400 font-sans">단기금융상품</div>
              <div className="col-span-2 text-xs text-slate-300 text-right italic">520,200,000</div>
              <div className="col-span-2 text-xs text-slate-600 text-right">570,000,000</div>
              <div className="col-span-2 text-xs text-rose-400 text-right">-8.7%</div>
            </div>

            {/* 부채 섹션 */}
            <div className="grid grid-cols-12 gap-4 px-8 py-8 border-b border-white/5 bg-white/[0.03] group hover:bg-white/[0.05] transition-colors items-center mt-6">
              <div className="col-span-6 text-sm font-black text-white flex items-center gap-3">
                <ChevronRight size={14} className="text-slate-500" /> [ II ] 부채 (Total Liabilities)
              </div>
              <div className="col-span-2 text-sm font-mono font-black text-white text-right italic">820,000,000</div>
              <div className="col-span-2 text-sm font-mono font-bold text-slate-400 text-right">800,000,000</div>
              <div className="col-span-2 text-sm font-black text-rose-400 text-right">+2.5%</div>
            </div>
          </div>
        </div>

        <div className="absolute top-0 right-0 p-12 opacity-[0.02] pointer-events-none">
           <FileBarChart size={300} className="text-white" />
        </div>
      </section>
    </div>
  );
}
