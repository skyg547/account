"use client";

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import * as LucideIcons from 'lucide-react';
import { useNav } from '@/context/NavContext';
import { allMenus, MenuGroup } from './menus';

/**
 * [K-Bank Style 사이드바]
 * lucide-react 아이콘을 동적으로 렌더링하고 KBank의 산뜻한 화이트/블루 테마 및 다크 모드를 지원하는 네비게이션 사이드바입니다.
 */
function getIcon(name: string): React.ElementType {
  const iconMap = LucideIcons as Record<string, unknown>;
  const Component = iconMap[name] as React.ElementType;
  return Component || LucideIcons.FileText;
}

export default function Sidebar() {
  const { activeCategory, isCollapsed, toggleSidebar, userRole, userAuthorizations } = useNav();
  const pathname = usePathname();

  // 개별 메뉴 아이템 접근 권한 체크 (Governance API 연동 및 데모 모드 지원)
  const hasItemAccess = (href: string) => {
    if (userRole === 'SYSTEM_ADMIN') return true;
    if (!userAuthorizations || userAuthorizations.length === 0) return true;
    if (userAuthorizations.some(auth => auth.functionCode === 'MENU:*')) return true;
    return userAuthorizations.some(auth => auth.functionCode === `MENU:${href}`);
  };

  // 현재 카테고리 필터링 및 권한 체크
  const filteredMenuItems: MenuGroup[] = allMenus
    .filter(group => group.category === activeCategory)
    .map(group => ({
      ...group,
      items: group.items.filter(item => hasItemAccess(item.href))
    }))
    .filter(group => group.items.length > 0);

  return (
    <aside className={`${isCollapsed ? 'w-20' : 'w-[280px]'} h-screen fixed top-0 left-0 bg-white dark:bg-[#131b2e] border-r border-[#eaedf4] dark:border-slate-800 flex flex-col z-[100] transition-all duration-500 overflow-hidden group/sidebar shadow-xs`}>
      {/* ── Header (KBank Brand) ── */}
      <div className={`h-[72px] px-6 flex items-center ${isCollapsed ? 'justify-center' : 'justify-between'} border-b border-[#eaedf4] dark:border-slate-800 bg-white dark:bg-[#131b2e]`}>
        <Link href="/" className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-[#4262ff] flex items-center justify-center text-white shadow-md shadow-blue-600/30 group-hover/sidebar:scale-105 transition-transform duration-300">
            <LucideIcons.Building2 size={20} />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col animate-in fade-in slide-in-from-left-2 duration-300">
              <h1 className="text-lg font-black text-[#17191e] dark:text-slate-100 tracking-tight leading-none">Account.AI</h1>
              <span className="text-[9px] font-extrabold text-[#4262ff] uppercase tracking-wider mt-0.5">Financial Banking</span>
            </div>
          )}
        </Link>
        <button
          onClick={toggleSidebar}
          className={`p-1.5 rounded-lg text-[#8c94a4] dark:text-slate-400 hover:text-[#17191e] dark:hover:text-white hover:bg-[#f7f8fb] dark:hover:bg-slate-800 transition-all outline-none cursor-pointer ${isCollapsed ? 'hidden group-hover/sidebar:flex items-center justify-center absolute bg-[#4262ff] text-white -right-3 w-7 h-7 rounded-full shadow-lg z-50' : ''}`}
          title={isCollapsed ? '사이드바 펼치기' : '사이드바 접기'}
        >
          {isCollapsed ? <LucideIcons.ChevronRight size={14} /> : <LucideIcons.ChevronLeft size={16} />}
        </button>
      </div>

      {/* ── Menu Groups ── */}
      <div className={`flex-1 overflow-y-auto ${isCollapsed ? 'px-2' : 'px-4'} py-6 custom-scrollbar space-y-6`}>
        {filteredMenuItems.map((group, idx) => (
          <div key={`${group.module}-${idx}`} className="animate-in slide-in-from-left duration-300" style={{ animationDelay: `${idx * 50}ms` }}>
            {!isCollapsed && (
              <h3 className="text-[11px] font-bold text-[#8c94a4] dark:text-slate-400 uppercase tracking-wider mb-2 px-3 flex items-center gap-2">
                <span className="w-1.5 h-1.5 bg-[#4262ff]/40 rounded-full" /> {group.group}
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
                      className={`flex items-center ${isCollapsed ? 'justify-center' : 'gap-3 px-3.5'} py-2.5 rounded-xl transition-all group/item whitespace-nowrap active:scale-98 ${
                        isActive
                          ? 'bg-[#eef2ff] dark:bg-blue-950/40 text-[#4262ff] dark:text-blue-400 font-bold border border-[#dbe3ff] dark:border-blue-800/60 shadow-xs'
                          : 'text-[#545b69] dark:text-slate-400 hover:text-[#17191e] dark:hover:text-slate-100 hover:bg-[#f7f8fb] dark:hover:bg-slate-800/80 border border-transparent'
                      }`}
                      title={isCollapsed ? item.label : ''}
                    >
                      <Icon size={17} className={`shrink-0 transition-all duration-200 ${
                        isActive
                          ? 'text-[#4262ff] dark:text-blue-400'
                          : 'text-[#8c94a4] dark:text-slate-500 group-hover/item:text-[#4262ff] dark:group-hover/item:text-blue-400 group-hover/item:scale-105'
                      }`} />
                      {!isCollapsed && (
                        <span className="text-xs font-semibold tracking-tight">
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

      {/* ── Footer / System Settings ── */}
      <div className={`p-4 border-t border-[#eaedf4] dark:border-slate-800 bg-[#f7f8fb]/60 dark:bg-slate-900/60 ${isCollapsed ? 'flex justify-center' : ''}`}>
        <Link href="/system/users" className={`flex items-center ${isCollapsed ? 'justify-center w-10 h-10' : 'gap-3 px-3 py-2.5 w-full'} rounded-xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 hover:border-[#4262ff]/40 dark:hover:border-blue-500/40 hover:shadow-xs cursor-pointer transition-all group/settings`}>
          <div className="w-8 h-8 rounded-lg bg-[#f7f8fb] dark:bg-slate-800 flex items-center justify-center text-[#8c94a4] dark:text-slate-400 group-hover/settings:text-[#4262ff] group-hover/settings:bg-blue-50 dark:group-hover/settings:bg-blue-950/40 transition-colors shrink-0">
            <LucideIcons.Settings size={16} />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col">
              <span className="text-xs font-bold text-[#17191e] dark:text-slate-100 leading-tight">시스템 설정</span>
              <span className="text-[10px] text-[#8c94a4] dark:text-slate-400 font-medium mt-0.5">운영 환경 관리</span>
            </div>
          )}
        </Link>
      </div>
    </aside>
  );
}
