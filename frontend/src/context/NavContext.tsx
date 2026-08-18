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
  userAuthorizations: Authorization[];
  isGovernanceConnected: boolean;
}

const NavContext = createContext<NavContextType | undefined>(undefined);

const GOVERNANCE_API_BASE_URL = process.env.NEXT_PUBLIC_GOVERNANCE_API_URL || '';

/**
 * [백엔드 미연결 시 로컬 데모용 Mock 권한 목록]
 * 백엔드 거버넌스 API가 꺼져 있어도 화면이 텅 비지 않도록 기본 전체 메뉴 접근을 보장합니다.
 */
const DEFAULT_ALL_ACCESS: Authorization[] = [
  { id: 1, roleCode: 'ALL', functionCode: 'MENU:*', accessType: 'READ_WRITE' },
];

export function NavProvider({ children }: { children: ReactNode }) {
  const [activeCategory, setActiveCategory] = useState<NavCategory>('DASHBOARD');
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [userRole, setUserRole] = useState<UserRole>('ACCOUNTING_ADMIN');
  const [userAuthorizations, setUserAuthorizations] = useState<Authorization[]>(DEFAULT_ALL_ACCESS);
  const [isGovernanceConnected, setIsGovernanceConnected] = useState(false);

  const toggleSidebar = () => setIsCollapsed(!isCollapsed);

  // 권한 그룹이 변경될 때마다 Governance 모듈에서 권한 목록을 불러오고, 실패 시 안전하게 데모 Mock 권한으로 폴백합니다.
  useEffect(() => {
    const fetchAuthorizations = async () => {
      if (userRole === 'SYSTEM_ADMIN') {
        setUserAuthorizations(DEFAULT_ALL_ACCESS);
        return;
      }

      if (!GOVERNANCE_API_BASE_URL) {
        // API Base URL이 설정되지 않은 로컬 독립 모드
        setUserAuthorizations(DEFAULT_ALL_ACCESS);
        setIsGovernanceConnected(false);
        return;
      }

      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500); // 1.5초 타임아웃 방어

        const res = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles/${userRole}/authorizations`, {
          signal: controller.signal
        });
        clearTimeout(timeoutId);

        if (res.ok) {
          const data = await res.json();
          setUserAuthorizations(data.length > 0 ? data : DEFAULT_ALL_ACCESS);
          setIsGovernanceConnected(true);
        } else {
          setUserAuthorizations(DEFAULT_ALL_ACCESS);
          setIsGovernanceConnected(false);
        }
      } catch (e) {
        // 백엔드 미구동 시 조용히 Mock 권한으로 안전하게 폴백
        setUserAuthorizations(DEFAULT_ALL_ACCESS);
        setIsGovernanceConnected(false);
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
      userAuthorizations,
      isGovernanceConnected
    }}>
      {children}
    </NavContext.Provider>
  );
}

export function useNav() {
  const context = useContext(NavContext);
  if (!context) {
    throw new Error('useNav must be used within a NavProvider');
  }
  return context;
}
