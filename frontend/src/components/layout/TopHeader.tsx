"use client";

import React from 'react';
import { Bell, Search, User, Moon } from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';

/**
 * [최상단 시스템 헤더]
 * 시스템 이름, 글로벌 메뉴, 로그인 세션 정보를 통합 관리하는 최상위 바입니다.
 */
export default function TopHeader() {
  const { activeCategory, setActiveCategory } = useNav();

  const handleCategoryClick = (category: NavCategory) => {
    setActiveCategory(category);
  };

  const navItems: { id: NavCategory; label: string; icon?: React.ReactNode }[] = [
    { id: 'DASHBOARD', label: '현황판' },
    { id: 'ACCOUNTING', label: '재무회계' },
    { id: 'OPERATIONS', label: '자금/자산' },
    { id: 'MASTER', label: '기준정보' },
    { id: 'ADMIN', label: '관리자' },
  ];

  return (
    <header className="h-[80px] fixed top-0 right-0 left-[300px] bg-[#020617]/80 backdrop-blur-2xl border-b border-white/5 flex items-center justify-between px-10 z-[90] transition-all duration-500">
      {/* 1. 중앙: 글로벌 메뉴 바 (카테고리 필터) */}
      <nav className="flex items-center gap-1.5 bg-white/[0.02] p-1.5 rounded-2xl border border-white/5 shadow-inner">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => handleCategoryClick(item.id)}
            className={`px-5 py-2.5 rounded-xl text-xs font-black tracking-tight transition-all duration-500 flex items-center gap-2 uppercase ${
              activeCategory === item.id 
                ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/30 ring-1 ring-blue-400/50' 
                : 'text-slate-500 hover:text-slate-200 hover:bg-white/5'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {/* 2. 우측: 검색 및 로그인 정보 영역 */}
      <div className="flex items-center gap-10">
        <div className="relative group hidden xl:block">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-hover:text-blue-400 transition-colors" size={16} />
          <input 
            type="text" 
            placeholder="Search Global..." 
            className="bg-slate-950 border border-white/5 rounded-2xl py-2.5 pl-12 pr-6 text-xs text-slate-300 outline-none focus:border-blue-500/50 focus:w-80 w-64 transition-all shadow-inner font-bold"
          />
        </div>
        
        <div className="flex items-center gap-3 pr-2">
          <button className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-500 hover:text-white hover:bg-white/5 transition-all relative group">
            <Moon size={18} className="group-hover:rotate-12 transition-transform" />
          </button>
          <button className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-500 hover:text-blue-400 hover:bg-blue-500/10 transition-all relative group">
            <Bell size={18} className="group-hover:animate-swing" />
            <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-[#020617] shadow-[0_0_8px_rgba(244,63,94,0.5)]" />
          </button>
        </div>

        <div className="flex items-center gap-4 pl-6 border-l border-white/5 cursor-pointer group">
          <div className="text-right flex flex-col items-end">
            <div className="text-sm font-black text-white tracking-tighter group-hover:text-blue-400 transition-colors leading-none italic uppercase">Kim J.M.</div>
            <div className="text-[10px] text-slate-600 font-bold uppercase tracking-widest mt-1">Financial Lead</div>
          </div>
          <div className="relative">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 border border-white/10 flex items-center justify-center text-slate-400 group-hover:border-blue-500/50 group-hover:shadow-[0_0_20px_rgba(59,130,246,0.15)] transition-all overflow-hidden">
               <User size={22} className="group-hover:scale-110 transition-transform duration-500" />
            </div>
            <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-emerald-500 border-2 border-[#020617] rounded-full" />
          </div>
        </div>
      </div>
    </header>
  );
}
