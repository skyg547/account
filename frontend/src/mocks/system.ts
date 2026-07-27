import { UserRole } from '@/context/NavContext';

export interface UserDto {
  userId: string;
  name: string;
  departmentId: string;
  departmentName: string;
  roles: UserRole[];
  status: 'ACTIVE' | 'LOCKED' | 'INACTIVE';
  lastLoginAt: string;
}

export const mockUsers: UserDto[] = [
  { userId: 'USR-001', name: '김회계', departmentId: 'D01', departmentName: '재무기획실', roles: ['ACCOUNTING_ADMIN', 'USER'], status: 'ACTIVE', lastLoginAt: '2026-07-28 08:30:00' },
  { userId: 'USR-002', name: '이위험', departmentId: 'D02', departmentName: '리스크관리부', roles: ['RISK_MANAGER', 'USER'], status: 'ACTIVE', lastLoginAt: '2026-07-27 18:45:12' },
  { userId: 'USR-003', name: '박감사', departmentId: 'D03', departmentName: '내부감사팀', roles: ['AUDITOR'], status: 'ACTIVE', lastLoginAt: '2026-07-25 10:15:00' },
  { userId: 'SYS-001', name: '시스템관리자', departmentId: 'D99', departmentName: 'IT본부', roles: ['SYSTEM_ADMIN'], status: 'ACTIVE', lastLoginAt: '2026-07-28 09:00:00' },
];

export interface DepartmentDto {
  id: string;
  name: string;
  parentId: string | null;
  costCenterCode: string;
}

export const mockDepartments: DepartmentDto[] = [
  { id: 'D00', name: '본사', parentId: null, costCenterCode: 'CC-0000' },
  { id: 'D01', name: '재무기획실', parentId: 'D00', costCenterCode: 'CC-1000' },
  { id: 'D02', name: '리스크관리부', parentId: 'D00', costCenterCode: 'CC-2000' },
  { id: 'D03', name: '내부감사팀', parentId: 'D00', costCenterCode: 'CC-3000' },
  { id: 'D99', name: 'IT본부', parentId: 'D00', costCenterCode: 'CC-9000' },
];

export interface AuditLogDto {
  logId: string;
  timestamp: string;
  actorId: string;
  action: string;
  resource: string;
  status: 'SUCCESS' | 'FAILURE';
  ipAddress: string;
}

export const mockAuditLogs: AuditLogDto[] = [
  { logId: 'AL-1001', timestamp: '2026-07-28 09:15:22', actorId: 'USR-001', action: 'APPROVE_JOURNAL', resource: 'JRN-2026-07-001', status: 'SUCCESS', ipAddress: '192.168.1.10' },
  { logId: 'AL-1002', timestamp: '2026-07-28 09:12:05', actorId: 'USR-002', action: 'UPDATE_ECL_PARAM', resource: 'PARAM-LGD-01', status: 'SUCCESS', ipAddress: '192.168.1.15' },
  { logId: 'AL-1003', timestamp: '2026-07-28 09:05:10', actorId: 'UNKNOWN', action: 'LOGIN_ATTEMPT', resource: 'SYS', status: 'FAILURE', ipAddress: '10.0.0.5' },
];
