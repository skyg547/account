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

export type AuthorizationStatus = 'loading' | 'success' | 'unavailable';

interface NavContextType {
  activeCategory: NavCategory;
  setActiveCategory: (category: NavCategory) => void;
  isCollapsed: boolean;
  toggleSidebar: () => void;
  userRole: UserRole;
  setUserRole: (role: UserRole) => void;
  userAuthorizations: Authorization[];
  authorizationStatus: AuthorizationStatus;
  isGovernanceConnected: boolean;
}

const NavContext = createContext<NavContextType | undefined>(undefined);

const GOVERNANCE_API_BASE_URL = process.env.NEXT_PUBLIC_GOVERNANCE_API_URL || '';

type AuthorizationSnapshot = {
  role: UserRole;
  status: AuthorizationStatus;
  authorizations: Authorization[];
  connected: boolean;
};

function isAuthorizationList(value: unknown, role: UserRole): value is Authorization[] {
  return Array.isArray(value) && value.every(item =>
    item !== null && typeof item === 'object' &&
    typeof item.id === 'number' &&
    item.roleCode === role &&
    typeof item.functionCode === 'string' && item.functionCode.length > 0 &&
    typeof item.accessType === 'string' && item.accessType.length > 0
  );
}

export function NavProvider({ children }: { children: ReactNode }) {
  const [activeCategory, setActiveCategory] = useState<NavCategory>('DASHBOARD');
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [userRole, setCurrentUserRole] = useState<UserRole>('ACCOUNTING_ADMIN');
  const [authorizationSnapshot, setAuthorizationSnapshot] = useState<AuthorizationSnapshot>({
    role: 'ACCOUNTING_ADMIN', status: 'loading', authorizations: [], connected: false,
  });

  const setUserRole = (role: UserRole) => {
    if (role === userRole) return;
    // Clear grants in the same update as the role change, before the next effect starts.
    setAuthorizationSnapshot({ role, status: 'loading', authorizations: [], connected: false });
    setCurrentUserRole(role);
  };

  // A response for an older role must never become the current role's menu grants.
  const currentSnapshot = authorizationSnapshot.role === userRole
    ? authorizationSnapshot
    : { role: userRole, status: 'loading' as const, authorizations: [], connected: false };

  const toggleSidebar = () => setIsCollapsed(!isCollapsed);

  // A successful empty list means no menu grants; missing or failed data is unavailable.
  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    const fetchAuthorizations = async () => {
      if (userRole === 'SYSTEM_ADMIN') {
        setAuthorizationSnapshot({ role: userRole, status: 'success', authorizations: [], connected: false });
        return;
      }

      if (!GOVERNANCE_API_BASE_URL) {
        setAuthorizationSnapshot({ role: userRole, status: 'unavailable', authorizations: [], connected: false });
        return;
      }

      setAuthorizationSnapshot({ role: userRole, status: 'loading', authorizations: [], connected: false });
      const timeoutId = setTimeout(() => controller.abort(), 1500);
      try {
        const res = await fetch(`${GOVERNANCE_API_BASE_URL}/api/audit/roles/${userRole}/authorizations`, {
          signal: controller.signal
        });
        if (!res.ok) throw new Error('Menu authorization request failed');
        const data: unknown = await res.json();
        if (!isAuthorizationList(data, userRole)) throw new Error('Invalid menu authorization response');
        if (!cancelled) setAuthorizationSnapshot({ role: userRole, status: 'success', authorizations: data, connected: true });
      } catch {
        if (!cancelled) setAuthorizationSnapshot({ role: userRole, status: 'unavailable', authorizations: [], connected: false });
      } finally {
        clearTimeout(timeoutId);
      }
    };

    fetchAuthorizations();
    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [userRole]);

  return (
    <NavContext.Provider value={{
      activeCategory,
      setActiveCategory,
      isCollapsed,
      toggleSidebar,
      userRole,
      setUserRole,
      userAuthorizations: currentSnapshot.authorizations,
      authorizationStatus: currentSnapshot.status,
      isGovernanceConnected: currentSnapshot.connected,
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
