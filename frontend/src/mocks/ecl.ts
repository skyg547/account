export interface EclResultDto {
  id: string;
  portfolio: string;
  portfolioCode: string;
  exposureAmount: number;
  stage1: number;
  stage2: number;
  stage3: number;
  totalEcl: number;
  coverageRatio: number; // %
  baseDate: string;
  previousEcl?: number;
  changeRate?: number; // %
}

export interface EclParameterDto {
  id: string;
  parameterCode: string;
  parameterName: string;
  type: 'PD' | 'LGD' | 'CCF' | 'FLI';
  portfolio: string;
  stage?: string;
  value: number; // e.g. 1.25 for 1.25%
  unit: '%' | '비율' | '가중치';
  validFrom: string;
  validTo: string;
  status: 'ACTIVE' | 'EXPIRED' | 'PENDING';
  updatedBy: string;
  updatedAt: string;
  description: string;
}

export interface EclExposureDto {
  id: string;
  contractNo: string;
  customerName: string;
  productType: string;
  portfolio: string;
  exposureAmount: number; // EAD
  creditGrade: string; // AAA, AA, A, BBB, BB, B, CCC, D
  stage: 'Stage 1' | 'Stage 2' | 'Stage 3';
  stageReason: string;
  pd: number; // PD (%)
  lgd: number; // LGD (%)
  eclAmount: number;
  collateralValue: number;
  delinquencyDays: number;
  baseDate: string;
}

export interface EclBatchDto {
  id: string;
  batchName: string;
  periodYearMonth: string;
  status: 'COMPLETED' | 'RUNNING' | 'FAILED' | 'WAITING';
  progressPercent: number;
  startTime: string;
  endTime?: string;
  totalRecords: number;
  processedRecords: number;
  errorRecords: number;
  executor: string;
  currentStep: string;
}

export interface EclBatchLogDto {
  id: string;
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'SUCCESS';
  step: string;
  message: string;
}

export interface EadSimulationDto {
  id: string;
  contractNo: string;
  customerName: string;
  productType: string;
  committedAmount: number;
  drawnAmount: number;
  undrawnAmount: number;
  ccf: number;
  calculatedEad: number;
  amortizationMethod: '원리금균등' | '만기일시' | '원금균등';
  remainingMonths: number;
  status: 'PASS' | 'WARN' | 'FAIL';
}

export const mockResults: EclResultDto[] = [
  {
    id: 'ecl-res-001',
    portfolio: '기업 여신 (Corporate Loans)',
    portfolioCode: 'CORP',
    exposureAmount: 450000000000,
    stage1: 1850000000,
    stage2: 3200000000,
    stage3: 4100000000,
    totalEcl: 9150000000,
    coverageRatio: 2.03,
    baseDate: '2026-06-30',
    previousEcl: 8800000000,
    changeRate: 3.98,
  },
  {
    id: 'ecl-res-002',
    portfolio: '가계 주택담보대출 (Mortgages)',
    portfolioCode: 'MORT',
    exposureAmount: 620000000000,
    stage1: 930000000,
    stage2: 1840000000,
    stage3: 1450000000,
    totalEcl: 4220000000,
    coverageRatio: 0.68,
    baseDate: '2026-06-30',
    previousEcl: 4100000000,
    changeRate: 2.93,
  },
  {
    id: 'ecl-res-003',
    portfolio: '개인 신용대출 (Personal Loans)',
    portfolioCode: 'RETAIL',
    exposureAmount: 180000000000,
    stage1: 1260000000,
    stage2: 2450000000,
    stage3: 3890000000,
    totalEcl: 7600000000,
    coverageRatio: 4.22,
    baseDate: '2026-06-30',
    previousEcl: 7900000000,
    changeRate: -3.80,
  },
  {
    id: 'ecl-res-004',
    portfolio: '신용카드 익스포저 (Credit Cards)',
    portfolioCode: 'CARD',
    exposureAmount: 95000000000,
    stage1: 850000000,
    stage2: 1720000000,
    stage3: 2980000000,
    totalEcl: 5550000000,
    coverageRatio: 5.84,
    baseDate: '2026-06-30',
    previousEcl: 5300000000,
    changeRate: 4.72,
  },
  {
    id: 'ecl-res-005',
    portfolio: '상장 유가증권 (Marketable Bonds)',
    portfolioCode: 'BOND',
    exposureAmount: 310000000000,
    stage1: 310000000,
    stage2: 620000000,
    stage3: 0,
    totalEcl: 930000000,
    coverageRatio: 0.30,
    baseDate: '2026-06-30',
    previousEcl: 950000000,
    changeRate: -2.11,
  },
];

export const mockParameters: EclParameterDto[] = [
  {
    id: 'param-001',
    parameterCode: 'PD-CORP-AAA',
    parameterName: '기업여신 우량등급(AAA) 12M 부도확률',
    type: 'PD',
    portfolio: '기업 여신',
    stage: 'Stage 1',
    value: 0.08,
    unit: '%',
    validFrom: '2026-01-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '김리시크 수석',
    updatedAt: '2026-01-15',
    description: 'K-IFRS 9 기준 리스크 모형 산출결과 (상반기 검증 완료)',
  },
  {
    id: 'param-002',
    parameterCode: 'PD-CORP-BBB',
    parameterName: '기업여신 중위등급(BBB) Lifetime 부도확률',
    type: 'PD',
    portfolio: '기업 여신',
    stage: 'Stage 2',
    value: 2.45,
    unit: '%',
    validFrom: '2026-01-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '김리시크 수석',
    updatedAt: '2026-01-15',
    description: 'SICR(유의적 신용위험 증가) 발생 구간 적용 부도율',
  },
  {
    id: 'param-003',
    parameterCode: 'LGD-RE-MORT',
    parameterName: '주택담보대출 부도시손실률(LGD)',
    type: 'LGD',
    portfolio: '가계 주택담보대출',
    stage: '전체',
    value: 18.50,
    unit: '%',
    validFrom: '2026-01-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '이모델 책임',
    updatedAt: '2026-02-01',
    description: '부동산 경매 낙찰가율 및 담보인정비율(LTV) 연동 산출 모형',
  },
  {
    id: 'param-004',
    parameterCode: 'LGD-UNSEC-RETAIL',
    parameterName: '개인 무보증 신용대출 LGD',
    type: 'LGD',
    portfolio: '개인 신용대출',
    stage: '전체',
    value: 46.20,
    unit: '%',
    validFrom: '2026-01-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '이모델 책임',
    updatedAt: '2026-02-01',
    description: '최근 3개년 회수율 데이터 기반 LGD 추정치',
  },
  {
    id: 'param-005',
    parameterCode: 'CCF-CORP-LIMIT',
    parameterName: '기업 한도대출 신용전환율(CCF)',
    type: 'CCF',
    portfolio: '기업 여신',
    stage: '전체',
    value: 72.00,
    unit: '%',
    validFrom: '2026-01-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '박파람 선임',
    updatedAt: '2026-03-10',
    description: '미인출 약정 잔액의 부도시 인출 예상 비율',
  },
  {
    id: 'param-006',
    parameterCode: 'CCF-CARD-LINE',
    parameterName: '신용카드 미사용 한도 CCF',
    type: 'CCF',
    portfolio: '신용카드',
    stage: '전체',
    value: 20.00,
    unit: '%',
    validFrom: '2026-01-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '박파람 선임',
    updatedAt: '2026-03-10',
    description: '바젤III 감독기준 및 내부 추정 표준 모형 적용',
  },
  {
    id: 'param-007',
    parameterCode: 'FLI-MACRO-BASE',
    parameterName: '거시경제 전망(FLI) 기준 시나리오 가중치',
    type: 'FLI',
    portfolio: '전체 포트폴리오',
    stage: '전체',
    value: 60.00,
    unit: '가중치',
    validFrom: '2026-06-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '최전망 연구원',
    updatedAt: '2026-06-01',
    description: '한국은행 GDP 성장률(2.1%) 및 기준금리 전망 반영',
  },
  {
    id: 'param-008',
    parameterCode: 'FLI-MACRO-ADVERSE',
    parameterName: '거시경제 전망(FLI) 비관 시나리오 가중치',
    type: 'FLI',
    portfolio: '전체 포트폴리오',
    stage: '전체',
    value: 25.00,
    unit: '가중치',
    validFrom: '2026-06-01',
    validTo: '2026-12-31',
    status: 'ACTIVE',
    updatedBy: '최전망 연구원',
    updatedAt: '2026-06-01',
    description: '고금리 장기화 및 부동산 경기 침체 시나리오',
  },
  {
    id: 'param-009',
    parameterCode: 'PD-RETAIL-CCC',
    parameterName: '개인신용 고위험(CCC 이하) PD',
    type: 'PD',
    portfolio: '개인 신용대출',
    stage: 'Stage 2',
    value: 12.80,
    unit: '%',
    validFrom: '2025-01-01',
    validTo: '2025-12-31',
    status: 'EXPIRED',
    updatedBy: '김리시크 수석',
    updatedAt: '2025-01-10',
    description: '2025년도 이전 파라미터 (만료됨)',
  },
];

export const mockExposures: EclExposureDto[] = [
  {
    id: 'exp-1001',
    contractNo: 'LN-2024-8841',
    customerName: '(주)삼성디스플레이협력사',
    productType: '기업시설자금대출',
    portfolio: '기업 여신',
    exposureAmount: 4500000000,
    creditGrade: 'AA',
    stage: 'Stage 1',
    stageReason: '신용등급 유지 및 연체 없음',
    pd: 0.12,
    lgd: 22.5,
    eclAmount: 12150000,
    collateralValue: 5200000000,
    delinquencyDays: 0,
    baseDate: '2026-06-30',
  },
  {
    id: 'exp-1002',
    contractNo: 'LN-2025-0192',
    customerName: '(주)한화솔루션하청',
    productType: '기업운전자금대출',
    portfolio: '기업 여신',
    exposureAmount: 1800000000,
    creditGrade: 'BB-',
    stage: 'Stage 2',
    stageReason: '신용등급 2단계 하락 (SICR 발생)',
    pd: 3.45,
    lgd: 38.0,
    eclAmount: 235980000,
    collateralValue: 800000000,
    delinquencyDays: 14,
    baseDate: '2026-06-30',
  },
  {
    id: 'exp-1003',
    contractNo: 'LN-2023-5510',
    customerName: '홍길동',
    productType: '아파트 주택담보대출',
    portfolio: '가계 주택담보대출',
    exposureAmount: 450000000,
    creditGrade: 'A+',
    stage: 'Stage 1',
    stageReason: '정상 상환 중',
    pd: 0.25,
    lgd: 15.0,
    eclAmount: 1687500,
    collateralValue: 750000000,
    delinquencyDays: 0,
    baseDate: '2026-06-30',
  },
  {
    id: 'exp-1004',
    contractNo: 'LN-2022-9901',
    customerName: '(주)태영건설하도급',
    productType: '부동산 PF 담보대출',
    portfolio: '기업 여신',
    exposureAmount: 3200000000,
    creditGrade: 'D',
    stage: 'Stage 3',
    stageReason: '90일 이상 장기 연체 및 기한이익상실(EOD)',
    pd: 100.0,
    lgd: 65.0,
    eclAmount: 2080000000,
    collateralValue: 1200000000,
    delinquencyDays: 120,
    baseDate: '2026-06-30',
  },
  {
    id: 'exp-1005',
    contractNo: 'LN-2025-4412',
    customerName: '김철수',
    productType: '개인 직장인 신용대출',
    portfolio: '개인 신용대출',
    exposureAmount: 65000000,
    creditGrade: 'BBB',
    stage: 'Stage 1',
    stageReason: '정상',
    pd: 1.10,
    lgd: 45.0,
    eclAmount: 3217500,
    collateralValue: 0,
    delinquencyDays: 0,
    baseDate: '2026-06-30',
  },
  {
    id: 'exp-1006',
    contractNo: 'CD-2024-7718',
    customerName: '이영희',
    productType: '플래티넘 신용카드 한도',
    portfolio: '신용카드',
    exposureAmount: 25000000,
    creditGrade: 'BB',
    stage: 'Stage 2',
    stageReason: '30일 이상 연체 발생',
    pd: 4.80,
    lgd: 55.0,
    eclAmount: 6600000,
    collateralValue: 0,
    delinquencyDays: 45,
    baseDate: '2026-06-30',
  },
  {
    id: 'exp-1007',
    contractNo: 'BD-2025-0012',
    customerName: '대한민국 국채 10년물',
    productType: '국공채',
    portfolio: '상장 유가증권',
    exposureAmount: 15000000000,
    creditGrade: 'AAA',
    stage: 'Stage 1',
    stageReason: '무위험 자산',
    pd: 0.01,
    lgd: 5.0,
    eclAmount: 750000,
    collateralValue: 15000000000,
    delinquencyDays: 0,
    baseDate: '2026-06-30',
  },
];

export const mockBatches: EclBatchDto[] = [
  {
    id: 'batch-202606',
    batchName: '2026년 06월말 IFRS9 ECL 정기 배치',
    periodYearMonth: '2026-06',
    status: 'RUNNING',
    progressPercent: 78,
    startTime: '2026-06-30 23:00:00',
    totalRecords: 145000,
    processedRecords: 113100,
    errorRecords: 0,
    executor: 'SYSTEM_CRON',
    currentStep: 'Step 4. ECL 손실충당금 산출 엔진 실행 중 (113,100 / 145,000)',
  },
  {
    id: 'batch-202605',
    batchName: '2026년 05월말 IFRS9 ECL 정기 배치',
    periodYearMonth: '2026-05',
    status: 'COMPLETED',
    progressPercent: 100,
    startTime: '2026-05-31 23:00:00',
    endTime: '2026-05-31 23:42:15',
    totalRecords: 142800,
    processedRecords: 142800,
    errorRecords: 0,
    executor: 'SYSTEM_CRON',
    currentStep: '완료 (원장 검증 및 회계전표 생성 완료)',
  },
  {
    id: 'batch-202604',
    batchName: '2026년 04월말 IFRS9 ECL 정기 배치',
    periodYearMonth: '2026-04',
    status: 'COMPLETED',
    progressPercent: 100,
    startTime: '2026-04-30 23:00:00',
    endTime: '2026-04-30 23:38:50',
    totalRecords: 141200,
    processedRecords: 141200,
    errorRecords: 0,
    executor: 'SYSTEM_CRON',
    currentStep: '완료',
  },
  {
    id: 'batch-202606-adhoc',
    batchName: 'FLI 스트레스 테스트 시뮬레이션 배치',
    periodYearMonth: '2026-06',
    status: 'FAILED',
    progressPercent: 42,
    startTime: '2026-06-25 14:20:00',
    endTime: '2026-06-25 14:28:10',
    totalRecords: 145000,
    processedRecords: 60900,
    errorRecords: 12,
    executor: '김리스크 차장',
    currentStep: '오류: 비관 시나리오 매정치 파라미터 규격 불일치 (Null Value)',
  },
];

export const mockBatchLogs: EclBatchLogDto[] = [
  { id: 'log-01', timestamp: '23:00:01', level: 'INFO', step: 'INIT', message: 'ECL 2026-06 정기 배치 연산 프로세스 시작' },
  { id: 'log-02', timestamp: '23:00:05', level: 'INFO', step: 'EXTRACT', message: '계정계 여신 원장 데이터 추출 완료 (145,000건)' },
  { id: 'log-03', timestamp: '23:05:12', level: 'INFO', step: 'STAGE', message: 'Stage 1/2/3 자동 분류 실행 완료 (Stage1: 128,400, Stage2: 14,200, Stage3: 2,400)' },
  { id: 'log-04', timestamp: '23:12:40', level: 'INFO', step: 'PARAM', message: 'PD / LGD / CCF 2026 상반기 적용 파라미터 매핑 완료' },
  { id: 'log-05', timestamp: '23:25:00', level: 'INFO', step: 'ECL_CALC', message: '기업 여신 포트폴리오 ECL 연산 완료 (45,000건 처리)' },
  { id: 'log-06', timestamp: '23:31:15', level: 'INFO', step: 'ECL_CALC', message: '가계 주택담보대출 ECL 연산 완료 (58,000건 처리)' },
  { id: 'log-07', timestamp: '23:35:40', level: 'WARN', step: 'ECL_CALC', message: '경고: 계약 LN-2022-9901 담보가액 재평가 기준일 확인 필요' },
  { id: 'log-08', timestamp: '23:40:02', level: 'INFO', step: 'ECL_CALC', message: '개인 신용대출 및 카드 익스포저 연산 진행 중 (78% 경과)' },
];

export const mockEadSimulations: EadSimulationDto[] = [
  {
    id: 'ead-001',
    contractNo: 'SIM-CORP-101',
    customerName: '(주)대우건설컨소시엄',
    productType: '기업 한도대출(Credit Line)',
    committedAmount: 10000000000,
    drawnAmount: 6000000000,
    undrawnAmount: 4000000000,
    ccf: 75.0,
    calculatedEad: 9000000000,
    amortizationMethod: '만기일시',
    remainingMonths: 36,
    status: 'PASS',
  },
  {
    id: 'ead-002',
    contractNo: 'SIM-MORT-202',
    customerName: '박지성',
    productType: '분양보증 대출',
    committedAmount: 500000000,
    drawnAmount: 350000000,
    undrawnAmount: 150000000,
    ccf: 50.0,
    calculatedEad: 425000000,
    amortizationMethod: '원리금균등',
    remainingMonths: 120,
    status: 'PASS',
  },
  {
    id: 'ead-003',
    contractNo: 'SIM-CARD-303',
    customerName: '최유리',
    productType: '기업구매카드 한도',
    committedAmount: 200000000,
    drawnAmount: 80000000,
    undrawnAmount: 120000000,
    ccf: 20.0,
    calculatedEad: 104000000,
    amortizationMethod: '만기일시',
    remainingMonths: 12,
    status: 'PASS',
  },
  {
    id: 'ead-004',
    contractNo: 'SIM-UNSEC-404',
    customerName: '(주)바이오벤처',
    productType: '마이너스 통장대출',
    committedAmount: 1500000000,
    drawnAmount: 1200000000,
    undrawnAmount: 300000000,
    ccf: 100.0,
    calculatedEad: 1500000000,
    amortizationMethod: '만기일시',
    remainingMonths: 6,
    status: 'WARN',
  },
];
