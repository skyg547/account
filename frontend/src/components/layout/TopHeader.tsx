"use client";

import React from 'react';
import Link from 'next/link';
import { Bell, Search, User, Moon } from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';

/**
 * [K-Bank Style 최상단 시스템 헤더]
 * 7개 업무 카테고리 탭, 글로벌 검색, 사용자 세션 정보를 관리하는 최상위 바입니다.
 */
export default function TopHeader() {
  const { activeCategory, setActiveCategory, isCollapsed } = useNav();

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
    <header className={`h-[72px] fixed top-0 right-0 ${isCollapsed ? 'left-20' : 'left-[280px]'} bg-white/95 backdrop-blur-md border-b border-[#eaedf4] flex items-center justify-between px-8 z-[90] transition-all duration-500 shadow-sm`}>
      {/* 1. 중앙: 글로벌 메뉴 바 (카테고리 필터) */}
      <nav className="flex items-center gap-1 bg-[#f7f8fb] p-1.5 rounded-2xl border border-[#eaedf4]">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => handleCategoryClick(item.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold tracking-tight transition-all duration-300 flex items-center gap-2 uppercase whitespace-nowrap ${
              activeCategory === item.id 
                ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/25 ring-2 ring-blue-400/20' 
                : 'text-[#545b69] hover:text-[#17191e] hover:bg-white/80'
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
            className="bg-[#f7f8fb] border border-[#eaedf4] rounded-xl py-2 pl-10 pr-4 text-xs text-[#17191e] placeholder:text-[#8c94a4] outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/15 focus:w-72 w-56 transition-all font-medium"
          />
        </div>
        
        <div className="flex items-center gap-2 pr-1">
          <button className="w-9 h-9 rounded-xl flex items-center justify-center text-[#545b69] hover:text-[#17191e] hover:bg-[#f7f8fb] transition-all relative border border-transparent hover:border-[#eaedf4]">
            <Moon size={17} />
          </button>
          <button className="w-9 h-9 rounded-xl flex items-center justify-center text-[#545b69] hover:text-[#4262ff] hover:bg-blue-50 transition-all relative group border border-transparent hover:border-[#eaedf4]">
            <Bell size={17} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-rose-500 rounded-full border-2 border-white" />
          </button>
        </div>

        <Link href="/login" className="flex items-center gap-3 pl-5 border-l border-[#eaedf4] cursor-pointer group">
          <div className="text-right flex flex-col items-end">
            <div className="text-xs font-bold text-[#17191e] group-hover:text-[#4262ff] transition-colors leading-tight">관리자 세션</div>
            <div className="text-[10px] text-emerald-600 font-bold tracking-tight mt-0.5 bg-emerald-50 px-1.5 py-0.2 rounded border border-emerald-200">인증 연동됨</div>
          </div>
          <div className="relative">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-200 flex items-center justify-center text-[#4262ff] group-hover:bg-[#4262ff] group-hover:text-white transition-all overflow-hidden shadow-sm">
               <User size={20} className="transition-transform duration-300" />
            </div>
            <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 border-2 border-white rounded-full" />
          </div>
        </Link>
      </div>
    </header>
  );
}
