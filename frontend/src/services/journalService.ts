import { fetchWithTimeout } from './apiClient';

export type JournalSide = 'DEBIT' | 'CREDIT';
export type JournalStatus =
  | 'DRAFT'
  | 'REQUESTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'POSTED'
  | 'REVERSED';

export interface JournalDetailDto {
  id?: number;
  side: JournalSide;
  accountCode: string;
  amount: number;
  baseAmount?: number;
  departmentCode?: string;
  businessPartnerCode?: string;
  detailDescription?: string;
}

export interface JournalEntryDto {
  id?: number;
  slipNo?: string;
  slipDate: string;
  accountingDate: string;
  description: string;
  status?: JournalStatus;
  entryType: string;
  currencyCode?: string;
  details: JournalDetailDto[];
  createdBy?: string;
}

export interface JournalQueryParams {
  startDate?: string;
  endDate?: string;
  status?: string;
}

interface JournalApiLine extends Omit<JournalDetailDto, 'detailDescription'> {
  description?: string;
  detailDescription?: string;
}

interface JournalApiEntry extends Omit<JournalEntryDto, 'details'> {
  lines?: JournalApiLine[];
  details?: JournalApiLine[];
}

const MOCK_JOURNALS: JournalEntryDto[] = [
  {
    id: 1,
    slipNo: 'J-20260422-001',
    slipDate: '2026-04-22',
    accountingDate: '2026-04-22',
    description: '4월 소모품 매입',
    status: 'POSTED',
    entryType: 'MANUAL',
    currencyCode: 'KRW',
    details: [
      { side: 'DEBIT', accountCode: '83000', amount: 125000 },
      { side: 'CREDIT', accountCode: '10100', amount: 125000 }
    ]
  },
  {
    id: 2,
    slipNo: 'J-20260423-001',
    slipDate: '2026-04-23',
    accountingDate: '2026-04-23',
    description: '출장비 정산',
    status: 'DRAFT',
    entryType: 'MANUAL',
    currencyCode: 'KRW',
    details: [
      { side: 'DEBIT', accountCode: '81100', amount: 80000 },
      { side: 'CREDIT', accountCode: '10100', amount: 80000 }
    ]
  },
  {
    id: 3,
    slipNo: 'J-20260424-001',
    slipDate: '2026-04-24',
    accountingDate: '2026-04-24',
    description: '예금 이자 수익',
    status: 'APPROVED',
    entryType: 'MANUAL',
    currencyCode: 'KRW',
    details: [
      { side: 'DEBIT', accountCode: '10100', amount: 35000 },
      { side: 'CREDIT', accountCode: '42100', amount: 35000 }
    ]
  }
];

function normalizeJournal(entry: JournalApiEntry): JournalEntryDto {
  const lines = entry.details ?? entry.lines ?? [];

  return {
    ...entry,
    details: lines.map(({ description, detailDescription, ...line }) => ({
      ...line,
      detailDescription: detailDescription ?? description
    }))
  };
}

function filterByStatus(journals: JournalEntryDto[], status?: string): JournalEntryDto[] {
  if (!status || status === 'ALL') return journals;
  return journals.filter((journal) => journal.status === status);
}

function filterByDate(journals: JournalEntryDto[], params: JournalQueryParams): JournalEntryDto[] {
  return journals.filter((journal) => {
    const afterStart = !params.startDate || journal.slipDate >= params.startDate;
    const beforeEnd = !params.endDate || journal.slipDate <= params.endDate;
    return afterStart && beforeEnd;
  });
}

class JournalService {
  private usingMockFallback = false;

  isUsingMockFallback(): boolean {
    return this.usingMockFallback;
  }

  async getJournalEntries(params: JournalQueryParams = {}): Promise<JournalEntryDto[]> {
    const query = new URLSearchParams();
    if (params.startDate) query.set('startDate', params.startDate);
    if (params.endDate) query.set('endDate', params.endDate);
    const queryString = query.toString();
    const url = `/api/journals${queryString ? `?${queryString}` : ''}`;

    try {
      const response = await fetchWithTimeout(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': 'frontend-user'
        },
        silentToast: true
      });
      if (response.ok) {
        const data = (await response.json()) as JournalApiEntry[];
        this.usingMockFallback = false;
        return filterByStatus(data.map(normalizeJournal), params.status);
      }
    } catch (error) {
      console.warn('Failed to fetch journals, returning mock:', error);
    }

    // 조회 API가 닿지 않을 때만 Mock을 사용하며, 날짜/상태 필터도 같은 의미로 적용합니다.
    this.usingMockFallback = true;
    return filterByStatus(filterByDate(MOCK_JOURNALS, params), params.status);
  }

  async createJournalEntry(entry: JournalEntryDto): Promise<JournalEntryDto> {
    // 화면 모델(details)을 백엔드 CreateRequest(lines) 계약으로 변환합니다.
    const payload = {
      slipDate: entry.slipDate,
      accountingDate: entry.accountingDate,
      description: entry.description,
      entryType: entry.entryType,
      currencyCode: entry.currencyCode,
      lines: entry.details.map(({ detailDescription, baseAmount, ...detail }) => ({
        ...detail,
        baseAmount: baseAmount ?? detail.amount,
        description: detailDescription
      }))
    };

    const response = await fetchWithTimeout('/api/journals', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-ID': 'frontend-user'
      },
      body: JSON.stringify(payload),
      timeoutMs: 15000
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `전표 생성에 실패했습니다. (${response.status})`);
    }

    return normalizeJournal((await response.json()) as JournalApiEntry);
  }

  async approveJournalEntry(id: number): Promise<void> {
    await this.updateJournalStatus(id, 'approve');
  }

  async postJournalEntry(id: number): Promise<void> {
    await this.updateJournalStatus(id, 'post');
  }

  private async updateJournalStatus(id: number, action: 'approve' | 'post'): Promise<void> {
    // Mock ID는 서버에 존재하지 않으므로 오프라인 목록에서는 네트워크 호출 없이 UI 전이만 허용합니다.
    if (this.usingMockFallback) return;

    const response = await fetchWithTimeout(`/api/journals/${id}/${action}`, {
      method: 'POST',
      headers: {
        'X-User-ID': 'frontend-user'
      }
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `전표 상태 변경에 실패했습니다. (${response.status})`);
    }
  }
}

export const journalService = new JournalService();
