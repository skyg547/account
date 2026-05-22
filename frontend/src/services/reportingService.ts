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
    anchor.download = `${type.toLowerCase()}_${baseDate.slice(0, 10)}.${format === 'PDF' ? 'pdf' : 'csv'}`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}

export const reportingService = new ReportingService();
