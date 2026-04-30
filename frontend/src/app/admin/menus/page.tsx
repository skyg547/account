"use client";

import React, { useState } from 'react';
import { 
  Layers, 
  ChevronRight, 
  ChevronDown, 
  Plus, 
  Menu, 
  Move, 
  Shield, 
  Eye, 
  EyeOff,
  Search,
  ExternalLink,
  Save,
  RotateCcw,
  Settings,
  MoreHorizontal
} from 'lucide-react';

// Mock 데이터: 메뉴 구조
const initialMenuStructure = [
  { 
    id: 'DASHBOARD', name: '현황판 (Dashboard)', slug: 'dashboard', 
    permissions: ['ALL'], active: true,
    sub: [
      { id: 'D1', name: '통합 대시보드', slug: '/', permissions: ['ALL'], active: true }
    ]
  },
  { 
    id: 'ACCOUNTING', name: '회계/전표 (Accounting)', slug: 'journal', 
    permissions: ['ACCOUNTING_ADMIN', 'USER'], active: true,
    sub: [
      { id: 'A1', name: '전표 조회/승인', slug: '/journal/list', permissions: ['ACCOUNTING_ADMIN'], active: true },
      { id: 'A2', name: '전표 입력', slug: '/journal/entry', permissions: ['USER'], active: true },
      { id: 'A3', name: '총계정원장(G/L)', slug: '/journal/ledger/gl', permissions: ['ALL'], active: true },
      { id: 'A4', name: '결산 대시보드', slug: '/closing', permissions: ['ACCOUNTING_ADMIN'], active: false },
    ]
  },
  { 
    id: 'RISK', name: '리스크 관리 (Risk)', slug: 'risk', 
    permissions: ['RISK_MANAGER', 'AUDITOR'], active: true,
    sub: [
      { id: 'R1', name: 'Basel III RWA', slug: '/risk/basel-iii/rwa', permissions: ['RISK_MANAGER'], active: true },
      { id: 'R2', name: 'IFRS 9 Simulation', slug: '/risk/ifrs-9/ecl', permissions: ['RISK_MANAGER'], active: true },
    ]
  },
];

const PermissionBadge = ({ role }: { role: string }) => {
  const styles: Record<string, string> = {
    ALL: 'bg-slate-700 text-slate-300 border-slate-600',
    ACCOUNTING_ADMIN: 'bg-blue-600/10 text-blue-400 border-blue-500/20',
    RISK_MANAGER: 'bg-purple-600/10 text-purple-400 border-purple-500/20',
    USER: 'bg-slate-300/10 text-slate-400 border-slate-500/10',
    AUDITOR: 'bg-amber-600/10 text-amber-400 border-amber-500/20',
  };
  return (
    <span className={`text-[10px] font-black px-2 py-0.5 rounded-md border ${styles[role] || styles.USER} tracking-tight`}>
       {role}
    </span>
  );
};

export default function MenuManagementPage() {
  const [expanded, setExpanded] = useState<string[]>(['ACCOUNTING']);

  const toggleExpand = (id: string) => {
    setExpanded(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-emerald-500 mb-2">
            <Layers size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">IA Architecture</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            메뉴 및 접근 권한 구조
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            시스템 내비게이션 구조를 관리하고 각 메뉴별로 접근 가능한 보안 역할(Role)을 매핑합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <RotateCcw size={18} /> 변경사항 취소
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Save size={18} /> 구조 저장
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Menu Tree List */}
        <div className="lg:col-span-7 glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
           <div className="flex items-center justify-between mb-8">
              <h3 className="text-xl font-black text-white italic tracking-tight">서비스 메뉴 트리</h3>
              <button className="p-2 bg-blue-600/10 text-blue-400 rounded-xl hover:bg-blue-600/20 transition-all">
                 <Plus size={20} />
              </button>
           </div>

           <div className="space-y-4">
              {initialMenuStructure.map((group) => (
                <div key={group.id} className="group/menu">
                  <div className={`flex items-center justify-between p-5 rounded-[1.5rem] border transition-all ${expanded.includes(group.id) ? 'bg-white/[0.03] border-white/10' : 'bg-transparent border-white/5 cursor-pointer hover:border-white/20'}`}>
                    <div className="flex items-center gap-4" onClick={() => toggleExpand(group.id)}>
                      <button className="p-1 rounded-md hover:bg-white/10 text-slate-500">
                        {expanded.includes(group.id) ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
                      </button>
                      <div className="w-10 h-10 rounded-xl bg-slate-900 border border-white/5 flex items-center justify-center text-blue-500">
                         <Menu size={18} />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-base font-black text-white tracking-tight">{group.name}</span>
                        <div className="flex gap-1 mt-1">
                          {group.permissions.map(p => <PermissionBadge key={p} role={p} />)}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 opacity-0 group-hover/menu:opacity-100 transition-opacity">
                       <button className="p-2 text-slate-500 hover:text-white transition-colors"><Plus size={16} /></button>
                       <button className="p-2 text-slate-500 hover:text-white transition-colors"><Settings size={16} /></button>
                       <div className="h-4 w-[1px] bg-white/10 mx-1" />
                       <button className="p-2 text-blue-400 hover:scale-110 transition-transform cursor-grab active:cursor-grabbing"><Move size={16} /></button>
                    </div>
                  </div>

                  {expanded.includes(group.id) && (
                    <div className="ml-14 mt-4 space-y-3 animate-in slide-in-from-top-2 duration-300">
                       {group.sub.map((sub) => (
                         <div key={sub.id} className="flex items-center justify-between p-4 rounded-2xl bg-white/[0.01] border border-white/5 group/sub hover:border-white/10 transition-all">
                           <div className="flex items-center gap-4">
                             <div className="w-1.5 h-1.5 bg-blue-600 rounded-full" />
                             <div className="flex flex-col">
                               <span className="text-sm font-bold text-slate-300">{sub.name}</span>
                               <span className="text-[10px] text-slate-600 font-medium tracking-tight uppercase">{sub.slug}</span>
                             </div>
                           </div>
                           <div className="flex items-center gap-4">
                              <div className="flex gap-1">
                                {sub.permissions.map(p => <PermissionBadge key={p} role={p} />)}
                              </div>
                              <button className={`p-2 transition-colors ${sub.active ? 'text-blue-500' : 'text-slate-700'}`}>
                                 {sub.active ? <Eye size={16} /> : <EyeOff size={16} />}
                              </button>
                              <button className="p-2 text-slate-700 hover:text-white transition-colors"><MoreHorizontal size={16} /></button>
                           </div>
                         </div>
                       ))}
                    </div>
                  )}
                </div>
              ))}
           </div>
        </div>

        {/* Menu Editor / Preview */}
        <div className="lg:col-span-5 space-y-8 sticky top-[120px]">
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-blue-600/[0.02]">
              <div className="flex items-center gap-3 mb-6">
                 <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center text-white">
                    <Shield size={20} />
                 </div>
                 <h3 className="text-xl font-black text-white italic tracking-tight">메뉴 접근 보안 로직</h3>
              </div>
              
              <div className="space-y-6 pt-4">
                 <div className="space-y-3">
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">메뉴 접근 제한 정책</label>
                    <select className="w-full bg-slate-950 border border-white/5 rounded-2xl p-4 text-white text-sm outline-none focus:border-blue-500/30 transition-all appearance-none cursor-pointer font-bold">
                       <option>RBAC (Role Based Access Control)</option>
                       <option>ABAC (Attribute Based Access Control)</option>
                    </select>
                 </div>

                 <div className="p-6 rounded-[2rem] bg-white/5 border border-white/5">
                    <h4 className="text-sm font-black text-white mb-2 italic">Hierarchy Inheritance</h4>
                    <p className="text-xs text-slate-500 leading-relaxed font-medium">상위 카테고리의 권한을 하위 메뉴가 자동으로 상속할지 여부를 결정합니다. 보안상 &apos;상속 해제&apos;를 권장합니다.</p>
                    <div className="mt-4 flex items-center justify-between">
                       <span className="text-xs font-black text-blue-400">상속 활성화</span>
                       <div className="w-10 h-5 bg-blue-600 rounded-full relative p-1 cursor-pointer">
                          <div className="w-3 h-3 bg-white rounded-full ml-auto" />
                       </div>
                    </div>
                 </div>
              </div>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-slate-950">
              <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] mb-6 flex items-center gap-2">
                 <Search size={14} className="text-blue-500" /> 메뉴 링크 프리뷰
              </h4>
              <div className="p-5 rounded-2xl bg-white/5 border border-dashed border-white/10 flex flex-col gap-4">
                 <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-400 font-mono">/journal/ledger/gl</span>
                    <button className="text-blue-500 hover:text-blue-400 transition-colors"><ExternalLink size={14} /></button>
                 </div>
                 <div className="h-32 rounded-xl bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center border border-white/5 italic text-slate-600 text-xs">
                    No Live Preview Available
                 </div>
              </div>
           </div>
        </div>
      </div>
    </div>
  );
}
