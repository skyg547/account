"use client";

import React from 'react';
import Link from 'next/link';
import { Bell, Search, User, Moon, Sun } from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';
import { useTheme } from '@/context/ThemeContext';

/**
 * [K-Bank Style 최상단 시스템 헤더 - 백엔드 MSA 모듈 1:1 완전 분리 16대 대메뉴]
 * 결산/보고, 지출/예산, 대사/데이터마트, 내부통제/시스템보안이 각각 1:1 독립 탭으로 분리되었습니다.
 */
export default function TopHeader() {
  const { activeCategory, setActiveCategory, isCollapsed } = useNav();
  const { resolvedTheme, toggleTheme } = useTheme();

  const handleCategoryClick = (category: NavCategory) => {
    setActiveCategory(category);
  };

  const navItems: { id: NavCategory; label: string; backendModule: string }[] = [
    { id: 'DASHBOARD',        label: '대시보드',      backendModule: 'BFF' },
    { id: 'JOURNAL',          label: '분개·원장',      backendModule: 'journal-ledger' },
    { id: 'CLOSING',          label: '결산관리',      backendModule: 'closing' },
    { id: 'REPORTING',        label: '재무보고서',    backendModule: 'reporting' },
    { id: 'EXPENDITURE',      label: '지출·지급',      backendModule: 'expenditure/payable' },
    { id: 'BUDGET',           label: '예산관리',      backendModule: 'budget' },
    { id: 'TAX',              label: '세무회계',      backendModule: 'tax' },
    { id: 'LOAN',             label: '여신관리',      backendModule: 'loan' },
    { id: 'DEPOSIT',          label: '수신관리',      backendModule: 'deposit' },
    { id: 'ASSET_LEASE',      label: '고정자산·리스',  backendModule: 'asset-lease' },
    { id: 'ECL',              label: '대손충당(ECL)', backendModule: 'ecl' },
    { id: 'RECONCILIATION',   label: '회계대사',      backendModule: 'reconciliation' },
    { id: 'ACCOUNT_MART',     label: '데이터마트',    backendModule: 'account-mart' },
    { id: 'MASTER',           label: '기준정보',      backendModule: 'master-data' },
    { id: 'INTERNAL_AUDIT',   label: '내부통제·감사',  backendModule: 'internal-audit' },
    { id: 'SYSTEM_SECURITY',  label: '시스템·보안',    backendModule: 'auth/admin' },
  ];

  return (
    <header className={`h-[72px] fixed top-0 right-0 ${isCollapsed ? 'left-20' : 'left-[280px]'} bg-white/95 dark:bg-[#131b2e]/95 backdrop-blur-md border-b border-[#eaedf4] dark:border-slate-800 flex items-center justify-between px-6 z-[90] transition-all duration-500 shadow-xs gap-3`}>
      {/* 1. 중앙: 백엔드 모듈 1:1 완전 분리 글로벌 대메뉴 바 */}
      <div className="flex-1 overflow-x-auto custom-scrollbar py-1">
        <nav className="flex items-center gap-1 bg-[#f7f8fb] dark:bg-slate-900/80 p-1.5 rounded-2xl border border-[#eaedf4] dark:border-slate-800 w-max">
          {navItems.map((item) => {
            const isActive = activeCategory === item.id;
            return (
              <button
                key={item.id}
                onClick={() => handleCategoryClick(item.id)}
                title={`백엔드 모듈: ${item.backendModule}`}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold tracking-tight transition-all duration-200 flex items-center gap-1 whitespace-nowrap cursor-pointer ${
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
      </div>

      {/* 2. 우측: 검색, 다크모드 토글, 세션 영역 */}
      <div className="flex items-center gap-3 shrink-0">
        <div className="relative group hidden 2xl:block">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#8c94a4] group-focus-within:text-[#4262ff] transition-colors" size={15} />
          <input 
            type="text" 
            placeholder="메뉴/전표 검색..." 
            className="bg-[#f7f8fb] dark:bg-slate-900 border border-[#eaedf4] dark:border-slate-800 rounded-xl py-1.5 pl-9 pr-3 text-xs text-[#17191e] dark:text-slate-100 placeholder:text-[#8c94a4] outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/15 focus:w-56 w-36 transition-all font-medium"
          />
        </div>
        
        <div className="flex items-center gap-1.5 pr-1">
          {/* 다크모드 토글 버튼 */}
          <button 
            onClick={toggleTheme}
            aria-label={resolvedTheme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            title={resolvedTheme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            className="w-8.5 h-8.5 rounded-xl flex items-center justify-center text-[#545b69] dark:text-amber-400 hover:text-[#17191e] dark:hover:text-amber-300 hover:bg-[#f7f8fb] dark:hover:bg-slate-800 transition-all border border-transparent hover:border-[#eaedf4] dark:hover:border-slate-700 cursor-pointer"
          >
            {resolvedTheme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
          </button>

          <button 
            aria-label="알림"
            className="w-8.5 h-8.5 rounded-xl flex items-center justify-center text-[#545b69] dark:text-slate-400 hover:text-[#4262ff] hover:bg-blue-50 dark:hover:bg-slate-800 transition-all relative group border border-transparent hover:border-[#eaedf4] dark:hover:border-slate-700 cursor-pointer"
          >
            <Bell size={16} />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-white dark:border-slate-900" />
          </button>
        </div>

        <Link href="/login" className="flex items-center gap-2 pl-2.5 border-l border-[#eaedf4] dark:border-slate-800 cursor-pointer group">
          <div className="text-right hidden sm:flex flex-col items-end">
            <div className="text-xs font-bold text-[#17191e] dark:text-slate-200 group-hover:text-[#4262ff] transition-colors leading-tight">관리자</div>
            <div className="text-[9px] text-emerald-600 dark:text-emerald-400 font-bold tracking-tight mt-0.5 bg-emerald-50 dark:bg-emerald-950/40 px-1 py-0.2 rounded border border-emerald-200 dark:border-emerald-800">인증 연동</div>
          </div>
          <div className="relative">
            <div className="w-8.5 h-8.5 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 flex items-center justify-center text-[#4262ff] group-hover:bg-[#4262ff] group-hover:text-white transition-all overflow-hidden shadow-xs">
               <User size={17} className="transition-transform duration-300" />
            </div>
            <div className="absolute -bottom-0.5 -right-0.5 w-2 h-2 bg-emerald-500 border-2 border-white dark:border-slate-900 rounded-full" />
          </div>
        </Link>
      </div>
    </header>
  );
}
