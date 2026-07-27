export interface ExchangeRateDto {
  id: string;
  currencyPair: string; // e.g. 'USD/KRW', 'EUR/KRW', 'JPY/KRW', 'CNY/KRW', 'GBP/KRW'
  baseCurrency: string;
  targetCurrency: string;
  baseRate: number;
  ttSelling: number;
  ttBuying: number;
  officialRateDate: string;
  source: string;
  changeAmount: number;
  changeRate: number;
  status: 'CONFIRMED' | 'PENDING' | 'OVERRIDDEN';
  updatedAt: string;
}

export interface YieldCurvePoint {
  tenor: string; // '1M', '3M', '6M', '9M', '1Y', '2Y', '3Y', '5Y', '10Y', '20Y', '30Y'
  maturityMonths: number;
  rate: number; // e.g. 3.45 (%)
  changeBp: number; // e.g. -1.2 (bp)
}

export interface YieldCurveDto {
  id: string;
  curveId: string;
  curveName: string;
  currency: string;
  valuationDate: string;
  interpolator: 'CUBIC_SPLINE' | 'NELSON_SIEGEL' | 'LINEAR';
  points: YieldCurvePoint[];
  status: 'ACTIVE' | 'DRAFT' | 'ARCHIVED';
  createdBy: string;
}

export interface ReconciliationDiffDto {
  id: string;
  ruleId: string;
  ruleName: string;
  sourceSystem: string;
  targetSystem: string;
  reconciliationDate: string;
  sourceAmount: number;
  targetAmount: number;
  diffAmount: number;
  status: 'UNRESOLVED' | 'IN_PROGRESS' | 'RESOLVED' | 'EXPLAINED' | 'IGNORED';
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  accountCode: string;
  accountName: string;
  resolutionMemo?: string;
  resolvedBy?: string;
  resolvedAt?: string;
  reasonCategory?: '시차 반영' | '환율 차이' | '수수료 미계상' | '시스템 오류' | '단수 차이';
}

export interface MarketRateDto {
  id: string;
  rateCode: string;
  rateName: string;
  category: '단기지표금리' | '국채/회사채' | '해외지표금리' | '코픽스/기타';
  rateValue: number;
  prevRateValue: number;
  changeBp: number;
  baseDate: string;
  provider: string;
  currency: string;
}

export interface DqMetricDto {
  tableId: string;
  tableName: string;
  domain: string;
  dqScore: number;
  totalRecords: number;
  errorRecords: number;
  completeness: number; // %
  validity: number; // %
  uniqueness: number; // %
  timeliness: number; // %
  lastAudited: string;
  status: 'EXCELLENT' | 'GOOD' | 'WARNING' | 'CRITICAL';
}

export interface MartDatasetDto {
  datasetId: string;
  datasetName: string;
  category: '원장마치' | '계정잔액' | '기준정보' | '대세요약';
  rowCount: number;
  columnCount: number;
  sizeMb: number;
  lastRefreshed: string;
  partitionKey: string;
  columns: Array<{
    name: string;
    type: string;
    nullable: boolean;
    description: string;
  }>;
}

export interface ReconciliationRuleDto {
  id: string;
  ruleName: string;
  category: '원장vs마트' | '계정대계정' | '파생/외화' | '시스템간';
  sourceDataset: string;
  targetDataset: string;
  toleranceAmount: number;
  tolerancePercent: number;
  matchingKeys: string[];
  schedule: 'REALTIME' | 'DAILY' | 'MONTHLY';
  isActive: boolean;
  lastRun: string;
  description: string;
}

export interface ReconciliationRunDto {
  runId: string;
  ruleId: string;
  ruleName: string;
  executedAt: string;
  status: 'SUCCESS' | 'WARNING' | 'FAILED' | 'RUNNING';
  totalRows: number;
  matchedRows: number;
  diffRows: number;
  durationSec: number;
  executedBy: string;
}

export const mockExchangeRates: ExchangeRateDto[] = [
  {
    id: 'EX-20260728-USD',
    currencyPair: 'USD/KRW',
    baseCurrency: 'USD',
    targetCurrency: 'KRW',
    baseRate: 1385.50,
    ttSelling: 1399.30,
    ttBuying: 1371.70,
    officialRateDate: '2026-07-28',
    source: '서울외환중개',
    changeAmount: 4.50,
    changeRate: 0.33,
    status: 'CONFIRMED',
    updatedAt: '2026-07-28 09:30:00',
  },
  {
    id: 'EX-20260728-EUR',
    currencyPair: 'EUR/KRW',
    baseCurrency: 'EUR',
    targetCurrency: 'KRW',
    baseRate: 1506.20,
    ttSelling: 1521.26,
    ttBuying: 1491.14,
    officialRateDate: '2026-07-28',
    source: '한국은행',
    changeAmount: -2.80,
    changeRate: -0.19,
    status: 'CONFIRMED',
    updatedAt: '2026-07-28 09:30:00',
  },
  {
    id: 'EX-20260728-JPY',
    currencyPair: 'JPY(100)/KRW',
    baseCurrency: 'JPY',
    targetCurrency: 'KRW',
    baseRate: 892.40,
    ttSelling: 901.32,
    ttBuying: 883.48,
    officialRateDate: '2026-07-28',
    source: '서울외환중개',
    changeAmount: 1.20,
    changeRate: 0.13,
    status: 'CONFIRMED',
    updatedAt: '2026-07-28 09:30:00',
  },
  {
    id: 'EX-20260728-CNY',
    currencyPair: 'CNY/KRW',
    baseCurrency: 'CNY',
    targetCurrency: 'KRW',
    baseRate: 190.85,
    ttSelling: 194.66,
    ttBuying: 187.04,
    officialRateDate: '2026-07-28',
    source: 'KEB하나은행',
    changeAmount: 0.45,
    changeRate: 0.24,
    status: 'CONFIRMED',
    updatedAt: '2026-07-28 09:30:00',
  },
  {
    id: 'EX-20260728-GBP',
    currencyPair: 'GBP/KRW',
    baseCurrency: 'GBP',
    targetCurrency: 'KRW',
    baseRate: 1782.10,
    ttSelling: 1800.00,
    ttBuying: 1764.20,
    officialRateDate: '2026-07-28',
    source: '한국은행',
    changeAmount: 6.80,
    changeRate: 0.38,
    status: 'CONFIRMED',
    updatedAt: '2026-07-28 09:30:00',
  },
  {
    id: 'EX-20260728-AUD',
    currencyPair: 'AUD/KRW',
    baseCurrency: 'AUD',
    targetCurrency: 'KRW',
    baseRate: 915.60,
    ttSelling: 924.75,
    ttBuying: 906.45,
    officialRateDate: '2026-07-28',
    source: '서울외환중개',
    changeAmount: -1.10,
    changeRate: -0.12,
    status: 'PENDING',
    updatedAt: '2026-07-28 09:15:00',
  },
];

export const mockExchangeRateHistory = [
  { date: '07-21', USD: 1378.0, EUR: 1498.5, JPY: 888.2, CNY: 189.5 },
  { date: '07-22', USD: 1381.2, EUR: 1501.0, JPY: 890.0, CNY: 190.1 },
  { date: '07-23', USD: 1379.5, EUR: 1499.2, JPY: 889.5, CNY: 189.8 },
  { date: '07-24', USD: 1383.0, EUR: 1504.1, JPY: 891.1, CNY: 190.4 },
  { date: '07-25', USD: 1382.0, EUR: 1505.5, JPY: 890.8, CNY: 190.2 },
  { date: '07-26', USD: 1384.1, EUR: 1507.0, JPY: 891.5, CNY: 190.6 },
  { date: '07-27', USD: 1381.0, EUR: 1509.0, JPY: 891.2, CNY: 190.4 },
  { date: '07-28', USD: 1385.5, EUR: 1506.2, JPY: 892.4, CNY: 190.85 },
];

export const mockYieldCurves: YieldCurveDto[] = [
  {
    id: 'YC-001',
    curveId: 'YC-KRW-GOV',
    curveName: '국고채 수익률곡선',
    currency: 'KRW',
    valuationDate: '2026-07-28',
    interpolator: 'CUBIC_SPLINE',
    status: 'ACTIVE',
    createdBy: '채권평가팀',
    points: [
      { tenor: '1M', maturityMonths: 1, rate: 3.25, changeBp: -0.5 },
      { tenor: '3M', maturityMonths: 3, rate: 3.32, changeBp: -0.8 },
      { tenor: '6M', maturityMonths: 6, rate: 3.38, changeBp: -1.0 },
      { tenor: '9M', maturityMonths: 9, rate: 3.41, changeBp: -1.2 },
      { tenor: '1Y', maturityMonths: 12, rate: 3.45, changeBp: -1.5 },
      { tenor: '2Y', maturityMonths: 24, rate: 3.42, changeBp: -2.0 },
      { tenor: '3Y', maturityMonths: 36, rate: 3.38, changeBp: -2.5 },
      { tenor: '5Y', maturityMonths: 60, rate: 3.35, changeBp: -2.8 },
      { tenor: '10Y', maturityMonths: 120, rate: 3.32, changeBp: -3.0 },
      { tenor: '20Y', maturityMonths: 240, rate: 3.28, changeBp: -3.2 },
      { tenor: '30Y', maturityMonths: 360, rate: 3.25, changeBp: -3.5 },
    ],
  },
  {
    id: 'YC-002',
    curveId: 'YC-KRW-CORP-AA',
    curveName: '회사채 AA- 수익률곡선',
    currency: 'KRW',
    valuationDate: '2026-07-28',
    interpolator: 'CUBIC_SPLINE',
    status: 'ACTIVE',
    createdBy: '채권평가팀',
    points: [
      { tenor: '1M', maturityMonths: 1, rate: 3.75, changeBp: 0.2 },
      { tenor: '3M', maturityMonths: 3, rate: 3.88, changeBp: 0.1 },
      { tenor: '6M', maturityMonths: 6, rate: 3.96, changeBp: -0.2 },
      { tenor: '9M', maturityMonths: 9, rate: 4.02, changeBp: -0.5 },
      { tenor: '1Y', maturityMonths: 12, rate: 4.10, changeBp: -0.8 },
      { tenor: '2Y', maturityMonths: 24, rate: 4.15, changeBp: -1.1 },
      { tenor: '3Y', maturityMonths: 36, rate: 4.18, changeBp: -1.5 },
      { tenor: '5Y', maturityMonths: 60, rate: 4.22, changeBp: -1.8 },
      { tenor: '10Y', maturityMonths: 120, rate: 4.30, changeBp: -2.0 },
      { tenor: '20Y', maturityMonths: 240, rate: 4.38, changeBp: -2.2 },
      { tenor: '30Y', maturityMonths: 360, rate: 4.42, changeBp: -2.5 },
    ],
  },
  {
    id: 'YC-003',
    curveId: 'YC-USD-TREASURY',
    curveName: '미 국채 (Treasury) 수익률곡선',
    currency: 'USD',
    valuationDate: '2026-07-28',
    interpolator: 'NELSON_SIEGEL',
    status: 'ACTIVE',
    createdBy: '글로벌리서치팀',
    points: [
      { tenor: '1M', maturityMonths: 1, rate: 5.25, changeBp: 1.0 },
      { tenor: '3M', maturityMonths: 3, rate: 5.20, changeBp: 0.8 },
      { tenor: '6M', maturityMonths: 6, rate: 5.05, changeBp: 0.5 },
      { tenor: '9M', maturityMonths: 9, rate: 4.85, changeBp: 0.0 },
      { tenor: '1Y', maturityMonths: 12, rate: 4.65, changeBp: -0.5 },
      { tenor: '2Y', maturityMonths: 24, rate: 4.35, changeBp: -1.2 },
      { tenor: '3Y', maturityMonths: 36, rate: 4.20, changeBp: -1.8 },
      { tenor: '5Y', maturityMonths: 60, rate: 4.12, changeBp: -2.0 },
      { tenor: '10Y', maturityMonths: 120, rate: 4.18, changeBp: -2.2 },
      { tenor: '20Y', maturityMonths: 240, rate: 4.45, changeBp: -1.5 },
      { tenor: '30Y', maturityMonths: 360, rate: 4.38, changeBp: -1.8 },
    ],
  },
];

export const mockDiffs: ReconciliationDiffDto[] = [
  {
    id: 'DIFF-2026-001',
    ruleId: 'RULE-01',
    ruleName: '원장 vs Mart 단기대출금 잔액 검증',
    sourceSystem: 'GL Core',
    targetSystem: 'Accounting Mart',
    reconciliationDate: '2026-07-28',
    sourceAmount: 14500000000,
    targetAmount: 14485000000,
    diffAmount: 15000000,
    status: 'UNRESOLVED',
    severity: 'HIGH',
    accountCode: '11100-10',
    accountName: '단기대출금 (원화)',
    reasonCategory: '시차 반영',
  },
  {
    id: 'DIFF-2026-002',
    ruleId: 'RULE-02',
    ruleName: '외화예금 미실현 평가손익 검증',
    sourceSystem: 'Trade Hub',
    targetSystem: 'Finance Mart',
    reconciliationDate: '2026-07-28',
    sourceAmount: 3420000000,
    targetAmount: 3420540000,
    diffAmount: -540000,
    status: 'UNRESOLVED',
    severity: 'MEDIUM',
    accountCode: '11200-20',
    accountName: '외화예치금',
    reasonCategory: '환율 차이',
  },
  {
    id: 'DIFF-2026-003',
    ruleId: 'RULE-03',
    ruleName: '파생상품 공정가치 차액 검증',
    sourceSystem: 'Derivatives Core',
    targetSystem: 'Risk Data Mart',
    reconciliationDate: '2026-07-27',
    sourceAmount: 89000000000,
    targetAmount: 88750000000,
    diffAmount: 250000000,
    status: 'IN_PROGRESS',
    severity: 'CRITICAL',
    accountCode: '12400-05',
    accountName: '통화스왑 파생자산',
    resolutionMemo: '7/27 거래분 장후 승인 전표 미인증 건 확인 중. 담당자 재승인 요청 완료.',
    resolvedBy: '김회계 과장',
    reasonCategory: '시스템 오류',
  },
  {
    id: 'DIFF-2026-004',
    ruleId: 'RULE-04',
    ruleName: '미지급수수료 계정 일치 검증',
    sourceSystem: 'GL Core',
    targetSystem: 'Accounting Mart',
    reconciliationDate: '2026-07-27',
    sourceAmount: 485000000,
    targetAmount: 485000012,
    diffAmount: -12,
    status: 'RESOLVED',
    severity: 'LOW',
    accountCode: '21300-40',
    accountName: '미지급수수료',
    resolutionMemo: '소수점 원화 반올림 처리 단수 차이 확인 (해소 소감 처리 완료)',
    resolvedBy: '이재무 대리',
    resolvedAt: '2026-07-27 17:40:12',
    reasonCategory: '단수 차이',
  },
  {
    id: 'DIFF-2026-005',
    ruleId: 'RULE-05',
    ruleName: '매출채권 총계정원장 vs 수납마장 일치',
    sourceSystem: 'AR Engine',
    targetSystem: 'Accounting Mart',
    reconciliationDate: '2026-07-26',
    sourceAmount: 65400000000,
    targetAmount: 65380000000,
    diffAmount: 20000000,
    status: 'EXPLAINED',
    severity: 'MEDIUM',
    accountCode: '11300-10',
    accountName: '외상매출금',
    resolutionMemo: '7/26 영업일 마감 후 은행 가상계좌 입금 처리 건 (7/27 익일 자동 상계 처리)',
    resolvedBy: '박자산 대리',
    resolvedAt: '2026-07-27 09:15:00',
    reasonCategory: '시차 반영',
  },
];

export const mockMarketRates: MarketRateDto[] = [
  {
    id: 'MR-001',
    rateCode: 'BOK_BASE',
    rateName: '한국은행 기준금리',
    category: '단기지표금리',
    rateValue: 3.50,
    prevRateValue: 3.50,
    changeBp: 0.0,
    baseDate: '2026-07-28',
    provider: '한국은행',
    currency: 'KRW',
  },
  {
    id: 'MR-002',
    rateCode: 'CD91',
    rateName: 'CD 91일물 고시금리',
    category: '단기지표금리',
    rateValue: 3.68,
    prevRateValue: 3.70,
    changeBp: -2.0,
    baseDate: '2026-07-28',
    provider: '금융투자협회',
    currency: 'KRW',
  },
  {
    id: 'MR-003',
    rateCode: 'CP91',
    rateName: 'CP 91일물 (A1등급)',
    category: '단기지표금리',
    rateValue: 4.12,
    prevRateValue: 4.15,
    changeBp: -3.0,
    baseDate: '2026-07-28',
    provider: '금융투자협회',
    currency: 'KRW',
  },
  {
    id: 'MR-004',
    rateCode: 'KORIBOR3M',
    rateName: 'KORIBOR 3개월',
    category: '단기지표금리',
    rateValue: 3.65,
    prevRateValue: 3.66,
    changeBp: -1.0,
    baseDate: '2026-07-28',
    provider: '연합인포맥스',
    currency: 'KRW',
  },
  {
    id: 'MR-005',
    rateCode: 'KTB_3Y',
    rateName: '국고채 3년물',
    category: '국채/회사채',
    rateValue: 3.38,
    prevRateValue: 3.405,
    changeBp: -2.5,
    baseDate: '2026-07-28',
    provider: '금융투자협회',
    currency: 'KRW',
  },
  {
    id: 'MR-006',
    rateCode: 'KTB_10Y',
    rateName: '국고채 10년물',
    category: '국채/회사채',
    rateValue: 3.32,
    prevRateValue: 3.35,
    changeBp: -3.0,
    baseDate: '2026-07-28',
    provider: '금융투자협회',
    currency: 'KRW',
  },
  {
    id: 'MR-007',
    rateCode: 'SOFR_1M',
    rateName: 'SOFR (미국 익일물 금리)',
    category: '해외지표금리',
    rateValue: 5.31,
    prevRateValue: 5.30,
    changeBp: 1.0,
    baseDate: '2026-07-28',
    provider: 'NY Fed',
    currency: 'USD',
  },
  {
    id: 'MR-008',
    rateCode: 'COFIX_NEW',
    rateName: 'COFIX (신규취급액기준)',
    category: '코픽스/기타',
    rateValue: 3.52,
    prevRateValue: 3.56,
    changeBp: -4.0,
    baseDate: '2026-07-15',
    provider: '은행연합회',
    currency: 'KRW',
  },
];

export const mockDqMetrics: DqMetricDto[] = [
  {
    tableId: 'TBL-001',
    tableName: 'DM_GL_JOURNAL_DAILY',
    domain: '원장마치',
    dqScore: 99.4,
    totalRecords: 1450200,
    errorRecords: 870,
    completeness: 99.9,
    validity: 99.2,
    uniqueness: 100.0,
    timeliness: 98.5,
    lastAudited: '2026-07-28 04:00',
    status: 'EXCELLENT',
  },
  {
    tableId: 'TBL-002',
    tableName: 'DM_ACCOUNT_BALANCE_M',
    domain: '계정잔액',
    dqScore: 98.8,
    totalRecords: 480000,
    errorRecords: 5760,
    completeness: 99.5,
    validity: 98.4,
    uniqueness: 100.0,
    timeliness: 97.3,
    lastAudited: '2026-07-28 04:00',
    status: 'EXCELLENT',
  },
  {
    tableId: 'TBL-003',
    tableName: 'DM_EXCHANGE_RATE_H',
    domain: '기준정보',
    dqScore: 100.0,
    totalRecords: 35000,
    errorRecords: 0,
    completeness: 100.0,
    validity: 100.0,
    uniqueness: 100.0,
    timeliness: 100.0,
    lastAudited: '2026-07-28 04:00',
    status: 'EXCELLENT',
  },
  {
    tableId: 'TBL-004',
    tableName: 'DM_RECON_SUMMARY_D',
    domain: '대세요약',
    dqScore: 94.2,
    totalRecords: 120500,
    errorRecords: 6989,
    completeness: 96.1,
    validity: 92.5,
    uniqueness: 99.8,
    timeliness: 98.4,
    lastAudited: '2026-07-28 04:00',
    status: 'WARNING',
  },
  {
    tableId: 'TBL-005',
    tableName: 'DM_DERIVATIVE_VALUATION',
    domain: '파생/외화',
    dqScore: 91.5,
    totalRecords: 45000,
    errorRecords: 3825,
    completeness: 94.0,
    validity: 89.5,
    uniqueness: 99.5,
    timeliness: 93.0,
    lastAudited: '2026-07-28 04:00',
    status: 'WARNING',
  },
];

export const mockMartDatasets: MartDatasetDto[] = [
  {
    datasetId: 'DS-01',
    datasetName: 'DM_GL_JOURNAL_DAILY',
    category: '원장마치',
    rowCount: 1450200,
    columnCount: 18,
    sizeMb: 420.5,
    lastRefreshed: '2026-07-28 03:30:00',
    partitionKey: 'POSTING_DATE',
    columns: [
      { name: 'JOURNAL_ID', type: 'VARCHAR(32)', nullable: false, description: '전표 식별키' },
      { name: 'POSTING_DATE', type: 'DATE', nullable: false, description: '전표 회계일자' },
      { name: 'ACCT_CODE', type: 'VARCHAR(20)', nullable: false, description: '계정과목 코드' },
      { name: 'ACCT_NAME', type: 'VARCHAR(100)', nullable: false, description: '계정과목명' },
      { name: 'DEBIT_AMT', type: 'DECIMAL(18,2)', nullable: false, description: '차변 금액' },
      { name: 'CREDIT_AMT', type: 'DECIMAL(18,2)', nullable: false, description: '대변 금액' },
      { name: 'CURRENCY', type: 'VARCHAR(3)', nullable: false, description: '통화' },
      { name: 'DEPT_CODE', type: 'VARCHAR(10)', nullable: true, description: '부서코드' },
    ],
  },
  {
    datasetId: 'DS-02',
    datasetName: 'DM_ACCOUNT_BALANCE_M',
    category: '계정잔액',
    rowCount: 480000,
    columnCount: 14,
    sizeMb: 115.2,
    lastRefreshed: '2026-07-28 04:00:00',
    partitionKey: 'YYYYMM',
    columns: [
      { name: 'YYYYMM', type: 'VARCHAR(6)', nullable: false, description: '결산 연월' },
      { name: 'ACCT_CODE', type: 'VARCHAR(20)', nullable: false, description: '계정과목 코드' },
      { name: 'OPENING_BAL', type: 'DECIMAL(18,2)', nullable: false, description: '기초 잔액' },
      { name: 'CLOSING_BAL', type: 'DECIMAL(18,2)', nullable: false, description: '기말 잔액' },
      { name: 'NET_CHANGE', type: 'DECIMAL(18,2)', nullable: false, description: '당월 변동액' },
    ],
  },
  {
    datasetId: 'DS-03',
    datasetName: 'DM_EXCHANGE_RATE_H',
    category: '기준정보',
    rowCount: 35000,
    columnCount: 10,
    sizeMb: 12.8,
    lastRefreshed: '2026-07-28 09:30:00',
    partitionKey: 'BASE_DATE',
    columns: [
      { name: 'BASE_DATE', type: 'DATE', nullable: false, description: '고시 일자' },
      { name: 'CURR_PAIR', type: 'VARCHAR(10)', nullable: false, description: '통화쌍' },
      { name: 'BASE_RATE', type: 'DECIMAL(12,4)', nullable: false, description: '매매기준율' },
      { name: 'SOURCE', type: 'VARCHAR(50)', nullable: false, description: '고시 기관' },
    ],
  },
  {
    datasetId: 'DS-04',
    datasetName: 'DM_RECON_SUMMARY_D',
    category: '대세요약',
    rowCount: 120500,
    columnCount: 16,
    sizeMb: 48.6,
    lastRefreshed: '2026-07-28 05:00:00',
    partitionKey: 'RECON_DATE',
    columns: [
      { name: 'RECON_DATE', type: 'DATE', nullable: false, description: '대사 일자' },
      { name: 'RULE_ID', type: 'VARCHAR(20)', nullable: false, description: '대사 규칙 ID' },
      { name: 'SRC_TOTAL_AMT', type: 'DECIMAL(18,2)', nullable: false, description: '소스 총액' },
      { name: 'TGT_TOTAL_AMT', type: 'DECIMAL(18,2)', nullable: false, description: '타겟 총액' },
      { name: 'DIFF_AMT', type: 'DECIMAL(18,2)', nullable: false, description: '차액' },
    ],
  },
];

export const mockExplorerRows = [
  {
    id: 1,
    POSTING_DATE: '2026-07-28',
    JOURNAL_ID: 'JNL-20260728-001',
    ACCT_CODE: '11100-10',
    ACCT_NAME: '단기대출금 (원화)',
    DEBIT_AMT: 150000000,
    CREDIT_AMT: 0,
    CURRENCY: 'KRW',
    DEPT_CODE: 'LOAN-01',
    REMARKS: '기업 운고자금 단기 대출 집행',
  },
  {
    id: 2,
    POSTING_DATE: '2026-07-28',
    JOURNAL_ID: 'JNL-20260728-002',
    ACCT_CODE: '11200-20',
    ACCT_NAME: '외화예치금',
    DEBIT_AMT: 54000000,
    CREDIT_AMT: 0,
    CURRENCY: 'USD',
    DEPT_CODE: 'TREASURY',
    REMARKS: 'USD 외화 정기예금 만기 재투자',
  },
  {
    id: 3,
    POSTING_DATE: '2026-07-28',
    JOURNAL_ID: 'JNL-20260728-003',
    ACCT_CODE: '21300-40',
    ACCT_NAME: '미지급수수료',
    DEBIT_AMT: 0,
    CREDIT_AMT: 12500000,
    CURRENCY: 'KRW',
    DEPT_CODE: 'FINANCE-02',
    REMARKS: '7월 파생상품 중개 수수료 정산계상',
  },
  {
    id: 4,
    POSTING_DATE: '2026-07-27',
    JOURNAL_ID: 'JNL-20260727-099',
    ACCT_CODE: '12400-05',
    ACCT_NAME: '통화스왑 파생자산',
    DEBIT_AMT: 250000000,
    CREDIT_AMT: 0,
    CURRENCY: 'KRW',
    DEPT_CODE: 'RISK-01',
    REMARKS: '기말 파생상품 평가이익 반영',
  },
  {
    id: 5,
    POSTING_DATE: '2026-07-27',
    JOURNAL_ID: 'JNL-20260727-100',
    ACCT_CODE: '11300-10',
    ACCT_NAME: '외상매출금',
    DEBIT_AMT: 0,
    CREDIT_AMT: 2000000000,
    CURRENCY: 'KRW',
    DEPT_CODE: 'SALES-01',
    REMARKS: '주요 법인 거래처 수납 입금 처리',
  },
];

export const mockReconciliationRules: ReconciliationRuleDto[] = [
  {
    id: 'RULE-01',
    ruleName: '원장 vs Mart 단기대출금 잔액 검증',
    category: '원장vs마트',
    sourceDataset: 'GL Core (TB_GL_BALANCE)',
    targetDataset: 'Accounting Mart (DM_ACCOUNT_BALANCE_M)',
    toleranceAmount: 10000,
    tolerancePercent: 0.01,
    matchingKeys: ['POSTING_DATE', 'ACCT_CODE'],
    schedule: 'DAILY',
    isActive: true,
    lastRun: '2026-07-28 05:00',
    description: '원장과 데이터마트 간 단기대출금 총 잔액 1만원 이내 일치성 검증',
  },
  {
    id: 'RULE-02',
    ruleName: '외화예금 미실현 평가손익 검증',
    category: '파생/외화',
    sourceDataset: 'Trade Hub (TB_FX_DEPOSIT)',
    targetDataset: 'Finance Mart (DM_EXCHANGE_RATE_H)',
    toleranceAmount: 50000,
    tolerancePercent: 0.05,
    matchingKeys: ['BASE_DATE', 'CURRENCY'],
    schedule: 'DAILY',
    isActive: true,
    lastRun: '2026-07-28 05:05',
    description: '외통 평가 고시환율 기반 외화예금 평가손익 원장 대사',
  },
  {
    id: 'RULE-03',
    ruleName: '파생상품 공정가치 차액 검증',
    category: '파생/외화',
    sourceDataset: 'Derivatives Core (TB_DERIV_VAL)',
    targetDataset: 'Risk Data Mart (DM_DERIVATIVE_VALUATION)',
    toleranceAmount: 1000000,
    tolerancePercent: 0.1,
    matchingKeys: ['VALUATION_DATE', 'DEAL_ID'],
    schedule: 'DAILY',
    isActive: true,
    lastRun: '2026-07-27 23:30',
    description: '파생상품 공정가치 원장 계상액과 리스크 마트 산출액 비교',
  },
  {
    id: 'RULE-04',
    ruleName: '미지급수수료 계정 일치 검증',
    category: '계정대계정',
    sourceDataset: 'GL Core (TB_AP_SLIP)',
    targetDataset: 'Accounting Mart (DM_GL_JOURNAL_DAILY)',
    toleranceAmount: 100,
    tolerancePercent: 0.0,
    matchingKeys: ['SLIP_NO', 'ACCT_CODE'],
    schedule: 'REALTIME',
    isActive: true,
    lastRun: '2026-07-28 08:00',
    description: '미지급 수수료 세부 전표 건별 100원 이내 자동 정산 일치 확인',
  },
  {
    id: 'RULE-05',
    ruleName: '매출채권 총계정원장 vs 수납마장 일치',
    category: '시스템간',
    sourceDataset: 'AR Engine (TB_RECEIVABLE)',
    targetDataset: 'Accounting Mart (DM_RECON_SUMMARY_D)',
    toleranceAmount: 500000,
    tolerancePercent: 0.02,
    matchingKeys: ['CUSTOMER_ID', 'DUE_DATE'],
    schedule: 'DAILY',
    isActive: true,
    lastRun: '2026-07-27 18:00',
    description: '매출채권 입금 수납내역과 총계정원장 상계전표 일괄 대사',
  },
];

export const mockReconciliationRuns: ReconciliationRunDto[] = [
  {
    runId: 'RUN-20260728-001',
    ruleId: 'RULE-01',
    ruleName: '원장 vs Mart 단기대출금 잔액 검증',
    executedAt: '2026-07-28 05:00:12',
    status: 'WARNING',
    totalRows: 14200,
    matchedRows: 14198,
    diffRows: 2,
    durationSec: 3.4,
    executedBy: 'SYSTEM_BATCH',
  },
  {
    runId: 'RUN-20260728-002',
    ruleId: 'RULE-02',
    ruleName: '외화예금 미실현 평가손익 검증',
    executedAt: '2026-07-28 05:05:44',
    status: 'WARNING',
    totalRows: 8500,
    matchedRows: 8499,
    diffRows: 1,
    durationSec: 2.1,
    executedBy: 'SYSTEM_BATCH',
  },
  {
    runId: 'RUN-20260728-003',
    ruleId: 'RULE-04',
    ruleName: '미지급수수료 계정 일치 검증',
    executedAt: '2026-07-28 08:00:02',
    status: 'SUCCESS',
    totalRows: 3400,
    matchedRows: 3400,
    diffRows: 0,
    durationSec: 1.2,
    executedBy: 'SYSTEM_BATCH',
  },
  {
    runId: 'RUN-20260727-004',
    ruleId: 'RULE-03',
    ruleName: '파생상품 공정가치 차액 검증',
    executedAt: '2026-07-27 23:30:15',
    status: 'FAILED',
    totalRows: 1250,
    matchedRows: 1248,
    diffRows: 2,
    durationSec: 5.8,
    executedBy: '강팀장 (수동)',
  },
  {
    runId: 'RUN-20260727-005',
    ruleId: 'RULE-05',
    ruleName: '매출채권 총계정원장 vs 수납마장 일치',
    executedAt: '2026-07-27 18:00:00',
    status: 'SUCCESS',
    totalRows: 42000,
    matchedRows: 41999,
    diffRows: 1,
    durationSec: 8.9,
    executedBy: 'SYSTEM_BATCH',
  },
];
