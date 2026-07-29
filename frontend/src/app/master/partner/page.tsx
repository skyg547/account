"use client";

import React, { useState, useEffect } from 'react';
import { Search, Plus, Filter, Download, Send, CreditCard, Landmark, Users } from 'lucide-react';
import { masterDataService, BusinessPartnerDto } from '@/services/masterDataService';

/**
 * [거래처 관리 화면]
 * 외부 비즈니스 파트너 데이터를 관리하며 일반 사용자의 등록 요청 기능을 포함합니다.
 * 백엔드 Master Data API 연동 완료.
 */
export default function PartnerPage() {
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [partners, setPartners] = useState<BusinessPartnerDto[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchPartners = async () => {
    setLoading(true);
    try {
      const data = await masterDataService.getBusinessPartners();
      setPartners(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchPartners();
  }, []);

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col md:flex-row justify-between items-end gap-6">
        <div>
          <div className="flex items-center gap-3 text-emerald-500 mb-2">
            <Users size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Business Registry</span>
          </div>
          <h2 className="text-4xl font-black text-white italic tracking-tighter italic">거래처 통합 관리</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">전사 파트너 DB 관리 및 사용자의 신규 등록 요청 워크플로우 지원</p>
        </div>
        <div className="flex gap-3">
          <button 
            onClick={() => setIsRequestModalOpen(true)}
            className="bg-white/5 hover:bg-white/10 text-white px-8 py-3.5 rounded-2xl border border-white/10 transition-all flex items-center gap-3 text-sm font-black uppercase tracking-widest active:scale-95 shadow-2xl"
          >
            <Send size={18} className="text-blue-500" /> 신규 거래처 요청
          </button>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-8 py-3.5 rounded-2xl border border-blue-500/20 transition-all flex items-center gap-3 text-sm font-black uppercase tracking-widest shadow-xl shadow-blue-600/20 active:scale-95">
            <Plus size={18} /> 직접 등록 (관리자)
          </button>
        </div>
      </header>

      {/* 필터 및 검색 바 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] p-6 backdrop-blur-xl flex flex-col sm:flex-row justify-between items-center gap-6">
        <div className="relative w-full max-w-md group">
          <Search className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-blue-500 transition-colors" size={20} />
          <input 
            type="text" 
            placeholder="Search by name, tax ID, or owner..." 
            className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 pl-14 pr-6 text-sm text-white outline-none focus:border-blue-500/50 transition-all shadow-inner font-bold"
          />
        </div>
        <div className="flex gap-3">
          <button className="flex items-center gap-2 px-6 py-3 rounded-2xl border border-white/10 text-slate-500 text-xs font-black uppercase tracking-widest hover:bg-white/5 transition-all">
            <Filter size={16} /> Filters
          </button>
          <button className="flex items-center gap-2 px-6 py-3 rounded-2xl border border-white/10 text-slate-500 text-xs font-black uppercase tracking-widest hover:bg-white/5 transition-all">
            <Download size={16} /> Export
          </button>
        </div>
      </section>

      {/* 거래처 목록 그리드 */}
      <section className="bg-white/5 border border-white/10 rounded-[3rem] overflow-hidden backdrop-blur-xl transition-all">
        {loading ? (
          <div className="flex justify-center items-center py-20">
            <span className="text-white">데이터를 불러오는 중입니다...</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="text-slate-600 text-[10px] font-black uppercase tracking-widest border-b border-white/5">
                  <th className="px-10 py-6">Partner Category</th>
                  <th className="px-6 py-6">Full Name / Code</th>
                  <th className="px-6 py-6">Tax Identifier</th>
                  <th className="px-6 py-6 font-mono">Status</th>
                  <th className="px-10 py-6 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/[0.02]">
                {partners.length > 0 ? (
                  partners.map((p, idx) => (
                    <tr key={idx} className="hover:bg-white/[0.02] transition-colors group">
                      <td className="px-10 py-8">
                        <span className={`text-[10px] font-black px-3.5 py-1.5 rounded-xl border ${
                          p.partnerType === 'CUSTOMER' ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' :
                          p.partnerType === 'BANK' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                          'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                        }`}>
                          {p.partnerType}
                        </span>
                      </td>
                      <td className="px-6 py-8">
                        <div className="flex flex-col gap-1">
                          <span className="text-base font-black text-white tracking-tight">{p.businessPartnerName}</span>
                          <span className="text-[10px] text-slate-600 font-bold tracking-widest uppercase">{p.businessPartnerCode}</span>
                        </div>
                      </td>
                      <td className="px-6 py-8 text-sm font-mono text-slate-400 font-bold tracking-tighter">{p.registrationNumber || '-'}</td>
                      <td className="px-6 py-8">
                        <div className={`flex items-center gap-2 font-black ${p.useYn ? 'text-emerald-500' : 'text-amber-500'}`}>
                          <div className={`w-1.5 h-1.5 rounded-full ${p.useYn ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-amber-500 animate-pulse'}`} />
                          <span className="text-[10px] uppercase tracking-widest">{p.useYn ? 'ACTIVE' : 'INACTIVE'}</span>
                        </div>
                      </td>
                      <td className="px-10 py-8 text-right">
                        <button className="text-[10px] font-black text-slate-500 hover:text-white uppercase tracking-widest transition-all hover:bg-white/5 px-4 py-2 rounded-xl border border-transparent hover:border-white/10">Details</button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="text-center py-10 text-slate-500">
                      조회된 거래처가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* [MODAL] Request for New Partner */}
      {isRequestModalOpen && (
        <div className="fixed inset-0 z-[1000] flex items-center justify-center p-6 sm:p-20">
           <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-md" onClick={() => setIsRequestModalOpen(false)} />
           <div className="relative w-full max-w-2xl bg-[#020617] border border-white/10 rounded-[3rem] p-10 shadow-2xl animate-in zoom-in-95 duration-300">
              <div className="flex items-center gap-4 mb-10">
                 <div className="w-16 h-16 rounded-2xl bg-emerald-600 flex items-center justify-center text-white shadow-xl shadow-emerald-900/20">
                    <Send size={28} />
                 </div>
                 <div>
                    <h3 className="text-2xl font-black text-white italic tracking-tight uppercase">New Partner Request</h3>
                    <p className="text-slate-500 text-sm font-medium">관리자에게 신규 비즈니스 파트너 등록을 요청합니다.</p>
                 </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                 <div className="space-y-6">
                    <div className="space-y-2">
                       <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">카테고리</label>
                       <div className="grid grid-cols-2 gap-2">
                          <button className="flex items-center gap-2 px-4 py-3 rounded-xl bg-blue-600/10 border border-blue-500/30 text-blue-400 text-xs font-black">
                             <CreditCard size={14} /> 매출처
                          </button>
                          <button className="flex items-center gap-2 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-slate-500 text-xs font-black hover:bg-white/10">
                             <Landmark size={14} /> 매입처
                          </button>
                       </div>
                    </div>
                    <div className="space-y-2">
                       <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">사업자 등록번호</label>
                       <input type="text" className="w-full bg-slate-900 border border-white/5 rounded-2xl p-4 text-white font-mono text-sm outline-none focus:border-blue-500/30" placeholder="000-00-00000" />
                    </div>
                 </div>

                 <div className="space-y-6">
                    <div className="space-y-2">
                       <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">거래처 상호명</label>
                       <input type="text" className="w-full bg-slate-900 border border-white/5 rounded-2xl p-4 text-white text-sm outline-none focus:border-blue-500/30 font-black" placeholder="(주)엔티그라비티" />
                    </div>
                    <div className="space-y-2">
                       <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">대표자 성명</label>
                       <input type="text" className="w-full bg-slate-900 border border-white/5 rounded-2xl p-4 text-white text-sm outline-none focus:border-blue-500/30 font-bold" placeholder="홍길동" />
                    </div>
                 </div>
              </div>

              <div className="mt-8 space-y-2">
                 <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">첨부 서류 (사업자등록증 등)</label>
                 <div className="w-full h-24 border-2 border-dashed border-white/5 rounded-2xl flex flex-col items-center justify-center text-slate-600 hover:border-blue-500/30 cursor-pointer transition-all">
                    <Download size={20} className="mb-2" />
                    <span className="text-[10px] font-black uppercase tracking-widest">Click to upload documents</span>
                 </div>
              </div>

              <div className="flex gap-4 pt-10">
                 <button 
                   onClick={() => setIsRequestModalOpen(false)}
                   className="flex-1 py-4 bg-white/5 hover:bg-white/10 rounded-2xl text-slate-400 font-black tracking-widest text-xs transition-all uppercase"
                 >
                   Discard
                 </button>
                 <button className="flex-[2] py-4 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white font-black tracking-widest text-xs transition-all uppercase shadow-xl shadow-blue-600/20 active:scale-95 transition-all">
                   Submit Partner Request
                 </button>
              </div>
           </div>
        </div>
      )}
    </div>
  );
}
