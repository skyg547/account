"use client";

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import * as LucideIcons from 'lucide-react';
import { useNav, UserRole } from '@/context/NavContext';
import { allMenus, MenuGroup } from './menus';

/**
 * lucide-react 아이콘을 문자열 이름으로 동적 로드합니다.
 * 메뉴 모듈 파일에서 icon: 'FileText' 같은 문자열로 정의하면
 * 이 함수가 실제 컴포넌트로 변환합니다.
 */
function getIcon(name: string): React.ElementType {
  const icons = LucideIcons as Record<string, React.ElementType>;
  return icons[name] || LucideIcons.FileText;
}

export default function Sidebar() {
  const { activeCategory, isCollapsed, toggleSidebar, userRole } = useNav();
  const pathname = usePathname();

  // 권한 체크
  const hasAccess = (requiredRoles?: UserRole[]) => {
    if (!requiredRoles || userRole === 'SYSTEM_ADMIN') return true;
    return requiredRoles.includes(userRole);
  };

  // 현재 카테고리 + 권한 기준 필터링
  const filteredMenuItems: MenuGroup[] = allMenus
    .filter(group => group.category === activeCategory && hasAccess(group.requiredRoles))
    .map(group => ({
      ...group,
      items: group.items.filter(item => hasAccess(item.requiredRoles))
    }))
    .filter(group => group.items.length > 0);

  return (
    <aside className={`${isCollapsed ? 'w-20' : 'w-[300px]'} h-screen fixed top-0 left-0 bg-[#020617] border-r border-white/5 flex flex-col z-[100] transition-all duration-500 overflow-hidden group/sidebar shadow-2xl`}>
      {/* ── Header (Brand) ── */}
      <div className={`h-[80px] px-6 flex items-center ${isCollapsed ? 'justify-center' : 'justify-between'} border-b border-white/5 bg-white/[0.01]`}>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-600/20 group-hover/sidebar:rotate-[90deg] transition-transform duration-700">
            <LucideIcons.Zap size={24} fill="currentColor" />
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
          {isCollapsed ? <LucideIcons.ChevronRight size={16} /> : <LucideIcons.ChevronLeft size={18} />}
        </button>
      </div>

      {/* ── Menu Groups ── */}
      <div className={`flex-1 overflow-y-auto ${isCollapsed ? 'px-3' : 'px-6'} py-8 custom-scrollbar space-y-8`}>
        {filteredMenuItems.map((group, idx) => (
          <div key={`${group.module}-${idx}`} className="animate-in slide-in-from-left duration-500" style={{ animationDelay: `${idx * 80}ms` }}>
            {!isCollapsed && (
              <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-[0.2em] mb-4 px-3 flex items-center gap-2">
                <div className="w-1 h-1 bg-slate-700 rounded-full" /> {group.group}
              </h3>
            )}
            <ul className="space-y-1">
              {group.items.map((item, itemIdx) => {
                const Icon = getIcon(item.icon);
                const isActive = pathname === item.href;
                return (
                  <li key={itemIdx}>
                    <Link
                      href={item.href}
                      className={`flex items-center ${isCollapsed ? 'justify-center' : 'gap-4 px-4'} py-2.5 rounded-2xl transition-all group/item whitespace-nowrap active:scale-95 ${
                        isActive
                          ? 'bg-blue-600/10 text-blue-400 border border-blue-500/20'
                          : 'text-slate-500 hover:text-white hover:bg-white/[0.03] border border-transparent hover:border-white/5'
                      }`}
                      title={isCollapsed ? item.label : ''}
                    >
                      <Icon size={18} className={`shrink-0 transition-all duration-300 ${
                        isActive
                          ? 'text-blue-400'
                          : 'text-slate-600 group-hover/item:text-blue-400 group-hover/item:scale-110'
                      }`} />
                      {!isCollapsed && (
                        <span className="text-sm font-black tracking-tight animate-in fade-in slide-in-from-left-2 duration-300">
                          {item.label}
                        </span>
                      )}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </div>

      {/* ── Footer ── */}
      <div className={`p-6 border-t border-white/5 bg-slate-950/50 ${isCollapsed ? 'flex justify-center' : ''}`}>
        <div className={`flex items-center ${isCollapsed ? 'justify-center w-12 h-12' : 'gap-4 px-4 py-4 w-full'} rounded-2xl bg-white/5 border border-white/5 hover:border-blue-500/30 cursor-pointer transition-all group/settings backdrop-blur-md`}>
          <div className="w-10 h-10 rounded-xl bg-slate-900 flex items-center justify-center text-slate-500 group-hover/settings:text-blue-400 transition-colors shrink-0">
            <LucideIcons.Settings size={20} className="group-hover/settings:rotate-90 transition-transform duration-500" />
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
