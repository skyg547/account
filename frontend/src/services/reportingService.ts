import { emitToast } from '@/context/ToastContext';
import { mockStatements } from '@/mocks/reporting';
import { fetchWithTimeout } from './apiClient';

export type StatementType = 'BALANCE_SHEET' | 'INCOME_STATEMENT';
export type StatementDataSource = 'API' | 'MOCK';
export type ExportDocumentResult = 'DOWNLOADED' | 'OFFLINE';

export interface ReportLineDto {
  lineCode: string;
  label: string;
  currentAmount: number;
  previousAmount: number;
  noteNumber: string | null;
  level: number;
  category?: string;
  parentLineCode?: string | null;
}

export interface FinancialStatementDto {
  statementId: string;
  type: StatementType;
  baseDate: string;
  lines: ReportLineDto[];
  status: 'DRAFT' | 'FINAL';
  dataSource: StatementDataSource;
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

const DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const QUARTER_PATTERN = /^(\d{4})-Q([1-4])$/;
const FISCAL_YEAR_PATTERN = /^(\d{4})-FY$/;

/**
 * Spring의 ISO DATE_TIME 파라미터가 받을 수 있도록 조회 기간을 해당 기간의 마지막 시각으로 바꿉니다.
 * 이미 시간 성분이 있는 값은 호출자가 지정한 시각을 보존합니다.
 */
export function normalizeStatementBaseDate(baseDate: string): string {
  const dateOnlyMatch = DATE_ONLY_PATTERN.exec(baseDate);
  if (dateOnlyMatch) {
    return `${dateOnlyMatch[1]}-${dateOnlyMatch[2]}-${dateOnlyMatch[3]}T23:59:59`;
  }

  const quarterMatch = QUARTER_PATTERN.exec(baseDate);
  if (quarterMatch) {
    const quarterEndDates = ['03-31', '06-30', '09-30', '12-31'];
    const quarterIndex = Number(quarterMatch[2]) - 1;
    return `${quarterMatch[1]}-${quarterEndDates[quarterIndex]}T23:59:59`;
  }

  const fiscalYearMatch = FISCAL_YEAR_PATTERN.exec(baseDate);
  if (fiscalYearMatch) {
    return `${fiscalYearMatch[1]}-12-31T23:59:59`;
  }

  return baseDate;
}

function createMockStatement(type: StatementType, baseDate: string): FinancialStatementDto {
  const mockType = type === 'BALANCE_SHEET' ? 'BS' : 'IS';
  const lines = mockStatements
    .filter((statement) => statement.statementType === mockType)
    .map((statement) => ({
      lineCode: statement.accountCode,
      label: statement.accountName,
      currentAmount: statement.amountCurrent,
      previousAmount: statement.amountPrevious,
      noteNumber: null,
      level: statement.level ?? 0,
      category: statement.category,
      parentLineCode: statement.parentId,
    }));

  return {
    statementId: `MOCK-${type}-${baseDate.slice(0, 10)}`,
    type,
    baseDate,
    lines,
    status: 'FINAL',
    dataSource: 'MOCK',
  };
}

function getDownloadExtension(response: Response, format: 'PDF' | 'EXCEL'): 'pdf' | 'xlsx' | 'csv' {
  if (format === 'PDF') return 'pdf';

  const contentType = response.headers.get('content-type')?.toLowerCase() ?? '';
  return contentType.includes('spreadsheetml') ? 'xlsx' : 'csv';
}

class ReportingService {
  async generateStatement(type: StatementType, baseDate: string): Promise<FinancialStatementDto> {
    const normalizedBaseDate = normalizeStatementBaseDate(baseDate);
    const params = new URLSearchParams({ type, baseDate: normalizedBaseDate });

    try {
      const response = await fetchWithTimeout(`/api/v1/reporting/generate?${params.toString()}`, {
        method: 'POST',
        headers: { 'X-User-ID': 'frontend-admin' },
        silentToast: true,
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`보고서 생성 실패: ${errorText || response.statusText}`);
      }

      const statement = (await response.json()) as Omit<FinancialStatementDto, 'dataSource'>;
      return { ...statement, dataSource: 'API' };
    } catch (error) {
      console.warn(`Falling back to mock ${type} statement:`, error);
      return createMockStatement(type, normalizedBaseDate);
    }
  }

  async exportDocument(
    type: StatementType,
    baseDate: string,
    format: 'PDF' | 'EXCEL'
  ): Promise<ExportDocumentResult> {
    const normalizedBaseDate = normalizeStatementBaseDate(baseDate);
    const params = new URLSearchParams({ type, baseDate: normalizedBaseDate, format });

    try {
      const response = await fetchWithTimeout(`/api/v1/reporting/generate/document?${params.toString()}`, {
        method: 'POST',
        headers: { 'X-User-ID': 'frontend-admin' },
        silentToast: true,
      });
      if (!response.ok) {
        throw new Error(`보고서 문서 생성에 실패했습니다. (${response.status})`);
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${type.toLowerCase()}_${normalizedBaseDate.slice(0, 10)}.${getDownloadExtension(response, format)}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      return 'DOWNLOADED';
    } catch (error) {
      console.warn(`Unable to export ${type} while reporting backend is offline:`, error);
      emitToast({
        type: 'warning',
        title: '보고서 내보내기 불가',
        message: '백엔드가 오프라인이어서 Mock 데이터로 문서를 생성할 수 없습니다. 연결 후 다시 시도해 주세요.',
      });
      return 'OFFLINE';
    }
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
