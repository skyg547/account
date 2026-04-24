import { Search, Plus, Filter, Download } from 'lucide-react';

/**
 * [거래처 관리 화면]
 * 외부 비즈니스 파트너(매입처, 매출처) 정보를 관리하는 화면입니다.
 * 설계서 파트 1-② 기반.
 */
export default function PartnerPage() {
  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">거래처 관리 (Registry)</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">회사의 모든 외부 비즈니스 파트너 및 매입/매출처 통합 데이터베이스 관리</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-3 rounded-2xl border border-blue-500/20 transition-all flex items-center gap-3 text-sm font-black uppercase tracking-widest shadow-xl shadow-blue-500/20 active:scale-95">
            <Plus size={18} /> 신규 파트너 등록
          </button>
        </div>
      </header>

      {/* 필터 및 검색 바 */}
      <section className="bg-white/5 border border-white/10 rounded-[28px] p-6 backdrop-blur-xl flex flex-col sm:flex-row justify-between items-center gap-6">
        <div className="relative w-full max-w-md group">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-hover:text-blue-400 transition-colors" />
          <input 
            type="text" 
            placeholder="Search by name, tax ID, or owner..." 
            className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3 pl-12 pr-4 text-sm text-slate-300 outline-none focus:border-blue-500/50 transition-all shadow-inner font-bold"
          />
        </div>
        <div className="flex gap-3">
          <button className="flex items-center gap-2 px-6 py-3 rounded-2xl border border-white/10 text-slate-400 text-xs font-black uppercase tracking-widest hover:bg-white/5 transition-all">
            <Filter size={16} /> Filters
          </button>
          <button className="flex items-center gap-2 px-6 py-3 rounded-2xl border border-white/10 text-slate-400 text-xs font-black uppercase tracking-widest hover:bg-white/5 transition-all">
            <Download size={16} /> Export Excel
          </button>
        </div>
      </section>

      {/* 거래처 목록 그리드 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-5">Partner Category</th>
                <th className="px-6 py-5">Full Name</th>
                <th className="px-6 py-5">Tax Identifier</th>
                <th className="px-6 py-5">Representative</th>
                <th className="px-6 py-5">Status</th>
                <th className="px-8 py-5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {[
                { type: '매출처', name: '코드마스터(주)', id: '123-45-67890', owner: '이코드', status: '활성' },
                { type: '매입처', name: '(주)재무기술', id: '220-81-12345', owner: '김재무', status: '활성' },
                { type: '금융', name: '신한은행(강남)', id: '110-22-33333', owner: '은행장', status: '활성' },
              ].map((p, idx) => (
                <tr key={idx} className="hover:bg-white/[0.03] transition-colors group">
                  <td className="px-8 py-6">
                    <span className={`text-[10px] font-black px-3 py-1 rounded-full border ${
                      p.type === '매출처' ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' :
                      p.type === '매입처' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                      'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    }`}>
                      {p.type}
                    </span>
                  </td>
                  <td className="px-6 py-6 text-sm font-black text-white tracking-tight">{p.name}</td>
                  <td className="px-6 py-6 text-sm font-mono text-slate-400 font-bold tracking-tighter">{p.id}</td>
                  <td className="px-6 py-6 text-sm font-bold text-slate-300">{p.owner}</td>
                  <td className="px-6 py-6 text-sm">
                    <div className="flex items-center gap-2 font-bold text-emerald-500">
                      <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                      <span className="text-[11px] font-black uppercase">{p.status}</span>
                    </div>
                  </td>
                  <td className="px-8 py-6 text-right">
                    <button className="text-xs font-black text-slate-500 hover:text-white uppercase tracking-widest transition-colors">Details</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
