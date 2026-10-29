"use client";

import { Fragment, useEffect, useRef, useState } from 'react';
import { Check, X, RotateCcw, Layers } from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import { allMenus } from '@/components/layout/menus';

interface SystemRole {
  roleCode: string;
  roleName: string;
}

interface Authorization {
  id: number;
  roleCode: string;
  functionCode: string;
  accessType: 'READ' | 'WRITE' | 'EXECUTE';
}

type AuthorizationMap = Record<string, Authorization[]>;
type LoadedPermissions = { roles: SystemRole[]; authorizations: AuthorizationMap };
type LoadStatus = 'loading' | 'ready' | 'unavailable';

function isRepresentableRoleCode(value: unknown): value is string {
  if (typeof value !== 'string' || value.length === 0 || value.length > 50 ||
    value !== value.trim() || value === '.' || value === '..' ||
    /[\\/\u0000-\u001f\u007f]/.test(value)) return false;
  try {
    encodeURIComponent(value);
    return true;
  } catch {
    return false; // A lone surrogate cannot form a URL path segment.
  }
}

function isRoleList(value: unknown): value is SystemRole[] {
  if (!Array.isArray(value)) return false;
  const codes = new Set<string>();
  return value.every(role => {
    if (role === null || typeof role !== 'object' ||
      !isRepresentableRoleCode(role.roleCode) ||
      typeof role.roleName !== 'string' || role.roleName.trim().length === 0 || role.roleName.length > 100 ||
      codes.has(role.roleCode)) return false;
    codes.add(role.roleCode);
    return true;
  });
}

function isAuthorizationList(value: unknown, roleCode: string): value is Authorization[] {
  return Array.isArray(value) && value.every(auth =>
    auth !== null && typeof auth === 'object' &&
    Number.isSafeInteger(auth.id) && auth.id > 0 &&
    auth.roleCode === roleCode &&
    typeof auth.functionCode === 'string' && auth.functionCode.length > 0 &&
    ['READ', 'WRITE', 'EXECUTE'].includes(auth.accessType)
  );
}

function hasMenuPermission(authorizations: AuthorizationMap, roleCode: string, href: string): boolean {
  if (roleCode === 'SYSTEM_ADMIN') return true;
  return (authorizations[roleCode] ?? []).some(auth =>
    auth.accessType === 'READ' &&
    (auth.functionCode === `MENU:${href}` || auth.functionCode === 'MENU:*')
  );
}

export default function SystemMenusPage() {
  const [roles, setRoles] = useState<SystemRole[]>([]);
  const [authorizations, setAuthorizations] = useState<AuthorizationMap>({});
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const requestVersion = useRef(0);
  const activeRead = useRef<AbortController | null>(null);
  const activeWrite = useRef<AbortController | null>(null);
  const writeInFlight = useRef(false);
  const mounted = useRef(true);

  const fetchRolesAndAuths = async (): Promise<LoadedPermissions | null> => {
    const version = ++requestVersion.current;
    activeRead.current?.abort();
    const controller = new AbortController();
    activeRead.current = controller;
    setStatus('loading');
    setRoles([]);
    setAuthorizations({});
    setError('');
    setNotice('');
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    try {
      const roleRes = await fetch('/api/audit/roles', {
        signal: controller.signal, credentials: 'same-origin', cache: 'no-store',
      });
      if (!roleRes.ok) throw new Error('Role request failed');
      const roleData: unknown = await roleRes.json();
      if (!isRoleList(roleData)) throw new Error('Invalid roles');

      // Commit the table only after every non-admin role's grants pass validation.
      const entries = await Promise.all(roleData.filter(role => role.roleCode !== 'SYSTEM_ADMIN').map(async role => {
        const authRes = await fetch(`/api/audit/roles/${encodeURIComponent(role.roleCode)}/authorizations`, {
          signal: controller.signal, credentials: 'same-origin', cache: 'no-store',
        });
        if (!authRes.ok) throw new Error('Authorization request failed');
        const data: unknown = await authRes.json();
        if (!isAuthorizationList(data, role.roleCode)) throw new Error('Invalid authorizations');
        return [role.roleCode, data] as const;
      }));
      const loaded = { roles: roleData, authorizations: Object.fromEntries(entries) as AuthorizationMap };
      if (mounted.current && version === requestVersion.current) {
        setRoles(loaded.roles);
        setAuthorizations(loaded.authorizations);
        setStatus('ready');
      }
      return mounted.current && version === requestVersion.current ? loaded : null;
    } catch {
      controller.abort(); // Stop sibling role requests when any response makes the table unusable.
      if (mounted.current && version === requestVersion.current) {
        setRoles([]);
        setAuthorizations({});
        setStatus('unavailable');
        setError('메뉴 권한을 불러오지 못했습니다. 인증과 서버 연결 상태를 확인해 주세요.');
      }
      return null;
    } finally {
      clearTimeout(timeoutId);
      if (activeRead.current === controller) activeRead.current = null;
    }
  };

  useEffect(() => {
    mounted.current = true;
    void fetchRolesAndAuths();
    return () => {
      mounted.current = false;
      activeRead.current?.abort();
      activeWrite.current?.abort();
    };
  }, []);

  const togglePermission = async (roleCode: string, href: string) => {
    if (status !== 'ready' || writeInFlight.current || roleCode === 'SYSTEM_ADMIN') return;
    const roleAuths = authorizations[roleCode];
    if (!roleAuths) return;
    const code = `MENU:${href}`;
    const wildcard = roleAuths.some(auth => auth.functionCode === 'MENU:*' && auth.accessType === 'READ');
    if (wildcard) return; // A wildcard grant cannot be revoked through an individual menu cell.
    const exact = roleAuths.find(auth => auth.functionCode === code && auth.accessType === 'READ');
    const shouldAllow = !exact;
    const controller = new AbortController();
    activeWrite.current = controller;
    writeInFlight.current = true;
    ++requestVersion.current;
    activeRead.current?.abort();
    setSaving(true);
    setStatus('loading');
    setRoles([]);
    setAuthorizations({});
    setError('');
    setNotice('');
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    try {
      const res = await fetch(exact
        ? `/api/audit/authorizations/${exact.id}`
        : `/api/audit/roles/${encodeURIComponent(roleCode)}/authorizations`, {
        method: exact ? 'DELETE' : 'POST',
        headers: exact ? undefined : { 'Content-Type': 'application/json' },
        body: exact ? undefined : JSON.stringify({ functionCode: code, accessType: 'READ', dataScope: 'GLOBAL' }),
        signal: controller.signal,
        credentials: 'same-origin',
        cache: 'no-store',
      });
      if (!res.ok) throw new Error('Authorization write failed');
      clearTimeout(timeoutId);

      const loaded = await fetchRolesAndAuths();
      if (!loaded) {
        if (mounted.current) setError('변경 요청 후 현재 권한을 확인하지 못했습니다. 승인 상태를 확인해 주세요.');
        return;
      }
      // The legacy API can queue approval (including HTTP 202); only the refetch is authoritative.
      if (hasMenuPermission(loaded.authorizations, roleCode, href) === shouldAllow) {
        if (mounted.current) setNotice('현재 권한 반영을 확인했습니다.');
      } else if (mounted.current) {
        setError('변경 요청이 접수되었지만 권한 반영은 확인되지 않았습니다. 승인 상태를 확인해 주세요.');
      }
    } catch {
      if (mounted.current) {
        setRoles([]);
        setAuthorizations({});
        setStatus('unavailable');
        setError('변경 요청 결과를 확인하지 못했습니다. 관리자에게 요청 상태를 확인해 주세요.');
      }
    } finally {
      clearTimeout(timeoutId);
      if (activeWrite.current === controller) activeWrite.current = null;
      writeInFlight.current = false;
      if (mounted.current) setSaving(false);
    }
  };

  return (
    <div className="space-y-8 p-2 max-w-7xl mx-auto">
      <PageHeader
        title="메뉴 권한 관리 (Governance 통제)"
        description="시스템 역할별 메뉴 권한을 조회합니다. 변경 요청은 승인 후 반영될 수 있습니다."
        breadcrumbs={[{ label: '시스템관리' }, { label: '메뉴 권한 관리' }]}
        icon={Layers}
        actions={
          <div className="flex items-center gap-3">
            <span className={`text-xs font-bold px-3 py-1.5 rounded-full border ${status === 'ready' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-50 text-slate-600 border-slate-200'}`}>
              {status === 'ready' ? '● 권한 조회 완료' : status === 'loading' ? '● 권한 확인 중' : '● 권한 조회 불가'}
            </span>
            <button
              onClick={() => { void fetchRolesAndAuths(); }}
              disabled={saving || status === 'loading'}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-[#eaedf4] hover:border-[#4262ff]/40 text-[#545b69] text-xs font-bold rounded-xl transition-all shadow-xs disabled:opacity-50"
            >
              <RotateCcw size={13} className={status === 'loading' ? 'animate-spin' : ''} />
              <span>새로고침</span>
            </button>
          </div>
        }
      />

      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
      {notice && <p role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">{notice}</p>}
      {status === 'ready' && roles.length === 0 && <p role="status" className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">등록된 역할이 없습니다.</p>}

      <div className="bg-white border border-[#eaedf4] rounded-2xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-[#f7f8fb] border-b border-[#eaedf4]">
                <th className="py-4 px-6 text-xs font-black text-[#17191e] uppercase tracking-wider min-w-[220px]">메뉴 카테고리 / 기능</th>
                {roles.map(role => (
                  <th key={role.roleCode} className="py-4 px-4 text-xs font-black text-center text-[#17191e] border-l border-[#eaedf4] min-w-[140px]">
                    <div>{role.roleName}</div>
                    <div className="text-[10px] text-[#8c94a4] font-mono font-medium">{role.roleCode}</div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#eaedf4] text-xs">
              {status === 'ready' && allMenus.map((group, groupIdx) => (
                <Fragment key={groupIdx}>
                  <tr className="bg-[#f7f8fb]/40 font-bold text-[#4262ff]">
                    <td colSpan={roles.length + 1} className="py-2.5 px-6 uppercase tracking-wider text-[11px] flex items-center gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#4262ff]" />
                      <span>{group.category} / {group.group}</span>
                    </td>
                  </tr>
                  {group.items.map((item, itemIdx) => (
                    <tr key={itemIdx} className="hover:bg-blue-50/30 transition-colors">
                      <td className="py-3 px-6 text-[#17191e] font-semibold pl-10">
                        <div className="flex items-center gap-2">
                          <span>{item.label}</span>
                          <span className="text-[10px] text-[#8c94a4] font-mono">({item.href})</span>
                        </div>
                      </td>
                      {roles.map(role => {
                        const allowed = hasMenuPermission(authorizations, role.roleCode, item.href);
                        const wildcard = (authorizations[role.roleCode] ?? []).some(auth => auth.functionCode === 'MENU:*' && auth.accessType === 'READ');
                        const isSystemAdmin = role.roleCode === 'SYSTEM_ADMIN';
                        return (
                          <td key={role.roleCode} className="py-2.5 px-4 text-center border-l border-[#eaedf4]">
                            <button
                              onClick={() => { void togglePermission(role.roleCode, item.href); }}
                              disabled={saving || isSystemAdmin || wildcard}
                              className={`w-7 h-7 rounded-lg inline-flex items-center justify-center transition-all ${
                                isSystemAdmin || wildcard
                                  ? 'bg-slate-100 text-[#8c94a4] cursor-not-allowed'
                                  : allowed
                                  ? 'bg-[#4262ff] text-white shadow-xs hover:bg-[#3452e6]'
                                  : 'bg-[#f7f8fb] text-[#8c94a4] hover:bg-slate-200 border border-[#eaedf4]'
                              }`}
                              title={isSystemAdmin ? 'SYSTEM_ADMIN UI 기본 표시' : wildcard ? '전체 메뉴 권한으로 허용됨' : allowed ? '권한 회수 요청' : '권한 부여 요청'}
                            >
                              {allowed ? <Check size={14} strokeWidth={3} /> : <X size={14} />}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
