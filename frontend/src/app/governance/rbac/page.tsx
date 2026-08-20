"use client";

import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Check, 
  Edit3, 
  Lock, 
  UserCheck, 
  Save, 
  Sparkles
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface RoleDefinition {
  code: string;
  name: string;
  description: string;
  memberCount: number;
  scope: 'System' | 'Domain' | 'Dept';
  permissions: string[];
}

const initialRoles: RoleDefinition[] = [
  {
    code: 'SYSTEM_ADMIN',
    name: '시스템 최고 관리자',
    description: '전사 인프라, 보안 설정, 메뉴 권한 및 감사 로그에 대한 전체 통제 권한',
    memberCount: 2,
    scope: 'System',
    permissions: ['USER_MANAGE', 'ROLE_MANAGE', 'AUDIT_VIEW', 'DB_CONFIG', 'ALL_SYSTEM']
  },
  {
    code: 'ACCOUNTING_ADMIN',
    name: '회계관리자',
    description: '회계 전표 입력, 승인/반려, 월말 결산 및 재무제표 작성 총괄 권한',
    memberCount: 8,
    scope: 'Domain',
    permissions: ['JOURNAL_CREATE', 'JOURNAL_APPROVE', 'CLOSING_EXEC', 'REPORT_VIEW']
  },
  {
    code: 'RISK_MANAGER',
    name: '리스크관리자',
    description: 'IFRS9 ECL 모형 파라미터(PD/LGD/EAD) 및 손상 산출 통제 권한',
    memberCount: 4,
    scope: 'Domain',
    permissions: ['ECL_PARAM_EDIT', 'STRESS_TEST_RUN', 'RISK_REPORT_VIEW']
  },
  {
    code: 'AUDITOR',
    name: '내부감사역',
    description: '전사 재무 데이터, 승인 이력 및 감사 로그에 대한 전천후 조회 권한',
    memberCount: 3,
    scope: 'System',
    permissions: ['AUDIT_VIEW', 'JOURNAL_VIEW', 'REPORT_VIEW', 'EXPORT_DATA']
  }
];

const availablePermissions = [
  { id: 'JOURNAL_CREATE', label: '전표 작성 및 수정', category: '회계 (Accounting)' },
  { id: 'JOURNAL_APPROVE', label: '전표 승인 및 반려', category: '회계 (Accounting)' },
  { id: 'CLOSING_EXEC', label: '월말/기말 결산 실행', category: '회계 (Accounting)' },
  { id: 'ECL_PARAM_EDIT', label: 'IFRS9 파라미터 변경', category: '리스크 (Risk)' },
  { id: 'STRESS_TEST_RUN', label: '스트레스 테스트 시뮬레이션', category: '리스크 (Risk)' },
  { id: 'USER_MANAGE', label: '사용자 계정 생성/잠금', category: '보안 (Security)' },
  { id: 'ROLE_MANAGE', label: '역할 및 권한 매트릭스 수정', category: '보안 (Security)' },
  { id: 'AUDIT_VIEW', label: '감사 로그 및 차적 조회', category: '감사 (Audit)' },
];

export default function RbacPage() {
  const [roles, setRoles] = useState<RoleDefinition[]>(initialRoles);
  const [newCode, setNewCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newScope, setNewScope] = useState<'System' | 'Domain' | 'Dept'>('Domain');
  const [selectedPerms, setSelectedPerms] = useState<string[]>(['JOURNAL_CREATE']);
  const [successMsg, setSuccessMsg] = useState('');

  const handleTogglePerm = (id: string) => {
    setSelectedPerms(prev => 
      prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]
    );
  };

  const handleAddRole = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCode || !newName) return;

    const newRole: RoleDefinition = {
      code: newCode.toUpperCase().replace(/\s+/g, '_'),
      name: newName,
      description: newDesc || '신규 등록된 보안 역할입니다.',
      memberCount: 0,
      scope: newScope,
      permissions: selectedPerms
    };

    setRoles([...roles, newRole]);
    setNewCode('');
    setNewName('');
    setNewDesc('');
    setSelectedPerms(['JOURNAL_CREATE']);
    setSuccessMsg(`신규 역할 [${newRole.code}]이 성공적으로 정의되었습니다.`);
    setTimeout(() => setSuccessMsg(''), 4000);
  };

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="역할/권한 매트릭스"
        description="접근 통제 보안 정책 정의"
        breadcrumbs={[
          { label: 'Governance', href: '/governance/rbac' },
          { label: '역할/권한 매트릭스' }
        ]}
        icon={ShieldCheck}
      />

      {successMsg && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-bold text-sm flex items-center gap-2">
          <Check size={18} /> {successMsg}
        </div>
      )}

      {/* Existing Roles Cards Grid */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-black text-white flex items-center gap-2">
            <Lock size={18} className="text-blue-400" /> 현재 정의된 보안 역할 목록 ({roles.length})
          </h3>
          <span className="text-xs text-slate-400">전사 RBAC 통제 정책에 동기화됨</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {roles.map((role) => (
            <div key={role.code} className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-4 relative overflow-hidden group hover:border-white/10 transition-all">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                      {role.code}
                    </span>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/5 text-slate-400 border border-white/5">
                      Scope: {role.scope}
                    </span>
                  </div>
                  <h4 className="text-xl font-black text-white mt-2 tracking-tight">{role.name}</h4>
                </div>
                <div className="flex items-center gap-1">
                  <button className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition-colors">
                    <Edit3 size={16} />
                  </button>
                </div>
              </div>

              <p className="text-xs text-slate-400 leading-relaxed font-medium">
                {role.description}
              </p>

              <div className="space-y-2 pt-2 border-t border-white/5">
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">기초 보유 권한 (Base Permissions)</span>
                <div className="flex flex-wrap gap-1.5">
                  {role.permissions.map(perm => (
                    <span key={perm} className="px-2.5 py-1 rounded-lg text-xs font-mono bg-white/5 text-slate-300 border border-white/5">
                      {perm}
                    </span>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-between pt-2 text-xs text-slate-400">
                <span className="flex items-center gap-1">
                  <UserCheck size={14} className="text-emerald-400" /> 할당된 임직원 수: <strong className="text-white">{role.memberCount}명</strong>
                </span>
                <StatusBadge status="활성 정책" variant="success" />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Define New Role & Assign Permissions Form */}
      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6">
        <div className="flex items-center gap-3 border-b border-white/10 pb-4">
          <div className="w-10 h-10 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
            <Sparkles size={20} />
          </div>
          <div>
            <h3 className="text-lg font-black text-white">신규 역할 정의 및 권한 할당</h3>
            <p className="text-xs text-slate-400">새로운 직무나 보안 등급에 맞춰 역할 및 기본 권한(Base Permissions)을 설정합니다.</p>
          </div>
        </div>

        <form onSubmit={handleAddRole} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase mb-2">역할 코드 (Role Code)</label>
              <input
                type="text"
                placeholder="예: TAX_SPECIALIST"
                value={newCode}
                onChange={(e) => setNewCode(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl py-2.5 px-4 text-white text-sm font-mono focus:outline-none focus:border-blue-500/50"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase mb-2">역할명 (Role Name)</label>
              <input
                type="text"
                placeholder="예: 세무관리 전문가"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl py-2.5 px-4 text-white text-sm focus:outline-none focus:border-blue-500/50"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase mb-2">적용 범위 (Scope)</label>
              <select
                value={newScope}
                onChange={(e) => setNewScope(e.target.value as 'System' | 'Domain' | 'Dept')}
                className="w-full bg-slate-900 border border-white/10 rounded-2xl py-2.5 px-4 text-white text-sm focus:outline-none focus:border-blue-500/50"
              >
                <option value="Domain">Domain (도메인 전체)</option>
                <option value="System">System (시스템 전체)</option>
                <option value="Dept">Dept (소속 부서 한정)</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase mb-2">역할 설명</label>
            <input
              type="text"
              placeholder="역할의 목적 및 접근 허용 범위를 명시하세요."
              value={newDesc}
              onChange={(e) => setNewDesc(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-2xl py-2.5 px-4 text-white text-sm focus:outline-none focus:border-blue-500/50"
            />
          </div>

          {/* Base Permissions Checkbox Selector */}
          <div className="space-y-3 pt-2">
            <label className="block text-xs font-bold text-slate-400 uppercase">기초 보유 권한 선택 (Base Permissions)</label>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
              {availablePermissions.map((perm) => {
                const isChecked = selectedPerms.includes(perm.id);
                return (
                  <div
                    key={perm.id}
                    onClick={() => handleTogglePerm(perm.id)}
                    className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-start gap-3 ${
                      isChecked
                        ? 'bg-blue-600/20 border-blue-500/40 text-white'
                        : 'bg-white/5 border-white/5 text-slate-400 hover:bg-white/10 hover:text-slate-200'
                    }`}
                  >
                    <div className={`w-5 h-5 rounded-md flex items-center justify-center shrink-0 mt-0.5 transition-all ${
                      isChecked ? 'bg-blue-600 text-white' : 'bg-white/10 text-transparent border border-white/10'
                    }`}>
                      <Check size={14} />
                    </div>
                    <div>
                      <p className="text-xs font-bold leading-tight">{perm.label}</p>
                      <p className="text-[10px] font-mono text-slate-400 mt-1">{perm.id}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex justify-end pt-4 border-t border-white/10">
            <button
              type="submit"
              className="px-6 py-3 bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm rounded-2xl shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Save size={16} /> 신규 역할 저장 및 보안 정책 반영
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
