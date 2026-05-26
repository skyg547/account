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
    const params = status ? new URLSearchParams({ status }) : '';
    const response = await fetch(`${API_BASE_URL}${params ? '?' + params.toString() : ''}`, {
      headers: { 'X-User-ID': 'frontend-user' }
    });
    if (!response.ok) throw new Error('Failed to fetch receivable invoices');
    return response.json();
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
