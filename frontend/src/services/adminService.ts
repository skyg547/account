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
}

class AdminService {
  // @todo [검수-헥사고날/UI] Mock 데이터는 화면 시연용이다. 운영 흐름은 auth 사용자 API와 governance 권한/승인 API를
  //       분리 호출하고, 변경 요청 ID/승인 상태/감사 로그 링크를 UI 모델에 포함해야 한다.
  // Mock 데이터
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
    // 실제 환경: return fetch('/api/admin/users').then(res => res.json());
    return new Promise((resolve) => {
      setTimeout(() => resolve(this.mockUsers), 300);
    });
  }

  /**
   * 사용자 역할 업데이트
   */
  async updateUserRole(userId: number, role: UserRole): Promise<boolean> {
    // @todo [검수-업무/통제] 역할 변경은 즉시 반영 API가 아니라 SOD 검증과 승인 요청 생성으로 처리해야 한다.
    //       성공 응답도 boolean 대신 requestId/status/effectiveDate를 반환하도록 계약을 재설계해야 한다.
    console.log(`[API] Updating user ${userId} role to ${role}`);
    return new Promise((resolve) => {
      setTimeout(() => {
        const user = this.mockUsers.find(u => u.id === userId);
        if (user) user.role = role;
        resolve(true);
      }, 500);
    });
  }
}

export const adminService = new AdminService();
