"use client";

import React, { useState } from 'react';
import { 
  CheckCircle2, 
  XCircle, 
  Clock, 
  User, 
  FileText, 
  Layers, 
  Users, 
  ExternalLink,
  ShieldCheck,
  Zap,
  ArrowRight
} from 'lucide-react';

// Mock 데이터: 승인 대기 중인 요청들
const pendingRequests = [
  { id: 'REQ-001', type: 'ACCOUNT', requester: 'jm.kim', date: '2026-04-24 18:05', title: '신규 법인용 유동자산 세부 계정', detail: '1100 그룹 하위 [1105. 외화보통예금] 추가 요청', priority: 'HIGH' },
  { id: 'REQ-002', type: 'PARTNER', requester: 'sy.lee', date: '2026-04-24 17:30', title: '신규 전략 파트너 (주)미래소프트', detail: '매출처 등록 요청 (사업자: 401-81-12345)', priority: 'NORMAL' },
  { id: 'REQ-003', type: 'PARTNER', requester: 'dw.park', date: '2026-04-24 16:15', title: '사무용품 공급업체 (주)오피스넷', detail: '매입처 등록 요청 (사업자: 105-86-99887)', priority: 'LOW' },
];

/**
 * [기준 정보 승인 관리 화면]
 * 계정과목, 거래처 등 사용자의 마스터 데이터 등록 요청을 심사합니다.
 */
export default function MasterApprovalPage() {
  const [activeTab, setActiveTab] = useState('PENDING');

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-emerald-500 mb-2">
            <ShieldCheck size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Governance & Approval</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            기준 정보 거버넌스 승인
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            데이터 정합성과 회계 투명성을 위해 사용자가 요청한 신규 기준 정보(거래처/계정)를 최종 심사합니다.
          </p>
        </div>

        <div className="flex items-center gap-6 bg-white/[0.02] border border-white/5 rounded-3xl p-2">
           <button 
             onClick={() => setActiveTab('PENDING')}
             className={`px-6 py-3 rounded-2xl text-xs font-black tracking-widest transition-all ${activeTab === 'PENDING' ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/20' : 'text-slate-500 hover:text-white'}`}
           >
             PENDING
           </button>
           <button 
             onClick={() => setActiveTab('HISTORY')}
             className={`px-6 py-3 rounded-2xl text-xs font-black tracking-widest transition-all ${activeTab === 'HISTORY' ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/20' : 'text-slate-500 hover:text-white'}`}
           >
             HISTORY
           </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Main Approval List */}
        <div className="lg:col-span-8 space-y-4">
           {pendingRequests.map((req) => (
             <div key={req.id} className="glass-panel p-8 rounded-[2.5rem] border border-white/10 bg-white/[0.01] hover:border-white/20 transition-all group/card">
                <div className="flex items-start justify-between mb-6">
                   <div className="flex items-center gap-4">
                      <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all ${
                        req.type === 'ACCOUNT' ? 'bg-blue-600/10 border-blue-500/20 text-blue-400' : 'bg-emerald-600/10 border-emerald-500/20 text-emerald-400'
                      }`}>
                         {req.type === 'ACCOUNT' ? <Layers size={22} /> : <Users size={22} />}
                      </div>
                      <div>
                         <div className="flex items-center gap-2">
                            <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{req.id}</span>
                            <div className="w-1 h-1 rounded-full bg-slate-800" />
                            <span className="text-[10px] font-black text-blue-500 uppercase tracking-widest">{req.type}</span>
                         </div>
                         <h4 className="text-xl font-black text-white italic tracking-tight">{req.title}</h4>
                      </div>
                   </div>
                   <div className="flex flex-col items-end">
                      <div className={`text-[10px] font-black px-2.5 py-1 rounded-md border ${
                        req.priority === 'HIGH' ? 'bg-rose-500/10 text-rose-500 border-rose-500/20' : 'bg-slate-500/10 text-slate-500 border-slate-500/20'
                      }`}>
                         {req.priority} PRIORITY
                      </div>
                      <span className="text-[10px] text-slate-600 mt-2 font-bold">{req.date}</span>
                   </div>
                </div>

                <div className="p-5 rounded-2xl bg-slate-950 border border-white/5 mb-8">
                   <p className="text-sm text-slate-400 font-medium leading-relaxed">{req.detail}</p>
                </div>

                <div className="flex items-center justify-between">
                   <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-slate-800 border border-white/10 flex items-center justify-center text-slate-500">
                         <User size={14} />
                      </div>
                      <span className="text-xs font-black text-slate-400">Requester: <span className="text-white">{req.requester}</span></span>
                   </div>
                   <div className="flex items-center gap-3 opacity-0 group-hover/card:opacity-100 transition-opacity translate-x-4 group-hover/card:translate-x-0 transition-transform">
                      <button className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-rose-500 text-xs font-black transition-all">
                        <XCircle size={16} /> REJECT
                      </button>
                      <button className="flex items-center gap-2 px-8 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black transition-all shadow-lg shadow-emerald-900/40">
                        <CheckCircle2 size={16} /> APPROVE
                      </button>
                   </div>
                </div>
             </div>
           ))}

           {pendingRequests.length === 0 && (
             <div className="glass-panel p-20 rounded-[3rem] border border-white/10 flex flex-col items-center justify-center text-center italic">
                <Zap size={48} className="text-slate-800 mb-6" />
                <p className="text-slate-600 font-black">All requests have been processed.</p>
             </div>
           )}
        </div>

        {/* Audit Stats & Sidebar */}
        <div className="lg:col-span-4 space-y-8">
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-blue-600/[0.02]">
              <div className="flex items-center justify-between mb-8">
                 <h3 className="text-lg font-black text-white italic">Governance Stats</h3>
                 <Clock size={20} className="text-slate-600" />
              </div>
              <div className="space-y-6">
                 <div>
                    <div className="flex justify-between text-[10px] font-black text-slate-500 uppercase tracking-widest mb-2">
                       <span>Data Accuracy</span>
                       <span>96%</span>
                    </div>
                    <div className="h-1.5 w-full bg-white/5 rounded-full overflow-hidden">
                       <div className="h-full bg-blue-600 w-[96%]" />
                    </div>
                 </div>
                 <div className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5">
                    <div className="flex flex-col">
                       <span className="text-[10px] font-black text-slate-600 uppercase">Monthly Approvals</span>
                       <span className="text-xl font-black text-white italic">1,240</span>
                    </div>
                    <ArrowRight size={20} className="text-blue-500" />
                 </div>
              </div>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-slate-950">
              <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] mb-6">Approval Guidelines</h4>
              <ul className="space-y-4">
                 {[
                   '계정과목 코드가 표준 마스터와 중복되는지 확인',
                   '거래처 사업자등록번호 유효성 검증 필수',
                   '외화 계정 요청 시 환율 평가 방식 지정 확인'
                 ].map((text, i) => (
                   <li key={i} className="flex gap-3 text-xs text-slate-600 font-medium leading-relaxed italic">
                      <FileText size={16} className="text-blue-500 shrink-0" />
                      {text}
                   </li>
                 ))}
              </ul>
              <button className="w-full mt-8 py-3.5 rounded-2xl bg-white/5 border border-white/10 text-white text-[10px] font-black tracking-widest uppercase hover:bg-white/10 transition-all flex items-center justify-center gap-2">
                 View Full Guidelines <ExternalLink size={14} />
              </button>
           </div>
        </div>
      </div>
    </div>
  );
}
