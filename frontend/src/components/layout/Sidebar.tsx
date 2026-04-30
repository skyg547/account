"use client";

import React from 'react';
import Link from 'next/link';
import { 
  LayoutDashboard, 
  FileText, 
  BookOpen, 
  Settings, 
  Wallet,
  Activity,
  Plus,
  Users,
  BarChart,
  PieChart,
  Zap,
  DollarSign,
  CreditCard,
  Percent,
  Package,
  Receipt,
  Landmark,
  CalendarDays,
  Network,
  CheckCircle,
  ChevronLeft,
  ChevronRight,
  Layers
} from 'lucide-react';
import { useNav, UserRole } from '@/context/NavContext';

interface MenuItem {
  icon: React.ElementType;
  label: string;
  href: string;
  requiredRoles?: UserRole[];
}

interface MenuGroup {
  category: string;
  group: string;
  requiredRoles?: UserRole[];
  items: MenuItem[];
}

const menuItems: MenuGroup[] = [
  { 
    category: 'DASHBOARD',
    group: '대시보드', 
    items: [
      { icon: LayoutDashboard, label: '통합 대시보드', href: '/' },
    ]
  },
  { 
    category: 'ACCOUNTING',
    group: '핵심 회계 업무', 
    requiredRoles: ['ACCOUNTING_ADMIN', 'USER', 'SYSTEM_ADMIN', 'AUDITOR'],
    items: [
      { icon: FileText, label: '전표 조회', href: '/journal/list' },
      { icon: Plus, label: '전표 입력', href: '/journal/entry', requiredRoles: ['USER', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: BarChart, label: '총계정원장', href: '/journal/ledger/gl' },
      { icon: PieChart, label: '보조원장', href: '/journal/ledger/sl' },
      { icon: Zap, label: '자동 분개 설정', href: '/journal/rules', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: CalendarDays, label: '결산/재무제표', href: '/closing', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN', 'AUDITOR'] },
      { icon: Percent, label: '부가세 신고 관리', href: '/finance/tax', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
    ]
  },
  { 
    category: 'OPERATIONS',
    group: '자금 및 운영', 
    requiredRoles: ['ACCOUNTING_ADMIN', 'USER', 'SYSTEM_ADMIN'],
    items: [
      { icon: DollarSign, label: '매출채권(AR)', href: '/finance/receivable' },
      { icon: CreditCard, label: '매입채무(AP)', href: '/finance/payable' },
      { icon: Wallet, label: '예산 편성/통제', href: '/finance/budget', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: Activity, label: '자금 수지/계획', href: '/finance/cashflow' },
      { icon: Receipt, label: '지출결의 포털', href: '/finance/expense' },
    ]
  },
  { 
    category: 'OPERATIONS',
    group: '자산 및 특수 회계', 
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: Package, label: '고정자산 관리', href: '/finance/assets' },
      { icon: FileText, label: '리스 회계', href: '/finance/lease' },
      { icon: Network, label: '연결/지분법 기초', href: '/finance/consolidation' },
    ]
  },
  { 
    category: 'MASTER',
    group: '기준 정보', 
    requiredRoles: ['MASTER_MANAGER', 'SYSTEM_ADMIN'],
    items: [
      { icon: BookOpen, label: '계정 과목 관리', href: '/master/account' },
      { icon: Users, label: '거래처 관리', href: '/master/partner' },
      { icon: Landmark, label: '귀속 부서 관리', href: '/master/dept' },
      { icon: CheckCircle, label: '기준 정보 승인', href: '/master/approval' },
    ]
  },
  { 
    category: 'ADMIN',
    group: '시스템 관리', 
    requiredRoles: ['SYSTEM_ADMIN', 'ACCOUNTING_ADMIN'],
    items: [
      { icon: Users, label: '사용자 그룹 관리', href: '/admin/users' },
      { icon: Layers, label: '메뉴 및 접근 권한', href: '/admin/menus' },
      { icon: FileText, label: '시스템 로그 조회', href: '/admin/logs' },
    ]
  },
];

export default function Sidebar() {
  const { activeCategory, isCollapsed, toggleSidebar, userRole } = useNav();

  // 권한 체크 함수
  const hasAccess = (requiredRoles?: UserRole[]) => {
    if (!requiredRoles || userRole === 'SYSTEM_ADMIN') return true;
    return requiredRoles.includes(userRole);
  };

  const filteredMenuItems = menuItems
    .filter(group => group.category === activeCategory && hasAccess(group.requiredRoles))
    .map(group => ({
      ...group,
      items: group.items.filter(item => hasAccess(item.requiredRoles))
    }))
    .filter(group => group.items.length > 0);

  return (
    <aside className={`${isCollapsed ? 'w-20' : 'w-[300px]'} h-screen fixed top-0 left-0 bg-[#020617] border-r border-white/5 flex flex-col z-[100] transition-all duration-500 overflow-hidden group/sidebar shadow-2xl`}>
      {/* Sidebar Header (Identity) */}
      <div className={`h-[80px] px-6 flex items-center ${isCollapsed ? 'justify-center' : 'justify-between'} border-b border-white/5 bg-white/[0.01]`}>
         <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-600/20 group-hover/sidebar:rotate-[90deg] transition-transform duration-700">
               <Zap size={24} fill="currentColor" />
            </div>
            {!isCollapsed && (
              <div className="flex flex-col animate-in fade-in slide-in-from-left-2 duration-500">
                 <h1 className="text-xl font-black text-white italic tracking-tighter leading-none">ANTIGRAV</h1>
                 <span className="text-[10px] font-black text-blue-500 uppercase tracking-[0.3em] mt-0.5 italic">FINANCIAL</span>
              </div>
            )}
         </div>
         <button 
           onClick={toggleSidebar}
           className={`p-2 rounded-xl text-slate-500 hover:text-white hover:bg-white/5 transition-all outline-none ${isCollapsed ? 'hidden group-hover/sidebar:flex items-center justify-center absolute bg-blue-600/90 text-white -right-4 w-8 h-8 rounded-full shadow-xl z-50' : ''}`}
         >
           {isCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={18} />}
         </button>
      </div>

      <div className={`flex-1 overflow-y-auto ${isCollapsed ? 'px-3' : 'px-6'} py-10 custom-scrollbar space-y-10`}>
        {filteredMenuItems.map((group, idx) => (
          <div key={idx} className="animate-in slide-in-from-left duration-500" style={{ animationDelay: `${idx * 100}ms` }}>
            {!isCollapsed && (
              <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-[0.2em] mb-6 px-3 flex items-center gap-2">
                 <div className="w-1 h-1 bg-slate-700 rounded-full" /> {group.group}
              </h3>
            )}
            <ul className="space-y-1.5">
              {group.items.map((item, itemIdx) => (
                <li key={itemIdx}>
                  <Link 
                    href={item.href} 
                    className={`flex items-center ${isCollapsed ? 'justify-center' : 'gap-4 px-4'} py-3 rounded-2xl text-slate-500 hover:text-white hover:bg-white/[0.03] border border-transparent hover:border-white/5 transition-all group/item whitespace-nowrap active:scale-95`}
                    title={isCollapsed ? item.label : ''}
                  >
                    <item.icon size={18} className="text-slate-600 group-hover/item:text-blue-400 group-hover/item:scale-110 transition-all duration-300 shrink-0" />
                    {!isCollapsed && <span className="text-sm font-black tracking-tight animate-in fade-in slide-in-from-left-2 duration-300">{item.label}</span>}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      {/* Sidebar Footer */}
      <div className={`p-6 border-t border-white/5 bg-slate-950/50 ${isCollapsed ? 'flex justify-center' : ''}`}>
        <div className={`flex items-center ${isCollapsed ? 'justify-center w-12 h-12' : 'gap-4 px-4 py-4 w-full'} rounded-2xl bg-white/5 border border-white/5 hover:border-blue-500/30 cursor-pointer transition-all group/settings backdrop-blur-md`}>
          <div className="w-10 h-10 rounded-xl bg-slate-900 flex items-center justify-center text-slate-500 group-hover/settings:text-blue-400 transition-colors shrink-0">
            <Settings size={20} className="group-hover/settings:rotate-90 transition-transform duration-500" />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col animate-in fade-in duration-300">
               <span className="text-xs font-black text-white tracking-tight uppercase leading-none">System Admin</span>
               <span className="text-[10px] font-bold text-slate-600 uppercase tracking-widest mt-1">Config Mode</span>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}
