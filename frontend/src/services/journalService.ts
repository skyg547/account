export type JournalSide = 'DEBIT' | 'CREDIT';

export interface JournalDetailDto {
  side: JournalSide;
  accountCode: string;
  amount: number;
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
  status?: string;
  entryType: string;
  currencyCode?: string;
  details: JournalDetailDto[];
  createdBy?: string;
}

class JournalService {
  async getJournalEntries(): Promise<JournalEntryDto[]> {
    try {
      const response = await fetch('/api/journals', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': 'frontend-user' // 임시 하드코딩
        }
      });
      if (response.ok) {
        return response.json();
      }
    } catch (e) {
      console.warn('Failed to fetch journals, returning mock:', e);
    }
    // API 연결 실패 시 Mock 데이터 반환
    return [
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
      }
    ];
  }

  async createJournalEntry(entry: JournalEntryDto): Promise<JournalEntryDto | null> {
    try {
      const response = await fetch('/api/journals', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': 'frontend-user'
        },
        body: JSON.stringify(entry)
      });
      
      if (response.ok) {
        return response.json();
      } else {
        const errorText = await response.text();
        console.error('Failed to create journal:', errorText);
        throw new Error(errorText);
      }
    } catch (e) {
      console.error('Error creating journal entry:', e);
      throw e;
    }
  }
}

export const journalService = new JournalService();
