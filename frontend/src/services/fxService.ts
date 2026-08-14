export interface FxPosition {
  currencyCode: string;
  currencyName: string;
  foreignAmount: number;
  averageRate: number;
  krwAmount: number;
  valuationGainLoss: number;
  limitStatus: 'SAFE' | 'WARNING' | 'EXCEEDED';
}

export interface FxRate {
  pair: string;
  rate: number;
  changeAmount: number;
  changePercent: number;
}

export interface FxDashboardData {
  totalNetPosition: number;
  totalKrwAmount: number;
  dailyValuationGainLoss: number;
  gainLossPercent: number;
  rates: FxRate[];
  positions: FxPosition[];
}

const API_BASE_URL = '/api/fx';

class FxService {
  async getDashboardData(): Promise<FxDashboardData> {
    try {
      const response = await fetch(`${API_BASE_URL}/dashboard`, {
        headers: { 'X-User-ID': 'frontend-user' }
      });
      if (response.ok) {
        return response.json();
      }
    } catch (e) {
      console.warn('Failed to fetch FX dashboard data, falling back to mock:', e);
    }
    
    // Fallback Mock Data
    return {
      totalNetPosition: 4250000,
      totalKrwAmount: 5888250000,
      dailyValuationGainLoss: 12450000,
      gainLossPercent: 15,
      rates: [
        { pair: 'USD/KRW', rate: 1385.40, changeAmount: 2.4, changePercent: 0.17 },
        { pair: 'JPY/KRW', rate: 894.20, changeAmount: -1.1, changePercent: -0.12 }
      ],
      positions: [
        { currencyCode: 'USD', currencyName: '미국 달러', foreignAmount: 2500000, averageRate: 1375.00, krwAmount: 3463500000, valuationGainLoss: 26000000, limitStatus: 'SAFE' },
        { currencyCode: 'JPY', currencyName: '일본 엔', foreignAmount: 150000000, averageRate: 905.00, krwAmount: 1341300000, valuationGainLoss: -8500000, limitStatus: 'SAFE' }
      ]
    };
  }
}

export const fxService = new FxService();
