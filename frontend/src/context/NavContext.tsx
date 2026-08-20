"use client";

import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';

/**
 * [상단 6대 메가 비즈니스 그룹 정의]
 * 스크롤바 없이 한눈에 들어오는 최상위 6대 그룹입니다.
 */
export type NavCategory = 
  | 'DASHBOARD'           // 📊 대시보드 (통합 재무 현황)
  | 'ACCOUNTING'          // 📝 회계·결산 (journal-ledger, closing, reporting)
  | 'OPERATIONS'          // 💳 자금·세무 (expenditure, payable, receivable, budget, tax)
  | 'BANKING_ASSET'       // 🏦 금융·자산 (loan, deposit, asset-lease)
  | 'RISK_DATA'           // 📉 리스크·데이터 (ecl, reconciliation, account-mart)
  | 'GOVERNANCE_SYSTEM';  // ⚙️ 거버넌스·시스템 (master-data, internal-audit, admin, auth)

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
      } catch {
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
      isGovernanceConnected,
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
