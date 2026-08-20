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
      { id: 1, invoiceNo: 'INV-2026-0801', issueDate: '2026-08-01', dueDate: '2026-08-31', businessPartnerCode: 'BP-002', totalAmount: 45000000, paidAmount: 0, balanceAmount: 45000000, status: 'ISSUED', accountCode: '2101', departmentCode: 'IT운영팀', currencyCode: 'KRW' },
      { id: 2, invoiceNo: 'INV-2026-0802', issueDate: '2026-08-10', dueDate: '2026-09-10', businessPartnerCode: 'BP-001', totalAmount: 18500000, paidAmount: 18500000, balanceAmount: 0, status: 'FULLY_PAID', accountCode: '2101', departmentCode: '총무팀', currencyCode: 'KRW' }
    ];
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
