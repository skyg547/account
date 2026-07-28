export interface AccountSubjectDto {
  code: string;
  name: string;
  category: 'GROUP' | 'SUBJECT';
  status: 'ACTIVE' | 'INACTIVE';
  parentCode: string | null;
  level: number;
  balanceType: 'DEBIT' | 'CREDIT';
}

export const mockAccounts: AccountSubjectDto[] = [
  { code: '1000000', name: '자산', category: 'GROUP', status: 'ACTIVE', parentCode: null, level: 1, balanceType: 'DEBIT' },
  { code: '1100000', name: '유동자산', category: 'GROUP', status: 'ACTIVE', parentCode: '1000000', level: 2, balanceType: 'DEBIT' },
  { code: '1110000', name: '현금및현금성자산', category: 'GROUP', status: 'ACTIVE', parentCode: '1100000', level: 3, balanceType: 'DEBIT' },
  { code: '1110100', name: '현금', category: 'SUBJECT', status: 'ACTIVE', parentCode: '1110000', level: 4, balanceType: 'DEBIT' },
  { code: '1110200', name: '보통예금', category: 'SUBJECT', status: 'ACTIVE', parentCode: '1110000', level: 4, balanceType: 'DEBIT' },
  { code: '2000000', name: '부채', category: 'GROUP', status: 'ACTIVE', parentCode: null, level: 1, balanceType: 'CREDIT' },
  { code: '2100000', name: '유동부채', category: 'GROUP', status: 'ACTIVE', parentCode: '2000000', level: 2, balanceType: 'CREDIT' },
  { code: '2110000', name: '매입채무', category: 'SUBJECT', status: 'ACTIVE', parentCode: '2100000', level: 3, balanceType: 'CREDIT' },
  { code: '3000000', name: '자본', category: 'GROUP', status: 'ACTIVE', parentCode: null, level: 1, balanceType: 'CREDIT' },
  { code: '4000000', name: '수익', category: 'GROUP', status: 'ACTIVE', parentCode: null, level: 1, balanceType: 'CREDIT' },
  { code: '5000000', name: '비용', category: 'GROUP', status: 'ACTIVE', parentCode: null, level: 1, balanceType: 'DEBIT' },
];

export interface PartnerDto {
  id: string;
  type: 'CORPORATE' | 'INDIVIDUAL';
  name: string;
  businessNumber: string;
  ceoName: string;
  status: 'ACTIVE' | 'PENDING' | 'BLOCKED';
  registeredAt: string;
}

export const mockPartners: PartnerDto[] = [
  { id: 'PT-001', type: 'CORPORATE', name: '(주)안티그래비티', businessNumber: '123-45-67890', ceoName: '김대표', status: 'ACTIVE', registeredAt: '2025-01-15' },
  { id: 'PT-002', type: 'CORPORATE', name: '글로벌테크', businessNumber: '234-56-78901', ceoName: '이회장', status: 'PENDING', registeredAt: '2025-06-20' },
  { id: 'PT-003', type: 'INDIVIDUAL', name: '홍길동', businessNumber: '891023-1xxxxxx', ceoName: '홍길동', status: 'BLOCKED', registeredAt: '2024-11-11' },
];
