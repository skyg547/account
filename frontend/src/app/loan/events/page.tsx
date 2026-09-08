'use client';

import React, { useState, useMemo } from 'react';
import {
  Zap,
  RotateCcw,
  Plus,
  Search,
  Sparkles,
  Play
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockLoanEvents, mockContracts, LoanEventDto } from '@/mocks/loan';

export default function LoanEventsPage() {
  const [events, setEvents] = useState<LoanEventDto[]>(mockLoanEvents);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Event registration form state
  const [selectedContractId, setSelectedContractId] = useState('');
  const [eventType, setEventType] = useState<LoanEventDto['eventType']>('EARLY_REPAYMENT');
  const [effectiveDate, setEffectiveDate] = useState('2026-03-25');
  const [details, setDetails] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);

  const tabs: TabItem[] = [
    { id: 'ALL', label: '전체 이벤트' },
    { id: 'EARLY_REPAYMENT', label: '조기 상환' },
    { id: 'RATE_CHANGE', label: '금리 변동' },
    { id: 'RESTRUCTURING', label: '조건 재조정' },
    { id: 'MATURITY_EXTENSION', label: '만기 연장' },
  ];

  const getEventTypeLabel = (type: LoanEventDto['eventType']) => {
    switch (type) {
      case 'EARLY_REPAYMENT':
        return '조기 상환';
      case 'RATE_CHANGE':
        return '금리 변동';
      case 'RESTRUCTURING':
        return '조건 재조정';
      case 'MATURITY_EXTENSION':
        return '만기 연장';
      default:
        return type;
    }
  };

  const handleRegisterEvent = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedContractId || !details) {
      alert('모든 필수 항목을 입력하세요.');
      return;
    }

    setIsProcessing(true);

    const contract = mockContracts.find(c => c.id === selectedContractId);
    setTimeout(() => {
      const newEvent: LoanEventDto = {
        eventId: `EVT-2026-00${events.length + 1}`,
        contractId: selectedContractId,
        contractNo: contract ? contract.contractNo : 'LN-20260399-001',
        borrowerName: contract ? contract.borrowerName : '차주',
        eventType,
        eventDate: '2026-03-25',
        effectiveDate,
        details,
        impactOnEir: 0.08,
        recalculationStatus: 'COMPLETED',
        processedBy: '관리자 직접입력',
      };

      setEvents([newEvent, ...events]);
      setIsProcessing(false);
      setSelectedContractId('');
      setDetails('');
      alert('대출 이벤트가 성공적으로 등록되었으며, EIR 재계산이 완료되었습니다.');
    }, 900);
  };

  const handleTriggerRecalculate = (eventId: string) => {
    setEvents(prev => prev.map(evt => {
      if (evt.eventId === eventId) {
        return { ...evt, recalculationStatus: 'COMPLETED' };
      }
      return evt;
    }));
    alert('해당 이벤트에 대한 EIR(유효이자율) 스케줄 재계산이 즉시 완료되었습니다.');
  };

  const filteredEvents = useMemo(() => {
    return events.filter(evt => {
      const matchesTab = activeTab === 'ALL' || evt.eventType === activeTab;
      const matchesSearch = 
        evt.contractNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
        evt.borrowerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        evt.details.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesSearch;
    });
  }, [events, activeTab, searchQuery]);

  const pendingRecalcCount = events.filter(e => e.recalculationStatus === 'PENDING').length;

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <PageHeader
        title="대출 이벤트 처리"
        description="조기상환, 금리변경, 대출조건 재조정 등 주요 여신 이벤트를 수집하여 유효이자율(EIR) 스케줄을 실시간 재계산합니다."
        breadcrumbs={[
          { label: '여신/대출 관리' },
          { label: '대출 이벤트 처리' },
        ]}
        icon={Zap}
      />

      {/* Top Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-1">
          <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">누적 이벤트 발생 건수</span>
          <div className="text-2xl font-black text-white font-mono">
            {events.length} <span className="text-sm font-normal text-slate-400">건</span>
          </div>
          <span className="text-xs text-slate-500 font-medium">조기상환, 금리변경 등 총계</span>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-1">
          <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">EIR 재계산 대기건</span>
          <div className="text-2xl font-black text-amber-400 font-mono">
            {pendingRecalcCount} <span className="text-sm font-normal text-slate-400">건</span>
          </div>
          <span className="text-xs text-slate-500 font-medium">배치 및 수시 재계산 처리 대상</span>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-1">
          <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">EIR 자동 정산 상태</span>
          <div className="text-2xl font-black text-emerald-400 font-mono">
            정상 연동 중
          </div>
          <span className="text-xs text-slate-500 font-medium">IFRS 9 유효이자율 엔진 작동</span>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Event Form */}
        <div className="lg:col-span-1 p-6 rounded-3xl bg-slate-900/50 border border-white/10 backdrop-blur-md space-y-6 h-fit">
          <div className="flex items-center gap-3 border-b border-white/10 pb-4">
            <div className="p-2.5 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
              <Plus size={20} />
            </div>
            <div>
              <h3 className="text-lg font-black text-white">신규 이벤트 등록</h3>
              <p className="text-xs text-slate-400">계약 조건 변경 시 EIR을 재산출합니다.</p>
            </div>
          </div>

          <form onSubmit={handleRegisterEvent} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">대출 계약 선택 *</label>
              <select
                value={selectedContractId}
                onChange={(e) => setSelectedContractId(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                required
              >
                <option value="">-- 대상 계약 선택 --</option>
                {mockContracts.map(c => (
                  <option key={c.id} value={c.id}>
                    [{c.contractNo}] {c.borrowerName}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">이벤트 유형 *</label>
              <select
                value={eventType}
                onChange={(e) => setEventType(e.target.value as LoanEventDto['eventType'])}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
              >
                <option value="EARLY_REPAYMENT">조기 상환 (Early Repayment)</option>
                <option value="RATE_CHANGE">금리 변동 (Rate Change)</option>
                <option value="RESTRUCTURING">대출 조건 재조정 (Restructuring)</option>
                <option value="MATURITY_EXTENSION">만기 연장 (Maturity Extension)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">효력 발생일 *</label>
              <input
                type="date"
                value={effectiveDate}
                onChange={(e) => setEffectiveDate(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">이벤트 세부 설명 *</label>
              <textarea
                rows={3}
                placeholder="상환금액, 변동 금리 폭, 변경 사유 등 상세 기록..."
                value={details}
                onChange={(e) => setDetails(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50 resize-none"
                required
              />
            </div>

            <button
              type="submit"
              disabled={isProcessing}
              className="w-full py-3 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-amber-600/20 flex items-center justify-center gap-2"
            >
              {isProcessing ? 'EIR 재계산 중...' : '이벤트 등록 및 EIR 재산출'}
              <Sparkles size={16} />
            </button>
          </form>
        </div>

        {/* Event Logs List */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />
            
            <div className="relative w-full sm:w-64">
              <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="계약, 설명 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-slate-900/50 border border-white/10 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
              />
            </div>
          </div>

          {filteredEvents.length === 0 ? (
            <EmptyState
              icon={Zap}
              title="등록된 이벤트가 없습니다"
              description="해당 조건의 대출 변경 이벤트 기록이 존재하지 않습니다."
            />
          ) : (
            <div className="rounded-2xl border border-white/5 bg-slate-900/50 backdrop-blur-md overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-300">
                  <thead className="bg-white/5 text-xs uppercase font-bold text-slate-400 border-b border-white/5">
                    <tr>
                      <th className="py-3.5 px-5">이벤트 ID / 유형</th>
                      <th className="py-3.5 px-5">계약번호 / 차주</th>
                      <th className="py-3.5 px-5">세부 내역</th>
                      <th className="py-3.5 px-5 text-center">EIR 영향</th>
                      <th className="py-3.5 px-5 text-center">재계산 상태</th>
                      <th className="py-3.5 px-5 text-right">실행</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {filteredEvents.map((evt) => (
                      <tr key={evt.eventId} className="hover:bg-white/[0.02] transition-colors">
                        <td className="py-3.5 px-5">
                          <div className="font-mono text-xs text-amber-400 font-bold">{evt.eventId}</div>
                          <span className="inline-block mt-0.5 text-[10px] font-bold px-2 py-0.5 rounded bg-white/5 border border-white/10 text-slate-300">
                            {getEventTypeLabel(evt.eventType)}
                          </span>
                        </td>

                        <td className="py-3.5 px-5">
                          <div className="font-mono text-xs text-blue-400 font-bold">{evt.contractNo}</div>
                          <div className="text-white font-bold text-xs mt-0.5">{evt.borrowerName}</div>
                        </td>

                        <td className="py-3.5 px-5">
                          <p className="text-xs text-slate-200 line-clamp-2 max-w-xs">{evt.details}</p>
                          <span className="text-[10px] text-slate-500 font-mono">발생일: {evt.eventDate}</span>
                        </td>

                        <td className="py-3.5 px-5 text-center font-mono font-bold text-xs">
                          <span className={evt.impactOnEir >= 0 ? 'text-amber-400' : 'text-blue-400'}>
                            {evt.impactOnEir >= 0 ? `+${evt.impactOnEir}%` : `${evt.impactOnEir}%`}
                          </span>
                        </td>

                        <td className="py-3.5 px-5 text-center">
                          <StatusBadge 
                            status={evt.recalculationStatus === 'COMPLETED' ? '재계산 완료' : '재계산 대기'} 
                            variant={evt.recalculationStatus === 'COMPLETED' ? 'success' : 'warning'} 
                          />
                        </td>

                        <td className="py-3.5 px-5 text-right">
                          {evt.recalculationStatus === 'PENDING' ? (
                            <button
                              onClick={() => handleTriggerRecalculate(evt.eventId)}
                              className="px-3 py-1.5 bg-amber-600/20 hover:bg-amber-600/30 text-amber-400 text-xs font-bold rounded-lg border border-amber-500/30 transition-all flex items-center gap-1 ml-auto"
                            >
                              <Play size={12} /> 수동 재계산
                            </button>
                          ) : (
                            <button
                              onClick={() => handleTriggerRecalculate(evt.eventId)}
                              className="p-2 bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white rounded-lg transition-all"
                              title="재계산 다시 실행"
                            >
                              <RotateCcw size={14} />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
