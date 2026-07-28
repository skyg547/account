export interface MonthlyTrend {
  month: string;
  assets: number;
  liabilities: number;
}

export interface KpiDto {
  totalAssets: number;
  totalLiabilities: number;
  netIncome: number;
  monthlyTrends: MonthlyTrend[];
}

export interface JournalLineDto {
  accountCode: string;
  debit: number;
  credit: number;
  desc: string;
  partnerId?: string;
}

export interface JournalEntryDto {
  id: string;
  date: string;
  status: 'POSTED' | 'DRAFT' | 'APPROVED' | 'REJECTED' | 'PENDING';
  lines: JournalLineDto[];
}

export const mockKpis: KpiDto = {
  totalAssets: 1240500000,
  totalLiabilities: 450200000,
  netIncome: 790300000,
  monthlyTrends: [
    { month: '1월', assets: 980000000, liabilities: 380000000 },
    { month: '2월', assets: 1020000000, liabilities: 390000000 },
    { month: '3월', assets: 1080000000, liabilities: 410000000 },
    { month: '4월', assets: 1150000000, liabilities: 430000000 },
    { month: '5월', assets: 1190000000, liabilities: 440000000 },
    { month: '6월', assets: 1240500000, liabilities: 450200000 },
  ],
};

export const mockJournals: JournalEntryDto[] = [
  {
    id: 'JRN-20250601-001',
    date: '2025-06-01',
    status: 'POSTED',
    lines: [
      { accountCode: '1110200', debit: 15000000, credit: 0, desc: '6월 매출 대금 입금', partnerId: 'PT-001' },
      { accountCode: '4000000', debit: 0, credit: 15000000, desc: '제품 매출 인식', partnerId: 'PT-001' },
    ],
  },
  {
    id: 'JRN-20250605-002',
    date: '2025-06-05',
    status: 'APPROVED',
    lines: [
      { accountCode: '5000000', debit: 3200000, credit: 0, desc: '사무용품 및 비품 구매', partnerId: 'PT-002' },
      { accountCode: '1110200', debit: 0, credit: 3200000, desc: '보통예금 출금', partnerId: 'PT-002' },
    ],
  },
  {
    id: 'JRN-20250610-003',
    date: '2025-06-10',
    status: 'DRAFT',
    lines: [
      { accountCode: '1110100', debit: 500000, credit: 0, desc: '소액 현금 인출', partnerId: undefined },
      { accountCode: '1110200', debit: 0, credit: 500000, desc: '보통예금 인출', partnerId: undefined },
    ],
  },
  {
    id: 'JRN-20250615-004',
    date: '2025-06-15',
    status: 'PENDING',
    lines: [
      { accountCode: '2110000', debit: 8000000, credit: 0, desc: '외상매입금 상환', partnerId: 'PT-003' },
      { accountCode: '1110200', debit: 0, credit: 8000000, desc: '보통예금 계좌이체', partnerId: 'PT-003' },
    ],
  },
  {
    id: 'JRN-20250620-005',
    date: '2025-06-20',
    status: 'POSTED',
    lines: [
      { accountCode: '5000000', debit: 12000000, credit: 0, desc: '임직원 급여 지급', partnerId: undefined },
      { accountCode: '1110200', debit: 0, credit: 12000000, desc: '보통예금 급여이체', partnerId: undefined },
    ],
  },
  {
    id: 'JRN-20250625-006',
    date: '2025-06-25',
    status: 'REJECTED',
    lines: [
      { accountCode: '5000000', debit: 2500000, credit: 0, desc: '접대비 결제 승인 반려건', partnerId: 'PT-002' },
      { accountCode: '2110000', debit: 0, credit: 2500000, desc: '미지급금 계상', partnerId: 'PT-002' },
    ],
  },
];
