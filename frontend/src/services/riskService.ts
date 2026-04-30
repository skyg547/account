/**
 * 리스크 관리 서비스 (Risk Service)
 * 백엔드 RiskAssessmentController와 연동하여 RWA 산출 데이터를 처리합니다.
 */

export interface RiskWeightedAsset {
  id: number;
  sourceReferenceId: string;
  businessPartnerName: string;
  calculationDate: string;
  approachType: string;
  eadAmount: number;
  riskWeight: number;
  rwaAmount: number;
}

const API_BASE_URL = '/api/risk/assessment';

export const riskService = {
  /**
   * RWA 산출 결과 조회
   */
  async getRwaResults(startDate: string, endDate: string): Promise<RiskWeightedAsset[]> {
    const params = new URLSearchParams({ startDate, endDate });
    const response = await fetch(`${API_BASE_URL}/rwa-results?${params.toString()}`);
    if (!response.ok) throw new Error('Failed to fetch RWA results');
    return response.json();
  },

  /**
   * 익스포저 생성 실행
   */
  async generateExposures(baseDate: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/generate-exposures?baseDate=${baseDate}`, {
      method: 'POST'
    });
    if (!response.ok) throw new Error('Failed to generate exposures');
  },

  /**
   * RWA 산출 실행
   */
  async calculateRwa(baseDate: string, approachType: string = 'SA'): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/calculate-rwa?baseDate=${baseDate}&approachType=${approachType}`, {
      method: 'POST'
    });
    if (!response.ok) throw new Error('Failed to calculate RWA');
  }
};
