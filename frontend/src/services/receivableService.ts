export interface ReceivableInvoice {
  id: number;
  invoiceNo: string;
  issueDate: string;
  dueDate: string;
  businessPartnerCode: string;
  totalAmount: number;
  collectedAmount: number;
  balanceAmount: number;
  status: 'ISSUED' | 'PARTIALLY_COLLECTED' | 'FULLY_COLLECTED' | 'DEFAULTED';
  accountCode: string;
  departmentCode: string;
  currencyCode: string;
}

const API_BASE_URL = '/api/receivable/invoices';

export const receivableService = {
  /**
   * 매출채권 목록 조회
   */
  async getInvoices(status?: string): Promise<ReceivableInvoice[]> {
    try {
      const params = status ? new URLSearchParams({ status }) : '';
      const response = await fetch(`${API_BASE_URL}${params ? '?' + params.toString() : ''}`, {
        headers: { 'X-User-ID': 'frontend-user' }
      });
      if (response.ok) {
        return await response.json();
      }
    } catch {
      // Fallback
    }
    return [
      { id: 1, invoiceNo: 'REC-2026-0801', issueDate: '2026-08-01', dueDate: '2026-08-31', businessPartnerCode: 'BP-001', totalAmount: 55000000, collectedAmount: 20000000, balanceAmount: 35000000, status: 'PARTIALLY_COLLECTED', accountCode: '1102', departmentCode: '영업본부', currencyCode: 'KRW' },
      { id: 2, invoiceNo: 'REC-2026-0802', issueDate: '2026-08-05', dueDate: '2026-09-05', businessPartnerCode: 'BP-003', totalAmount: 120000000, collectedAmount: 120000000, balanceAmount: 0, status: 'FULLY_COLLECTED', accountCode: '1102', departmentCode: '금융사업부', currencyCode: 'KRW' }
    ];
  },

  /**
   * 채권 단건 조회
   */
  async getInvoiceById(id: number): Promise<ReceivableInvoice> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      headers: { 'X-User-ID': 'frontend-user' }
    });
    if (!response.ok) throw new Error('Invoice not found');
    return response.json();
  }
};
