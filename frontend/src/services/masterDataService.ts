export interface AccountSubjectDto {
  code: string;
  name: string;
  type: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'INCOME' | 'EXPENSE';
  category: 'GROUP' | 'SUBJECT';
  parentCode?: string;
  status: 'ACTIVE' | 'INACTIVE';
  validFrom: string;
  validTo: string;
}

export interface BusinessPartnerDto {
  code: string;
  name: string;
  type: 'CORPORATE' | 'INDIVIDUAL' | 'FINANCIAL';
  registrationNumber?: string;
  status: 'ACTIVE' | 'INACTIVE';
  taxRegistrationNumber?: string;
  validFrom: string;
  validTo: string;
}

class MasterDataService {
  async getAccountSubjects(): Promise<AccountSubjectDto[]> {
    try {
      const response = await fetch('/api/basic/account-subjects', {
        method: 'GET',
        headers: { 'X-User-ID': 'frontend-user' }
      });
      if (response.ok) {
        return response.json();
      }
    } catch (e) {
      console.warn('Failed to fetch account subjects:', e);
    }
    return [];
  }

  async getBusinessPartners(): Promise<BusinessPartnerDto[]> {
    try {
      const response = await fetch('/api/basic/business-partners', {
        method: 'GET',
        headers: { 'X-User-ID': 'frontend-user' }
      });
      if (response.ok) {
        return response.json();
      }
    } catch (e) {
      console.warn('Failed to fetch business partners:', e);
    }
    return [];
  }
}

export const masterDataService = new MasterDataService();
