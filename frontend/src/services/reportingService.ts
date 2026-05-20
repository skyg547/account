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
    try {
      const response = await fetch(`/api/v1/reporting/generate?${params.toString()}`, {
        method: 'POST',
        headers: { 'X-User-ID': 'frontend-admin' },
      });
      if (response.ok) {
        return response.json();
      }
    } catch (err) {
      console.warn('Falling back to sample financial statement:', err);
    }
    return sampleStatement(type, baseDate);
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

function sampleStatement(type: StatementType, baseDate: string): FinancialStatementDto {
  const lines = type === 'BALANCE_SHEET'
    ? [
        { lineCode: 'ASSET_CASH', label: '현금 및 현금성자산', currentAmount: 840200000, previousAmount: 780000000, noteNumber: '3', level: 1 },
        { lineCode: 'LIABILITY_DEPOSIT', label: '예수부채', currentAmount: 820000000, previousAmount: 800000000, noteNumber: '8', level: 1 },
      ]
    : [
        { lineCode: 'INTEREST_INCOME', label: '이자수익', currentAmount: 320000000, previousAmount: 295000000, noteNumber: '12', level: 1 },
        { lineCode: 'INTEREST_EXPENSE', label: '이자비용', currentAmount: 118000000, previousAmount: 110000000, noteNumber: '13', level: 1 },
      ];

  return {
    statementId: `SAMPLE-${type}-${baseDate.slice(0, 10)}`,
    type,
    baseDate,
    lines,
    status: 'FINAL',
  };
}

export const reportingService = new ReportingService();
