"use client";

import React, { useState } from 'react';
import { 
  Lock, 
  Check, 
  X, 
  Save, 
  RotateCcw, 
  ShieldCheck, 
  Sliders, 
  Layers, 
  CheckSquare, 
  FileText,
  Settings,
  PieChart,
  BookOpen
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface RoleDef {
  key: string;
  name: string;
  badgeColor: string;
}

const roles: RoleDef[] = [
  { key: 'SYSTEM_ADMIN', name: '시스템관리자', badgeColor: 'text-rose-400 border-rose-500/30 bg-rose-500/10' },
  { key: 'ACCOUNTING_ADMIN', name: '회계관리자', badgeColor: 'text-blue-400 border-blue-500/30 bg-blue-500/10' },
  { key: 'RISK_MANAGER', name: '리스크관리자', badgeColor: 'text-purple-400 border-purple-500/30 bg-purple-500/10' },
  { key: 'AUDITOR', name: '내부감사역', badgeColor: 'text-amber-400 border-amber-500/30 bg-amber-500/10' },
  { key: 'USER', name: '일반사용자', badgeColor: 'text-slate-400 border-slate-500/30 bg-slate-500/10' },
];

interface MenuGroup {
  groupName: string;
  icon: React.ElementType;
  items: {
    id: string;
    name: string;
    path: string;
    permissions: Record<string, boolean>;
  }[];
}

const initialMenuGroups: MenuGroup[] = [
  {
    groupName: '기준정보 관리 (Master Data)',
    icon: BookOpen,
    items: [
      { id: 'm1', name: '계정과목 체계', path: '/master/account', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: true, AUDITOR: true, USER: false } },
      { id: 'm2', name: '거래처 Master', path: '/master/partner', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: true, AUDITOR: true, USER: true } },
      { id: 'm3', name: '부서/코스트센터', path: '/system/departments', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: false, AUDITOR: true, USER: false } },
    ]
  },
  {
    groupName: '전표 및 장부 (Journals & Ledgers)',
    icon: FileText,
    items: [
      { id: 'j1', name: '전표 입력 및 검수', path: '/journal/entry', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: false, AUDITOR: true, USER: true } },
      { id: 'j2', name: '전표 결재/승인', path: '/master/approval', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: true, AUDITOR: true, USER: false } },
      { id: 'j3', name: '총계정원장 (GL)', path: '/journal/ledger/gl', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: true, AUDITOR: true, USER: true } },
    ]
  },
  {
    groupName: '결산 및 재무보고 (Closing & Reports)',
    icon: PieChart,
    items: [
      { id: 'c1', name: '월말 결산 프로세스', path: '/closing', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: false, AUDITOR: true, USER: false } },
      { id: 'c2', name: '재무제표 출력', path: '/reports/statements', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: true, AUDITOR: true, USER: true } },
      { id: 'c3', name: '공시 주석 생성기', path: '/reports/disclosure-notes', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: false, AUDITOR: true, USER: false } },
    ]
  },
  {
    groupName: '시스템 통제 (System Governance)',
    icon: Settings,
    items: [
      { id: 's1', name: '사용자 관리', path: '/system/users', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: false, RISK_MANAGER: false, AUDITOR: true, USER: false } },
      { id: 's2', name: '메뉴 권한 매트릭스', path: '/system/menus', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: false, RISK_MANAGER: false, AUDITOR: false, USER: false } },
      { id: 's3', name: '감사 및 시스템 로그', path: '/governance/audit-logs', permissions: { SYSTEM_ADMIN: true, ACCOUNTING_ADMIN: true, RISK_MANAGER: true, AUDITOR: true, USER: false } },
    ]
  }
];

export default function SystemMenusPage() {
  const [menuGroups, setMenuGroups] = useState<MenuGroup[]>(initialMenuGroups);
  const [isSaved, setIsSaved] = useState(false);

  const togglePermission = (groupIdx: number, itemIdx: number, roleKey: string) => {
    setMenuGroups(prev => {
      const next = JSON.parse(JSON.stringify(prev));
      next[groupIdx].items[itemIdx].permissions[roleKey] = !next[groupIdx].items[itemIdx].permissions[roleKey];
      return next;
    });
    setIsSaved(false);
  };

  const handleSave = () => {
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 3000);
  };

  const handleReset = () => {
    setMenuGroups(initialMenuGroups);
    setIsSaved(false);
  };

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="메뉴 권한 관리"
        description="역할별 접근 제어 매트릭스"
        breadcrumbs={[
          { label: 'System', href: '/system/menus' },
          { label: '메뉴 권한 관리' }
        ]}
        icon={Sliders}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleReset}
              className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 text-sm font-bold transition-all flex items-center gap-2"
            >
              <RotateCcw size={16} /> 초기화
            </button>
            <button
              onClick={handleSave}
              className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-2xl text-sm font-bold shadow-lg shadow-emerald-600/20 transition-all flex items-center gap-2"
            >
              <Save size={16} /> 변경사항 저장
            </button>
          </div>
        }
      />

      {isSaved && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-bold text-sm flex items-center gap-2">
          <CheckSquare size={18} /> 메뉴 접근 권한 매트릭스가 성공적으로 적용되었습니다.
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">총 메뉴 수</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">12개 메뉴</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Layers size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">정의된 Role 수</p>
              <h3 className="text-3xl font-black text-purple-400 mt-2 tracking-tight">5개 역할</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Lock size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">접근 통제 방식</p>
              <h3 className="text-xl font-black text-emerald-400 mt-2 tracking-tight">RBAC Strict</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <ShieldCheck size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">마지막 동기화</p>
              <h3 className="text-base font-black text-slate-300 mt-2 tracking-tight">오늘 09:00:00</h3>
            </div>
            <StatusBadge status="적용완료" variant="success" />
          </div>
        </div>
      </div>

      {/* Roles vs Menu Matrix Table */}
      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white">역할별 메뉴 접근 권한 매트릭스</h3>
            <p className="text-xs text-slate-400 mt-1">셀을 클릭하여 권한을 즉시 토글(허용/차단)할 수 있습니다.</p>
          </div>
          <div className="flex items-center gap-4 text-xs font-bold text-slate-400">
            <span className="flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-lg bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400"><Check size={12} /></span> 접근 허용
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400"><X size={12} /></span> 접근 차단
            </span>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-xs font-black text-slate-400 uppercase">
                <th className="py-4 px-4 w-72">메뉴 그룹 및 화면명</th>
                {roles.map(role => (
                  <th key={role.key} className="py-4 px-4 text-center">
                    <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold border ${role.badgeColor}`}>
                      {role.name}
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-sm">
              {menuGroups.map((group, groupIdx) => {
                const GroupIcon = group.icon;
                return (
                  <React.Fragment key={group.groupName}>
                    {/* Group Header Row */}
                    <tr className="bg-white/[0.02]">
                      <td colSpan={6} className="py-3 px-4 text-xs font-black text-blue-400 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <GroupIcon size={16} />
                          {group.groupName}
                        </div>
                      </td>
                    </tr>
                    {/* Menu Items Rows */}
                    {group.items.map((item, itemIdx) => (
                      <tr key={item.id} className="hover:bg-white/5 transition-colors">
                        <td className="py-3.5 px-4 pl-8">
                          <div className="font-bold text-white text-sm">{item.name}</div>
                          <div className="text-xs text-slate-500 font-mono mt-0.5">{item.path}</div>
                        </td>
                        {roles.map(role => {
                          const isAllowed = item.permissions[role.key];
                          return (
                            <td key={role.key} className="py-3.5 px-4 text-center">
                              <button
                                onClick={() => togglePermission(groupIdx, itemIdx, role.key)}
                                className={`w-8 h-8 rounded-xl inline-flex items-center justify-center transition-all ${
                                  isAllowed
                                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/30 shadow-md shadow-emerald-500/10'
                                    : 'bg-rose-500/10 text-rose-400 border border-rose-500/20 hover:bg-rose-500/20'
                                }`}
                              >
                                {isAllowed ? <Check size={16} /> : <X size={16} />}
                              </button>
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
