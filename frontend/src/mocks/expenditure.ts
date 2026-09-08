export interface ResolutionLineItem {
  id: string;
  accountCode: string;
  accountName: string;
  description: string;
  amount: number;
  taxAmount: number;
  costCenter: string;
  remark?: string;
}

export interface ApprovalStep {
  step: number;
  approverId: string;
  approverName: string;
  position: string;
  status: 'APPROVED' | 'REJECTED' | 'PENDING' | 'WAITING';
  comment?: string;
  approvedAt?: string;
}

export interface ResolutionDto {
  id: string;
  resolutionNo: string;
  title: string;
  writerId: string;
  writerName: string;
  department: string;
  requestDate: string;
  dueDate: string;
  totalAmount: number;
  totalTax: number;
  status: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'PAID';
  paymentMethod: 'BANK_TRANSFER' | 'CORPORATE_CARD' | 'CASH';
  vendorName: string;
  vendorBank?: string;
  vendorAccount?: string;
  lineItems: ResolutionLineItem[];
  approvalLine: ApprovalStep[];
  remarks?: string;
}

export interface PayableDto {
  id: string;
  apNumber: string;
  vendorId: string;
  vendorName: string;
  invoiceNumber: string;
  issueDate: string;
  dueDate: string;
  amount: number;
  paidAmount: number;
  balance: number;
  status: 'UNPAID' | 'PARTIAL' | 'OVERDUE' | 'COMPLETED';
  agingDays: number;
  agingCategory: 'CURRENT' | '1-30' | '31-60' | '61-90' | '90+';
  description: string;
}

export interface ReceivableDto {
  id: string;
  arNumber: string;
  customerId: string;
  customerName: string;
  invoiceNumber: string;
  issueDate: string;
  dueDate: string;
  amount: number;
  collectedAmount: number;
  balance: number;
  status: 'UNCOLLECTED' | 'PARTIAL' | 'OVERDUE' | 'COMPLETED';
  agingDays: number;
  agingCategory: 'CURRENT' | '1-30' | '31-60' | '61-90' | '90+';
  description: string;
}

export interface BudgetItem {
  id: string;
  costCenter: string;
  department: string;
  accountCode: string;
  accountName: string;
  annualBudget: number;
  allocatedBudget: number;
  usedAmount: number;
  encumberedAmount: number;
  remainingAmount: number;
  utilizationRate: number;
  status: 'NORMAL' | 'WARNING' | 'EXCEEDED';
}

export interface CashflowItem {
  id: string;
  date: string;
  period: string;
  type: 'INFLOW' | 'OUTFLOW';
  category: string;
  description: string;
  amount: number;
  status: 'PLANNED' | 'CONFIRMED' | 'EXECUTED';
  sourceModule: 'AR' | 'AP' | 'LOAN' | 'TAX' | 'MANUAL';
  counterparty: string;
}

// ----------------------------------------------------
// Mock Data Exports
// ----------------------------------------------------

export const mockResolutions: ResolutionDto[] = [
  {
    id: 'res-101',
    resolutionNo: 'EXP-2026-0701',
    title: '클라우드 인프라 서버 확충 및 소프트웨어 라이선스 결제',
    writerId: 'emp-012',
    writerName: '김민준',
    department: 'IT개발팀',
    requestDate: '2026-07-20',
    dueDate: '2026-07-31',
    totalAmount: 18500000,
    totalTax: 1850000,
    status: 'PENDING',
    paymentMethod: 'BANK_TRANSFER',
    vendorName: '(주)메가존클라우드',
    vendorBank: '신한은행',
    vendorAccount: '110-384-992019',
    remarks: '3분기 데이터센터 워크로드 증가에 따른 고성능 인스턴스 증설 건',
    lineItems: [
      {
        id: 'line-1',
        accountCode: '51400',
        accountName: '지급임차료/서버사용료',
        description: 'AWS Cloud EC2/RDS 7월분 사용료',
        amount: 14000000,
        taxAmount: 1400000,
        costCenter: 'CC-101 (IT실)',
        remark: 'RI 할인 적용'
      },
      {
        id: 'line-2',
        accountCode: '51500',
        accountName: '소프트웨어구독료',
        description: 'GitHub Enterprise & Slack 100 User 갱신',
        amount: 4500000,
        taxAmount: 450000,
        costCenter: 'CC-101 (IT실)',
        remark: '연간 계약 갱신'
      }
    ],
    approvalLine: [
      { step: 1, approverId: 'emp-012', approverName: '김민준', position: '대리', status: 'APPROVED', approvedAt: '2026-07-20 10:00' },
      { step: 2, approverId: 'emp-005', approverName: '박서준', position: '팀장', status: 'APPROVED', approvedAt: '2026-07-20 14:30' },
      { step: 3, approverId: 'emp-002', approverName: '이현우', position: '재무이사', status: 'PENDING', comment: '예산 승인 검토 중' },
      { step: 4, approverId: 'emp-001', approverName: '정하은', position: '대표이사', status: 'WAITING' }
    ]
  },
  {
    id: 'res-102',
    resolutionNo: 'EXP-2026-0702',
    title: '2026 하반기 글로벌 파트너십 컨퍼런스 대관 및 행사 비용',
    writerId: 'emp-034',
    writerName: '이지은',
    department: '마케팅팀',
    requestDate: '2026-07-22',
    dueDate: '2026-08-05',
    totalAmount: 32000000,
    totalTax: 3200000,
    status: 'PENDING',
    paymentMethod: 'BANK_TRANSFER',
    vendorName: '(주)파르나스호텔',
    vendorBank: '하나은행',
    vendorAccount: '298-910034-11004',
    remarks: '초청 VIP 200명 대상 세미나 및 뷔페 오찬 포함',
    lineItems: [
      {
        id: 'line-3',
        accountCode: '52000',
        accountName: '광고선전비',
        description: '그랜드볼룸 대관 및 음향 장비 대여',
        amount: 22000000,
        taxAmount: 2200000,
        costCenter: 'CC-202 (마케팅본부)',
      },
      {
        id: 'line-4',
        accountCode: '51300',
        accountName: '접대비',
        description: '컨퍼런스 참석자 오찬 뷔페 200인분',
        amount: 10000000,
        taxAmount: 1000000,
        costCenter: 'CC-202 (마케팅본부)',
      }
    ],
    approvalLine: [
      { step: 1, approverId: 'emp-034', approverName: '이지은', position: '과장', status: 'APPROVED', approvedAt: '2026-07-22 11:15' },
      { step: 2, approverId: 'emp-008', approverName: '최동현', position: '마케팅이사', status: 'APPROVED', approvedAt: '2026-07-22 16:20' },
      { step: 3, approverId: 'emp-002', approverName: '이현우', position: '재무이사', status: 'PENDING' },
      { step: 4, approverId: 'emp-001', approverName: '정하은', position: '대표이사', status: 'WAITING' }
    ]
  },
  {
    id: 'res-103',
    resolutionNo: 'EXP-2026-0688',
    title: '전사 사무용 PC 및 데스크톱 모니터 일체 구입',
    writerId: 'emp-055',
    writerName: '윤아름',
    department: '총무팀',
    requestDate: '2026-07-15',
    dueDate: '2026-07-25',
    totalAmount: 12500000,
    totalTax: 1250000,
    status: 'APPROVED',
    paymentMethod: 'CORPORATE_CARD',
    vendorName: '델테크놀로지스코리아',
    vendorBank: '국민은행',
    vendorAccount: '817-21-0922-811',
    remarks: '신규 입사자 10명분 XPS 랩톱 및 4K 모니터 구입',
    lineItems: [
      {
        id: 'line-5',
        accountCode: '21100',
        accountName: '비품(자산)',
        description: 'Dell XPS 15 랩톱 10대',
        amount: 10000000,
        taxAmount: 1000000,
        costCenter: 'CC-301 (경영지원실)',
      },
      {
        id: 'line-6',
        accountCode: '21100',
        accountName: '비품(자산)',
        description: 'Dell 27인치 4K 모니터 10대',
        amount: 2500000,
        taxAmount: 250000,
        costCenter: 'CC-301 (경영지원실)',
      }
    ],
    approvalLine: [
      { step: 1, approverId: 'emp-055', approverName: '윤아름', position: '대리', status: 'APPROVED', approvedAt: '2026-07-15 09:30' },
      { step: 2, approverId: 'emp-010', approverName: '한성호', position: '총무팀장', status: 'APPROVED', approvedAt: '2026-07-15 13:00' },
      { step: 3, approverId: 'emp-002', approverName: '이현우', position: '재무이사', status: 'APPROVED', approvedAt: '2026-07-16 10:45', comment: '자산 등록 후 집행' }
    ]
  },
  {
    id: 'res-104',
    resolutionNo: 'EXP-2026-0670',
    title: '본사 사옥 3층 사무실 환경 개선 및 인테리어 보수작업',
    writerId: 'emp-055',
    writerName: '윤아름',
    department: '총무팀',
    requestDate: '2026-07-10',
    dueDate: '2026-07-20',
    totalAmount: 8800000,
    totalTax: 880000,
    status: 'PAID',
    paymentMethod: 'BANK_TRANSFER',
    vendorName: '(주)한샘인테리어',
    vendorBank: '기업은행',
    vendorAccount: '054-0821-445',
    remarks: '지급 완료 (전표번호 JP-2026-07-0099)',
    lineItems: [
      {
        id: 'line-7',
        accountCode: '52200',
        accountName: '수선비',
        description: '파티션 설치 및 벽면 도장 작업',
        amount: 8800000,
        taxAmount: 880000,
        costCenter: 'CC-301 (경영지원실)',
      }
    ],
    approvalLine: [
      { step: 1, approverId: 'emp-055', approverName: '윤아름', position: '대리', status: 'APPROVED', approvedAt: '2026-07-10 14:00' },
      { step: 2, approverId: 'emp-010', approverName: '한성호', position: '총무팀장', status: 'APPROVED', approvedAt: '2026-07-10 17:10' },
      { step: 3, approverId: 'emp-002', approverName: '이현우', position: '재무이사', status: 'APPROVED', approvedAt: '2026-07-11 09:15' }
    ]
  },
  {
    id: 'res-105',
    resolutionNo: 'EXP-2026-0650',
    title: '해외 출장 항공권 및 숙박비용 사전 청구',
    writerId: 'emp-089',
    writerName: '강태양',
    department: '해외영업팀',
    requestDate: '2026-07-08',
    dueDate: '2026-07-18',
    totalAmount: 6400000,
    totalTax: 0,
    status: 'REJECTED',
    paymentMethod: 'BANK_TRANSFER',
    vendorName: '하나투어 기업출장센터',
    vendorBank: '우리은행',
    vendorAccount: '1002-883-120491',
    remarks: '미국 세안 솔루션 박람회 참석 목적',
    lineItems: [
      {
        id: 'line-8',
        accountCode: '51200',
        accountName: '여비교통비',
        description: '인천-SF 왕복 비즈니스석 및 4박 숙박비',
        amount: 6400000,
        taxAmount: 0,
        costCenter: 'CC-401 (영업본부)',
      }
    ],
    approvalLine: [
      { step: 1, approverId: 'emp-089', approverName: '강태양', position: '과장', status: 'APPROVED', approvedAt: '2026-07-08 11:00' },
      { step: 2, approverId: 'emp-004', approverName: '송지호', position: '영업본부장', status: 'REJECTED', approvedAt: '2026-07-08 15:40', comment: '출장 일정 재조정 필요 (이코노미 규정 확인)' }
    ]
  },
  {
    id: 'res-106',
    resolutionNo: 'EXP-2026-0705',
    title: '법률 자문 및 세무 회계 감사 사전 수수료',
    writerId: 'emp-002',
    writerName: '이현우',
    department: '재무팀',
    requestDate: '2026-07-25',
    dueDate: '2026-08-10',
    totalAmount: 15000000,
    totalTax: 1500000,
    status: 'DRAFT',
    paymentMethod: 'BANK_TRANSFER',
    vendorName: '법무법인 세종',
    vendorBank: '신한은행',
    vendorAccount: '140-009-88123',
    remarks: 'M&A 관련 법률 자문 1차 계약금',
    lineItems: [
      {
        id: 'line-9',
        accountCode: '51600',
        accountName: '지급수수료',
        description: '법률 리스크 검토 및 법률 자문료',
        amount: 15000000,
        taxAmount: 1500000,
        costCenter: 'CC-301 (경영지원실)',
      }
    ],
    approvalLine: [
      { step: 1, approverId: 'emp-002', approverName: '이현우', position: '재무이사', status: 'PENDING' },
      { step: 2, approverId: 'emp-001', approverName: '정하은', position: '대표이사', status: 'WAITING' }
    ]
  }
];

export const mockPayables: PayableDto[] = [
  {
    id: 'ap-001',
    apNumber: 'AP-2026-0701',
    vendorId: 'VND-1001',
    vendorName: '(주)삼성SDS',
    invoiceNumber: 'INV-2026-00912',
    issueDate: '2026-06-15',
    dueDate: '2026-07-15',
    amount: 45000000,
    paidAmount: 0,
    balance: 45000000,
    status: 'OVERDUE',
    agingDays: 13,
    agingCategory: '1-30',
    description: 'ERP 연동 전용선 및 서버 관리비 (6월분)'
  },
  {
    id: 'ap-002',
    apNumber: 'AP-2026-0702',
    vendorId: 'VND-1002',
    vendorName: '(주)메가존클라우드',
    invoiceNumber: 'INV-2026-01124',
    issueDate: '2026-07-01',
    dueDate: '2026-07-31',
    amount: 20350000,
    paidAmount: 5000000,
    balance: 15350000,
    status: 'PARTIAL',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: 'AWS 클라우드 인프라 이용 수수료'
  },
  {
    id: 'ap-003',
    apNumber: 'AP-2026-0601',
    vendorId: 'VND-1003',
    vendorName: '오라클코리아 유한회사',
    invoiceNumber: 'INV-2026-00445',
    issueDate: '2026-05-10',
    dueDate: '2026-06-10',
    amount: 38000000,
    paidAmount: 0,
    balance: 38000000,
    status: 'OVERDUE',
    agingDays: 48,
    agingCategory: '31-60',
    description: 'Database DBMS 연간 라이선스 유지보수'
  },
  {
    id: 'ap-004',
    apNumber: 'AP-2026-0501',
    vendorId: 'VND-1004',
    vendorName: '(주)한샘인테리어',
    invoiceNumber: 'INV-2026-00210',
    issueDate: '2026-04-05',
    dueDate: '2026-05-05',
    amount: 18000000,
    paidAmount: 0,
    balance: 18000000,
    status: 'OVERDUE',
    agingDays: 84,
    agingCategory: '61-90',
    description: '신규 스마트 오피스 분할 집구 납품'
  },
  {
    id: 'ap-005',
    apNumber: 'AP-2026-0301',
    vendorId: 'VND-1005',
    vendorName: '(주)글로벌물류솔루션',
    invoiceNumber: 'INV-2026-00088',
    issueDate: '2026-03-01',
    dueDate: '2026-03-31',
    amount: 12000000,
    paidAmount: 0,
    balance: 12000000,
    status: 'OVERDUE',
    agingDays: 119,
    agingCategory: '90+',
    description: '1분기 해외 물류 및 포워딩 정산 잔액'
  },
  {
    id: 'ap-006',
    apNumber: 'AP-2026-0703',
    vendorId: 'VND-1006',
    vendorName: '델테크놀로지스코리아',
    invoiceNumber: 'INV-2026-01550',
    issueDate: '2026-07-10',
    dueDate: '2026-08-10',
    amount: 13750000,
    paidAmount: 0,
    balance: 13750000,
    status: 'UNPAID',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: '업무용 PC 10대 구매 납품서'
  },
  {
    id: 'ap-007',
    apNumber: 'AP-2026-0704',
    vendorId: 'VND-1007',
    vendorName: '삼일회계법인',
    invoiceNumber: 'INV-2026-01820',
    issueDate: '2026-07-15',
    dueDate: '2026-08-15',
    amount: 27500000,
    paidAmount: 0,
    balance: 27500000,
    status: 'UNPAID',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: '2분기 중간회계감사 및 세무조정 착수금'
  },
  {
    id: 'ap-008',
    apNumber: 'AP-2026-0705',
    vendorId: 'VND-1008',
    vendorName: '(주)SK텔링크',
    invoiceNumber: 'INV-2026-01990',
    issueDate: '2026-07-20',
    dueDate: '2026-08-20',
    amount: 4200000,
    paidAmount: 4200000,
    balance: 0,
    status: 'COMPLETED',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: '전사 국제전화 및 SMS 발송 시스템 이용료'
  }
];

export const mockReceivables: ReceivableDto[] = [
  {
    id: 'ar-001',
    arNumber: 'AR-2026-0701',
    customerId: 'CUST-2001',
    customerName: '(주)카카오뱅크',
    invoiceNumber: 'TX-2026-0881',
    issueDate: '2026-06-20',
    dueDate: '2026-07-20',
    amount: 88000000,
    collectedAmount: 0,
    balance: 88000000,
    status: 'OVERDUE',
    agingDays: 8,
    agingCategory: '1-30',
    description: '금융 솔루션 구축 2차 중도금'
  },
  {
    id: 'ar-002',
    arNumber: 'AR-2026-0702',
    customerId: 'CUST-2002',
    customerName: '(주)엔씨소프트',
    invoiceNumber: 'TX-2026-0912',
    issueDate: '2026-07-05',
    dueDate: '2026-08-05',
    amount: 55000000,
    collectedAmount: 20000000,
    balance: 35000000,
    status: 'PARTIAL',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: 'AI 보안 엔진 연동 라이선스 청구'
  },
  {
    id: 'ar-003',
    arNumber: 'AR-2026-0510',
    customerId: 'CUST-2003',
    customerName: '(주)쿠팡',
    invoiceNumber: 'TX-2026-0520',
    issueDate: '2026-05-01',
    dueDate: '2026-06-01',
    amount: 62000000,
    collectedAmount: 0,
    balance: 62000000,
    status: 'OVERDUE',
    agingDays: 57,
    agingCategory: '31-60',
    description: '물류 자동화 플랫폼 데이터 컨설팅'
  },
  {
    id: 'ar-004',
    arNumber: 'AR-2026-0415',
    customerId: 'CUST-2004',
    customerName: '(주)토스인슈어런스',
    invoiceNumber: 'TX-2026-0390',
    issueDate: '2026-04-10',
    dueDate: '2026-05-10',
    amount: 24000000,
    collectedAmount: 0,
    balance: 24000000,
    status: 'OVERDUE',
    agingDays: 79,
    agingCategory: '61-90',
    description: '보험 청구 API 연동 개발비'
  },
  {
    id: 'ar-005',
    arNumber: 'AR-2026-0201',
    customerId: 'CUST-2005',
    customerName: '(주)스타트업네트웍스',
    invoiceNumber: 'TX-2026-0110',
    issueDate: '2026-02-15',
    dueDate: '2026-03-15',
    amount: 15000000,
    collectedAmount: 0,
    balance: 15000000,
    status: 'OVERDUE',
    agingDays: 135,
    agingCategory: '90+',
    description: '초기 세팅비 및 서버 호스팅 연간계약'
  },
  {
    id: 'ar-006',
    arNumber: 'AR-2026-0703',
    customerId: 'CUST-2006',
    customerName: '현대자동차(주)',
    invoiceNumber: 'TX-2026-0980',
    issueDate: '2026-07-15',
    dueDate: '2026-08-15',
    amount: 120000000,
    collectedAmount: 0,
    balance: 120000000,
    status: 'UNCOLLECTED',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: '스마트 팩토리 IoT 센서 통합 제어 프로그램'
  },
  {
    id: 'ar-007',
    arNumber: 'AR-2026-0704',
    customerId: 'CUST-2007',
    customerName: '(주)LG유플러스',
    invoiceNumber: 'TX-2026-1010',
    issueDate: '2026-07-22',
    dueDate: '2026-08-22',
    amount: 43000000,
    collectedAmount: 43000000,
    balance: 0,
    status: 'COMPLETED',
    agingDays: 0,
    agingCategory: 'CURRENT',
    description: '5G 네트워크 트래픽 모니터링 모듈 공급'
  }
];

export const mockBudget: BudgetItem[] = [
  {
    id: 'bg-001',
    costCenter: 'CC-101',
    department: 'IT개발실',
    accountCode: '51400',
    accountName: '지급임차료/서버사용료',
    annualBudget: 240000000,
    allocatedBudget: 140000000,
    usedAmount: 118000000,
    encumberedAmount: 15400000,
    remainingAmount: 6600000,
    utilizationRate: 95.2,
    status: 'WARNING'
  },
  {
    id: 'bg-002',
    costCenter: 'CC-101',
    department: 'IT개발실',
    accountCode: '51500',
    accountName: '소프트웨어구독료',
    annualBudget: 60000000,
    allocatedBudget: 35000000,
    usedAmount: 28000000,
    encumberedAmount: 4950000,
    remainingAmount: 2050000,
    utilizationRate: 94.1,
    status: 'WARNING'
  },
  {
    id: 'bg-003',
    costCenter: 'CC-202',
    department: '마케팅본부',
    accountCode: '52000',
    accountName: '광고선전비',
    annualBudget: 500000000,
    allocatedBudget: 300000000,
    usedAmount: 265000000,
    encumberedAmount: 42000000,
    remainingAmount: -7000000,
    utilizationRate: 102.3,
    status: 'EXCEEDED'
  },
  {
    id: 'bg-004',
    costCenter: 'CC-202',
    department: '마케팅본부',
    accountCode: '51300',
    accountName: '접대비',
    annualBudget: 80000000,
    allocatedBudget: 45000000,
    usedAmount: 31000000,
    encumberedAmount: 11000000,
    remainingAmount: 3000000,
    utilizationRate: 93.3,
    status: 'NORMAL'
  },
  {
    id: 'bg-005',
    costCenter: 'CC-301',
    department: '경영지원실',
    accountCode: '21100',
    accountName: '비품구입비(자산)',
    annualBudget: 120000000,
    allocatedBudget: 70000000,
    usedAmount: 42000000,
    encumberedAmount: 13750000,
    remainingAmount: 14250000,
    utilizationRate: 79.6,
    status: 'NORMAL'
  },
  {
    id: 'bg-006',
    costCenter: 'CC-301',
    department: '경영지원실',
    accountCode: '51600',
    accountName: '지급수수료',
    annualBudget: 150000000,
    allocatedBudget: 90000000,
    usedAmount: 64000000,
    encumberedAmount: 16500000,
    remainingAmount: 9500000,
    utilizationRate: 89.4,
    status: 'NORMAL'
  },
  {
    id: 'bg-007',
    costCenter: 'CC-401',
    department: '영업본부',
    accountCode: '51200',
    accountName: '여비교통비',
    annualBudget: 180000000,
    allocatedBudget: 100000000,
    usedAmount: 68000000,
    encumberedAmount: 7040000,
    remainingAmount: 24960000,
    utilizationRate: 75.0,
    status: 'NORMAL'
  }
];

export const mockCashflow: {
  summary: {
    beginningBalance: number;
    expectedInflow: number;
    expectedOutflow: number;
    netCashflow: number;
    endingBalance: number;
  };
  weeklyTrends: Array<{ week: string; inflow: number; outflow: number; net: number }>;
  items: CashflowItem[];
} = {
  summary: {
    beginningBalance: 1450000000,
    expectedInflow: 387000000,
    expectedOutflow: 268600000,
    netCashflow: 118400000,
    endingBalance: 1568400000
  },
  weeklyTrends: [
    { week: '7월 1주', inflow: 95000000, outflow: 62000000, net: 33000000 },
    { week: '7월 2주', inflow: 110000000, outflow: 85000000, net: 25000000 },
    { week: '7월 3주', inflow: 78000000, outflow: 41000000, net: 37000000 },
    { week: '7월 4주', inflow: 104000000, outflow: 80600000, net: 23400000 },
    { week: '8월 1주(예상)', inflow: 125000000, outflow: 92000000, net: 33000000 },
    { week: '8월 2주(예상)', inflow: 88000000, outflow: 55000000, net: 33000000 }
  ],
  items: [
    {
      id: 'cf-001',
      date: '2026-07-28',
      period: '2026-W30',
      type: 'INFLOW',
      category: '매출채권 회수',
      description: '(주)엔씨소프트 라이선스 1차 입금',
      amount: 20000000,
      status: 'EXECUTED',
      sourceModule: 'AR',
      counterparty: '(주)엔씨소프트'
    },
    {
      id: 'cf-002',
      date: '2026-07-31',
      period: '2026-W30',
      type: 'OUTFLOW',
      category: '매입채무 지급',
      description: '(주)메가존클라우드 7월 AWS 이용료',
      amount: 15350000,
      status: 'CONFIRMED',
      sourceModule: 'AP',
      counterparty: '(주)메가존클라우드'
    },
    {
      id: 'cf-003',
      date: '2026-08-05',
      period: '2026-W31',
      type: 'INFLOW',
      category: '매출채권 회수 예정',
      description: '(주)엔씨소프트 잔액 입금 예정',
      amount: 35000000,
      status: 'PLANNED',
      sourceModule: 'AR',
      counterparty: '(주)엔씨소프트'
    },
    {
      id: 'cf-004',
      date: '2026-08-10',
      period: '2026-W31',
      type: 'OUTFLOW',
      category: '매입채무 지급 예정',
      description: '델테크놀로지스 PC 납품 대금',
      amount: 13750000,
      status: 'PLANNED',
      sourceModule: 'AP',
      counterparty: '델테크놀로지스코리아'
    },
    {
      id: 'cf-005',
      date: '2026-08-15',
      period: '2026-W32',
      type: 'INFLOW',
      category: '매출채권 회수 예정',
      description: '현대자동차(주) IoT 센서 솔루션 입금',
      amount: 120000000,
      status: 'PLANNED',
      sourceModule: 'AR',
      counterparty: '현대자동차(주)'
    },
    {
      id: 'cf-006',
      date: '2026-08-25',
      period: '2026-W33',
      type: 'OUTFLOW',
      category: '인건비 및 급여',
      description: '8월 전사 정기 급여 지급',
      amount: 185000000,
      status: 'PLANNED',
      sourceModule: 'MANUAL',
      counterparty: '전 임직원'
    }
  ]
};
