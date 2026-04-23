"use client";

import React, { createContext, useContext, useState, ReactNode } from 'react';

/**
 * [내비게이션 카테고리 정의]
 * 상단 헤더에서 선택할 수 있는 대분류입니다.
 */
export type NavCategory = 
  | 'DASHBOARD'   // 대시보드
  | 'ACCOUNTING'  // 재무업무 (전표, 원장, 결산)
  | 'OPERATIONS'  // 재무운영 (기업 실무, 채권/채무)
  | 'BANKING'     // 은행특화
  | 'MASTER'      // 기준관리
  | 'ADMIN';      // 시스템관리

interface NavContextType {
  activeCategory: NavCategory;
  setActiveCategory: (category: NavCategory) => void;
}

const NavContext = createContext<NavContextType | undefined>(undefined);

/**
 * [내비게이션 상태 제공자]
 * 상단 헤더와 사이드바가 소통할 수 있게 상태를 공유해줍니다.
 */
export function NavProvider({ children }: { children: ReactNode }) {
  const [activeCategory, setActiveCategory] = useState<NavCategory>('ACCOUNTING');

  return (
    <NavContext.Provider value={{ activeCategory, setActiveCategory }}>
      {children}
    </NavContext.Provider>
  );
}

/**
 * [내비게이션 상태 사용 훅]
 */
export function useNav() {
  const context = useContext(NavContext);
  if (!context) {
    throw new Error('useNav must be used within a NavProvider');
  }
  return context;
}
