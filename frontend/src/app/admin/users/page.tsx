import { useEffect, useState } from 'react';
import { 
  Users, 
  UserPlus, 
  Search, 
  Filter, 
  MoreVertical, 
  Shield, 
  ShieldAlert, 
  ShieldCheck, 
  Mail, 
  Key, 
  Clock,
  Eye,
  Edit2,
  Server,
  History
} from 'lucide-react';
import { useNav, UserRole } from '@/context/NavContext';
import { adminService, UserInfo } from '@/services/adminService';

const roleStyles: Record<string, any> = {
  SYSTEM_ADMIN: { label: '시스템관리자', color: 'text-red-400', bg: 'bg-red-400/10', icon: ShieldCheck },
  ACCOUNTING_ADMIN: { label: '회계관리자', color: 'text-blue-400', bg: 'bg-blue-400/10', icon: ShieldCheck },
  RISK_MANAGER: { label: '리스크관리자', color: 'text-purple-400', bg: 'bg-purple-400/10', icon: ShieldAlert },
  MASTER_MANAGER: { label: '마스터관리자', color: 'text-emerald-400', bg: 'bg-emerald-400/10', icon: Shield },
  AUDITOR: { label: '감사역', color: 'text-amber-400', bg: 'bg-amber-400/10', icon: Eye },
  USER: { label: '일반사용자', color: 'text-slate-400', bg: 'bg-slate-400/10', icon: Users },
};

export default function UserManagementPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const { userRole, setUserRole } = useNav();

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    const data = await adminService.getUsers();
    setUsers(data);
    setLoading(false);
  };

  const handleRoleChange = async (userId: number, role: UserRole) => {
    const success = await adminService.updateUserRole(userId, role);
    if (success) {
      // 내 역할도 변경하여 사이드바 동기화 확인 (시뮬레이션)
      setUserRole(role);
      // 목록 새로고침
      loadUsers();
    }
  };

  const filteredUsers = users.filter(u => 
    u.name.includes(searchTerm) || 
    u.email.includes(searchTerm) || 
    u.dept.includes(searchTerm)
  );

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <ShieldCheck size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">System Security</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            사용자 그룹 및 권한 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            전사 시스템 사용자의 역할(Role) 기반 접근 제어(RBAC) 및 보안 정책을 관리합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-white text-sm font-black transition-all flex items-center gap-2">
            <Filter size={18} className="text-slate-400" /> 필터링
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
            <UserPlus size={18} /> 신규 사용자 초대
          </button>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {[
          { label: '전체 사용자', value: users.length.toString(), sub: 'Active', color: 'blue' },
          { label: '관리자 권한', value: users.filter(u => u.role.includes('ADMIN')).length.toString(), sub: 'System/Domain', color: 'purple' },
          { label: '미승인 요청', value: users.filter(u => u.status === 'PENDING').length.toString(), sub: 'Pending Approval', color: 'amber' },
          { label: '보안 이슈(24h)', value: '0', sub: 'Clean Status', color: 'emerald' },
        ].map((stat, i) => (
          <div key={i} className="p-6 rounded-[2.5rem] bg-white/[0.02] border border-white/5 hover:border-white/10 transition-all group/stat relative overflow-hidden">
             <div className={`absolute top-0 right-0 w-32 h-32 bg-blue-500/5 blur-[50px] rounded-full group-hover/stat:scale-150 transition-transform duration-700`} />
             <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-3">{stat.label}</p>
             <div className="flex items-end gap-3 text-white">
                <span className="text-3xl font-black italic tracking-tighter leading-none">{stat.value}</span>
                <span className="text-[10px] font-bold text-slate-600 mb-1">{stat.sub}</span>
             </div>
          </div>
        ))}
      </div>

      {/* User Table Section */}
      <div className="glass-panel p-8 rounded-[3rem] border border-white/10 relative overflow-hidden">
        <div className="flex items-center justify-between mb-8">
           <div className="relative group/search max-w-md w-full">
              <Search className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within/search:text-blue-500 transition-colors" size={20} />
              <input 
                type="text" 
                placeholder="사용자명, 이메일, 부서 검색..."
                className="w-full bg-slate-950 border border-white/5 focus:border-blue-500/50 rounded-2xl py-4 pl-14 pr-6 text-white text-sm outline-none transition-all placeholder:text-slate-700 font-bold"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
           </div>
           {loading && <div className="text-blue-500 font-black text-xs animate-pulse">Syncing with Central Security...</div>}
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-white/5">
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">사용자 정보</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">소속 및 부서</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">보안 역할 (Role)</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">상태</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">최종 접속</th>
                <th className="pb-6 px-4 text-right"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.02]">
              {filteredUsers.map((user) => {
                const style = roleStyles[user.role] || roleStyles.USER;
                const RoleIcon = style.icon;
                const isCurrentRole = userRole === user.role;
                
                return (
                  <tr key={user.id} className="group/row hover:bg-white/[0.02] transition-colors">
                    <td className="py-6 px-4">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 border border-white/5 flex items-center justify-center text-white font-black text-lg group-hover/row:scale-110 transition-transform">
                           {user.name.charAt(0)}
                        </div>
                        <div className="flex flex-col gap-1">
                          <span className="text-base font-black text-white leading-tight">{user.name}</span>
                          <span className="text-xs text-slate-500 font-medium flex items-center gap-1">
                             <Mail size={12} className="text-slate-600" /> {user.email}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="py-6 px-4">
                       <span className="text-sm font-bold text-slate-400 bg-white/5 px-3 py-1.5 rounded-xl border border-white/5">
                          {user.dept}
                       </span>
                    </td>
                    <td className="py-6 px-4">
                      <button 
                        onClick={() => handleRoleChange(user.id, user.role)}
                        className={`inline-flex items-center gap-2 px-4 py-2 rounded-2xl ${style.bg} ${style.color} border ${isCurrentRole ? 'border-current shadow-[0_0_10px_rgba(0,0,0,0.3)]' : 'border-current/10'} hover:scale-105 transition-all relative group/role`}
                      >
                        <RoleIcon size={14} />
                        <span className="text-xs font-black tracking-tight">{style.label}</span>
                        {isCurrentRole && (
                          <div className="absolute -top-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-slate-900 animate-pulse" />
                        )}
                      </button>
                    </td>
                    <td className="py-6 px-4">
                      <div className="flex items-center gap-2">
                        <div className={`w-2 h-2 rounded-full ${
                          user.status === 'ACTIVE' ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 
                          user.status === 'PENDING' ? 'bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.5)]' : 
                          'bg-slate-700'
                        }`} />
                        <span className={`text-xs font-black ${
                          user.status === 'ACTIVE' ? 'text-emerald-500' : 
                          user.status === 'PENDING' ? 'text-amber-500' : 
                          'text-slate-600'
                        }`}>
                          {user.status}
                        </span>
                      </div>
                    </td>
                    <td className="py-6 px-4">
                       <span className="text-sm font-bold text-slate-500 tracking-tight">{user.lastLogin}</span>
                    </td>
                    <td className="py-6 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                         <button className="p-3 rounded-xl hover:bg-blue-500/10 text-slate-500 hover:text-blue-400 transition-all opacity-0 group-hover/row:opacity-100">
                            <Edit2 size={16} />
                         </button>
                         <button className="p-3 rounded-xl hover:bg-slate-500/10 text-slate-500 hover:text-white transition-all">
                            <MoreVertical size={18} />
                         </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
         <div className="glass-panel p-8 rounded-[3rem] border border-white/10 relative overflow-hidden group/card shadow-2xl shadow-blue-500/5">
            <div className="flex items-center gap-3 mb-6">
               <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center text-white">
                  <Key size={20} />
               </div>
               <h3 className="text-xl font-black text-white italic tracking-tight">비밀번호 및 보안 정책</h3>
            </div>
            <p className="text-slate-500 text-sm mb-6 font-medium">최근 90일 내 비밀번호 변경 강제, 2-Factor 인증(MFA) 활성화 여부 등을 설정합니다.</p>
            <button className="text-blue-400 text-sm font-black hover:underline underline-offset-4 flex items-center gap-2">
               설정 바로가기 <History size={14} />
            </button>
         </div>

         <div className="glass-panel p-8 rounded-[3rem] border border-white/10 relative overflow-hidden group/card shadow-2xl shadow-emerald-500/5">
            <div className="flex items-center gap-3 mb-6">
               <div className="w-10 h-10 rounded-2xl bg-emerald-600 flex items-center justify-center text-white">
                  <ShieldCheck size={20} />
               </div>
               <h3 className="text-xl font-black text-white italic tracking-tight">IP 접속 보안 관리</h3>
            </div>
            <p className="text-slate-500 text-sm mb-6 font-medium">사내망 IP 및 특정 화이트리스트 외 접속을 차단하며, 이상 징후 발생 시 즉각 알림을 발송합니다.</p>
            <button className="text-emerald-400 text-sm font-black hover:underline underline-offset-4 flex items-center gap-2">
               차단 목록 확인 <Server size={14} />
            </button>
         </div>
      </div>
    </div>
  );
}
