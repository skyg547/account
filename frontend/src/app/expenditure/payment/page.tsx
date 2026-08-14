'use client';

import React, { useState } from 'react';
import { 
  Banknote, 
  Building2, 
  CreditCard, 
  CheckCircle2, 
  Clock, 
  AlertCircle, 
  Play, 
  Send, 
  FileText, 
  ShieldCheck, 
  Search,
  Zap,
  ArrowRight
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockResolutions, mockPayables } from '@/mocks/expenditure';

export default function PaymentPage() {
  // Available bank accounts
  const [selectedBank, setSelectedBank] = useState('shinhan');
  const bankAccounts = [
    { id: 'shinhan', name: '신한은행 당좌예금', account: '110-123-456789', balance: 1450000000 },
    { id: 'hana', name: '하나은행 법인계좌', account: '298-910034-11004', balance: 820000000 },
    { id: 'kb', name: '국민은행 사업자계좌', account: '817-21-0922-811', balance: 530000000 },
  ];

  // Approved items ready for payment run
  const approvedResolutions = mockResolutions.filter(r => r.status === 'APPROVED' || r.status === 'PENDING');
  const unpaidPayables = mockPayables.filter(p => p.balance > 0);

  const [selectedResolutions, setSelectedResolutions] = useState<string[]>(['res-103']);
  const [selectedPayables, setSelectedPayables] = useState<string[]>(['ap-002', 'ap-006']);
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionStep, setExecutionStep] = useState<'IDLE' | 'PROCESSING' | 'DONE'>('IDLE');

  const toggleResolution = (id: string) => {
    if (selectedResolutions.includes(id)) {
      setSelectedResolutions(selectedResolutions.filter(item => item !== id));
    } else {
      setSelectedResolutions([...selectedResolutions, id]);
    }
  };

  const togglePayable = (id: string) => {
    if (selectedPayables.includes(id)) {
      setSelectedPayables(selectedPayables.filter(item => item !== id));
    } else {
      setSelectedPayables([...selectedPayables, id]);
    }
  };

  // Compute payment proposal totals
  const totalResolutionPayout = approvedResolutions
    .filter(r => selectedResolutions.includes(r.id))
    .reduce((sum, r) => sum + (r.totalAmount + r.totalTax), 0);

  const totalPayablePayout = unpaidPayables
    .filter(p => selectedPayables.includes(p.id))
    .reduce((sum, p) => sum + p.balance, 0);

  const totalPayout = totalResolutionPayout + totalPayablePayout;
  const currentAccount = bankAccounts.find(b => b.id === selectedBank);

  const handleExecutePaymentRun = () => {
    if (totalPayout === 0) {
      alert('지급 실행할 항목을 최소 1개 이상 선택해 주세요.');
      return;
    }
    if ((currentAccount?.balance || 0) < totalPayout) {
      alert('선택한 은행 계좌의 잔액이 부족합니다.');
      return;
    }

    setIsExecuting(true);
    setExecutionStep('PROCESSING');

    setTimeout(() => {
      setExecutionStep('DONE');
      setIsExecuting(false);
      alert(`[펌뱅킹 지급 실행 완료]\n총 ${selectedResolutions.length + selectedPayables.length}건에 대해 ₩${totalPayout.toLocaleString()} 이체가 정상 실행되었습니다.`);
    }, 2000);
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="지급 실행 (Payment Run)"
        description="승인된 지출결의서 및 매입채무 건에 대해 펌뱅킹(FirmBanking) 연동 이체를 일괄 실행합니다."
        breadcrumbs={[
          { label: '지출관리' },
          { label: '지급 실행' }
        ]}
        icon={Banknote}
        actions={
          <div className="flex items-center gap-3 bg-slate-900/50 backdrop-blur-md px-4 py-2 rounded-2xl border border-white/5 text-xs text-slate-400">
            <ShieldCheck size={16} className="text-blue-400" />
            <span>펌뱅킹 OTP 이체 연동 활성화</span>
          </div>
        }
      />

      {/* Account Selection Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {bankAccounts.map((acc) => {
          const isSelected = selectedBank === acc.id;
          return (
            <div
              key={acc.id}
              onClick={() => setSelectedBank(acc.id)}
              className={`p-6 rounded-3xl cursor-pointer border transition-all duration-300 ${
                isSelected 
                  ? 'bg-blue-600/10 border-blue-500 shadow-xl shadow-blue-500/10' 
                  : 'bg-slate-900/50 border-white/5 hover:border-white/20'
              }`}
            >
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs font-black uppercase text-slate-400 tracking-wider">{acc.name}</span>
                <Building2 size={20} className={isSelected ? 'text-blue-400' : 'text-slate-600'} />
              </div>

              <div className="text-xs font-mono font-bold text-slate-300 mb-2">{acc.account}</div>
              <div className="text-2xl font-black text-white italic tracking-tight font-mono">
                <AmountDisplay amount={acc.balance} />
              </div>

              <div className="mt-4 pt-3 border-t border-white/5 flex justify-between items-center text-[11px]">
                <span className="text-slate-500 font-medium">출금 가능 계좌</span>
                <span className={`font-black ${isSelected ? 'text-blue-400' : 'text-slate-500'}`}>
                  {isSelected ? '선택됨 ✓' : '선택'}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Execution Wizard Container */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Select Items to Pay (2/3 width) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Section 1: Approved Resolutions */}
          <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-white/5 pb-4">
              <div>
                <h3 className="text-base font-black text-white tracking-tight flex items-center gap-2">
                  <FileText className="text-blue-400" size={18} />
                  지급 대기 지출결의서 ({approvedResolutions.length}건)
                </h3>
                <p className="text-xs text-slate-400 font-medium mt-1">
                  결재가 최종 완료되어 즉시 이체 실행이 가능한 건입니다.
                </p>
              </div>
            </div>

            <div className="space-y-3">
              {approvedResolutions.map((res) => {
                const isChecked = selectedResolutions.includes(res.id);
                const grandTotal = res.totalAmount + res.totalTax;
                return (
                  <div
                    key={res.id}
                    onClick={() => toggleResolution(res.id)}
                    className={`flex items-center justify-between p-4 rounded-2xl border transition-all cursor-pointer ${
                      isChecked 
                        ? 'bg-blue-600/10 border-blue-500/40' 
                        : 'bg-white/[0.02] border-white/5 hover:border-white/10'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="w-4 h-4 rounded accent-blue-600 cursor-pointer"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-black text-blue-400">{res.resolutionNo}</span>
                          <StatusBadge status={res.status === 'APPROVED' ? '승인완료' : '검토대기'} variant={res.status === 'APPROVED' ? 'success' : 'warning'} />
                        </div>
                        <div className="text-sm font-black text-white mt-1">{res.title}</div>
                        <div className="text-xs text-slate-400 font-medium mt-0.5">수령인: {res.vendorName} ({res.vendorBank} {res.vendorAccount})</div>
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-mono font-black text-white italic">
                        <AmountDisplay amount={grandTotal} />
                      </span>
                      <span className="text-[10px] text-slate-500 block font-medium">만기일: {res.dueDate}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 2: Unpaid AP Items */}
          <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-white/5 pb-4">
              <div>
                <h3 className="text-base font-black text-white tracking-tight flex items-center gap-2">
                  <CreditCard className="text-amber-400" size={18} />
                  지급 대상 매입채무 AP ({unpaidPayables.length}건)
                </h3>
                <p className="text-xs text-slate-400 font-medium mt-1">
                  만기 도래 또는 연체 중인 매입채무 지급 건입니다.
                </p>
              </div>
            </div>

            <div className="space-y-3">
              {unpaidPayables.map((ap) => {
                const isChecked = selectedPayables.includes(ap.id);
                return (
                  <div
                    key={ap.id}
                    onClick={() => togglePayable(ap.id)}
                    className={`flex items-center justify-between p-4 rounded-2xl border transition-all cursor-pointer ${
                      isChecked 
                        ? 'bg-amber-500/10 border-amber-500/40' 
                        : 'bg-white/[0.02] border-white/5 hover:border-white/10'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="w-4 h-4 rounded accent-amber-500 cursor-pointer"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-black text-amber-400">{ap.apNumber}</span>
                          <StatusBadge status={ap.status === 'OVERDUE' ? '연체' : '미지급'} variant={ap.status === 'OVERDUE' ? 'error' : 'warning'} />
                        </div>
                        <div className="text-sm font-black text-white mt-1">{ap.vendorName} - {ap.description}</div>
                        <div className="text-xs text-slate-400 font-medium mt-0.5">계산서: {ap.invoiceNumber}</div>
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-mono font-black text-amber-400 italic">
                        <AmountDisplay amount={ap.balance} />
                      </span>
                      <span className="text-[10px] text-slate-500 block font-medium">만기일: {ap.dueDate}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right Column: Execution Summary Card (1/3 width) */}
        <div className="space-y-6">
          <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6 sticky top-6">
            <div className="border-b border-white/5 pb-4">
              <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
                <Zap className="text-blue-400" size={20} />
                지급 배치 실행 요약
              </h3>
              <p className="text-xs text-slate-400 font-medium mt-1">
                펌뱅킹 배치 총 이체 금액 및 잔액 검증
              </p>
            </div>

            <div className="space-y-4 text-xs font-medium">
              <div className="flex justify-between py-2 border-b border-white/5 text-slate-400">
                <span>출금 계좌:</span>
                <span className="text-white font-black">{currentAccount?.name}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-white/5 text-slate-400">
                <span>선택된 지출결의:</span>
                <span className="text-white font-black">{selectedResolutions.length}건</span>
              </div>
              <div className="flex justify-between py-2 border-b border-white/5 text-slate-400">
                <span>선택된 매입채무:</span>
                <span className="text-white font-black">{selectedPayables.length}건</span>
              </div>
              <div className="flex justify-between py-2 border-b border-white/5 text-slate-400">
                <span>이체 수수료(면제):</span>
                <span className="text-emerald-400 font-black">₩0</span>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 text-right space-y-1">
              <span className="text-[10px] text-slate-500 uppercase tracking-widest block font-bold">총 출금 이체 예정액</span>
              <div className="text-2xl font-black text-emerald-400 italic font-mono tracking-tight">
                <AmountDisplay amount={totalPayout} />
              </div>
            </div>

            <button
              onClick={handleExecutePaymentRun}
              disabled={isExecuting || totalPayout === 0}
              className={`w-full py-4 rounded-2xl font-black text-sm transition-all shadow-xl flex items-center justify-center gap-2 ${
                isExecuting 
                  ? 'bg-slate-800 text-slate-500 cursor-not-allowed' 
                  : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-600/20'
              }`}
            >
              {isExecuting ? (
                <>
                  <Clock className="animate-spin" size={18} />
                  펌뱅킹 이체 처리 중...
                </>
              ) : (
                <>
                  <Play size={18} />
                  지급 실행 (펌뱅킹 송금)
                </>
              )}
            </button>

            <div className="text-[11px] text-slate-500 text-center font-medium">
              실행 클릭 시 즉시 펌뱅킹 금융망으로 송금 전문이 송신됩니다.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
