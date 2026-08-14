"use client";

import React, { useState } from 'react';
import { 
  Users, 
  UserCheck, 
  Shield, 
  Search, 
  Filter, 
  Plus, 
  RefreshCw, 
  MoreVertical, 
  Building, 
  Clock, 
  Key,
  Lock
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { mockUsers, UserDto } from '@/mocks/system';

const roleLabels: Record<string, { label: string; color: string }> = {
  SYSTEM_ADMIN: { label: '시스템관리자', color: 'bg-rose-500/10 text-rose-400 border-rose-500/20' },
  ACCOUNTING_ADMIN: { label: '회계관리자', color: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  RISK_MANAGER: { label: '리스크관리자', color: 'bg-purple-500/10 text-purple-400 border-purple-500/20' },
  AUDITOR: { label: '감사역', color: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  USER: { label: '일반사용자', color: 'bg-slate-500/10 text-slate-400 border-slate-500/20' },
};

export default function SystemUsersPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [userList, setUserList] = useState<UserDto[]>(mockUsers);

  const filteredUsers = userList.filter(user => {
    const matchesSearch = 
      user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.userId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.departmentName.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || user.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const getStatusVariant = (status: UserDto['status']) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'LOCKED': return 'warning';
      case 'INACTIVE': return 'error';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: UserDto['status']) => {
    switch (status) {
      case 'ACTIVE': return '정상 (Active)';
      case 'LOCKED': return '잠김 (Locked)';
      case 'INACTIVE': return '비활성 (Inactive)';
      default: return status;
    }
  };

  const activeCount = userList.filter(u => u.status === 'ACTIVE').length;
  const lockedCount = userList.filter(u => u.status === 'LOCKED').length;

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="사용자 관리"
        description="시스템 접근 사용자 및 권한 통제"
        breadcrumbs={[
          { label: 'System', href: '/system/users' },
          { label: '사용자 관리' }
        ]}
        icon={Users}
        actions={
          <div className="flex items-center gap-3">
            <button 
              onClick={() => setUserList([...mockUsers])}
              className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 text-sm font-bold transition-all flex items-center gap-2"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
            <button className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2">
              <Plus size={16} /> 사용자 등록
            </button>
          </div>
        }
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">전체 사용자</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">{userList.length}명</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Users size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">정상 계정</p>
              <h3 className="text-3xl font-black text-emerald-400 mt-2 tracking-tight">{activeCount}명</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <UserCheck size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">잠금 계정</p>
              <h3 className="text-3xl font-black text-amber-400 mt-2 tracking-tight">{lockedCount}명</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Lock size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">보안 정책 상태</p>
              <h3 className="text-xl font-black text-blue-400 mt-2 tracking-tight">강력 (Enforced)</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Shield size={22} />
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Table Container */}
      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6">
        {/* Controls */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
            <input
              type="text"
              placeholder="사용자명, ID, 부서 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-blue-500/50 transition-all"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto pb-2 sm:pb-0">
            <span className="text-xs font-bold text-slate-400 mr-2 flex items-center gap-1">
              <Filter size={14} /> 상태:
            </span>
            {['ALL', 'ACTIVE', 'LOCKED', 'INACTIVE'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                  statusFilter === st
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white'
                }`}
              >
                {st === 'ALL' ? '전체' : st}
              </button>
            ))}
          </div>
        </div>

        {/* User Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-xs font-black text-slate-400 uppercase tracking-wider">
                <th className="py-4 px-4">사용자 ID / 이름</th>
                <th className="py-4 px-4">부서 (Cost Center)</th>
                <th className="py-4 px-4">할당된 역할 (Roles)</th>
                <th className="py-4 px-4">상태</th>
                <th className="py-4 px-4">최종 접속 일시</th>
                <th className="py-4 px-4 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-sm">
              {filteredUsers.map((user) => (
                <tr key={user.userId} className="hover:bg-white/5 transition-colors group">
                  <td className="py-4 px-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600/30 to-purple-600/30 border border-white/10 flex items-center justify-center text-white font-bold">
                        {user.name.charAt(0)}
                      </div>
                      <div>
                        <div className="font-bold text-white group-hover:text-blue-400 transition-colors">
                          {user.name}
                        </div>
                        <div className="text-xs text-slate-400 flex items-center gap-1">
                          <Key size={12} className="text-slate-500" /> {user.userId}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 px-4">
                    <div className="flex items-center gap-2 text-slate-300">
                      <Building size={14} className="text-slate-500" />
                      <span className="font-medium">{user.departmentName}</span>
                      <span className="text-xs text-slate-500 font-mono">({user.departmentId})</span>
                    </div>
                  </td>
                  <td className="py-4 px-4">
                    <div className="flex flex-wrap gap-1.5">
                      {user.roles.map((r) => {
                        const roleInfo = roleLabels[r] || { label: r, color: 'bg-slate-500/10 text-slate-400 border-slate-500/20' };
                        return (
                          <span
                            key={r}
                            className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${roleInfo.color}`}
                          >
                            {roleInfo.label}
                          </span>
                        );
                      })}
                    </div>
                  </td>
                  <td className="py-4 px-4">
                    <StatusBadge status={getStatusLabel(user.status)} variant={getStatusVariant(user.status)} />
                  </td>
                  <td className="py-4 px-4 text-slate-400 font-mono text-xs">
                    <div className="flex items-center gap-1.5">
                      <Clock size={14} className="text-slate-500" />
                      {user.lastLoginAt}
                    </div>
                  </td>
                  <td className="py-4 px-4 text-right">
                    <button className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition-all">
                      <MoreVertical size={18} />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500 font-medium">
                    검색 조건에 일치하는 사용자가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
