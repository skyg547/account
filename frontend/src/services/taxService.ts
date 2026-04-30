/**
 * 세무 관리 서비스 (Tax Service)
 * 백엔드 APInvoiceController와 연동하여 세금계산서 데이터를 처리합니다.
 */

export interface TaxInvoice {
  id: number;
  issueId: string;
  type: 'SALES' | 'PURCHASE';
  issueDate: string;
  businessPartnerCode: string;
  businessPartnerName: string;
  supplyAmount: number;
  taxAmount: number;
  totalAmount: number;
}

export interface TaxInvoiceRequest {
  issueId: string;
  type: 'SALES' | 'PURCHASE';
  issueDate: string;
  businessPartnerCode: string;
  supplyAmount: number;
  taxAmount: number;
  totalAmount: number;
}

const API_BASE_URL = '/api/ap/invoices';

export const taxService = {
  /**
   * 세금계산서 목록 조회 (기간별)
   */
  async getInvoices(startDate: string, endDate: string): Promise<TaxInvoice[]> {
    const params = new URLSearchParams({ startDate, endDate });
    const response = await fetch(`${API_BASE_URL}?${params.toString()}`);
    if (!response.ok) throw new Error('Failed to fetch tax invoices');
    return response.json();
  },

  /**
   * 세금계산서 단건 조회
   */
  async getInvoiceById(id: number): Promise<TaxInvoice> {
    const response = await fetch(`${API_BASE_URL}/${id}`);
    if (!response.ok) throw new Error('Invoice not found');
    return response.json();
  },

  /**
   * 승인번호로 세금계산서 조회
   */
  async getInvoiceByIssueId(issueId: string): Promise<TaxInvoice> {
    const response = await fetch(`${API_BASE_URL}/issue-id/${issueId}`);
    if (!response.ok) throw new Error('Invoice not found');
    return response.json();
  },

  /**
   * 세금계산서 생성
   */
  async createInvoice(request: TaxInvoiceRequest): Promise<TaxInvoice> {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!response.ok) throw new Error('Failed to create tax invoice');
    return response.json();
  },

  /**
   * 세금계산서 수정
   */
  async updateInvoice(id: number, request: TaxInvoiceRequest): Promise<TaxInvoice> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!response.ok) throw new Error('Failed to update tax invoice');
    return response.json();
  },

  /**
   * 세금계산서 삭제
   */
  async deleteInvoice(id: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete tax invoice');
  }
};
