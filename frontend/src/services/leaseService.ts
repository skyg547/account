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
    const response = await fetch(API_BASE_URL);
    if (!response.ok) throw new Error('Failed to fetch lease contracts');
    return response.json();
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
