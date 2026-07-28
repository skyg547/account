export interface LoanContractDto {
  id: string;
  contractNo: string;
  borrowerName: string;
  borrowerId: string;
  productName: string;
  principalAmount: number;
  nominalRate: number; // %
  effectiveRate: number; // EIR %
  startDate: string;
  maturityDate: string;
  termMonths: number;
  repaymentType: 'BULLET' | 'EQUAL_PRINCIPAL' | 'EQUAL_INSTALLMENT';
  status: 'ACTIVE' | 'PENDING_DISBURSAL' | 'COMPLETED' | 'DEFAULTED' | 'RESTRUCTURED';
  disbursedAmount: number;
  remainingBalance: number;
  deferredFee: number;
  deferredCost: number;
  collateralType?: string;
  currency: string;
}

export interface AmortizationScheduleDto {
  scheduleId: string;
  contractId: string;
  period: number;
  dueDate: string;
  beginningBalance: number;
  scheduledPayment: number;
  principalComponent: number;
  interestComponent: number;
  effectiveInterest: number;
  feeAmortization: number;
  costAmortization: number;
  endingBalance: number;
  status: 'PAID' | 'SCHEDULED' | 'OVERDUE';
}

export interface DisbursalDto {
  disbursalId: string;
  contractId: string;
  contractNo: string;
  borrowerName: string;
  requestedAmount: number;
  disbursedAmount: number;
  bankName: string;
  accountNumber: string;
  requestDate: string;
  disbursalDate: string;
  status: 'PENDING' | 'APPROVED' | 'DISBURSED' | 'REJECTED';
  approvedBy: string;
}

export interface DeferredFeeCostDto {
  id: string;
  contractId: string;
  contractNo: string;
  borrowerName: string;
  type: 'FEE' | 'COST';
  category: string;
  amount: number;
  amortizedAmount: number;
  unamortizedBalance: number;
  amortizationMethod: 'EIR' | 'STRAIGHT_LINE';
  startDate: string;
  endDate: string;
  status: 'AMORTIZING' | 'FULLY_AMORTIZED';
}

export interface LoanEventDto {
  eventId: string;
  contractId: string;
  contractNo: string;
  borrowerName: string;
  eventType: 'EARLY_REPAYMENT' | 'RATE_CHANGE' | 'RESTRUCTURING' | 'MATURITY_EXTENSION';
  eventDate: string;
  effectiveDate: string;
  details: string;
  impactOnEir: number; // Delta %
  recalculationStatus: 'PENDING' | 'COMPLETED' | 'FAILED';
  processedBy: string;
}

export interface DepositAccountDto {
  accountId: string;
  accountNumber: string;
  accountName: string;
  bankName: string;
  depositType: 'TIME_DEPOSIT' | 'SAVINGS' | 'MONEY_MARKET' | 'NOTICE_DEPOSIT';
  principal: number;
  currentBalance: number;
  interestRate: number;
  accruedInterest: number;
  taxRate: number;
  netInterest: number;
  startDate: string;
  maturityDate: string;
  status: 'ACTIVE' | 'MATURED' | 'CLOSED';
}

export const mockContracts: LoanContractDto[] = [
  {
    id: 'LN-2026-001',
    contractNo: 'LN-20260115-001',
    borrowerName: '(주)한국글로벌테크',
    borrowerId: 'CORP-100293',
    productName: '기업시설자금 대출 (EIR 적용)',
    principalAmount: 1200000000,
    nominalRate: 4.50,
    effectiveRate: 4.82,
    startDate: '2026-01-15',
    maturityDate: '2029-01-15',
    termMonths: 36,
    repaymentType: 'EQUAL_PRINCIPAL',
    status: 'ACTIVE',
    disbursedAmount: 1200000000,
    remainingBalance: 1100000000,
    deferredFee: 15000000,
    deferredCost: 3000000,
    collateralType: '공장부지 및 건물 근저당 1순위',
    currency: 'KRW',
  },
  {
    id: 'LN-2026-002',
    contractNo: 'LN-20260201-002',
    borrowerName: '대성에너지공학 (주)',
    borrowerId: 'CORP-200411',
    productName: '운전자금 한도대출',
    principalAmount: 850000000,
    nominalRate: 5.20,
    effectiveRate: 5.45,
    startDate: '2026-02-01',
    maturityDate: '2028-02-01',
    termMonths: 24,
    repaymentType: 'BULLET',
    status: 'ACTIVE',
    disbursedAmount: 850000000,
    remainingBalance: 850000000,
    deferredFee: 8500000,
    deferredCost: 2000000,
    collateralType: '신용보증기금 보증서 80%',
    currency: 'KRW',
  },
  {
    id: 'LN-2026-003',
    contractNo: 'LN-20260310-003',
    borrowerName: '(주)미래바이오텍',
    borrowerId: 'CORP-309120',
    productName: '신성장 R&D 지원 대출',
    principalAmount: 500000000,
    nominalRate: 3.80,
    effectiveRate: 4.10,
    startDate: '2026-03-10',
    maturityDate: '2031-03-10',
    termMonths: 60,
    repaymentType: 'EQUAL_INSTALLMENT',
    status: 'PENDING_DISBURSAL',
    disbursedAmount: 0,
    remainingBalance: 500000000,
    deferredFee: 5000000,
    deferredCost: 1500000,
    collateralType: '특허권 양도담보',
    currency: 'KRW',
  },
  {
    id: 'LN-2026-004',
    contractNo: 'LN-20251120-008',
    borrowerName: '삼우물류 주식회사',
    borrowerId: 'CORP-401823',
    productName: '물류장비 리스대출',
    principalAmount: 2000000000,
    nominalRate: 4.90,
    effectiveRate: 5.15,
    startDate: '2025-11-20',
    maturityDate: '2030-11-20',
    termMonths: 60,
    repaymentType: 'EQUAL_INSTALLMENT',
    status: 'ACTIVE',
    disbursedAmount: 2000000000,
    remainingBalance: 1750000000,
    deferredFee: 20000000,
    deferredCost: 4500000,
    collateralType: '화물차량 및 수송장비 양도담보',
    currency: 'KRW',
  },
  {
    id: 'LN-2026-005',
    contractNo: 'LN-20240812-015',
    borrowerName: '(주)한일정밀',
    borrowerId: 'CORP-550192',
    productName: '중소기업 창업지원대출',
    principalAmount: 300000000,
    nominalRate: 3.50,
    effectiveRate: 3.75,
    startDate: '2024-08-12',
    maturityDate: '2026-08-12',
    termMonths: 24,
    repaymentType: 'EQUAL_PRINCIPAL',
    status: 'RESTRUCTURED',
    disbursedAmount: 300000000,
    remainingBalance: 75000000,
    deferredFee: 3000000,
    deferredCost: 800000,
    collateralType: '대표이사 개인보증',
    currency: 'KRW',
  },
  {
    id: 'LN-2026-006',
    contractNo: 'LN-20230501-002',
    borrowerName: '동양스틸 (주)',
    borrowerId: 'CORP-602911',
    productName: '원자재 구매 자금대출',
    principalAmount: 1500000000,
    nominalRate: 6.10,
    effectiveRate: 6.40,
    startDate: '2023-05-01',
    maturityDate: '2026-05-01',
    termMonths: 36,
    repaymentType: 'BULLET',
    status: 'COMPLETED',
    disbursedAmount: 1500000000,
    remainingBalance: 0,
    deferredFee: 18000000,
    deferredCost: 3500000,
    collateralType: '원자재 재고자산 담보',
    currency: 'KRW',
  },
  {
    id: 'LN-2026-007',
    contractNo: 'LN-20250415-011',
    borrowerName: '세종소프트 (주)',
    borrowerId: 'CORP-710293',
    productName: 'SW개발사 운영자금 대출',
    principalAmount: 400000000,
    nominalRate: 5.80,
    effectiveRate: 6.12,
    startDate: '2025-04-15',
    maturityDate: '2027-04-15',
    termMonths: 24,
    repaymentType: 'EQUAL_INSTALLMENT',
    status: 'DEFAULTED',
    disbursedAmount: 400000000,
    remainingBalance: 240000000,
    deferredFee: 4000000,
    deferredCost: 1000000,
    collateralType: '매출채권 양도담보',
    currency: 'KRW',
  }
];

export const mockSchedules: AmortizationScheduleDto[] = [
  {
    scheduleId: 'SCH-001-01',
    contractId: 'LN-2026-001',
    period: 1,
    dueDate: '2026-02-15',
    beginningBalance: 1200000000,
    scheduledPayment: 37833333,
    principalComponent: 33333333,
    interestComponent: 4500000,
    effectiveInterest: 4820000,
    feeAmortization: 416666,
    costAmortization: 83333,
    endingBalance: 1166666667,
    status: 'PAID',
  },
  {
    scheduleId: 'SCH-001-02',
    contractId: 'LN-2026-001',
    period: 2,
    dueDate: '2026-03-15',
    beginningBalance: 1166666667,
    scheduledPayment: 37708333,
    principalComponent: 33333333,
    interestComponent: 4375000,
    effectiveInterest: 4686000,
    feeAmortization: 416666,
    costAmortization: 83333,
    endingBalance: 1133333334,
    status: 'PAID',
  },
  {
    scheduleId: 'SCH-001-03',
    contractId: 'LN-2026-001',
    period: 3,
    dueDate: '2026-04-15',
    beginningBalance: 1133333334,
    scheduledPayment: 37583333,
    principalComponent: 33333333,
    interestComponent: 4250000,
    effectiveInterest: 4552000,
    feeAmortization: 416666,
    costAmortization: 83333,
    endingBalance: 1100000001,
    status: 'PAID',
  },
  {
    scheduleId: 'SCH-001-04',
    contractId: 'LN-2026-001',
    period: 4,
    dueDate: '2026-05-15',
    beginningBalance: 1100000001,
    scheduledPayment: 37458333,
    principalComponent: 33333333,
    interestComponent: 4125000,
    effectiveInterest: 4418000,
    feeAmortization: 416666,
    costAmortization: 83333,
    endingBalance: 1066666668,
    status: 'SCHEDULED',
  },
  {
    scheduleId: 'SCH-001-05',
    contractId: 'LN-2026-001',
    period: 5,
    dueDate: '2026-06-15',
    beginningBalance: 1066666668,
    scheduledPayment: 37333333,
    principalComponent: 33333333,
    interestComponent: 4000000,
    effectiveInterest: 4284000,
    feeAmortization: 416666,
    costAmortization: 83333,
    endingBalance: 1033333335,
    status: 'SCHEDULED',
  },
  {
    scheduleId: 'SCH-001-06',
    contractId: 'LN-2026-001',
    period: 6,
    dueDate: '2026-07-15',
    beginningBalance: 1033333335,
    scheduledPayment: 37208333,
    principalComponent: 33333333,
    interestComponent: 3875000,
    effectiveInterest: 4150000,
    feeAmortization: 416666,
    costAmortization: 83333,
    endingBalance: 1000000002,
    status: 'SCHEDULED',
  },
  {
    scheduleId: 'SCH-002-01',
    contractId: 'LN-2026-002',
    period: 1,
    dueDate: '2026-03-01',
    beginningBalance: 850000000,
    scheduledPayment: 3683333,
    principalComponent: 0,
    interestComponent: 3683333,
    effectiveInterest: 3860000,
    feeAmortization: 354166,
    costAmortization: 83333,
    endingBalance: 850000000,
    status: 'PAID',
  },
  {
    scheduleId: 'SCH-002-02',
    contractId: 'LN-2026-002',
    period: 2,
    dueDate: '2026-04-01',
    beginningBalance: 850000000,
    scheduledPayment: 3683333,
    principalComponent: 0,
    interestComponent: 3683333,
    effectiveInterest: 3860000,
    feeAmortization: 354166,
    costAmortization: 83333,
    endingBalance: 850000000,
    status: 'PAID',
  },
  {
    scheduleId: 'SCH-002-03',
    contractId: 'LN-2026-002',
    period: 3,
    dueDate: '2026-05-01',
    beginningBalance: 850000000,
    scheduledPayment: 3683333,
    principalComponent: 0,
    interestComponent: 3683333,
    effectiveInterest: 3860000,
    feeAmortization: 354166,
    costAmortization: 83333,
    endingBalance: 850000000,
    status: 'OVERDUE',
  },
  {
    scheduleId: 'SCH-004-01',
    contractId: 'LN-2026-004',
    period: 1,
    dueDate: '2025-12-20',
    beginningBalance: 2000000000,
    scheduledPayment: 37650000,
    principalComponent: 29483333,
    interestComponent: 8166667,
    effectiveInterest: 8583333,
    feeAmortization: 333333,
    costAmortization: 75000,
    endingBalance: 1970516667,
    status: 'PAID',
  },
];

export const mockDisbursals: DisbursalDto[] = [
  {
    disbursalId: 'DISB-2026-001',
    contractId: 'LN-2026-003',
    contractNo: 'LN-20260310-003',
    borrowerName: '(주)미래바이오텍',
    requestedAmount: 500000000,
    disbursedAmount: 0,
    bankName: '신한은행',
    accountNumber: '110-482-910283',
    requestDate: '2026-03-12',
    disbursalDate: '2026-03-15',
    status: 'PENDING',
    approvedBy: '김선임 팀장',
  },
  {
    disbursalId: 'DISB-2026-002',
    contractId: 'LN-2026-001',
    contractNo: 'LN-20260115-001',
    borrowerName: '(주)한국글로벌테크',
    requestedAmount: 1200000000,
    disbursedAmount: 1200000000,
    bankName: '하나은행',
    accountNumber: '284-910-001923',
    requestDate: '2026-01-14',
    disbursalDate: '2026-01-15',
    status: 'DISBURSED',
    approvedBy: '박수석 이사',
  },
  {
    disbursalId: 'DISB-2026-003',
    contractId: 'LN-2026-002',
    contractNo: 'LN-20260201-002',
    borrowerName: '대성에너지공학 (주)',
    requestedAmount: 850000000,
    disbursedAmount: 850000000,
    bankName: 'KB국민은행',
    accountNumber: '043-21-0891-231',
    requestDate: '2026-01-28',
    disbursalDate: '2026-02-01',
    status: 'DISBURSED',
    approvedBy: '최영희 본부장',
  },
  {
    disbursalId: 'DISB-2026-004',
    contractId: 'LN-2026-008',
    contractNo: 'LN-20260320-009',
    borrowerName: '(주)네오소프트',
    requestedAmount: 300000000,
    disbursedAmount: 300000000,
    bankName: '기업은행',
    accountNumber: '010-8273-1928',
    requestDate: '2026-03-21',
    disbursalDate: '2026-03-22',
    status: 'APPROVED',
    approvedBy: '이재용 수석',
  },
];

export const mockDeferredItems: DeferredFeeCostDto[] = [
  {
    id: 'DEF-FEE-001',
    contractId: 'LN-2026-001',
    contractNo: 'LN-20260115-001',
    borrowerName: '(주)한국글로벌테크',
    type: 'FEE',
    category: '대출취급수수료 (Origination Fee)',
    amount: 15000000,
    amortizedAmount: 1250000,
    unamortizedBalance: 13750000,
    amortizationMethod: 'EIR',
    startDate: '2026-01-15',
    endDate: '2029-01-15',
    status: 'AMORTIZING',
  },
  {
    id: 'DEF-COST-001',
    contractId: 'LN-2026-001',
    contractNo: 'LN-20260115-001',
    borrowerName: '(주)한국글로벌테크',
    type: 'COST',
    category: '근저당 설정 및 수수료 비용',
    amount: 3000000,
    amortizedAmount: 250000,
    unamortizedBalance: 2750000,
    amortizationMethod: 'EIR',
    startDate: '2026-01-15',
    endDate: '2029-01-15',
    status: 'AMORTIZING',
  },
  {
    id: 'DEF-FEE-002',
    contractId: 'LN-2026-004',
    contractNo: 'LN-20251120-008',
    borrowerName: '삼우물류 주식회사',
    type: 'FEE',
    category: '리스 계약수수료',
    amount: 20000000,
    amortizedAmount: 1333332,
    unamortizedBalance: 18666668,
    amortizationMethod: 'EIR',
    startDate: '2025-11-20',
    endDate: '2030-11-20',
    status: 'AMORTIZING',
  },
  {
    id: 'DEF-COST-002',
    contractId: 'LN-2026-004',
    contractNo: 'LN-20251120-008',
    borrowerName: '삼우물류 주식회사',
    type: 'COST',
    category: '감정평가 및 법률 자문료',
    amount: 4500000,
    amortizedAmount: 300000,
    unamortizedBalance: 4200000,
    amortizationMethod: 'EIR',
    startDate: '2025-11-20',
    endDate: '2030-11-20',
    status: 'AMORTIZING',
  },
  {
    id: 'DEF-FEE-003',
    contractId: 'LN-2026-006',
    contractNo: 'LN-20230501-002',
    borrowerName: '동양스틸 (주)',
    type: 'FEE',
    category: '약정 이행 수수료',
    amount: 18000000,
    amortizedAmount: 18000000,
    unamortizedBalance: 0,
    amortizationMethod: 'EIR',
    startDate: '2023-05-01',
    endDate: '2026-05-01',
    status: 'FULLY_AMORTIZED',
  },
];

export const mockLoanEvents: LoanEventDto[] = [
  {
    eventId: 'EVT-2026-001',
    contractId: 'LN-2026-001',
    contractNo: 'LN-20260115-001',
    borrowerName: '(주)한국글로벌테크',
    eventType: 'EARLY_REPAYMENT',
    eventDate: '2026-03-20',
    effectiveDate: '2026-03-20',
    details: '원금 일부 조기상환 100,000,000원 처리',
    impactOnEir: 0.12,
    recalculationStatus: 'COMPLETED',
    processedBy: '김선임 과장',
  },
  {
    eventId: 'EVT-2026-002',
    contractId: 'LN-2026-005',
    contractNo: 'LN-20240812-015',
    borrowerName: '(주)한일정밀',
    eventType: 'RESTRUCTURING',
    eventDate: '2026-02-10',
    effectiveDate: '2026-02-15',
    details: '만기 연장 12개월 및 약정이율 인하 (4.0% -> 3.5%)',
    impactOnEir: -0.35,
    recalculationStatus: 'COMPLETED',
    processedBy: '이수석 차장',
  },
  {
    eventId: 'EVT-2026-003',
    contractId: 'LN-2026-002',
    contractNo: 'LN-20260201-002',
    borrowerName: '대성에너지공학 (주)',
    eventType: 'RATE_CHANGE',
    eventDate: '2026-03-01',
    effectiveDate: '2026-03-01',
    details: '기준금리 변동 반영 (COFIX 3.2% -> 3.45%)',
    impactOnEir: 0.25,
    recalculationStatus: 'PENDING',
    processedBy: '시스템 자동',
  },
  {
    eventId: 'EVT-2026-004',
    contractId: 'LN-2026-007',
    contractNo: 'LN-20250415-011',
    borrowerName: '세종소프트 (주)',
    eventType: 'MATURITY_EXTENSION',
    eventDate: '2026-03-15',
    effectiveDate: '2026-04-01',
    details: '상환유예 신청 및 EIR 재계산 수행 필요',
    impactOnEir: 0.05,
    recalculationStatus: 'PENDING',
    processedBy: '박관리 팀장',
  },
];

export const mockDepositAccounts: DepositAccountDto[] = [
  {
    accountId: 'DEP-2026-001',
    accountNumber: '1002-892-102938',
    accountName: '기업 정기예금 (3년)',
    bankName: '우리은행',
    depositType: 'TIME_DEPOSIT',
    principal: 2000000000,
    currentBalance: 2000000000,
    interestRate: 3.85,
    accruedInterest: 38500000,
    taxRate: 15.4,
    netInterest: 32571000,
    startDate: '2025-10-01',
    maturityDate: '2028-10-01',
    status: 'ACTIVE',
  },
  {
    accountId: 'DEP-2026-002',
    accountNumber: '081-9102-4412-01',
    accountName: 'MMDA 단기자금운용계좌',
    bankName: '하나은행',
    depositType: 'MONEY_MARKET',
    principal: 1500000000,
    currentBalance: 1542000000,
    interestRate: 3.20,
    accruedInterest: 12260000,
    taxRate: 15.4,
    netInterest: 10371960,
    startDate: '2026-01-01',
    maturityDate: '2026-12-31',
    status: 'ACTIVE',
  },
  {
    accountId: 'DEP-2026-003',
    accountNumber: '356-09281-22-11',
    accountName: '회계정산용 자유저축예금',
    bankName: 'NH농협은행',
    depositType: 'SAVINGS',
    principal: 500000000,
    currentBalance: 506500000,
    interestRate: 2.50,
    accruedInterest: 3125000,
    taxRate: 15.4,
    netInterest: 2643750,
    startDate: '2026-02-15',
    maturityDate: '2027-02-15',
    status: 'ACTIVE',
  },
  {
    accountId: 'DEP-2026-004',
    accountNumber: '110-992-001294',
    accountName: '특약부 통지예금',
    bankName: '신한은행',
    depositType: 'NOTICE_DEPOSIT',
    principal: 3000000000,
    currentBalance: 3000000000,
    interestRate: 4.10,
    accruedInterest: 123000000,
    taxRate: 15.4,
    netInterest: 104058000,
    startDate: '2025-04-01',
    maturityDate: '2026-04-01',
    status: 'MATURED',
  },
];
