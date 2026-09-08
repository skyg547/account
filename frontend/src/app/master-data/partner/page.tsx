"use client";

import React, { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  ClipboardCheck,
  Plus,
  RefreshCw,
  Send,
  XCircle,
} from 'lucide-react';
import EmptyState from '@/components/ui/EmptyState';
import LoadingSkeleton from '@/components/ui/LoadingSkeleton';
import StatusBadge from '@/components/ui/StatusBadge';
import {
  BusinessPartnerCommand,
  BusinessPartnerDto,
  KycStatus,
  MasterDataChangeRequestDto,
  PartnerType,
  RiskRating,
  masterDataService,
} from '@/services/masterDataService';

type PartnerForm = BusinessPartnerCommand & { reason: string };

const inputClass =
  'w-full rounded-xl border border-white/10 bg-slate-950/70 px-4 py-3 text-sm text-white outline-none transition-colors placeholder:text-slate-600 focus:border-blue-500 disabled:opacity-50';
const labelClass = 'mb-2 block text-xs font-bold text-slate-400';

function localToday(): string {
  const now = new Date();
  const localTime = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return localTime.toISOString().slice(0, 10);
}

function initialForm(): PartnerForm {
  return {
    businessPartnerCode: '',
    businessPartnerName: '',
    registrationNumber: '',
    ceoName: '',
    businessType: '',
    businessItem: '',
    partnerType: 'CUSTOMER',
    useYn: true,
    kycStatus: 'PENDING',
    riskRating: 'MEDIUM',
    validFrom: localToday(),
    validTo: '9999-12-31',
    reason: '',
  };
}

const partnerTypes: readonly string[] = ['CUSTOMER', 'VENDOR', 'BANK', 'OTHER_BP'];
const kycStatuses: readonly string[] = ['PENDING', 'APPROVED', 'REJECTED', 'REVIEW_REQUIRED'];
const riskRatings: readonly string[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

function parsePartnerPayload(request: MasterDataChangeRequestDto): {
  payload: BusinessPartnerCommand | null;
  error: string | null;
} {
  if (!request.payloadJson) {
    return { payload: null, error: '심사할 거래처 payload가 없습니다.' };
  }

  try {
    const value = JSON.parse(request.payloadJson) as unknown;
    if (!value || typeof value !== 'object') {
      return { payload: null, error: '거래처 payload 형식이 올바르지 않습니다.' };
    }

    const payload = value as Partial<BusinessPartnerCommand>;
    const valid =
      typeof payload.businessPartnerCode === 'string' &&
      payload.businessPartnerCode.trim().length > 0 &&
      typeof payload.businessPartnerName === 'string' &&
      payload.businessPartnerName.trim().length > 0 &&
      typeof payload.registrationNumber === 'string' &&
      typeof payload.ceoName === 'string' &&
      typeof payload.businessType === 'string' &&
      typeof payload.businessItem === 'string' &&
      typeof payload.partnerType === 'string' &&
      partnerTypes.includes(payload.partnerType) &&
      typeof payload.useYn === 'boolean' &&
      typeof payload.kycStatus === 'string' &&
      kycStatuses.includes(payload.kycStatus) &&
      typeof payload.riskRating === 'string' &&
      riskRatings.includes(payload.riskRating) &&
      typeof payload.validFrom === 'string' &&
      /^\d{4}-\d{2}-\d{2}$/.test(payload.validFrom) &&
      typeof payload.validTo === 'string' &&
      /^\d{4}-\d{2}-\d{2}$/.test(payload.validTo);

    return valid
      ? { payload: payload as BusinessPartnerCommand, error: null }
      : { payload: null, error: '거래처 payload의 필수값 또는 코드값이 올바르지 않습니다.' };
  } catch {
    return { payload: null, error: '거래처 payload JSON을 해석할 수 없습니다.' };
  }
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '요청을 처리하지 못했습니다.';
}

export default function BusinessPartnerApprovalPage() {
  const [partners, setPartners] = useState<BusinessPartnerDto[]>([]);
  const [pendingRequests, setPendingRequests] = useState<MasterDataChangeRequestDto[]>([]);
  const [form, setForm] = useState<PartnerForm>(initialForm);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [actionId, setActionId] = useState<number | 'create' | null>(null);
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [partnerData, requestData] = await Promise.all([
        masterDataService.getBusinessPartners(),
        masterDataService.getPendingBusinessPartnerChangeRequests(),
      ]);
      setPartners(partnerData);
      setPendingRequests(requestData);
    } catch (error) {
      setLoadError(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {

    void loadData();
  }, [loadData]);

  const updateForm = <K extends keyof PartnerForm>(key: K, value: PartnerForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setActionId('create');
    setActionError(null);
    setActionMessage(null);
    const { reason, ...partner } = form;

    try {
      await masterDataService.createBusinessPartnerChangeRequest(partner, reason);
      setForm(initialForm());
      setActionMessage('신규 거래처 변경 요청을 등록했습니다.');
      await loadData();
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setActionId(null);
    }
  };

  const handleApprove = async (id: number) => {
    setActionId(id);
    setActionError(null);
    setActionMessage(null);
    try {
      await masterDataService.approveBusinessPartnerChangeRequest(id);
      setActionMessage(`요청 #${id}을 승인했습니다.`);
      await loadData();
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (id: number) => {
    if (!rejectReason.trim()) {
      setActionError('반려 사유를 입력해 주세요.');
      return;
    }

    setActionId(id);
    setActionError(null);
    setActionMessage(null);
    try {
      await masterDataService.rejectBusinessPartnerChangeRequest(id, rejectReason);
      setRejectingId(null);
      setRejectReason('');
      setActionMessage(`요청 #${id}을 반려했습니다.`);
      await loadData();
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setActionId(null);
    }
  };

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-3 text-blue-400">
            <ClipboardCheck size={20} aria-hidden="true" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Master Data Approval</span>
          </div>
          <h1 className="text-4xl font-black tracking-tight text-white">거래처 등록 및 승인</h1>
          <p className="mt-2 text-sm text-slate-500">
            신규 거래처를 변경 요청으로 등록하고 대기 중인 요청을 심사합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadData()}
          disabled={loading || actionId !== null}
          aria-label="거래처와 승인 요청 새로고침"
          className="flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-5 py-3 text-xs font-black text-slate-300 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <RefreshCw size={16} className={loading ? 'animate-spin' : ''} aria-hidden="true" />
          새로고침
        </button>
      </header>

      {(loadError || actionError || actionMessage) && (
        <div
          role={loadError || actionError ? 'alert' : 'status'}
          className={`flex items-start gap-3 rounded-2xl border p-4 text-sm ${
            loadError || actionError
              ? 'border-rose-500/20 bg-rose-500/10 text-rose-300'
              : 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300'
          }`}
        >
          {loadError || actionError ? (
            <AlertCircle size={20} className="mt-0.5 shrink-0" aria-hidden="true" />
          ) : (
            <CheckCircle2 size={20} className="mt-0.5 shrink-0" aria-hidden="true" />
          )}
          <span>{loadError || actionError || actionMessage}</span>
        </div>
      )}

      <section className="rounded-[2rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
        <div className="mb-6 flex items-center gap-3">
          <div className="rounded-xl bg-blue-500/10 p-3 text-blue-400">
            <Plus size={20} aria-hidden="true" />
          </div>
          <div>
            <h2 className="font-black text-white">신규 거래처 요청</h2>
            <p className="text-xs text-slate-500">승인 후 적용되는 CREATE 요청을 생성합니다.</p>
          </div>
        </div>

        <form onSubmit={handleCreate} className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-4">
          <div>
            <label htmlFor="partner-code" className={labelClass}>거래처 코드 *</label>
            <input id="partner-code" className={inputClass} value={form.businessPartnerCode} onChange={(e) => updateForm('businessPartnerCode', e.target.value)} required maxLength={100} disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="partner-name" className={labelClass}>거래처명 *</label>
            <input id="partner-name" className={inputClass} value={form.businessPartnerName} onChange={(e) => updateForm('businessPartnerName', e.target.value)} required disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="registration-number" className={labelClass}>사업자등록번호 *</label>
            <input id="registration-number" className={inputClass} value={form.registrationNumber} onChange={(e) => updateForm('registrationNumber', e.target.value)} required disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="ceo-name" className={labelClass}>대표자명 *</label>
            <input id="ceo-name" className={inputClass} value={form.ceoName} onChange={(e) => updateForm('ceoName', e.target.value)} required disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="business-type" className={labelClass}>업태</label>
            <input id="business-type" className={inputClass} value={form.businessType} onChange={(e) => updateForm('businessType', e.target.value)} disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="business-item" className={labelClass}>종목</label>
            <input id="business-item" className={inputClass} value={form.businessItem} onChange={(e) => updateForm('businessItem', e.target.value)} disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="partner-type" className={labelClass}>거래처 유형 *</label>
            <select id="partner-type" className={inputClass} value={form.partnerType} onChange={(e) => updateForm('partnerType', e.target.value as PartnerType)} disabled={actionId !== null}>
              <option value="CUSTOMER">CUSTOMER</option>
              <option value="VENDOR">VENDOR</option>
              <option value="BANK">BANK</option>
              <option value="OTHER_BP">OTHER_BP</option>
            </select>
          </div>
          <div>
            <label htmlFor="kyc-status" className={labelClass}>KYC 상태 *</label>
            <select id="kyc-status" className={inputClass} value={form.kycStatus} onChange={(e) => updateForm('kycStatus', e.target.value as KycStatus)} disabled={actionId !== null}>
              <option value="PENDING">PENDING</option>
              <option value="APPROVED">APPROVED</option>
              <option value="REVIEW_REQUIRED">REVIEW_REQUIRED</option>
              <option value="REJECTED">REJECTED</option>
            </select>
          </div>
          <div>
            <label htmlFor="risk-rating" className={labelClass}>위험 등급 *</label>
            <select id="risk-rating" className={inputClass} value={form.riskRating} onChange={(e) => updateForm('riskRating', e.target.value as RiskRating)} disabled={actionId !== null}>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div>
            <label htmlFor="valid-from" className={labelClass}>적용 시작일 *</label>
            <input id="valid-from" type="date" className={inputClass} value={form.validFrom} onChange={(e) => updateForm('validFrom', e.target.value)} required disabled={actionId !== null} />
          </div>
          <div>
            <label htmlFor="valid-to" className={labelClass}>적용 종료일 *</label>
            <input id="valid-to" type="date" className={inputClass} value={form.validTo} onChange={(e) => updateForm('validTo', e.target.value)} required disabled={actionId !== null} />
          </div>
          <div className="flex items-end">
            <label className="flex min-h-11 w-full cursor-pointer items-center gap-3 rounded-xl border border-white/10 bg-slate-950/70 px-4 text-sm text-slate-300">
              <input type="checkbox" checked={form.useYn} onChange={(e) => updateForm('useYn', e.target.checked)} disabled={actionId !== null} />
              사용 상태로 등록
            </label>
          </div>
          <div className="md:col-span-2 xl:col-span-3">
            <label htmlFor="request-reason" className={labelClass}>요청 사유</label>
            <input id="request-reason" className={inputClass} value={form.reason} onChange={(e) => updateForm('reason', e.target.value)} maxLength={500} disabled={actionId !== null} placeholder="등록 목적이나 검토 참고사항" />
          </div>
          <div className="flex items-end">
            <button type="submit" disabled={actionId !== null} aria-label="신규 거래처 승인 요청 제출" className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 text-sm font-black text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">
              <Send size={16} aria-hidden="true" />
              {actionId === 'create' ? '등록 중...' : '승인 요청'}
            </button>
          </div>
        </form>
      </section>

      <section className="rounded-[2rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
        <div className="mb-6 flex items-center justify-between gap-4">
          <div>
            <h2 className="font-black text-white">승인 대기함</h2>
            <p className="mt-1 text-xs text-slate-500">BUSINESS_PARTNER 요청만 표시합니다.</p>
          </div>
          <StatusBadge status={`${pendingRequests.length} PENDING`} variant="warning" />
        </div>

        {loading ? (
          <LoadingSkeleton rows={3} height="h-28" />
        ) : loadError ? (
          <EmptyState icon={AlertCircle} title="승인 요청을 불러오지 못했습니다" description="오류 내용을 확인한 뒤 새로고침해 주세요." />
        ) : pendingRequests.length === 0 ? (
          <EmptyState icon={ClipboardCheck} title="대기 중인 요청이 없습니다" description="새로운 거래처 변경 요청이 등록되면 여기에 표시됩니다." />
        ) : (
          <div className="space-y-4">
            {pendingRequests.map((request) => {
              const { payload, error: payloadError } = parsePartnerPayload(request);
              const busy = actionId === request.id;
              return (
                <article key={request.id} className="rounded-2xl border border-white/10 bg-slate-950/50 p-5">
                  <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
                    <div className="min-w-0">
                      <div className="mb-2 flex flex-wrap items-center gap-2">
                        <StatusBadge status={request.status === 'REQUESTED' ? 'PENDING' : request.status} variant="warning" />
                        <span className="text-xs font-bold text-slate-600">요청 #{request.id}</span>
                      </div>
                      <h3 className="truncate text-lg font-black text-white">{payload?.businessPartnerName || request.targetKey}</h3>
                      <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-xs text-slate-400">
                        <span>코드: {request.targetKey}</span>
                        <span>유형: {payload?.partnerType || '-'}</span>
                        <span>사업자번호: {payload?.registrationNumber || '-'}</span>
                        <span>대표자: {payload?.ceoName || '-'}</span>
                        <span>업태/종목: {payload?.businessType || '-'} / {payload?.businessItem || '-'}</span>
                        <span>KYC/위험: {payload?.kycStatus || '-'} / {payload?.riskRating || '-'}</span>
                        <span>사용 여부: {payload ? (payload.useYn ? '사용' : '미사용') : '-'}</span>
                        <span>유효기간: {payload?.validFrom || '-'} ~ {payload?.validTo || '-'}</span>
                        <span>요청자: {request.requestedBy}</span>
                        <span>적용일: {request.effectiveDate}</span>
                        <span>요청 버전: {request.requestedVersion}</span>
                      </div>
                      {request.reason && <p className="mt-3 text-xs text-slate-500">요청 사유: {request.reason}</p>}
                      {payloadError && (
                        <p className="mt-3 rounded-lg border border-rose-500/20 bg-rose-500/10 px-3 py-2 text-xs font-bold text-rose-300">
                          {payloadError} 승인할 수 없습니다.
                        </p>
                      )}
                    </div>
                    <div className="flex shrink-0 gap-2">
                      <button type="button" onClick={() => void handleApprove(request.id)} disabled={actionId !== null || payloadError !== null} aria-label={`${request.targetKey} 거래처 요청 승인`} className="flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-3 text-xs font-black text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50">
                        <CheckCircle2 size={16} aria-hidden="true" />
                        {busy ? '처리 중...' : '승인'}
                      </button>
                      <button type="button" onClick={() => { setRejectingId(request.id); setRejectReason(''); setActionError(null); }} disabled={actionId !== null} aria-label={`${request.targetKey} 거래처 요청 반려 사유 입력`} className="flex items-center gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-xs font-black text-rose-300 transition hover:bg-rose-500/20 disabled:cursor-not-allowed disabled:opacity-50">
                        <XCircle size={16} aria-hidden="true" />
                        반려
                      </button>
                    </div>
                  </div>
                  {rejectingId === request.id && (
                    <div className="mt-5 rounded-xl border border-rose-500/20 bg-rose-500/5 p-4">
                      <label htmlFor={`reject-reason-${request.id}`} className={labelClass}>반려 사유 *</label>
                      <textarea id={`reject-reason-${request.id}`} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} required maxLength={500} rows={3} disabled={busy} aria-required="true" className={inputClass} />
                      <div className="mt-3 flex justify-end gap-2">
                        <button type="button" onClick={() => { setRejectingId(null); setRejectReason(''); }} disabled={busy} className="rounded-lg px-4 py-2 text-xs font-bold text-slate-400 hover:bg-white/5 disabled:opacity-50">취소</button>
                        <button type="button" onClick={() => void handleReject(request.id)} disabled={busy || !rejectReason.trim()} aria-label={`${request.targetKey} 거래처 요청 반려 확정`} className="rounded-lg bg-rose-600 px-4 py-2 text-xs font-black text-white hover:bg-rose-500 disabled:cursor-not-allowed disabled:opacity-50">
                          {busy ? '처리 중...' : '반려 확정'}
                        </button>
                      </div>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="rounded-[2rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
        <div className="mb-6 flex items-center gap-3">
          <Building2 size={20} className="text-emerald-400" aria-hidden="true" />
          <div>
            <h2 className="font-black text-white">기존 거래처</h2>
            <p className="text-xs text-slate-500">현재 API에 등록된 거래처 {partners.length}건</p>
          </div>
        </div>
        {loading ? (
          <LoadingSkeleton rows={3} height="h-16" />
        ) : loadError ? (
          <EmptyState icon={AlertCircle} title="거래처를 불러오지 못했습니다" description="오류 내용을 확인한 뒤 새로고침해 주세요." />
        ) : partners.length === 0 ? (
          <EmptyState icon={Building2} title="등록된 거래처가 없습니다" description="승인된 CREATE 요청이 적용되면 거래처 목록에 표시됩니다." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-white/10 text-xs uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-3">코드 / 거래처명</th>
                  <th className="px-4 py-3">유형</th>
                  <th className="px-4 py-3">대표자</th>
                  <th className="px-4 py-3">KYC / 위험</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">유효기간</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {partners.map((partner) => (
                  <tr key={partner.id} className="text-slate-300">
                    <td className="px-4 py-4">
                      <div className="font-black text-white">{partner.businessPartnerName}</div>
                      <div className="mt-1 text-xs text-slate-600">{partner.businessPartnerCode}</div>
                    </td>
                    <td className="px-4 py-4">{partner.partnerType}</td>
                    <td className="px-4 py-4">{partner.ceoName || '-'}</td>
                    <td className="px-4 py-4">{partner.kycStatus} / {partner.riskRating}</td>
                    <td className="px-4 py-4"><StatusBadge status={partner.useYn ? 'ACTIVE' : 'INACTIVE'} variant={partner.useYn ? 'success' : 'neutral'} /></td>
                    <td className="px-4 py-4 text-xs">{partner.validFrom} ~ {partner.validTo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
