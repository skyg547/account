import React from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  CreditCard, 
  Clock, 
  ArrowUpRight 
} from 'lucide-react';
export default function Dashboard() {
  return (
    <div className="flex flex-col gap-10">
      
      {/* 1. 상단 타이틀 영역 */}
      <header>
        <h1 className="text-4xl font-black tracking-tight text-white">재무 현황 대시보드</h1>
        <p className="text-slate-400 mt-2 font-medium">AI 기반 실시간 데이터 분석 및 전표 현황 파이프라인</p>
      </header>

      {/* 2. 요약 카드 섹션 */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* 2-1. 총 자산 카드 */}
        <div className="bg-white/5 backdrop-blur-md border border-white/10 p-8 rounded-[32px] hover:scale-[1.02] transition-all duration-300 group cursor-pointer shadow-xl shadow-blue-900/10">
          <div className="flex items-center justify-between mb-6">
            <div className="w-14 h-14 bg-blue-500/10 rounded-2xl flex items-center justify-center text-blue-400 group-hover:bg-blue-500 group-hover:text-white transition-colors duration-300">
              <DollarSign size={28} />
            </div>
            <div className="flex items-center gap-1.5 text-emerald-400 bg-emerald-400/10 px-3 py-1 rounded-full text-xs font-bold">
               <TrendingUp size={14} /> 12.5%
            </div>
          </div>
          <span className="text-slate-500 text-sm font-bold uppercase tracking-wider">Total Assets</span>
          <div className="text-3xl font-black italic mt-1 text-white">₩1,240,500,000</div>
          <div className="mt-4 h-1 w-full bg-white/5 rounded-full overflow-hidden">
             <div className="h-full bg-blue-500 w-[75%]" />
          </div>
        </div>

        {/* 2-2. 총 부채 카드 */}
        <div className="bg-white/5 backdrop-blur-md border border-white/10 p-8 rounded-[32px] hover:scale-[1.02] transition-all duration-300 group cursor-pointer shadow-xl shadow-red-900/10">
          <div className="flex items-center justify-between mb-6">
            <div className="w-14 h-14 bg-red-500/10 rounded-2xl flex items-center justify-center text-red-400 group-hover:bg-red-500 group-hover:text-white transition-colors duration-300">
              <CreditCard size={28} />
            </div>
            <div className="flex items-center gap-1.5 text-orange-400 bg-orange-400/10 px-3 py-1 rounded-full text-xs font-bold">
               <TrendingDown size={14} /> 3.2%
            </div>
          </div>
          <span className="text-slate-500 text-sm font-bold uppercase tracking-wider">Total Liabilities</span>
          <div className="text-3xl font-black italic mt-1 text-white">₩450,200,000</div>
           <div className="mt-4 h-1 w-full bg-white/5 rounded-full overflow-hidden">
             <div className="h-full bg-red-500 w-[40%]" />
          </div>
        </div>

        {/* 2-3. 당기순이익 카드 */}
        <div className="bg-white/5 backdrop-blur-md border border-white/10 p-8 rounded-[32px] hover:scale-[1.02] transition-all duration-300 group cursor-pointer shadow-xl shadow-emerald-900/10">
          <div className="flex items-center justify-between mb-6">
            <div className="w-14 h-14 bg-emerald-500/10 rounded-2xl flex items-center justify-center text-emerald-400 group-hover:bg-emerald-500 group-hover:text-white transition-colors duration-300">
              <ArrowUpRight size={28} />
            </div>
            <div className="flex items-center gap-1.5 text-blue-400 bg-blue-400/10 px-3 py-1 rounded-full text-xs font-bold">
               <TrendingUp size={14} /> 8.1%
            </div>
          </div>
          <span className="text-slate-500 text-sm font-bold uppercase tracking-wider">Net Income</span>
          <div className="text-3xl font-black italic mt-1 text-white">₩790,300,000</div>
           <div className="mt-4 h-1 w-full bg-white/5 rounded-full overflow-hidden">
             <div className="h-full bg-emerald-500 w-[65%]" />
          </div>
        </div>
      </section>

      {/* 3. 최근 활동 내역 섹션 */}
      <section>
        <div className="bg-white/5 border border-white/10 rounded-[32px] p-10 overflow-hidden relative">
          <div className="flex justify-between items-center mb-8">
            <h3 className="text-xl font-bold text-white flex items-center gap-3">
               <div className="w-2 h-8 bg-blue-600 rounded-full" /> 최근 전표 유입 현황
            </h3>
            <button className="text-blue-500 hover:text-blue-400 text-sm font-black uppercase tracking-tighter transition-colors">View All Feed</button>
          </div>
          
          <div className="space-y-4">
            {[
              { id: 'J-20240422001', desc: '삼성전자 비품 매입', amount: '₩12,500,000', status: '자동분개완료', time: '5분 전' },
              { id: 'J-20240422002', desc: '커피빈 운영비 지출', amount: '₩8,500', status: '검토대기', time: '12분 전' },
              { id: 'J-20240422003', desc: '스타트업 클라우드 결제', amount: '₩1,200,000', status: '자동분개완료', time: '1시간 전' },
            ].map((item, idx) => (
              <div 
                key={item.id} 
                className="flex items-center justify-between p-6 rounded-2xl bg-white/5 hover:bg-white/[0.08] transition-all duration-300 border border-transparent hover:border-white/10 group animate-in slide-in-from-right duration-500"
                style={{ animationDelay: `${idx * 100}ms` }}
              >
                <div className="flex items-center gap-6">
                   <div className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center font-mono text-[10px] text-slate-500 border border-white/5">
                      {idx + 1}
                   </div>
                   <div>
                      <div className="text-xs text-blue-500 font-black mb-1 font-mono tracking-tighter">{item.id}</div>
                      <div className="text-slate-200 font-bold">{item.desc}</div>
                   </div>
                </div>
                <div className="text-right flex flex-col items-end gap-1.5">
                  <div className="text-lg font-black text-white italic tracking-tight">{item.amount}</div>
                  <div className="flex items-center gap-2 text-[10px] font-bold">
                    <span className="flex items-center gap-1 text-slate-500"><Clock size={12} /> {item.time}</span>
                    <span className="text-slate-700">·</span>
                    <span className={item.status === '검토대기' ? 'text-amber-500' : 'text-emerald-500'}>
                       {item.status}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="absolute top-0 right-0 p-10 opacity-10 pointer-events-none">
             <DollarSign size={200} className="text-blue-500" />
          </div>
        </div>
      </section>
    </div>
  );
}
