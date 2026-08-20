"use client";

import React from 'react';
import Link from 'next/link';
import { Bell, Search, User, Moon, Sun } from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';
import { useTheme } from '@/context/ThemeContext';

/**
 * [K-Bank Style 최상단 시스템 헤더 - 스크롤바 없는 6대 메가 카테고리 바]
 * 가로 스크롤 없이 한눈에 들어오는 6대 핵심 비즈니스 탭입니다.
 */
export default function TopHeader() {
  const { activeCategory, setActiveCategory, isCollapsed } = useNav();
  const { resolvedTheme, toggleTheme } = useTheme();

  const handleCategoryClick = (category: NavCategory) => {
    setActiveCategory(category);
  };

  const navItems: { id: NavCategory; label: string; desc: string }[] = [
    { id: 'DASHBOARD',         label: '대시보드',      desc: '통합 재무 현황' },
    { id: 'ACCOUNTING',        label: '회계·결산',      desc: '원장, 결산, 보고서' },
    { id: 'OPERATIONS',        label: '자금·세무',      desc: '지출, 예산, 세금계산서' },
    { id: 'BANKING_ASSET',     label: '금융·자산',      desc: '여신, 수신, 리스회계' },
    { id: 'RISK_DATA',         label: '리스크·데이터',   desc: 'ECL, 회계대사, 마트' },
    { id: 'GOVERNANCE_SYSTEM', label: '거버넌스·시스템', desc: '기준정보, 감사, 보안' },
  ];

  return (
    <header className={`h-[72px] fixed top-0 right-0 ${isCollapsed ? 'left-20' : 'left-[280px]'} bg-white/95 dark:bg-[#131b2e]/95 backdrop-blur-md border-b border-[#eaedf4] dark:border-slate-800 flex items-center justify-between px-8 z-[90] transition-all duration-500 shadow-xs gap-4`}>
      {/* 1. 중앙: 스크롤바 없는 6대 메가 카테고리 바 */}
      <nav className="flex items-center gap-1.5 bg-[#f7f8fb] dark:bg-slate-900/80 p-1.5 rounded-2xl border border-[#eaedf4] dark:border-slate-800">
        {navItems.map((item) => {
          const isActive = activeCategory === item.id;
          return (
            <button
              key={item.id}
              onClick={() => handleCategoryClick(item.id)}
              title={item.desc}
              className={`px-4 py-2 rounded-xl text-xs font-bold tracking-tight transition-all duration-200 flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                isActive 
                  ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/25 ring-2 ring-blue-400/20' 
                  : 'text-[#545b69] dark:text-slate-400 hover:text-[#17191e] dark:hover:text-white hover:bg-white/80 dark:hover:bg-slate-800'
              }`}
            >
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* 2. 우측: 검색, 다크모드 토글, 세션 영역 */}
      <div className="flex items-center gap-4 shrink-0">
        <div className="relative group hidden xl:block">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#8c94a4] group-focus-within:text-[#4262ff] transition-colors" size={15} />
          <input 
            type="text" 
            placeholder="글로벌 메뉴 및 전표 검색..." 
            className="bg-[#f7f8fb] dark:bg-slate-900 border border-[#eaedf4] dark:border-slate-800 rounded-xl py-2 pl-9 pr-3 text-xs text-[#17191e] dark:text-slate-100 placeholder:text-[#8c94a4] outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/15 focus:w-64 w-48 transition-all font-medium"
          />
        </div>
        
        <div className="flex items-center gap-2 pr-1">
          {/* 다크모드 토글 버튼 */}
          <button 
            onClick={toggleTheme}
            aria-label={resolvedTheme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            title={resolvedTheme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            className="w-9 h-9 rounded-xl flex items-center justify-center text-[#545b69] dark:text-amber-400 hover:text-[#17191e] dark:hover:text-amber-300 hover:bg-[#f7f8fb] dark:hover:bg-slate-800 transition-all border border-transparent hover:border-[#eaedf4] dark:hover:border-slate-700 cursor-pointer"
          >
            {resolvedTheme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          <button 
            aria-label="알림"
            className="w-9 h-9 rounded-xl flex items-center justify-center text-[#545b69] dark:text-slate-400 hover:text-[#4262ff] hover:bg-blue-50 dark:hover:bg-slate-800 transition-all relative group border border-transparent hover:border-[#eaedf4] dark:hover:border-slate-700 cursor-pointer"
          >
            <Bell size={17} />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-white dark:border-slate-900" />
          </button>
        </div>

        <Link href="/login" className="flex items-center gap-3 pl-3.5 border-l border-[#eaedf4] dark:border-slate-800 cursor-pointer group">
          <div className="text-right hidden sm:flex flex-col items-end">
            <div className="text-xs font-bold text-[#17191e] dark:text-slate-200 group-hover:text-[#4262ff] transition-colors leading-tight">관리자</div>
            <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold tracking-tight mt-0.5 bg-emerald-50 dark:bg-emerald-950/40 px-1.5 py-0.2 rounded border border-emerald-200 dark:border-emerald-800">인증 연동됨</div>
          </div>
          <div className="relative">
            <div className="w-9 h-9 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 flex items-center justify-center text-[#4262ff] group-hover:bg-[#4262ff] group-hover:text-white transition-all overflow-hidden shadow-xs">
               <User size={18} className="transition-transform duration-300" />
            </div>
            <div className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-500 border-2 border-white dark:border-slate-900 rounded-full" />
          </div>
        </Link>
      </div>
    </header>
  );
}
