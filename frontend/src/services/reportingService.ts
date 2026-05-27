export type StatementType = 'BALANCE_SHEET' | 'INCOME_STATEMENT';

export interface ReportLineDto {
  lineCode: string;
  label: string;
  currentAmount: number;
  previousAmount: number;
  noteNumber: string;
  level: number;
}

export interface FinancialStatementDto {
  statementId: string;
  type: StatementType;
  baseDate: string;
  lines: ReportLineDto[];
  status: 'DRAFT' | 'FINAL';
}

export interface DisclosureNoteMartEntryDto {
  entryId: string;
  noteNumber: string;
  noteCategory: string;
  sourceLineCode: string;
  sourceLineLabel: string;
  maturityBucket: string;
  rateType: string;
  currencyCode: string;
  riskCategory: string;
  currentAmount: number;
  previousAmount: number;
}

export interface DisclosureNoteMartDto {
  martId: string;
  statementId: string;
  statementType: StatementType;
  baseDate: string;
  generatedBy: string;
  generatedAt: string;
  entries: DisclosureNoteMartEntryDto[];
}

export interface JournalDetailSummaryDto {
  id: number;
  side: string;
  accountCode: string;
  accountName: string;
  accountingDate: string;
  amount: number;
  slipNo: string;
  detailDescription: string;
  headerDescription: string;
}

class ReportingService {
  async generateStatement(type: StatementType, baseDate: string): Promise<FinancialStatementDto> {
    const params = new URLSearchParams({ type, baseDate });
    const response = await fetch(`/api/v1/reporting/generate?${params.toString()}`, {
      method: 'POST',
      headers: { 'X-User-ID': 'frontend-admin' },
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`보고서 생성 실패: ${errorText || response.statusText}`);
    }
    return response.json();
  }

  async exportDocument(type: StatementType, baseDate: string, format: 'PDF' | 'EXCEL'): Promise<void> {
    const params = new URLSearchParams({ type, baseDate, format });
    const response = await fetch(`/api/v1/reporting/generate/document?${params.toString()}`, {
      method: 'POST',
      headers: { 'X-User-ID': 'frontend-admin' },
    });
    if (!response.ok) {
      throw new Error('보고서 문서 생성에 실패했습니다.');
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${type.toLowerCase()}_${baseDate.slice(0, 10)}.${format === 'PDF' ? 'csv' : 'csv'}`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  async getDisclosureNoteMart(type: StatementType, baseDate: string): Promise<DisclosureNoteMartDto> {
    const params = new URLSearchParams({ type, baseDate });
    const response = await fetch(`/api/v1/reporting/disclosure-notes?${params.toString()}`, {
      method: 'GET',
    });
    if (!response.ok) {
      throw new Error('주석 마트를 불러오는데 실패했습니다.');
    }
    return response.json();
  }

  async generateDisclosureNoteMart(type: StatementType, baseDate: string): Promise<DisclosureNoteMartDto> {
    const params = new URLSearchParams({ type, baseDate });
    const response = await fetch(`/api/v1/reporting/disclosure-notes/generate?${params.toString()}`, {
      method: 'POST',
      headers: { 'X-User-ID': 'frontend-admin' },
    });
    if (!response.ok) {
      throw new Error('주석 마트 생성에 실패했습니다.');
    }
    return response.json();
  }

  async drillDownDisclosureNote(type: StatementType, baseDate: string, entryId: string): Promise<JournalDetailSummaryDto[]> {
    const params = new URLSearchParams({ type, baseDate, entryId });
    const response = await fetch(`/api/v1/reporting/disclosure-notes/drill-down?${params.toString()}`, {
      method: 'GET',
    });
    if (!response.ok) {
      throw new Error('역추적 정보를 불러오는데 실패했습니다.');
    }
    return response.json();
  }
}

export const reportingService = new ReportingService();
