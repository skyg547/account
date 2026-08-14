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

interface LoginSession {
  token?: unknown;
}

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || '/api').replace(
  /\/+$/,
  '',
);

function apiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  if (API_BASE_URL.endsWith('/api') && normalizedPath.startsWith('/api/')) {
    return `${API_BASE_URL}${normalizedPath.slice('/api'.length)}`;
  }
  return `${API_BASE_URL}${normalizedPath}`;
}

function getAuthToken(): string {
  if (typeof window === 'undefined') {
    throw new Error('로그인 세션은 브라우저에서만 사용할 수 있습니다.');
  }

  const rawUserInfo = localStorage.getItem('user_info');
  const storedToken = localStorage.getItem('auth_token');
  let sessionToken = '';
  if (rawUserInfo) {
    try {
      const session = JSON.parse(rawUserInfo) as LoginSession;
      sessionToken = typeof session.token === 'string' ? session.token.trim() : '';
    } catch {
      throw new Error('로그인 세션이 올바르지 않습니다. 다시 로그인해 주세요.');
    }
  }

  const token = storedToken?.trim() || sessionToken;
  if (!token) {
    throw new Error('인증 토큰이 없습니다. 다시 로그인해 주세요.');
  }

  return token;
}

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const response = await fetch(apiUrl(url), {
    ...init,
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${token}`,
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

class MasterDataService {
  async getAccountSubjects(): Promise<AccountSubjectDto[]> {
    return requestJson<AccountSubjectDto[]>('/api/basic/account-subjects');
  }

  async getBusinessPartners(): Promise<BusinessPartnerDto[]> {
    return requestJson<BusinessPartnerDto[]>('/api/basic/businesspartners');
  }

  async getPendingBusinessPartnerChangeRequests(): Promise<MasterDataChangeRequestDto[]> {
    const requests = await requestJson<MasterDataChangeRequestDto[]>(
      '/api/master-data/change-requests/pending',
    );
    return requests.filter((request) => request.targetType === 'BUSINESS_PARTNER');
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
