export interface ClosingTaskDto {
  id: string;
  title: string;
  category: '수익/비용' | '자산/부채' | '전표마감' | '세무/검증' | '보고서';
  assignee: string;
  dueDate: string;
  status: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING' | 'OVERDUE';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  progress: number;
  dependencies?: string[];
  description?: string;
  completedAt?: string;
}

export interface PeriodDto {
  id: string;
  year: number;
  month: number;
  periodName: string;
  status: 'LOCKED' | 'OPEN' | 'CLOSING_IN_PROGRESS' | 'PENDING_APPROVAL';
  lockedAt?: string;
  lockedBy?: string;
  canReopen: boolean;
  note?: string;
  totalJournals?: number;
  closedJournals?: number;
}

export const mockTasks: ClosingTaskDto[] = [
  {
    id: 'TASK-2026-001',
    title: '미수금 및 미지급금 정산 잔액 검증',
    category: '자산/부채',
    assignee: '김회계 과장 (회계1팀)',
    dueDate: '2026-07-25',
    status: 'COMPLETED',
    priority: 'HIGH',
    progress: 100,
    dependencies: [],
    description: '전 거래처 거래처별 미수금/미지급금 잔액 일치 여부 총계정원장 상호 검증',
    completedAt: '2026-07-24 16:30',
  },
  {
    id: 'TASK-2026-002',
    title: '외화 통화별 기말 평가 배치 실행',
    category: '자산/부채',
    assignee: '이재무 대리 (자금팀)',
    dueDate: '2026-07-26',
    status: 'COMPLETED',
    priority: 'HIGH',
    progress: 100,
    dependencies: ['TASK-2026-001'],
    description: 'USD, EUR, JPY 기말 최초 고시 환율 적용 후 미실현 외화평가손익 계상',
    completedAt: '2026-07-25 11:15',
  },
  {
    id: 'TASK-2026-003',
    title: '감가상각비 계산 및 결산 전표 반영',
    category: '수익/비용',
    assignee: '박자산 대리 (자산관리팀)',
    dueDate: '2026-07-27',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    progress: 75,
    dependencies: [],
    description: '유형자산 및 무형자산 당월 상각비 삼분법 자동 산출 후 결산 조정 전표 발행',
  },
  {
    id: 'TASK-2026-004',
    title: '선급비용 및 미지급비용 기간 안분',
    category: '수익/비용',
    assignee: '최비용 사원 (회계2팀)',
    dueDate: '2026-07-28',
    status: 'IN_PROGRESS',
    priority: 'MEDIUM',
    progress: 40,
    dependencies: [],
    description: '보험료, 임차료, 소프트웨어 구독료 기간 경과분 안분 전표 생성',
  },
  {
    id: 'TASK-2026-005',
    title: '임시계정 (가지급금/가수금) 정산',
    category: '전표마감',
    assignee: '김회계 과장 (회계1팀)',
    dueDate: '2026-07-28',
    status: 'PENDING',
    priority: 'HIGH',
    progress: 10,
    dependencies: ['TASK-2026-001'],
    description: '미정산 임시 계정 잔액 0원 일치 확인 및 관련 증빙 첨부',
  },
  {
    id: 'TASK-2026-006',
    title: '법인세 추산액 산출 및 당기순이익 가결산',
    category: '세무/검증',
    assignee: '정세무 차장 (세무팀)',
    dueDate: '2026-07-29',
    status: 'PENDING',
    priority: 'HIGH',
    progress: 0,
    dependencies: ['TASK-2026-003', 'TASK-2026-004'],
    description: '세무조정 대상 품목 가산 및 유보 항목 반영 후 당월 산출 법인세 계상',
  },
  {
    id: 'TASK-2026-007',
    title: '월간 재무제표 (BS/IS) 최종 대사 및 검증',
    category: '보고서',
    assignee: '강팀장 (회계팀)',
    dueDate: '2026-07-30',
    status: 'PENDING',
    priority: 'HIGH',
    progress: 0,
    dependencies: ['TASK-2026-006'],
    description: '재무상태표 및 손익계산서 밸런스 검증 및 경영진 보고용 패키지 마감',
  },
  {
    id: 'TASK-2026-008',
    title: '부가가치세 매입/매출 전표 차액 검증',
    category: '세무/검증',
    assignee: '정세무 차장 (세무팀)',
    dueDate: '2026-07-26',
    status: 'OVERDUE',
    priority: 'MEDIUM',
    progress: 60,
    dependencies: [],
    description: '홈택스 세금계산서 합계표와 회계시스템 전표 간 10원 이상 차액 내역 조치',
  },
];

export const mockPeriods: PeriodDto[] = [
  {
    id: 'PER-2026-07',
    year: 2026,
    month: 7,
    periodName: '2026년 07월',
    status: 'CLOSING_IN_PROGRESS',
    canReopen: false,
    note: '당월 정기 결산 진행 중 (공정율 62%)',
    totalJournals: 1420,
    closedJournals: 950,
  },
  {
    id: 'PER-2026-06',
    year: 2026,
    month: 6,
    periodName: '2026년 06월',
    status: 'LOCKED',
    lockedAt: '2026-07-05 18:00',
    lockedBy: '강팀장 (회계팀)',
    canReopen: true,
    note: '6월 정기 결산 승인 마감 완료',
    totalJournals: 1850,
    closedJournals: 1850,
  },
  {
    id: 'PER-2026-05',
    year: 2026,
    month: 5,
    periodName: '2026년 05월',
    status: 'LOCKED',
    lockedAt: '2026-06-04 17:30',
    lockedBy: '강팀장 (회계팀)',
    canReopen: true,
    note: '5월 정기 결산 및 감사 승인 완료',
    totalJournals: 1690,
    closedJournals: 1690,
  },
  {
    id: 'PER-2026-04',
    year: 2026,
    month: 4,
    periodName: '2026년 04월',
    status: 'LOCKED',
    lockedAt: '2026-05-06 19:10',
    lockedBy: '강팀장 (회계팀)',
    canReopen: true,
    note: '4월 정기 결산 확정',
    totalJournals: 1540,
    closedJournals: 1540,
  },
  {
    id: 'PER-2026-03',
    year: 2026,
    month: 3,
    periodName: '2026년 03월 (1분기)',
    status: 'LOCKED',
    lockedAt: '2026-04-08 20:00',
    lockedBy: '최CFO (경영지원본부)',
    canReopen: false,
    note: '1분기 분기 재무제표 확정 및 외감 완료 (잠금 해제 불가)',
    totalJournals: 2100,
    closedJournals: 2100,
  },
  {
    id: 'PER-2026-08',
    year: 2026,
    month: 8,
    periodName: '2026년 08월',
    status: 'OPEN',
    canReopen: false,
    note: '미래 회계 기간 (일반 전표 입력 가능)',
    totalJournals: 120,
    closedJournals: 0,
  },
  {
    id: 'PER-2026-09',
    year: 2026,
    month: 9,
    periodName: '2026년 09월',
    status: 'OPEN',
    canReopen: false,
    note: '미래 회계 기간',
    totalJournals: 0,
    closedJournals: 0,
  },
];
