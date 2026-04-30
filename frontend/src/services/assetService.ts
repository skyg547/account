/**
 * 고정자산 관리 서비스 (Asset Service)
 * 백엔드 FixedAssetController와 연동하여 자산 데이터를 처리합니다.
 */

export interface FixedAsset {
  id: number;
  assetCode: string;
  assetName: string;
  accountCode: string;
  accumulatedAccountCode: string;
  expenseAccountCode: string;
  acquisitionDate: string;
  acquisitionCost: number;
  usefulLife: number;
  depreciationMethod: string;
  residualValue: number;
  accumulatedDepreciation: number;
  currentBookValue: number;
  depreciationAmountPerPeriod: number;
  lastDepreciationDate: string | null;
  status: 'ACTIVE' | 'DISPOSED' | 'FULLY_DEPRECIATED';
  departmentCode: string;
}

export interface FixedAssetRequest {
  assetCode: string;
  assetName: string;
  acquisitionDate: string;
  acquisitionCost: number;
  usefulLife: number;
  depreciationMethod: string;
  residualValue: number;
  accountSubjectCode: string;
  accumulatedAccountCode: string;
  expenseAccountCode: string;
  departmentCode: string;
}

const API_BASE_URL = '/api/fixed-assets';

export const assetService = {
  /**
   * 고정자산 목록 조회
   */
  async getAssets(status?: string): Promise<FixedAsset[]> {
    const params = status ? new URLSearchParams({ status }) : '';
    const response = await fetch(`${API_BASE_URL}${params ? '?' + params.toString() : ''}`);
    if (!response.ok) throw new Error('Failed to fetch fixed assets');
    return response.json();
  },

  /**
   * 고정자산 단건 조회
   */
  async getAssetById(id: number): Promise<FixedAsset> {
    const response = await fetch(`${API_BASE_URL}/${id}`);
    if (!response.ok) throw new Error('Asset not found');
    return response.json();
  },

  /**
   * 고정자산 등록
   */
  async registerAsset(request: FixedAssetRequest): Promise<FixedAsset> {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!response.ok) throw new Error('Failed to register fixed asset');
    return response.json();
  },

  /**
   * 감가상각 실행
   */
  async runDepreciation(processDate: string): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/depreciate/${processDate}`, {
      method: 'POST',
    });
    if (!response.ok) throw new Error('Failed to process depreciation');
    return response.text();
  },

  /**
   * 고정자산 처분
   */
  async disposeAsset(assetId: number, disposalDate: string, salePrice: number): Promise<FixedAsset> {
    const response = await fetch(`${API_BASE_URL}/dispose`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ assetId, disposalDate, salePrice }),
    });
    if (!response.ok) throw new Error('Failed to dispose fixed asset');
    return response.json();
  }
};
