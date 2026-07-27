export interface AssetDto {
  id: string;
  assetCode: string;
  assetName: string;
  category: '유형자산' | '무형자산' | '투자자산';
  subCategory: string;
  acquisitionDate: string;
  acquisitionCost: number;
  accumulatedDepreciation: number;
  bookValue: number;
  usefulLifeYears: number;
  depreciationMethod: 'STRAIGHT_LINE' | 'DECLINING_BALANCE' | 'UNITS_OF_PRODUCTION';
  salvageValue: number;
  department: string;
  location: string;
  status: 'ACTIVE' | 'DISPOSED' | 'IMPAIRED' | 'UNDER_MAINTENANCE';
  lastDepreciationDate?: string;
  notes?: string;
}

export interface LeaseContractDto {
  id: string;
  contractNo: string;
  contractName: string;
  lessor: string;
  lesseeDepartment: string;
  assetType: '부동산' | '차량' | 'IT장비' | '기계설비';
  startDate: string;
  endDate: string;
  leaseTermMonths: number;
  monthlyPayment: number;
  discountRate: number;
  rouAssetValue: number; // 사용권자산 금액
  leaseLiability: number; // 리스부채 잔액
  accumulatedDepreciation: number; // 사용권자산 감가상각누계액
  status: 'ACTIVE' | 'TERMINATED' | 'MODIFIED' | 'EXPIRED';
  underlyingAsset: string;
}

export interface DepreciationBatchDto {
  id: string;
  periodYearMonth: string;
  executionDate: string;
  targetAssetCount: number;
  totalDepreciationAmount: number;
  status: 'COMPLETED' | 'IN_PROGRESS' | 'FAILED' | 'DRAFT';
  executor: string;
  journalEntryNo?: string;
}

export interface AssetDisposalDto {
  id: string;
  disposalNo: string;
  assetId: string;
  assetCode: string;
  assetName: string;
  disposalDate: string;
  disposalType: 'SALE' | 'SCRAP' | 'DONATION' | 'LOSS';
  acquisitionCost: number;
  accumulatedDepreciation: number;
  bookValue: number;
  saleAmount: number;
  gainLossAmount: number; // 처분손익 (+ 이익, - 손실)
  approvalStatus: 'APPROVED' | 'PENDING' | 'REJECTED';
  approvedBy?: string;
  reason: string;
}

export interface AssetRevaluationDto {
  id: string;
  revaluationNo: string;
  assetId: string;
  assetCode: string;
  assetName: string;
  valuationDate: string;
  type: 'REVALUATION' | 'IMPAIRMENT' | 'REVERSAL';
  preValuationBookValue: number;
  fairMarketValue: number;
  valuationGainLoss: number;
  appraiser: string;
  approvalStatus: 'APPROVED' | 'PENDING' | 'REJECTED';
  reason: string;
}

export interface LeaseMonthlyDto {
  id: string;
  closingPeriod: string; // YYYY-MM
  contractId: string;
  contractNo: string;
  contractName: string;
  monthlyPayment: number;
  interestExpense: number; // 이자비용
  principalRepayment: number; // 리스부채 상감(원금)
  rouDepreciation: number; // 사용권자산 상각비
  closingStatus: 'COMPLETED' | 'PENDING' | 'SKIPPED';
  closingDate?: string;
}

export interface LeaseRemeasureDto {
  id: string;
  remeasureNo: string;
  contractId: string;
  contractNo: string;
  contractName: string;
  remeasureDate: string;
  changeReason: 'OPTION_EXERCISE' | 'RATE_CHANGE' | 'PAYMENT_MODIFICATION' | 'TERM_EXTENSION';
  preLiability: number;
  postLiability: number;
  adjustmentAmount: number;
  preRouAsset: number;
  postRouAsset: number;
  newDiscountRate: number;
  status: 'APPROVED' | 'PENDING' | 'REJECTED';
  approvalDate?: string;
}

export const mockAssets: AssetDto[] = [
  {
    id: 'ast-001',
    assetCode: 'FA-2023-001',
    assetName: '강남 본사 사옥 건물 (8~12층)',
    category: '유형자산',
    subCategory: '건물',
    acquisitionDate: '2021-01-15',
    acquisitionCost: 15000000000,
    accumulatedDepreciation: 1800000000,
    bookValue: 13200000000,
    usefulLifeYears: 40,
    depreciationMethod: 'STRAIGHT_LINE',
    salvageValue: 600000000,
    department: '경영지원본부',
    location: '서울시 강남구 테헤란로 123',
    status: 'ACTIVE',
    lastDepreciationDate: '2026-06-30',
    notes: '본사 건물 5개 층 보유분',
  },
  {
    id: 'ast-002',
    assetCode: 'FA-2023-002',
    assetName: '초고속 SMT 반도체 실장 기계라인 A',
    category: '유형자산',
    subCategory: '기계장치',
    acquisitionDate: '2022-04-10',
    acquisitionCost: 3200000000,
    accumulatedDepreciation: 1280000000,
    bookValue: 1920000000,
    usefulLifeYears: 10,
    depreciationMethod: 'DECLINING_BALANCE',
    salvageValue: 100000000,
    department: '생산1팀',
    location: '평택 제1공장 2라인',
    status: 'ACTIVE',
    lastDepreciationDate: '2026-06-30',
    notes: '정율법 0.206 적용',
  },
  {
    id: 'ast-003',
    assetCode: 'FA-2023-003',
    assetName: '임원 전용 제네시스 G90 차량',
    category: '유형자산',
    subCategory: '차량운반구',
    acquisitionDate: '2023-02-01',
    acquisitionCost: 110000000,
    accumulatedDepreciation: 66000000,
    bookValue: 44000000,
    usefulLifeYears: 5,
    depreciationMethod: 'STRAIGHT_LINE',
    salvageValue: 10000000,
    department: '임원실',
    location: '본사 지하 주차장',
    status: 'ACTIVE',
    lastDepreciationDate: '2026-06-30',
  },
  {
    id: 'ast-004',
    assetCode: 'FA-2024-004',
    assetName: '차세대 ERP 코어 솔루션 라이선스',
    category: '무형자산',
    subCategory: '소프트웨어',
    acquisitionDate: '2024-01-05',
    acquisitionCost: 850000000,
    accumulatedDepreciation: 255000000,
    bookValue: 595000000,
    usefulLifeYears: 5,
    depreciationMethod: 'STRAIGHT_LINE',
    salvageValue: 0,
    department: 'IT전략팀',
    location: '클라우드 데이터센터',
    status: 'ACTIVE',
    lastDepreciationDate: '2026-06-30',
  },
  {
    id: 'ast-005',
    assetCode: 'FA-2022-008',
    assetName: '구형 정밀 레이저 절단기',
    category: '유형자산',
    subCategory: '기계장치',
    acquisitionDate: '2019-08-20',
    acquisitionCost: 480000000,
    accumulatedDepreciation: 410000000,
    bookValue: 70000000,
    usefulLifeYears: 7,
    depreciationMethod: 'STRAIGHT_LINE',
    salvageValue: 20000000,
    department: '생산2팀',
    location: '평택 제2공장',
    status: 'IMPAIRED',
    lastDepreciationDate: '2026-06-30',
    notes: '2025년 손상차손 인식 완료',
  },
  {
    id: 'ast-006',
    assetCode: 'FA-2021-012',
    assetName: '물류용 2.5톤 리치 지게차',
    category: '유형자산',
    subCategory: '차량운반구',
    acquisitionDate: '2020-03-12',
    acquisitionCost: 45000000,
    accumulatedDepreciation: 40000000,
    bookValue: 5000000,
    usefulLifeYears: 5,
    depreciationMethod: 'STRAIGHT_LINE',
    salvageValue: 500000,
    department: '물류운영팀',
    location: '용인 물류센터',
    status: 'DISPOSED',
    lastDepreciationDate: '2025-03-31',
    notes: '2025년 4월 매각 처분됨',
  },
  {
    id: 'ast-007',
    assetCode: 'FA-2025-001',
    assetName: '연구소 고성능 AI 서버 랙 4대',
    category: '유형자산',
    subCategory: '비품',
    acquisitionDate: '2025-02-15',
    acquisitionCost: 350000000,
    accumulatedDepreciation: 70000000,
    bookValue: 280000000,
    usefulLifeYears: 4,
    depreciationMethod: 'STRAIGHT_LINE',
    salvageValue: 10000000,
    department: 'R&D연구소',
    location: '판교 R&D 센터 IDC룸',
    status: 'ACTIVE',
    lastDepreciationDate: '2026-06-30',
  }
];

export const mockLeases: LeaseContractDto[] = [
  {
    id: 'ls-001',
    contractNo: 'IFRS16-2024-01',
    contractName: '판교 R&D 연구소 업무동 3~4층 임대차',
    lessor: '(주)판교테크노밸리자산관리',
    lesseeDepartment: 'R&D연구소',
    assetType: '부동산',
    startDate: '2024-01-01',
    endDate: '2028-12-31',
    leaseTermMonths: 60,
    monthlyPayment: 45000000,
    discountRate: 4.5,
    rouAssetValue: 2450000000,
    leaseLiability: 1820000000,
    accumulatedDepreciation: 612500000,
    status: 'ACTIVE',
    underlyingAsset: '판교 IT밸리 A동 3,4층 사무실 (전용 1,200m²)',
  },
  {
    id: 'ls-002',
    contractNo: 'IFRS16-2023-04',
    contractName: '전사 임원진 장기렌트 차량 5대',
    lessor: '현대캐피탈(주)',
    lesseeDepartment: '경영지원본부',
    assetType: '차량',
    startDate: '2023-07-01',
    endDate: '2026-06-30',
    leaseTermMonths: 36,
    monthlyPayment: 8500000,
    discountRate: 5.0,
    rouAssetValue: 280000000,
    leaseLiability: 48000000,
    accumulatedDepreciation: 233330000,
    status: 'ACTIVE',
    underlyingAsset: '제네시스 G80 3대, GV80 2대',
  },
  {
    id: 'ls-003',
    contractNo: 'IFRS16-2025-02',
    contractName: '물류센터 자동화 분류 설비 리스',
    lessor: '하나캡탈(주)',
    lesseeDepartment: '물류운영팀',
    assetType: '기계설비',
    startDate: '2025-03-01',
    endDate: '2030-02-28',
    leaseTermMonths: 60,
    monthlyPayment: 18000000,
    discountRate: 4.8,
    rouAssetValue: 980000000,
    leaseLiability: 860000000,
    accumulatedDepreciation: 130666000,
    status: 'ACTIVE',
    underlyingAsset: '용인센터 소팅 컨베이어 시스템 2세트',
  },
  {
    id: 'ls-004',
    contractNo: 'IFRS16-2024-03',
    contractName: '데이터센터 엔터프라이즈 서버 랙 리스',
    lessor: 'HPE Financial Services',
    lesseeDepartment: 'IT전략팀',
    assetType: 'IT장비',
    startDate: '2024-05-01',
    endDate: '2027-04-30',
    leaseTermMonths: 36,
    monthlyPayment: 12500000,
    discountRate: 4.2,
    rouAssetValue: 410000000,
    leaseLiability: 245000000,
    accumulatedDepreciation: 182200000,
    status: 'MODIFIED',
    underlyingAsset: 'HPE ProLiant DL380 16대 및 스토리지',
  },
  {
    id: 'ls-005',
    contractNo: 'IFRS16-2022-01',
    contractName: '부산지사 오피스 임대차',
    lessor: '(주)부산빌딩관리',
    lesseeDepartment: '영업2팀',
    assetType: '부동산',
    startDate: '2022-02-01',
    endDate: '2025-01-31',
    leaseTermMonths: 36,
    monthlyPayment: 6000000,
    discountRate: 4.0,
    rouAssetValue: 200000000,
    leaseLiability: 0,
    accumulatedDepreciation: 200000000,
    status: 'EXPIRED',
    underlyingAsset: '부산 해운대 센텀시티 타워 15층',
  }
];

export const mockDepreciationBatches: DepreciationBatchDto[] = [
  {
    id: 'dep-202606',
    periodYearMonth: '2026-06',
    executionDate: '2026-06-30 18:30',
    targetAssetCount: 142,
    totalDepreciationAmount: 184500000,
    status: 'COMPLETED',
    executor: '김재무 차장',
    journalEntryNo: 'JV-20260630-0042',
  },
  {
    id: 'dep-202605',
    periodYearMonth: '2026-05',
    executionDate: '2026-05-31 19:10',
    targetAssetCount: 140,
    totalDepreciationAmount: 182100000,
    status: 'COMPLETED',
    executor: '김재무 차장',
    journalEntryNo: 'JV-20260531-0038',
  },
  {
    id: 'dep-202607',
    periodYearMonth: '2026-07',
    executionDate: '2026-07-28 (시뮬레이션)',
    targetAssetCount: 145,
    totalDepreciationAmount: 186200000,
    status: 'DRAFT',
    executor: '시스템 자동',
  }
];

export const mockDisposals: AssetDisposalDto[] = [
  {
    id: 'disp-001',
    disposalNo: 'DISP-2026-001',
    assetId: 'ast-006',
    assetCode: 'FA-2021-012',
    assetName: '물류용 2.5톤 리치 지게차',
    disposalDate: '2026-04-15',
    disposalType: 'SALE',
    acquisitionCost: 45000000,
    accumulatedDepreciation: 40000000,
    bookValue: 5000000,
    saleAmount: 7500000,
    gainLossAmount: 2500000,
    approvalStatus: 'APPROVED',
    approvedBy: '이이사 본부장',
    reason: '내용연수 만료 및 신규 전기지게차 교체에 따른 중고 매각',
  },
  {
    id: 'disp-002',
    disposalNo: 'DISP-2026-002',
    assetId: 'ast-099',
    assetCode: 'FA-2018-044',
    assetName: '구형 랙 서버 2대 (폐기)',
    disposalDate: '2026-06-20',
    disposalType: 'SCRAP',
    acquisitionCost: 28000000,
    accumulatedDepreciation: 28000000,
    bookValue: 0,
    saleAmount: 0,
    gainLossAmount: 0,
    approvalStatus: 'APPROVED',
    approvedBy: '이이사 본부장',
    reason: '고장 및 부품 수급 불가로 인한 불용 폐기',
  },
  {
    id: 'disp-003',
    disposalNo: 'DISP-2026-003',
    assetId: 'ast-005',
    assetCode: 'FA-2022-008',
    assetName: '구형 정밀 레이저 절단기',
    disposalDate: '2026-07-20',
    disposalType: 'SALE',
    acquisitionCost: 480000000,
    accumulatedDepreciation: 410000000,
    bookValue: 70000000,
    saleAmount: 55000000,
    gainLossAmount: -15000000,
    approvalStatus: 'PENDING',
    reason: '생산라인 개편으로 외부 매각 추진 (자산처분손실 예상)',
  }
];

export const mockRevaluations: AssetRevaluationDto[] = [
  {
    id: 'reval-001',
    revaluationNo: 'REV-2025-001',
    assetId: 'ast-001',
    assetCode: 'FA-2023-001',
    assetName: '강남 본사 사옥 건물 (8~12층)',
    valuationDate: '2025-12-31',
    type: 'REVALUATION',
    preValuationBookValue: 12500000000,
    fairMarketValue: 14800000000,
    valuationGainLoss: 2300000000,
    appraiser: '한국감정평가법인',
    approvalStatus: 'APPROVED',
    reason: 'K-IFRS 자산재평가모형 적용에 따른 공정가치 평가 증액',
  },
  {
    id: 'reval-002',
    revaluationNo: 'IMP-2025-002',
    assetId: 'ast-005',
    assetCode: 'FA-2022-008',
    assetName: '구형 정밀 레이저 절단기',
    valuationDate: '2025-12-31',
    type: 'IMPAIRMENT',
    preValuationBookValue: 1500000000,
    fairMarketValue: 70000000,
    valuationGainLoss: -80000000,
    appraiser: '삼일회계법인 기술평가팀',
    approvalStatus: 'APPROVED',
    reason: '신기술 도입으로 인한 시장가치 급락 손상차손 인식',
  }
];

export const mockLeaseMonthlies: LeaseMonthlyDto[] = [
  {
    id: 'lsm-202606-01',
    closingPeriod: '2026-06',
    contractId: 'ls-001',
    contractNo: 'IFRS16-2024-01',
    contractName: '판교 R&D 연구소 업무동 3~4층 임대차',
    monthlyPayment: 45000000,
    interestExpense: 6825000,
    principalRepayment: 38175000,
    rouDepreciation: 40833333,
    closingStatus: 'COMPLETED',
    closingDate: '2026-06-30',
  },
  {
    id: 'lsm-202606-02',
    closingPeriod: '2026-06',
    contractId: 'ls-002',
    contractNo: 'IFRS16-2023-04',
    contractName: '전사 임원진 장기렌트 차량 5대',
    monthlyPayment: 8500000,
    interestExpense: 200000,
    principalRepayment: 8300000,
    rouDepreciation: 7777777,
    closingStatus: 'COMPLETED',
    closingDate: '2026-06-30',
  },
  {
    id: 'lsm-202607-01',
    closingPeriod: '2026-07',
    contractId: 'ls-001',
    contractNo: 'IFRS16-2024-01',
    contractName: '판교 R&D 연구소 업무동 3~4층 임대차',
    monthlyPayment: 45000000,
    interestExpense: 6680000,
    principalRepayment: 38320000,
    rouDepreciation: 40833333,
    closingStatus: 'PENDING',
  },
  {
    id: 'lsm-202607-02',
    closingPeriod: '2026-07',
    contractId: 'ls-003',
    contractNo: 'IFRS16-2025-02',
    contractName: '물류센터 자동화 분류 설비 리스',
    monthlyPayment: 18000000,
    interestExpense: 3440000,
    principalRepayment: 14560000,
    rouDepreciation: 16333333,
    closingStatus: 'PENDING',
  }
];

export const mockLeaseRemeasures: LeaseRemeasureDto[] = [
  {
    id: 'lsr-001',
    remeasureNo: 'REM-2026-001',
    contractId: 'ls-004',
    contractNo: 'IFRS16-2024-03',
    contractName: '데이터센터 엔터프라이즈 서버 랙 리스',
    remeasureDate: '2026-05-01',
    changeReason: 'PAYMENT_MODIFICATION',
    preLiability: 310000000,
    postLiability: 245000000,
    adjustmentAmount: -65000000,
    preRouAsset: 475000000,
    postRouAsset: 410000000,
    newDiscountRate: 4.2,
    status: 'APPROVED',
    approvalDate: '2026-05-05',
  },
  {
    id: 'lsr-002',
    remeasureNo: 'REM-2026-002',
    contractId: 'ls-001',
    contractNo: 'IFRS16-2024-01',
    contractName: '판교 R&D 연구소 업무동 3~4층 임대차',
    remeasureDate: '2026-07-15',
    changeReason: 'TERM_EXTENSION',
    preLiability: 1820000000,
    postLiability: 2600000000,
    adjustmentAmount: 780000000,
    preRouAsset: 1837500000,
    postRouAsset: 2617500000,
    newDiscountRate: 4.8,
    status: 'PENDING',
  }
];
