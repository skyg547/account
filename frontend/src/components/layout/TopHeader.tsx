"use client";

import React from 'react';
import Link from 'next/link';
import { Bell, Search, User, Moon, Sun } from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';
import { useTheme } from '@/context/ThemeContext';

/**
 * [K-Bank Style 최상단 시스템 헤더]
 * 7개 업무 카테고리 탭, 글로벌 검색, 테마 토글, 사용자 세션 정보를 관리하는 최상위 바입니다.
 */
export default function TopHeader() {
  const { activeCategory, setActiveCategory, isCollapsed } = useNav();
  const { resolvedTheme, toggleTheme } = useTheme();

  const handleCategoryClick = (category: NavCategory) => {
    setActiveCategory(category);
  };

  const navItems: { id: NavCategory; label: string }[] = [
    { id: 'DASHBOARD',  label: '대시보드' },
    { id: 'ACCOUNTING', label: '재무회계' },
    { id: 'OPERATIONS', label: '자금운영' },
    { id: 'CREDIT',     label: '여신·자산' },
    { id: 'RISK',       label: '리스크' },
    { id: 'MASTER',     label: '기준정보' },
    { id: 'SYSTEM',     label: '시스템' },
  ];

  return (
    <header className={`h-[72px] fixed top-0 right-0 ${isCollapsed ? 'left-20' : 'left-[280px]'} bg-white/95 dark:bg-[#131b2e]/95 backdrop-blur-md border-b border-[#eaedf4] dark:border-slate-800 flex items-center justify-between px-8 z-[90] transition-all duration-500 shadow-xs`}>
      {/* 1. 중앙: 글로벌 메뉴 바 (카테고리 필터) */}
      <nav className="flex items-center gap-1 bg-[#f7f8fb] dark:bg-slate-900/80 p-1.5 rounded-2xl border border-[#eaedf4] dark:border-slate-800">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => handleCategoryClick(item.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold tracking-tight transition-all duration-300 flex items-center gap-2 uppercase whitespace-nowrap ${
              activeCategory === item.id 
                ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/25 ring-2 ring-blue-400/20' 
                : 'text-[#545b69] dark:text-slate-400 hover:text-[#17191e] dark:hover:text-white hover:bg-white/80 dark:hover:bg-slate-800'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {/* 2. 우측: 검색 및 로그인 정보 영역 */}
      <div className="flex items-center gap-6">
        <div className="relative group hidden xl:block">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#8c94a4] group-focus-within:text-[#4262ff] transition-colors" size={16} />
          <input 
            type="text" 
            placeholder="글로벌 메뉴 및 데이터 검색..." 
            className="bg-[#f7f8fb] dark:bg-slate-900 border border-[#eaedf4] dark:border-slate-800 rounded-xl py-2 pl-10 pr-4 text-xs text-[#17191e] dark:text-slate-100 placeholder:text-[#8c94a4] outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/15 focus:w-72 w-56 transition-all font-medium"
          />
        </div>
        
        <div className="flex items-center gap-2 pr-1">
          {/* 다크모드 토글 버튼 */}
          <button 
            onClick={toggleTheme}
            aria-label={resolvedTheme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            title={resolvedTheme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            className="w-9 h-9 rounded-xl flex items-center justify-center text-[#545b69] dark:text-amber-400 hover:text-[#17191e] dark:hover:text-amber-300 hover:bg-[#f7f8fb] dark:hover:bg-slate-800 transition-all relative border border-transparent hover:border-[#eaedf4] dark:hover:border-slate-700 cursor-pointer"
          >
            {resolvedTheme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          <button 
            aria-label="알림"
            className="w-9 h-9 rounded-xl flex items-center justify-center text-[#545b69] dark:text-slate-400 hover:text-[#4262ff] hover:bg-blue-50 dark:hover:bg-slate-800 transition-all relative group border border-transparent hover:border-[#eaedf4] dark:hover:border-slate-700 cursor-pointer"
          >
            <Bell size={17} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-rose-500 rounded-full border-2 border-white dark:border-slate-900" />
          </button>
        </div>

        <Link href="/login" className="flex items-center gap-3 pl-5 border-l border-[#eaedf4] dark:border-slate-800 cursor-pointer group">
          <div className="text-right flex flex-col items-end">
            <div className="text-xs font-bold text-[#17191e] dark:text-slate-200 group-hover:text-[#4262ff] transition-colors leading-tight">관리자 세션</div>
            <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold tracking-tight mt-0.5 bg-emerald-50 dark:bg-emerald-950/40 px-1.5 py-0.2 rounded border border-emerald-200 dark:border-emerald-800">인증 연동됨</div>
          </div>
          <div className="relative">
            <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 flex items-center justify-center text-[#4262ff] group-hover:bg-[#4262ff] group-hover:text-white transition-all overflow-hidden shadow-xs">
               <User size={20} className="transition-transform duration-300" />
            </div>
            <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 border-2 border-white dark:border-slate-900 rounded-full" />
          </div>
        </Link>
      </div>
    </header>
  );
}
