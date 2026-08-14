"use client";

import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';

/**
 * [내비게이션 카테고리 정의]
 * 상단 헤더에서 선택할 수 있는 7개 대분류입니다.
 */
export type NavCategory = 
  | 'DASHBOARD'    // 대시보드
  | 'ACCOUNTING'   // 재무회계 (원장, 결산, 보고서)
  | 'OPERATIONS'   // 자금운영 (지출, 세무)
  | 'CREDIT'       // 여신·자산 (대출/이연, 공정가치)
  | 'RISK'         // 리스크·데이터 (ECL, 마트/대사)
  | 'MASTER'       // 기준정보 (계정과목, 거래처)
  | 'SYSTEM';      // 시스템관리 (내부회계, 사용자/부서)

/**
 * [보안 역할 정의]
 * RBAC(Role Based Access Control)를 위한 사용자 역할입니다.
 */
export type UserRole = 
  | 'SYSTEM_ADMIN'
  | 'ACCOUNTING_ADMIN'
  | 'RISK_MANAGER'
  | 'RISK_ANALYST'
  | 'MASTER_MANAGER'
  | 'AUDITOR'
  | 'USER';

export interface Authorization {
  id: number;
  roleCode: string;
  functionCode: string;
  accessType: string;
}

interface NavContextType {
  activeCategory: NavCategory;
  setActiveCategory: (category: NavCategory) => void;
  isCollapsed: boolean;
  toggleSidebar: () => void;
  userRole: UserRole;
  setUserRole: (role: UserRole) => void;
  // 💡 [초보자 팁] 현재 로그인한 사용자가 접근 가능한 '메뉴 권한 목록'을 전역 상태로 관리합니다.
  userAuthorizations: Authorization[];
}

const NavContext = createContext<NavContextType | undefined>(undefined);

const GOVERNANCE_API_BASE_URL = process.env.NEXT_PUBLIC_GOVERNANCE_API_URL || 'http://localhost:8083';

/**
 * [내비게이션 상태 제공자]
 * 상단 헤더와 사이드바가 소통할 수 있게 상태를 공유해줍니다.
 */
export function NavProvider({ children }: { children: ReactNode }) {
  const [activeCategory, setActiveCategory] = useState<NavCategory>('ACCOUNTING');
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [userRole, setUserRole] = useState<UserRole>('ACCOUNTING_ADMIN');
  const [userAuthorizations, setUserAuthorizations] = useState<Authorization[]>([]);

  const toggleSidebar = () => setIsCollapsed(!isCollapsed);

  // 권한 그룹이 변경될 때마다 Governance 모듈에서 권한 목록(메뉴 등)을 불러옵니다.
  useEffect(() => {
    // 💡 [초보자 팁] 사용자가 로그인/로그아웃하여 Role(역할)이 바뀔 때마다,
    // Governance API를 호출해 해당 역할이 볼 수 있는 새로운 권한 목록을 가져옵니다.
    const fetchAuthorizations = async () => {
      if (userRole === 'SYSTEM_ADMIN') {
        setUserAuthorizations([]); // SYSTEM_ADMIN은 모든 권한 패스
        return;
      }
      try {
        const res = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles/${userRole}/authorizations`);
        if (res.ok) {
          const data = await res.json();
          setUserAuthorizations(data);
        } else {
          setUserAuthorizations([]);
        }
      } catch (e) {
        console.error('Failed to fetch authorizations', e);
        setUserAuthorizations([]);
      }
    };
    fetchAuthorizations();
  }, [userRole]);

  return (
    <NavContext.Provider value={{ 
      activeCategory, 
      setActiveCategory, 
      isCollapsed, 
      toggleSidebar,
      userRole,
      setUserRole,
      userAuthorizations
    }}>
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
