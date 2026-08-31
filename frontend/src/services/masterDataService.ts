export interface AccountSubjectDto {
  code: string;
  name: string;
  type: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'INCOME' | 'EXPENSE';
  category: 'GROUP' | 'SUBJECT';
  parentCode?: string;
  status: 'ACTIVE' | 'INACTIVE';
  validFrom: string;
  validTo: string;
}

export type PartnerType = 'CUSTOMER' | 'VENDOR' | 'BANK' | 'OTHER_BP';
export type KycStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVIEW_REQUIRED';
export type RiskRating = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface BusinessPartnerCommand {
  businessPartnerCode: string;
  businessPartnerName: string;
  registrationNumber: string;
  ceoName: string;
  businessType: string;
  businessItem: string;
  partnerType: PartnerType;
  useYn: boolean;
  kycStatus: KycStatus;
  riskRating: RiskRating;
  validFrom: string;
  validTo: string;
}

export interface BusinessPartnerDto extends BusinessPartnerCommand {
  id: number;
}

export type ChangeRequestStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'APPLIED';

export interface MasterDataChangeRequestDto {
  id: number;
  targetType: 'BUSINESS_PARTNER' | string;
  targetKey: string;
  changeType: 'CREATE' | 'UPDATE' | 'DEACTIVATE';
  status: ChangeRequestStatus;
  effectiveDate: string;
  requestedVersion: number;
  requestedBy: string;
  approvedBy: string | null;
  requestedAt: string;
  approvedAt: string | null;
  reason: string | null;
  payloadJson: string | null;
  sourceReference: string | null;
  appliedAt: string | null;
}

const API_BASE_URL = '/api';

function apiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  if (API_BASE_URL.endsWith('/api') && normalizedPath.startsWith('/api/')) {
    return `${API_BASE_URL}${normalizedPath.slice('/api'.length)}`;
  }
  return `${API_BASE_URL}${normalizedPath}`;
}

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(apiUrl(url), {
    ...init,
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...init.headers,
    },
  });

  if (!response.ok) {
    const responseText = await response.text();
    let detail = responseText.trim();
    if (detail) {
      try {
        const parsed = JSON.parse(detail) as { message?: unknown; error?: unknown };
        detail =
          (typeof parsed.message === 'string' && parsed.message) ||
          (typeof parsed.error === 'string' && parsed.error) ||
          detail;
      } catch {
        // Preserve a plain-text error body.
      }
    }
    throw new Error(detail || `요청에 실패했습니다. (HTTP ${response.status})`);
  }

  return response.json() as Promise<T>;
}

const MOCK_ACCOUNT_SUBJECTS: AccountSubjectDto[] = [
  { code: '1000', name: '자산 (Assets)', type: 'ASSET', category: 'GROUP', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '1100', name: '유동자산', type: 'ASSET', category: 'GROUP', parentCode: '1000', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '1101', name: '현금및현금성자산', type: 'ASSET', category: 'SUBJECT', parentCode: '1100', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '1102', name: '당좌예금', type: 'ASSET', category: 'SUBJECT', parentCode: '1100', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '1200', name: '비유동자산', type: 'ASSET', category: 'GROUP', parentCode: '1000', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '1201', name: '유형자산 (토지/건물)', type: 'ASSET', category: 'SUBJECT', parentCode: '1200', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '2000', name: '부채 (Liabilities)', type: 'LIABILITY', category: 'GROUP', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '2100', name: '유동부채', type: 'LIABILITY', category: 'GROUP', parentCode: '2000', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '2101', name: '외상매입금', type: 'LIABILITY', category: 'SUBJECT', parentCode: '2100', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '3000', name: '자본 (Equity)', type: 'EQUITY', category: 'GROUP', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '4000', name: '수익 (Revenue)', type: 'INCOME', category: 'GROUP', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { code: '5000', name: '비용 (Expenses)', type: 'EXPENSE', category: 'GROUP', status: 'ACTIVE', validFrom: '2026-01-01', validTo: '9999-12-31' },
];

const MOCK_PARTNERS: BusinessPartnerDto[] = [
  { id: 1, businessPartnerCode: 'BP-001', businessPartnerName: '(주)한국전자', registrationNumber: '120-81-12345', ceoName: '홍길동', businessType: '제조업', businessItem: '반도체', partnerType: 'CUSTOMER', useYn: true, kycStatus: 'APPROVED', riskRating: 'LOW', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { id: 2, businessPartnerCode: 'BP-002', businessPartnerName: '글로벌소프트(주)', registrationNumber: '214-85-67890', ceoName: '김철수', businessType: '서비스', businessItem: 'IT컨설팅', partnerType: 'VENDOR', useYn: true, kycStatus: 'APPROVED', riskRating: 'LOW', validFrom: '2026-01-01', validTo: '9999-12-31' },
  { id: 3, businessPartnerCode: 'BP-003', businessPartnerName: '케이뱅크(주)', registrationNumber: '101-86-99999', ceoName: '이은행', businessType: '금융업', businessItem: '은행/여수신', partnerType: 'BANK', useYn: true, kycStatus: 'APPROVED', riskRating: 'LOW', validFrom: '2026-01-01', validTo: '9999-12-31' },
];

class MasterDataService {
  async getAccountSubjects(): Promise<AccountSubjectDto[]> {
    try {
      return await requestJson<AccountSubjectDto[]>('/api/basic/account-subjects');
    } catch {
      return MOCK_ACCOUNT_SUBJECTS;
    }
  }

  async getBusinessPartners(): Promise<BusinessPartnerDto[]> {
    try {
      return await requestJson<BusinessPartnerDto[]>('/api/basic/businesspartners');
    } catch {
      return MOCK_PARTNERS;
    }
  }

  async getPendingBusinessPartnerChangeRequests(): Promise<MasterDataChangeRequestDto[]> {
    try {
      const requests = await requestJson<MasterDataChangeRequestDto[]>(
        '/api/master-data/change-requests/pending',
      );
      return requests.filter((request) => request.targetType === 'BUSINESS_PARTNER');
    } catch {
      return [];
    }
  }

  async createBusinessPartnerChangeRequest(
    partner: BusinessPartnerCommand,
    reason?: string,
  ): Promise<MasterDataChangeRequestDto> {
    return requestJson<MasterDataChangeRequestDto>('/api/master-data/change-requests', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        targetType: 'BUSINESS_PARTNER',
        targetKey: partner.businessPartnerCode,
        changeType: 'CREATE',
        effectiveDate: partner.validFrom,
        requestedVersion: 1,
        reason: reason?.trim() || null,
        payloadJson: JSON.stringify(partner),
      }),
    });
  }

  async approveBusinessPartnerChangeRequest(id: number): Promise<MasterDataChangeRequestDto> {
    return requestJson<MasterDataChangeRequestDto>(
      `/api/master-data/change-requests/${id}/approve`,
      {
        method: 'POST',
      },
    );
  }

  async rejectBusinessPartnerChangeRequest(
    id: number,
    reason: string,
  ): Promise<MasterDataChangeRequestDto> {
    const normalizedReason = reason.trim();
    if (!normalizedReason) {
      throw new Error('반려 사유를 입력해 주세요.');
    }

    return requestJson<MasterDataChangeRequestDto>(
      `/api/master-data/change-requests/${id}/reject`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: normalizedReason }),
      },
    );
  }
}

export const masterDataService = new MasterDataService();
