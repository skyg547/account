export type TaskCategory = 'PRE_CLOSING' | 'CLOSING_ENTRY' | 'POST_CLOSING' | 'REPORTING';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'SKIPPED';

export interface ClosingTaskDto {
  id: number;
  name: string;
  category: TaskCategory;
  status: TaskStatus;
  assignedTo: string;
  dueDate: string;
  isMandatory: boolean;
  errorMessage?: string;
}

export interface ValuationBatchRequestDto {
  fiscalPeriodId: number;
  valuationType: 'FX_RATE' | 'FINANCIAL_INSTRUMENT';
  runBy: string;
}

export interface ProvisionBatchRequestDto {
  fiscalPeriodId: number;
  provisionType: 'ECL' | 'BAD_DEBT' | 'IMPAIRMENT';
  runBy: string;
}

class ClosingService {
  /**
   * 결산 태스크 목록 조회 (API가 없을 경우 Mock 반환)
   */
  async getTasks(calendarId: number): Promise<ClosingTaskDto[]> {
    try {
      const response = await fetch(`/api/closing/calendars/${calendarId}/tasks`, {
        method: 'GET',
        headers: { 'X-User-ID': 'frontend-admin' },
      });
      if (response.ok) {
        return response.json();
      }
    } catch (err) {
      console.warn('Falling back to mock closing tasks:', err);
    }
    return getMockTasks();
  }

  /**
   * 결산 태스크 상태 업데이트
   */
  async updateTaskStatus(taskId: number, status: TaskStatus, user: string): Promise<ClosingTaskDto | null> {
    try {
      const response = await fetch(`/api/closing/tasks/${taskId}/status`, {
        method: 'PUT',
        headers: { 
          'Content-Type': 'application/json',
          'X-User-ID': user 
        },
        body: JSON.stringify({ status, user })
      });
      if (response.ok) {
        return response.json();
      }
    } catch (err) {
      console.error('Failed to update task status:', err);
    }
    return null;
  }

  /**
   * 외화 평가(FX Valuation) 배치 실행
   */
  async runValuationBatch(request: ValuationBatchRequestDto): Promise<boolean> {
    try {
      const response = await fetch('/api/closing/valuation-batches/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return response.ok;
    } catch (err) {
      console.error('Valuation batch run failed:', err);
      return false;
    }
  }

  /**
   * 충당/손상(ECL) 배치 실행
   */
  async runProvisionBatch(request: ProvisionBatchRequestDto): Promise<boolean> {
    try {
      const response = await fetch('/api/closing/provision-batches/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return response.ok;
    } catch (err) {
      console.error('Provision batch run failed:', err);
      return false;
    }
  }
}

function getMockTasks(): ClosingTaskDto[] {
  return [
    { id: 1, name: '전표 마감 및 대조', category: 'PRE_CLOSING', status: 'COMPLETED', assignedTo: '회계팀', dueDate: '2026-04-20', isMandatory: true },
    { id: 2, name: '은행 잔액 대조 (Reconciliation)', category: 'PRE_CLOSING', status: 'COMPLETED', assignedTo: '자금팀', dueDate: '2026-04-21', isMandatory: true },
    { id: 3, name: '외화 평가(FX Valuation) 배치', category: 'CLOSING_ENTRY', status: 'COMPLETED', assignedTo: '자금팀', dueDate: '2026-04-22', isMandatory: true },
    { id: 4, name: '감가상각비 계상', category: 'CLOSING_ENTRY', status: 'IN_PROGRESS', assignedTo: '고정자산팀', dueDate: '2026-04-23', isMandatory: true },
    { id: 5, name: '기대신용손실(ECL) 충당금 산출 배치', category: 'CLOSING_ENTRY', status: 'FAILED', assignedTo: '리스크관리팀', dueDate: '2026-04-23', isMandatory: true, errorMessage: '대출채권 잔액 데이터 수신 지연으로 인한 타임아웃' },
    { id: 6, name: '이익잉여금 처분 계산', category: 'POST_CLOSING', status: 'PENDING', assignedTo: '재무기획팀', dueDate: '2026-04-24', isMandatory: true },
    { id: 7, name: '표준 재무제표 확정', category: 'REPORTING', status: 'PENDING', assignedTo: 'CFO', dueDate: '2026-04-25', isMandatory: true },
  ];
}

export const closingService = new ClosingService();