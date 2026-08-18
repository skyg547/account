/**
 * 리스 회계 관리 서비스 (Lease Service)
 * 백엔드 LeaseAccountingController와 연동하여 리스 데이터를 처리합니다.
 */

export interface LeaseContract {
  id: number;
  contractNo: string;
  contractName: string;
  lessorCode: string;
  startDate: string;
  endDate: string;
  monthlyPayment: number;
  paymentDay: number;
  discountRate: number;
  initialRightOfUseAssetValue: number;
  initialLeaseLiabilityValue: number;
  status: 'ACTIVE' | 'TERMINATED' | 'MODIFIED';
  departmentCode: string;
  expenseAccountCode: string;
  ifrs16Applicable: boolean;
  shortTermLease: boolean;
  lowValueLease: boolean;
}

export interface LeaseContractRequest {
  contractNo: string;
  contractName: string;
  startDate: string;
  endDate: string;
  monthlyPayment: number;
  paymentDay: number;
  lessorBusinessPartnerCode: string;
  departmentCode: string;
  expenseAccountCode: string;
  discountRate: number;
  initialRightOfUseAssetValue: number;
  initialLeaseLiabilityValue: number;
  ifrs16Applicable?: boolean;
  shortTermLease?: boolean;
  lowValueLease?: boolean;
  status?: string;
}

const API_BASE_URL = '/api/ifrs16/leases';

export const leaseService = {
  /**
   * 리스 계약 목록 조회
   */
  async getLeases(): Promise<LeaseContract[]> {
    try {
      const response = await fetch(API_BASE_URL);
      if (response.ok) {
        return await response.json();
      }
    } catch {
      // Fallback to mock
    }
    return [
      {
        id: 1,
        contractNo: 'LS-2026-001',
        contractName: '을지로 본사 사옥 12층',
        lessorCode: 'BP-002',
        startDate: '2026-01-01',
        endDate: '2031-12-31',
        monthlyPayment: 12500000,
        paymentDay: 25,
        discountRate: 4.5,
        initialRightOfUseAssetValue: 785000000,
        initialLeaseLiabilityValue: 783000000,
        status: 'ACTIVE',
        departmentCode: '총무팀',
        expenseAccountCode: '5202',
        ifrs16Applicable: true,
        shortTermLease: false,
        lowValueLease: false,
      },
      {
        id: 2,
        contractNo: 'LS-2026-002',
        contractName: '판교 R&D 센터 업무용 차량 3대',
        lessorCode: 'BP-003',
        startDate: '2026-03-01',
        endDate: '2029-02-28',
        monthlyPayment: 2800000,
        paymentDay: 10,
        discountRate: 5.0,
        initialRightOfUseAssetValue: 94000000,
        initialLeaseLiabilityValue: 93500000,
        status: 'ACTIVE',
        departmentCode: 'IT운영팀',
        expenseAccountCode: '5202',
        ifrs16Applicable: true,
        shortTermLease: false,
        lowValueLease: false,
      },
    ];
  },

  /**
   * 리스 계약 단건 조회
   */
  async getLeaseById(id: number): Promise<LeaseContract> {
    const response = await fetch(`${API_BASE_URL}/${id}`);
    if (!response.ok) throw new Error('Lease contract not found');
    return response.json();
  },

  /**
   * 리스 계약 등록
   */
  async createLease(request: LeaseContractRequest): Promise<LeaseContract> {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!response.ok) throw new Error('Failed to create lease contract');
    return response.json();
  },

  /**
   * 월별 리스 회계 처리 실행
   */
  async processMonthly(processDate: string): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/process-monthly/${processDate}`, {
      method: 'POST',
    });
    if (!response.ok) throw new Error('Failed to process monthly lease accounting');
    return response.text();
  }
};
