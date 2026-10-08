import { UserRole } from "@/context/NavContext";

/**
 * [Admin 서비스 인터페이스]
 * 사용자 권한 및 시스템 설정을 관리하는 API 호출을 담당합니다.
 */
export interface UserInfo {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  status: 'ACTIVE' | 'PENDING' | 'INACTIVE';
  lastLogin: string;
  dept: string;
  pendingRequestId?: string;
}

export interface ApprovalRequestResult {
  requestId: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  effectiveDate: string | null;
}

class AdminService {
  private mockUsers: UserInfo[] = [
    { id: 1, name: '김재무', email: 'jm.kim@antigrav.ai', role: 'ACCOUNTING_ADMIN', status: 'ACTIVE', lastLogin: '10분 전', dept: '재무회계팀' },
    { id: 2, name: '이리스크', email: 'risk.lee@antigrav.ai', role: 'RISK_MANAGER', status: 'ACTIVE', lastLogin: '2시간 전', dept: '리스크관리부' },
    { id: 3, name: '박기준', email: 'base.park@antigrav.ai', role: 'MASTER_MANAGER', status: 'PENDING', lastLogin: '어제', dept: 'IT운영팀' },
    { id: 4, name: '최감사', email: 'audit.choi@antigrav.ai', role: 'AUDITOR', status: 'ACTIVE', lastLogin: '3일 전', dept: '감사실' },
    { id: 5, name: '정일반', email: 'normal.jung@antigrav.ai', role: 'USER', status: 'INACTIVE', lastLogin: '1달 전', dept: '영업기획팀' },
    { id: 6, name: '관리자', email: 'admin@antigrav.ai', role: 'SYSTEM_ADMIN', status: 'ACTIVE', lastLogin: '방금 전', dept: '시스템관리팀' },
  ];

  /**
   * 전사 사용자 목록 조회
   */
  async getUsers(): Promise<UserInfo[]> {
    try {
      const response = await fetch('/api/admin/users', {
        headers: { 'X-User-ID': 'frontend-admin' },
      });
      if (response.ok) {
        return response.json();
      }
    } catch (err) {
      console.warn('Falling back to mock admin users:', err);
    }

    return new Promise((resolve) => {
      setTimeout(() => resolve(this.mockUsers), 300);
    });
  }

  /**
   * 사용자 역할 변경 승인 요청
   */
  async requestRoleChange(user: UserInfo, role: UserRole): Promise<ApprovalRequestResult> {
    const effectiveDate = new Date().toISOString().slice(0, 10);
    const payload = {
      masterType: 'AUTH_USER_ROLE',
      masterKey: user.email,
      requestType: 'UPDATE',
      payload: JSON.stringify({ username: user.email, userId: String(user.id), role }),
      requestUser: 'frontend-admin',
      effectiveDate,
      requestedVersion: 1,
    };

    try {
      const response = await fetch('/api/audit/approvals/requests', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': 'frontend-admin',
        },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        throw new Error('Role approval request was not accepted');
      }

      const data: unknown = await response.json();
      if (typeof data !== 'object' || data === null || Array.isArray(data)) {
        throw new Error('Invalid role approval response');
      }

      const approval = data as Record<string, unknown>;
      const id = approval.id;
      // The approval API returns its persisted Long id; an uncertain id must never become a local success.
      const validId = typeof id === 'number'
        ? Number.isSafeInteger(id) && id > 0
        : typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id));
      const status = approval.status;
      const validStatus = status === 'PENDING' || status === 'APPROVED' || status === 'REJECTED';
      const responseDate = approval.effectiveDate;
      const validDate = responseDate == null ||
        (typeof responseDate === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(responseDate));
      if (!validId || !validStatus || !validDate) {
        throw new Error('Incomplete role approval response');
      }

      return {
        requestId: String(id),
        status,
        effectiveDate: responseDate ?? null,
      };
    } catch {
      // Transport and response failures have the same safe UI outcome; never expose server details.
      throw new Error('권한 변경 요청 결과를 확인하지 못했습니다. 관리자에게 요청 상태를 확인해 주세요.');
    }
  }

  async requestUserOnboarding(): Promise<ApprovalRequestResult> {
    const effectiveDate = new Date().toISOString().slice(0, 10);
    const requestId = `USER-${Date.now()}`;
    return { requestId, status: 'PENDING', effectiveDate };
  }
}

export const adminService = new AdminService();
