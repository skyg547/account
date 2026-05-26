export interface PayableInvoice {
  id: number;
  invoiceNo: string;
  issueDate: string;
  dueDate: string;
  businessPartnerCode: string;
  totalAmount: number;
  paidAmount: number;
  balanceAmount: number;
  status: 'ISSUED' | 'PARTIALLY_PAID' | 'FULLY_PAID' | 'CANCELLED';
  accountCode: string;
  departmentCode: string;
  currencyCode: string;
  description?: string;
}

const API_BASE_URL = '/api/payable/invoices';

export const payableService = {
  /**
   * 매입채무 목록 조회
   */
  async getInvoices(status?: string): Promise<PayableInvoice[]> {
    const params = status ? new URLSearchParams({ status }) : '';
    const response = await fetch(`${API_BASE_URL}${params ? '?' + params.toString() : ''}`, {
      headers: { 'X-User-ID': 'frontend-user' }
    });
    if (!response.ok) throw new Error('Failed to fetch payable invoices');
    return response.json();
  },

  /**
   * 채무 단건 조회
   */
  async getInvoiceById(id: number): Promise<PayableInvoice> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      headers: { 'X-User-ID': 'frontend-user' }
    });
    if (!response.ok) throw new Error('Invoice not found');
    return response.json();
  }
};
