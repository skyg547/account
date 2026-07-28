"use client";

import React, { useState, useEffect } from 'react';
import { 
  Lock, Check, X, RotateCcw, ShieldCheck, Sliders, Layers, FileText, Settings, PieChart, BookOpen, AlertCircle
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { allMenus } from '@/components/layout/menus';

const GOVERNANCE_API_BASE_URL = process.env.NEXT_PUBLIC_GOVERNANCE_API_URL || 'http://localhost:8083';

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

/**
 * 🐣 [초보자를 위한 가이드: 시스템 메뉴 권한 관리 매트릭스]
 * 
 * 이 페이지는 백엔드의 `governance` 모듈(내부회계통제 인프라)과 통신하여,
 * 프론트엔드의 각 메뉴에 어떤 역할(Role)이 접근 가능한지 실시간으로 조회하고 제어하는 곳입니다.
 * 
 * - `SystemRole`: 시스템에 등록된 역할 (예: ACCOUNTING_ADMIN, RISK_MANAGER)
 * - `Authorization`: 특정 역할이 특정 기능(functionCode)에 대해 가지는 권한
 * - `functionCode`: 프론트엔드에서는 `MENU:/system/menus` 처럼 "MENU:경로" 형태로 약속하여 사용합니다.
 */
export default function SystemMenusPage() {
  const [roles, setRoles] = useState<SystemRole[]>([]);
  const [authorizations, setAuthorizations] = useState<Record<string, Authorization[]>>({}); // roleCode -> auths
  const [loading, setLoading] = useState(true);

  const fetchRolesAndAuths = async () => {
    setLoading(true);
    try {
      // 1. Fetch Roles
      const roleRes = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles`);
      if (!roleRes.ok) throw new Error('Failed to fetch roles');
      const roleData: SystemRole[] = await roleRes.json();
      
      setRoles(roleData);

      // 2. Fetch Auths for each role
      const authMap: Record<string, Authorization[]> = {};
      for (const role of roleData) {
        if (role.roleCode === 'SYSTEM_ADMIN') continue; // SYSTEM_ADMIN usually bypasses
        const authRes = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles/${role.roleCode}/authorizations`);
        if (authRes.ok) {
          authMap[role.roleCode] = await authRes.json();
        }
      }
      setAuthorizations(authMap);
    } catch (e) {
      console.error('Failed to fetch roles/auths', e);
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
    return roleAuths.some(auth => auth.functionCode === `MENU:${href}`);
  };

  const getAuthorizationId = (roleCode: string, href: string) => {
    // 💡 [초보자용 팁] 특정 역할이 특정 메뉴에 대한 권한 레코드 ID를 가지고 있는지 찾습니다.
    // 권한을 취소(DELETE)할 때 이 ID가 필요하기 때문입니다.
    const roleAuths = authorizations[roleCode] || [];
    const auth = roleAuths.find(a => a.functionCode === `MENU:${href}`);
    return auth ? auth.id : null;
  };

  const togglePermission = async (roleCode: string, href: string) => {
    if (roleCode === 'SYSTEM_ADMIN') {
      alert("SYSTEM_ADMIN 권한은 변경할 수 없습니다.");
      return;
    }

    const currentAuthId = getAuthorizationId(roleCode, href);
    const isAllowed = !!currentAuthId;

    try {
      if (isAllowed) {
        // Revoke (DELETE)
        const res = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/authorizations/${currentAuthId}`, {
          method: 'DELETE'
        });
        if (res.ok) {
           if (res.status === 202) {
             alert('권한 회수 승인(MasterApproval)이 요청되었습니다. 통제 정책상 승인자가 결재해야 최종 반영됩니다.');
           } else {
             fetchRolesAndAuths(); // Refresh
           }
        }
      } else {
        // Grant (POST)
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
        } else {
           alert('권한 부여에 실패했습니다.');
        }
      }
    } catch (e) {
       console.error(e);
       alert('API 요청 실패: 서버가 켜져 있는지 확인하세요.');
    }
  };

  return (
    <div className="space-y-8 p-2 max-w-7xl mx-auto">
      <PageHeader
        title="메뉴 권한 관리 (Governance 연동)"
        description="시스템 역할별로 접근할 수 있는 메뉴 통제 정책을 관리합니다. 거버넌스 승인 통제가 자동 적용됩니다."
        breadcrumbs={[
          { label: 'System', href: '/system/menus' },
          { label: '메뉴 권한 관리' }
        ]}
        icon={Sliders}
        actions={
          <button
            onClick={fetchRolesAndAuths}
            className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 text-sm font-bold transition-all flex items-center gap-2"
          >
            <RotateCcw size={16} /> 매트릭스 새로고침
          </button>
        }
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md shadow-2xl">
           <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">등록된 메뉴 화면</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">{allMenus.reduce((acc, g) => acc + g.items.length, 0)}개</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Layers size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md shadow-2xl">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">조회된 시스템 Role</p>
              <h3 className="text-3xl font-black text-purple-400 mt-2 tracking-tight">{roles.length}개</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Lock size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md shadow-2xl">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">접근 통제 엔진</p>
              <h3 className="text-xl font-black text-emerald-400 mt-2 tracking-tight">Governance</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <ShieldCheck size={22} />
            </div>
          </div>
        </div>
      </div>

      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md shadow-2xl space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white">역할별 메뉴 접근 권한 매트릭스</h3>
            <p className="text-xs text-slate-400 mt-1">셀을 클릭하여 권한을 즉시 토글할 수 있습니다. 권한 회수 시 Maker-Checker 결재 정책이 자동 적용됩니다.</p>
          </div>
        </div>

        {loading ? (
          <div className="py-12 text-center text-slate-400 animate-pulse">Governance 서버에서 실시간 권한 매트릭스를 불러오는 중...</div>
        ) : (
          <div className="overflow-x-auto custom-scrollbar">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-xs font-black text-slate-400 uppercase bg-black/20">
                  <th className="py-4 px-4 w-72 whitespace-nowrap">메뉴 카테고리 / 화면명</th>
                  {roles.map(role => (
                    <th key={role.roleCode} className="py-4 px-4 text-center">
                      <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold border ${role.roleCode === 'SYSTEM_ADMIN' ? 'bg-rose-500/10 border-rose-500/30 text-rose-400' : 'bg-slate-800 border-white/10 text-slate-300'}`}>
                        {role.roleName || role.roleCode}
                      </span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-sm">
                {allMenus.map((group, groupIdx) => (
                  <React.Fragment key={group.group}>
                    <tr className="bg-white/[0.02]">
                      <td colSpan={roles.length + 1} className="py-3 px-4 text-xs font-black text-blue-400 uppercase tracking-widest bg-blue-900/10">
                        {group.group}
                      </td>
                    </tr>
                    {group.items.map((item, itemIdx) => (
                      <tr key={item.href} className="hover:bg-white/5 transition-colors">
                        <td className="py-3.5 px-4 pl-8">
                          <div className="font-bold text-white text-sm flex items-center gap-2">
                            {item.label}
                          </div>
                          <div className="text-xs text-slate-500 font-mono mt-0.5">{item.href}</div>
                        </td>
                        {roles.map(role => {
                          const isAllowed = hasPermission(role.roleCode, item.href);
                          const isAdmin = role.roleCode === 'SYSTEM_ADMIN';
                          return (
                            <td key={role.roleCode} className="py-3.5 px-4 text-center">
                              <button
                                onClick={() => togglePermission(role.roleCode, item.href)}
                                disabled={isAdmin}
                                className={`w-9 h-9 rounded-xl inline-flex items-center justify-center transition-all ${
                                  isAdmin ? 'opacity-50 cursor-not-allowed ' : ''
                                }${
                                  isAllowed
                                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/30 shadow-lg shadow-emerald-500/10'
                                    : 'bg-rose-500/10 text-rose-400 border border-rose-500/20 hover:bg-rose-500/20'
                                }`}
                              >
                                {isAllowed ? <Check size={18} /> : <X size={18} />}
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
        )}
      </div>
    </div>
  );
}
