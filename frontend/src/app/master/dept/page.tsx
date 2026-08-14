"use client";

import React, { useState } from 'react';
import { 
  Landmark, 
  Plus, 
  Search, 
  MoreVertical, 
  MapPin, 
  Users, 
  CheckCircle2,
  Building2,
  TreePine,
  Edit2,
} from 'lucide-react';

// Mock 데이터: 귀속 부서 정보
const deptData = [
  { id: 'HQ', name: '본사 (Main Office)', head: '김대표', count: 45, status: 'ACTIVE', type: '본부', location: '서울 영등포구' },
  { id: 'FIN', name: '재무회계본부', head: '이본부', count: 12, status: 'ACTIVE', type: '본부', location: '서울 영등포구' },
  { id: 'ACC', name: '회계팀', head: '박부장', count: 8, status: 'ACTIVE', type: '팀', location: '서울 영등포구' },
  { id: 'RISK', name: '리스크관리부', head: '최부장', count: 6, status: 'ACTIVE', type: '팀', location: '서울 영등포구' },
  { id: 'IT', name: '디지털혁신실', head: '정실장', count: 15, status: 'ACTIVE', type: '실', location: '경기 판교' },
];

/**
 * [귀속 부서 관리 화면]
 * 기준 정보(Master Data)로서의 부서 조직도를 관리합니다.
 */
export default function DeptManagementPage() {
  const [searchTerm, setSearchTerm] = useState('');

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <Building2 size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Master Data Management</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            귀속 부서 및 조직 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            전사 조직 체계를 정의하고 전표 및 리스크 데이터의 귀속 경계가 되는 부서 코드를 관리합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-white text-sm font-black transition-all flex items-center gap-2">
            <TreePine size={18} className="text-emerald-500" /> 조직도 보기
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
            <Plus size={18} /> 부서 신규 등록
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Department List List */}
        <div className="lg:col-span-8 glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
          <div className="flex items-center justify-between mb-8">
             <div className="relative group/search max-w-sm w-full">
                <Search className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within/search:text-blue-500 transition-colors" size={20} />
                <input 
                  type="text" 
                  placeholder="부서명, 코드, 관리자 검색..."
                  className="w-full bg-slate-950 border border-white/5 focus:border-blue-500/50 rounded-2xl py-4 pl-14 pr-6 text-white text-sm outline-none transition-all placeholder:text-slate-700 font-bold"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
             </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-white/5">
                  <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">부서 코드/명</th>
                  <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">관리자/인원</th>
                  <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">유형</th>
                  <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">상태</th>
                  <th className="pb-6 px-4 text-right"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/[0.02]">
                {deptData.map((dept) => (
                  <tr key={dept.id} className="group/row hover:bg-white/[0.02] transition-colors">
                    <td className="py-6 px-4">
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-xl bg-slate-900 border border-white/5 flex items-center justify-center text-blue-500 group-hover/row:scale-110 transition-transform">
                           <Landmark size={18} />
                        </div>
                        <div className="flex flex-col">
                          <span className="text-sm font-black text-white">{dept.name}</span>
                          <span className="text-[10px] text-slate-600 font-bold uppercase tracking-wider">{dept.id}</span>
                        </div>
                      </div>
                    </td>
                    <td className="py-6 px-4">
                      <div className="flex flex-col gap-1">
                        <span className="text-sm font-bold text-slate-300">{dept.head}</span>
                        <span className="text-xs text-slate-600 flex items-center gap-1 font-medium italic">
                           <Users size={12} /> {dept.count} 명
                        </span>
                      </div>
                    </td>
                    <td className="py-6 px-4">
                       <span className="text-xs font-black text-slate-500 bg-white/5 px-2.5 py-1 rounded-lg border border-white/5 uppercase">
                          {dept.type}
                       </span>
                    </td>
                    <td className="py-6 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]" />
                        <span className="text-xs font-black text-emerald-500 uppercase">{dept.status}</span>
                      </div>
                    </td>
                    <td className="py-6 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button className="p-2.5 rounded-xl hover:bg-white/5 text-slate-600 hover:text-white transition-all opacity-0 group-hover/row:opacity-100">
                          <Edit2 size={16} />
                        </button>
                        <button className="p-2.5 rounded-xl hover:bg-white/5 text-slate-600 hover:text-white transition-all">
                          <MoreVertical size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Info Sidebar */}
        <div className="lg:col-span-4 space-y-8">
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-blue-600/[0.02]">
              <h3 className="text-lg font-black text-white italic mb-6">부서 관리 가이드</h3>
              <ul className="space-y-4">
                 {[
                   '상위 부서 삭제 시 하위 부서 귀속 여부 확인 필수',
                   '부서 폐쇄 시 잔여 전표 정산 처리 선행',
                   '거래처 귀속 부서 변경 시 이력 자동 생성'
                 ].map((text, i) => (
                   <li key={i} className="flex gap-3 text-xs text-slate-500 font-medium leading-relaxed">
                      <CheckCircle2 size={16} className="text-blue-500 shrink-0" />
                      {text}
                   </li>
                 ))}
              </ul>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-slate-950">
              <div className="flex items-center gap-3 mb-6">
                 <MapPin size={20} className="text-slate-600" />
                 <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">부서 상세 위치 정보</span>
              </div>
              <div className="h-40 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 border border-white/5 flex items-center justify-center italic text-slate-700 text-xs text-center px-6">
                 Select a department to view <br /> Geographic assignment map
              </div>
           </div>
        </div>
      </div>
    </div>
  );
}
