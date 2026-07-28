export interface TaxInvoiceDto {
  id: string;
  ntsApprovalNum: string;
  issueDate: string;
  type: 'PURCHASE' | 'SALES';
  invoiceType: 'NORMAL' | 'MODIFIED' | 'ZERO_RATE';
  supplierName: string;
  supplierBizNum: string;
  buyerName: string;
  buyerBizNum: string;
  supplyAmount: number;
  taxAmount: number;
  totalAmount: number;
  ntsStatus: 'APPROVED' | 'SYNCHRONIZED' | 'PENDING' | 'REJECTED' | 'DISCREPANCY';
  ntsStatusDate: string;
  itemSummary: string;
  remark?: string;
  deductible?: boolean;
  modifiedReason?: string;
}

export interface VatSummaryDto {
  period: string;
  year: number;
  quarter: string;
  salesTotalSupply: number;
  salesTotalTax: number;
  purchaseTotalSupply: number;
  purchaseTotalTax: number;
  deductiblePurchaseTax: number;
  nonDeductiblePurchaseTax: number;
  netTaxPayable: number;
  status: 'DRAFT' | 'READY' | 'SUBMITTED';
  filingDueDate: string;
  salesBreakdown: {
    taxableNormal: { supply: number; tax: number };
    taxableElectronic: { supply: number; tax: number };
    zeroRateDirect: { supply: number; tax: number };
    zeroRateLocalLC: { supply: number; tax: number };
  };
  purchaseBreakdown: {
    generalPurchase: { supply: number; tax: number };
    fixedAssetPurchase: { supply: number; tax: number };
    nonDeductible: { supply: number; tax: number };
  };
}

export interface NtsComparisonDto {
  id: string;
  invoiceId: string;
  ntsApprovalNum: string;
  partnerName: string;
  partnerBizNum: string;
  erpSupplyAmount: number;
  erpTaxAmount: number;
  ntsSupplyAmount: number;
  ntsTaxAmount: number;
  diffType: 'AMOUNT_MISMATCH' | 'MISSING_IN_ERP' | 'MISSING_IN_NTS' | 'STATUS_MISMATCH';
  status: 'UNRESOLVED' | 'RESOLVED' | 'IGNORED';
  issueDate: string;
  note: string;
}

export const mockPurchaseInvoices: TaxInvoiceDto[] = [
  {
    id: 'TXP-202507-001',
    ntsApprovalNum: '20250701-41000000-00018921',
    issueDate: '2025-07-01',
    type: 'PURCHASE',
    invoiceType: 'NORMAL',
    supplierName: '(주)한국클라우드시스템',
    supplierBizNum: '107-86-12345',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: 15000000,
    taxAmount: 1500000,
    totalAmount: 16500000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-01 14:30',
    itemSummary: '7월 클라우드 서버 인프라 호스팅 비용',
    deductible: true,
    remark: '정기 결제'
  },
  {
    id: 'TXP-202507-002',
    ntsApprovalNum: '20250705-41000000-00019002',
    issueDate: '2025-07-05',
    type: 'PURCHASE',
    invoiceType: 'NORMAL',
    supplierName: '넥스트소프트웨어(주)',
    supplierBizNum: '214-87-34567',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: 8400000,
    taxAmount: 840000,
    totalAmount: 9240000,
    ntsStatus: 'SYNCHRONIZED',
    ntsStatusDate: '2025-07-05 16:10',
    itemSummary: 'ERP 개발 라이선스 유지보수료 (3분기)',
    deductible: true
  },
  {
    id: 'TXP-202507-003',
    ntsApprovalNum: '20250710-41000000-00019150',
    issueDate: '2025-07-10',
    type: 'PURCHASE',
    invoiceType: 'NORMAL',
    supplierName: '삼정로지스틱스(주)',
    supplierBizNum: '119-81-67890',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: 3200000,
    taxAmount: 320000,
    totalAmount: 3520000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-10 10:15',
    itemSummary: '물류 물품 운송 서비스',
    deductible: true
  },
  {
    id: 'TXP-202507-004',
    ntsApprovalNum: '20250712-41000000-00019230',
    issueDate: '2025-07-12',
    type: 'PURCHASE',
    invoiceType: 'NORMAL',
    supplierName: '프라임임대관리(주)',
    supplierBizNum: '305-81-11223',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: 22000000,
    taxAmount: 2200000,
    totalAmount: 24200000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-12 11:00',
    itemSummary: '7월 본사 오피스 임차료',
    deductible: true
  },
  {
    id: 'TXP-202507-005',
    ntsApprovalNum: '20250718-41000000-00019488',
    issueDate: '2025-07-18',
    type: 'PURCHASE',
    invoiceType: 'NORMAL',
    supplierName: '명품모터스(주)',
    supplierBizNum: '101-85-44332',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: 45000000,
    taxAmount: 4500000,
    totalAmount: 49500000,
    ntsStatus: 'DISCREPANCY',
    ntsStatusDate: '2025-07-18 17:05',
    itemSummary: '임원용 비영업용 승용차 구입',
    deductible: false,
    remark: '비영업용 소형승용차 불공제 대상'
  },
  {
    id: 'TXP-202507-006',
    ntsApprovalNum: '20250722-41000000-00019670',
    issueDate: '2025-07-22',
    type: 'PURCHASE',
    invoiceType: 'MODIFIED',
    supplierName: '(주)한성사무기기',
    supplierBizNum: '108-82-99887',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: -1200000,
    taxAmount: -120000,
    totalAmount: -1320000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-22 09:40',
    itemSummary: '[수정] 복합기 반품에 따른 기재사항 착오정정',
    deductible: true,
    modifiedReason: '품목 반품'
  },
  {
    id: 'TXP-202507-007',
    ntsApprovalNum: '20250725-41000000-00019810',
    issueDate: '2025-07-25',
    type: 'PURCHASE',
    invoiceType: 'NORMAL',
    supplierName: '(주)글로벌마케팅그룹',
    supplierBizNum: '211-88-77665',
    buyerName: '(주)스카이솔루션',
    buyerBizNum: '220-81-98765',
    supplyAmount: 18000000,
    taxAmount: 1800000,
    totalAmount: 19800000,
    ntsStatus: 'PENDING',
    ntsStatusDate: '2025-07-25 15:20',
    itemSummary: '3분기 디지털 광고 대행 수수료',
    deductible: true
  }
];

export const mockSalesInvoices: TaxInvoiceDto[] = [
  {
    id: 'TXS-202507-001',
    ntsApprovalNum: '20250702-41000000-00088101',
    issueDate: '2025-07-02',
    type: 'SALES',
    invoiceType: 'NORMAL',
    supplierName: '(주)스카이솔루션',
    supplierBizNum: '220-81-98765',
    buyerName: '(주)미래에셋증권',
    buyerBizNum: '116-81-01010',
    supplyAmount: 85000000,
    taxAmount: 8500000,
    totalAmount: 93500000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-02 11:20',
    itemSummary: '금융 데이터 분석 플랫폼 구축 2차 중도금',
    remark: '계약서 2025-FIN-04 참조'
  },
  {
    id: 'TXS-202507-002',
    ntsApprovalNum: '20250708-41000000-00088215',
    issueDate: '2025-07-08',
    type: 'SALES',
    invoiceType: 'NORMAL',
    supplierName: '(주)스카이솔루션',
    supplierBizNum: '220-81-98765',
    buyerName: '현대글로비스(주)',
    buyerBizNum: '104-81-54321',
    supplyAmount: 42000000,
    taxAmount: 4200000,
    totalAmount: 46200000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-08 14:50',
    itemSummary: 'SM 스마트물류 솔루션 월간 운영료',
    remark: '7월분'
  },
  {
    id: 'TXS-202507-003',
    ntsApprovalNum: '20250715-41000000-00088410',
    issueDate: '2025-07-15',
    type: 'SALES',
    invoiceType: 'ZERO_RATE',
    supplierName: '(주)스카이솔루션',
    supplierBizNum: '220-81-98765',
    buyerName: 'GLOBAL TECH USA INC.',
    buyerBizNum: '999-88-00001',
    supplyAmount: 120000000,
    taxAmount: 0,
    totalAmount: 120000000,
    ntsStatus: 'APPROVED',
    ntsStatusDate: '2025-07-15 16:00',
    itemSummary: 'SaaS 해외 라이선스 공급 (영세율)',
    remark: '구매확인서 첨부'
  },
  {
    id: 'TXS-202507-004',
    ntsApprovalNum: '20250720-41000000-00088590',
    issueDate: '2025-07-20',
    type: 'SALES',
    invoiceType: 'NORMAL',
    supplierName: '(주)스카이솔루션',
    supplierBizNum: '220-81-98765',
    buyerName: 'SK텔레콤(주)',
    buyerBizNum: '104-81-33221',
    supplyAmount: 38000000,
    taxAmount: 3800000,
    totalAmount: 41800000,
    ntsStatus: 'SYNCHRONIZED',
    ntsStatusDate: '2025-07-20 18:30',
    itemSummary: 'AI 에이전트 커스텀 모듈 공급',
    remark: '1차 납품'
  },
  {
    id: 'TXS-202507-005',
    ntsApprovalNum: '20250726-41000000-00088732',
    issueDate: '2025-07-26',
    type: 'SALES',
    invoiceType: 'NORMAL',
    supplierName: '(주)스카이솔루션',
    supplierBizNum: '220-81-98765',
    buyerName: '(주)카카오뱅크',
    buyerBizNum: '314-86-55443',
    supplyAmount: 29000000,
    taxAmount: 2900000,
    totalAmount: 31900000,
    ntsStatus: 'PENDING',
    ntsStatusDate: '2025-07-26 10:10',
    itemSummary: '보안 솔루션 연동 개발비',
    remark: '발행 완료 / 전송 대기'
  }
];

export const mockVatSummary: VatSummaryDto = {
  period: '2025년 2기 예정 (07월~09월)',
  year: 2025,
  quarter: '2기 예정',
  salesTotalSupply: 314000000,
  salesTotalTax: 19400000,
  purchaseTotalSupply: 114600000,
  purchaseTotalTax: 11460000,
  deductiblePurchaseTax: 6960000,
  nonDeductiblePurchaseTax: 4500000,
  netTaxPayable: 12440000,
  status: 'DRAFT',
  filingDueDate: '2025-10-25',
  salesBreakdown: {
    taxableNormal: { supply: 194000000, tax: 19400000 },
    taxableElectronic: { supply: 0, tax: 0 },
    zeroRateDirect: { supply: 120000000, tax: 0 },
    zeroRateLocalLC: { supply: 0, tax: 0 }
  },
  purchaseBreakdown: {
    generalPurchase: { supply: 69600000, tax: 6960000 },
    fixedAssetPurchase: { supply: 0, tax: 0 },
    nonDeductible: { supply: 45000000, tax: 4500000 }
  }
};

export const mockNtsComparisons: NtsComparisonDto[] = [
  {
    id: 'COMP-001',
    invoiceId: 'TXP-202507-005',
    ntsApprovalNum: '20250718-41000000-00019488',
    partnerName: '명품모터스(주)',
    partnerBizNum: '101-85-44332',
    erpSupplyAmount: 45000000,
    erpTaxAmount: 4500000,
    ntsSupplyAmount: 45000000,
    ntsTaxAmount: 4500000,
    diffType: 'STATUS_MISMATCH',
    status: 'UNRESOLVED',
    issueDate: '2025-07-18',
    note: 'ERP상 불공제 세액 분류 필요 (비영업용 승용차)'
  },
  {
    id: 'COMP-002',
    invoiceId: 'TXP-202507-099',
    ntsApprovalNum: '20250720-41000000-00099881',
    partnerName: '(주)데이타기술',
    partnerBizNum: '128-81-00112',
    erpSupplyAmount: 0,
    erpTaxAmount: 0,
    ntsSupplyAmount: 5000000,
    ntsTaxAmount: 500000,
    diffType: 'MISSING_IN_ERP',
    status: 'UNRESOLVED',
    issueDate: '2025-07-20',
    note: '국세청 홈택스에만 존재하는 매입 세금계산서 (전표 미등록)'
  },
  {
    id: 'COMP-003',
    invoiceId: 'TXS-202507-088',
    ntsApprovalNum: '20250722-41000000-00077665',
    partnerName: '(주)네오시스템',
    partnerBizNum: '215-88-33441',
    erpSupplyAmount: 12000000,
    erpTaxAmount: 1200000,
    ntsSupplyAmount: 10000000,
    ntsTaxAmount: 1000000,
    diffType: 'AMOUNT_MISMATCH',
    status: 'UNRESOLVED',
    issueDate: '2025-07-22',
    note: '공급가액 차이 발생 (ERP 1,200만원 vs 홈택스 1,000만원)'
  },
  {
    id: 'COMP-004',
    invoiceId: 'TXS-202507-090',
    ntsApprovalNum: '20250724-41000000-00088990',
    partnerName: '(주)이노베이션랩',
    partnerBizNum: '110-86-77889',
    erpSupplyAmount: 6500000,
    erpTaxAmount: 650000,
    ntsSupplyAmount: 0,
    ntsTaxAmount: 0,
    diffType: 'MISSING_IN_NTS',
    status: 'RESOLVED',
    issueDate: '2025-07-24',
    note: '국세청 전송 승인 대기 중 (소기업 전송 지연)'
  }
];
