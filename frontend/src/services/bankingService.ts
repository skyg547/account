export interface InterBranchTransaction {
  id: number;
  sourceBranch: string;
  targetBranch: string;
  transactionType: string;
  amount: number;
  status: 'MATCHED' | 'DISCREPANCY' | 'PENDING';
}

export interface InterBranchDashboardData {
  unmatchedCount: number;
  totalDiscrepancyAmount: number;
  autoMatchRate: number;
  unexplainedDepositsCount: number;
  transactions: InterBranchTransaction[];
}

const API_BASE_URL = '/api/finance/banking/inter-branch';

class BankingService {
  async getInterBranchData(): Promise<InterBranchDashboardData> {
    try {
      const response = await fetch(`${API_BASE_URL}/dashboard`, {
        headers: { 'X-User-ID': 'frontend-user' }
      });
      if (response.ok) {
        return response.json();
      }
    } catch (e) {
      console.warn('Failed to fetch inter-branch data, falling back to mock:', e);
    }
    
    // Fallback Mock Data
    return {
      unmatchedCount: 12,
      totalDiscrepancyAmount: 45200000,
      autoMatchRate: 85,
      unexplainedDepositsCount: 3,
      transactions: [
        { id: 1, sourceBranch: '강남금융센터', targetBranch: '본점영업부', transactionType: '본지점 자금 이체', amount: 10000000, status: 'DISCREPANCY' },
        { id: 2, sourceBranch: '여의도지점', targetBranch: '삼성역지점', transactionType: '타점권 추심', amount: 5500000, status: 'MATCHED' }
      ]
    };
  }

  async runAutoMatch(): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE_URL}/auto-match`, {
        method: 'POST',
        headers: { 'X-User-ID': 'frontend-user' }
      });
      return response.ok;
    } catch (e) {
      console.error('Auto match execution failed', e);
      return false;
    }
  }
}

export const bankingService = new BankingService();
