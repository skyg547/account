"use client";

import React, { useState, useEffect } from 'react';
import { 
  Lock, Check, X, RotateCcw, ShieldCheck, Sliders, Layers, FileText, Settings, PieChart, BookOpen, AlertCircle
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { allMenus } from '@/components/layout/menus';

const GOVERNANCE_API_BASE_URL = process.env.NEXT_PUBLIC_GOVERNANCE_API_URL || '';

interface SystemRole {
  roleCode: string;
  roleName: string;
}

interface Authorization {
  id: number;
  roleCode: string;
  functionCode: string;
  accessType: string;
}

const MOCK_ROLES: SystemRole[] = [
  { roleCode: 'SYSTEM_ADMIN', roleName: '시스템 최고관리자' },
  { roleCode: 'ACCOUNTING_ADMIN', roleName: '재무회계 관리자' },
  { roleCode: 'RISK_MANAGER', roleName: '리스크 책임자' },
  { roleCode: 'MASTER_MANAGER', roleName: '기준정보 관리자' },
  { roleCode: 'AUDITOR', roleName: '내부감사관' },
  { roleCode: 'USER', roleName: '일반 실무자' },
];

const MOCK_AUTHS: Record<string, Authorization[]> = {
  ACCOUNTING_ADMIN: [
    { id: 101, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:/', accessType: 'READ_WRITE' },
    { id: 102, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:/journal/entry', accessType: 'READ_WRITE' },
    { id: 103, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:/journal/list', accessType: 'READ_WRITE' },
    { id: 104, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:/ledger/gl', accessType: 'READ_WRITE' },
    { id: 105, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:/closing', accessType: 'READ_WRITE' },
    { id: 106, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:/reports/statements', accessType: 'READ_WRITE' },
  ],
  RISK_MANAGER: [
    { id: 201, roleCode: 'RISK_MANAGER', functionCode: 'MENU:/', accessType: 'READ' },
    { id: 202, roleCode: 'RISK_MANAGER', functionCode: 'MENU:/ecl/results', accessType: 'READ_WRITE' },
    { id: 203, roleCode: 'RISK_MANAGER', functionCode: 'MENU:/ecl/parameters', accessType: 'READ_WRITE' },
    { id: 204, roleCode: 'RISK_MANAGER', functionCode: 'MENU:/mart/reconciliation-diff', accessType: 'READ_WRITE' },
  ],
  MASTER_MANAGER: [
    { id: 301, roleCode: 'MASTER_MANAGER', functionCode: 'MENU:/', accessType: 'READ' },
    { id: 302, roleCode: 'MASTER_MANAGER', functionCode: 'MENU:/master/account', accessType: 'READ_WRITE' },
    { id: 303, roleCode: 'MASTER_MANAGER', functionCode: 'MENU:/master/partner', accessType: 'READ_WRITE' },
  ],
  AUDITOR: [
    { id: 401, roleCode: 'AUDITOR', functionCode: 'MENU:/', accessType: 'READ' },
    { id: 402, roleCode: 'AUDITOR', functionCode: 'MENU:/governance/audit-logs', accessType: 'READ' },
    { id: 403, roleCode: 'AUDITOR', functionCode: 'MENU:/governance/controls', accessType: 'READ' },
    { id: 404, roleCode: 'AUDITOR', functionCode: 'MENU:/system/logs', accessType: 'READ' },
  ],
  USER: [
    { id: 501, roleCode: 'USER', functionCode: 'MENU:/', accessType: 'READ' },
    { id: 502, roleCode: 'USER', functionCode: 'MENU:/expenditure/resolution', accessType: 'READ_WRITE' },
  ],
};

export default function SystemMenusPage() {
  const [roles, setRoles] = useState<SystemRole[]>(MOCK_ROLES);
  const [authorizations, setAuthorizations] = useState<Record<string, Authorization[]>>(MOCK_AUTHS);
  const [loading, setLoading] = useState(false);
  const [isLiveConnected, setIsLiveConnected] = useState(false);

  const fetchRolesAndAuths = async () => {
    if (!GOVERNANCE_API_BASE_URL) {
      setRoles(MOCK_ROLES);
      setAuthorizations(MOCK_AUTHS);
      setLoading(false);
      setIsLiveConnected(false);
      return;
    }

    setLoading(true);
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 1500);

      const roleRes = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles`, { signal: controller.signal });
      clearTimeout(timeoutId);

      if (!roleRes.ok) throw new Error('Failed to fetch roles');
      const roleData: SystemRole[] = await roleRes.json();
      setRoles(roleData);

      const authMap: Record<string, Authorization[]> = {};
      for (const role of roleData) {
        if (role.roleCode === 'SYSTEM_ADMIN') continue;
        try {
          const authRes = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles/${role.roleCode}/authorizations`);
          if (authRes.ok) {
            authMap[role.roleCode] = await authRes.json();
          }
        } catch {
          // ignore individual role fail
        }
      }
      setAuthorizations(authMap);
      setIsLiveConnected(true);
    } catch {
      // Mock Fallback
      setRoles(MOCK_ROLES);
      setAuthorizations(MOCK_AUTHS);
      setIsLiveConnected(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRolesAndAuths();
  }, []);

  const hasPermission = (roleCode: string, href: string) => {
    if (roleCode === 'SYSTEM_ADMIN') return true;
    const roleAuths = authorizations[roleCode] || [];
    return roleAuths.some(auth => auth.functionCode === `MENU:${href}` || auth.functionCode === 'MENU:*');
  };

  const getAuthorizationId = (roleCode: string, href: string) => {
    const roleAuths = authorizations[roleCode] || [];
    const auth = roleAuths.find(a => a.functionCode === `MENU:${href}`);
    return auth ? auth.id : null;
  };

  const togglePermission = async (roleCode: string, href: string) => {
    if (roleCode === 'SYSTEM_ADMIN') {
      alert("SYSTEM_ADMIN 권한은 항상 모든 메뉴에 접근 가능합니다.");
      return;
    }

    const currentAuthId = getAuthorizationId(roleCode, href);
    const isAllowed = !!currentAuthId;

    if (!isLiveConnected || !GOVERNANCE_API_BASE_URL) {
      // 로컬 Mock 상태 즉시 토글 시뮬레이션
      setAuthorizations(prev => {
        const currentList = prev[roleCode] || [];
        if (isAllowed) {
          return {
            ...prev,
            [roleCode]: currentList.filter(a => a.functionCode !== `MENU:${href}`)
          };
        } else {
          const newAuth: Authorization = {
            id: Date.now(),
            roleCode,
            functionCode: `MENU:${href}`,
            accessType: 'READ_WRITE'
          };
          return {
            ...prev,
            [roleCode]: [...currentList, newAuth]
          };
        }
      });
      return;
    }

    try {
      if (isAllowed) {
        const res = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/authorizations/${currentAuthId}`, {
          method: 'DELETE'
        });
        if (res.ok) {
          fetchRolesAndAuths();
        }
      } else {
        const res = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles/${roleCode}/authorizations`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            functionCode: `MENU:${href}`,
            accessType: 'READ',
            dataScope: 'GLOBAL'
          })
        });
        if (res.ok) {
          fetchRolesAndAuths();
        }
      }
    } catch {
      // 로컬 시뮬레이션 폴백
      setAuthorizations(prev => {
        const currentList = prev[roleCode] || [];
        return {
          ...prev,
          [roleCode]: isAllowed 
            ? currentList.filter(a => a.functionCode !== `MENU:${href}`)
            : [...currentList, { id: Date.now(), roleCode, functionCode: `MENU:${href}`, accessType: 'READ_WRITE' }]
        };
      });
    }
  };

  return (
    <div className="space-y-8 p-2 max-w-7xl mx-auto">
      <PageHeader
        title="메뉴 권한 관리 (Governance 통제)"
        description="시스템 역할별로 접근할 수 있는 메뉴 통제 정책을 관리합니다. 백엔드 미구동 시에도 로컬 시뮬레이션을 지원합니다."
        breadcrumbs={[
          { label: '시스템관리' },
          { label: '메뉴 권한 관리' }
        ]}
        icon={Layers}
        actions={
          <div className="flex items-center gap-3">
            <span className={`text-xs font-bold px-3 py-1.5 rounded-full border ${isLiveConnected ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-blue-50 text-[#4262ff] border-blue-200'}`}>
              {isLiveConnected ? '● 백엔드 Governance 연동됨' : '● 로컬 Mock 권한 시뮬레이션'}
            </span>
            <button 
              onClick={fetchRolesAndAuths}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-[#eaedf4] hover:border-[#4262ff]/40 text-[#545b69] text-xs font-bold rounded-xl transition-all shadow-xs"
            >
              <RotateCcw size={13} className={loading ? 'animate-spin' : ''} />
              <span>새로고침</span>
            </button>
          </div>
        }
      />

      {/* Grid Container */}
      <div className="bg-white border border-[#eaedf4] rounded-2xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-[#f7f8fb] border-b border-[#eaedf4]">
                <th className="py-4 px-6 text-xs font-black text-[#17191e] uppercase tracking-wider min-w-[220px]">
                  메뉴 카테고리 / 기능
                </th>
                {roles.map(role => (
                  <th key={role.roleCode} className="py-4 px-4 text-xs font-black text-center text-[#17191e] border-l border-[#eaedf4] min-w-[140px]">
                    <div>{role.roleName}</div>
                    <div className="text-[10px] text-[#8c94a4] font-mono font-medium">{role.roleCode}</div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#eaedf4] text-xs">
              {allMenus.map((group, groupIdx) => (
                <React.Fragment key={groupIdx}>
                  {/* Category Subheader */}
                  <tr className="bg-[#f7f8fb]/40 font-bold text-[#4262ff]">
                    <td colSpan={roles.length + 1} className="py-2.5 px-6 uppercase tracking-wider text-[11px] flex items-center gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#4262ff]" />
                      <span>{group.category} / {group.group}</span>
                    </td>
                  </tr>

                  {/* Menu Items */}
                  {group.items.map((item, itemIdx) => (
                    <tr key={itemIdx} className="hover:bg-blue-50/30 transition-colors">
                      <td className="py-3 px-6 text-[#17191e] font-semibold pl-10">
                        <div className="flex items-center gap-2">
                          <span>{item.label}</span>
                          <span className="text-[10px] text-[#8c94a4] font-mono">({item.href})</span>
                        </div>
                      </td>

                      {roles.map(role => {
                        const allowed = hasPermission(role.roleCode, item.href);
                        const isSystemAdmin = role.roleCode === 'SYSTEM_ADMIN';

                        return (
                          <td key={role.roleCode} className="py-2.5 px-4 text-center border-l border-[#eaedf4]">
                            <button
                              onClick={() => togglePermission(role.roleCode, item.href)}
                              disabled={isSystemAdmin}
                              className={`w-7 h-7 rounded-lg inline-flex items-center justify-center transition-all ${
                                isSystemAdmin
                                  ? 'bg-slate-100 text-[#8c94a4] cursor-not-allowed'
                                  : allowed
                                  ? 'bg-[#4262ff] text-white shadow-xs hover:bg-[#3452e6]'
                                  : 'bg-[#f7f8fb] text-[#8c94a4] hover:bg-slate-200 border border-[#eaedf4]'
                              }`}
                              title={isSystemAdmin ? 'SYSTEM_ADMIN 항상 허용' : allowed ? '권한 회수' : '권한 부여'}
                            >
                              {allowed ? <Check size={14} strokeWidth={3} /> : <X size={14} />}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
