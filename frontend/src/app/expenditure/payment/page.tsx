"use client";

import React, { useState } from 'react';
import {
  Banknote,
  Building2,
  Clock,
  FileText,
  CreditCard,
  ShieldCheck,
  Play,
  Zap
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockResolutions, mockPayables } from '@/mocks/expenditure';

interface BankAccount {
  id: string;
  name: string;
  account: string;
  balance: number;
}

export default function PaymentRunPage() {
  const [bankAccounts] = useState<BankAccount[]>([
    { id: 'BANK-01', name: '신한은행 당좌예금', account: '110-123-456789', balance: 1450000000 },
    { id: 'BANK-02', name: '하나은행 법인계좌', account: '298-910034-11004', balance: 820000000 },
    { id: 'BANK-03', name: '국민은행 사업자계좌', account: '817-21-0922-811', balance: 530000000 },
  ]);

  const [selectedBank, setSelectedBank] = useState<string>('BANK-02');
  const [selectedResolutions, setSelectedResolutions] = useState<string[]>(['res-1']);
  const [selectedPayables, setSelectedPayables] = useState<string[]>(['ap-1', 'ap-2']);
  const [isExecuting, setIsExecuting] = useState(false);
  const [, setExecutionStep] = useState<'IDLE' | 'PROCESSING' | 'DONE'>('IDLE');

  // Filter approved resolutions and unpaid payables
  const approvedResolutions = mockResolutions.filter(r => r.status === 'APPROVED' || r.status === 'PENDING');
  const unpaidPayables = mockPayables.filter(p => p.status === 'UNPAID' || p.status === 'OVERDUE');

  const toggleResolution = (id: string) => {
    if (selectedResolutions.includes(id)) {
      setSelectedResolutions(selectedResolutions.filter(rId => rId !== id));
    } else {
      setSelectedResolutions([...selectedResolutions, id]);
    }
  };

  const togglePayable = (id: string) => {
    if (selectedPayables.includes(id)) {
      setSelectedPayables(selectedPayables.filter(pId => pId !== id));
    } else {
      setSelectedPayables([...selectedPayables, id]);
    }
  };

  // Calculate totals
  const totalResolutionPayout = approvedResolutions
    .filter(r => selectedResolutions.includes(r.id))
    .reduce((sum, r) => sum + r.totalAmount + r.totalTax, 0);

  const totalPayablePayout = unpaidPayables
    .filter(p => selectedPayables.includes(p.id))
    .reduce((sum, p) => sum + p.balance, 0);

  const totalPayout = totalResolutionPayout + totalPayablePayout;
  const currentAccount = bankAccounts.find(b => b.id === selectedBank);

  const handleExecutePaymentRun = () => {
    if (totalPayout === 0) {
      alert("지급을 실행할 항목을 최소 1건 이상 선택해주세요.");
      return;
    }

    if (currentAccount && totalPayout > currentAccount.balance) {
      alert("출금 계좌의 잔액이 부족합니다. 다른 계좌를 선택하거나 지급 대상 건수를 조정하세요.");
      return;
    }

    setIsExecuting(true);
    setExecutionStep('PROCESSING');

    setTimeout(() => {
      setExecutionStep('DONE');
      setIsExecuting(false);
      alert(`[펌뱅킹 지급 실행 완료]\n총 ${selectedResolutions.length + selectedPayables.length}건에 대해 ₩${totalPayout.toLocaleString()} 이체가 정상 실행되었습니다.`);
    }, 1500);
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="지급 실행 (Payment Run)"
        description="승인된 지출결의서 및 매입채무 건에 대해 펌뱅킹(FirmBanking) 연동 이체를 일괄 실행합니다."
        breadcrumbs={[
          { label: '자금운영' },
          { label: '지급 실행' }
        ]}
        icon={Banknote}
        actions={
          <div className="flex items-center gap-2 bg-white px-3.5 py-2 rounded-xl border border-[#eaedf4] text-xs font-semibold text-[#545b69] shadow-sm">
            <ShieldCheck size={16} className="text-[#4262ff]" />
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
              className={`p-6 rounded-2xl cursor-pointer transition-all duration-300 ${
                isSelected 
                  ? 'bg-blue-50/50 border-2 border-[#4262ff] shadow-md shadow-blue-600/10' 
                  : 'bg-white border border-[#eaedf4] hover:border-blue-200 hover:shadow-md shadow-sm'
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold uppercase text-[#545b69] tracking-wider">{acc.name}</span>
                <Building2 size={20} className={isSelected ? 'text-[#4262ff]' : 'text-[#8c94a4]'} />
              </div>

              <div className="text-xs font-mono font-semibold text-[#8c94a4] mb-1">{acc.account}</div>
              <div className="text-2xl font-black text-[#17191e] tracking-tight font-mono">
                <AmountDisplay amount={acc.balance} />
              </div>

              <div className="mt-4 pt-3 border-t border-[#eaedf4] flex justify-between items-center text-[11px]">
                <span className="text-[#8c94a4] font-medium">출금 가능 계좌</span>
                <span className={`font-bold ${isSelected ? 'text-[#4262ff]' : 'text-[#8c94a4]'}`}>
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
          <div className="rounded-2xl bg-white border border-[#eaedf4] p-7 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-[#eaedf4] pb-4">
              <div>
                <h3 className="text-base font-bold text-[#17191e] tracking-tight flex items-center gap-2">
                  <FileText className="text-[#4262ff]" size={18} />
                  지급 대기 지출결의서 ({approvedResolutions.length}건)
                </h3>
                <p className="text-xs text-[#545b69] font-medium mt-0.5">
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
                    className={`flex items-center justify-between p-4 rounded-xl border transition-all cursor-pointer ${
                      isChecked 
                        ? 'bg-blue-50/60 border-blue-300' 
                        : 'bg-[#f7f8fb] border-[#eaedf4] hover:bg-blue-50/30 hover:border-blue-200'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="w-4 h-4 rounded accent-[#4262ff] cursor-pointer"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-[#4262ff]">{res.resolutionNo}</span>
                          <StatusBadge status={res.status === 'APPROVED' ? '승인완료' : '검토대기'} variant={res.status === 'APPROVED' ? 'success' : 'warning'} />
                        </div>
                        <div className="text-sm font-bold text-[#17191e] mt-1">{res.title}</div>
                        <div className="text-xs text-[#545b69] font-medium mt-0.5">수령인: {res.vendorName} ({res.vendorBank} {res.vendorAccount})</div>
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-mono font-bold text-[#17191e]">
                        <AmountDisplay amount={grandTotal} />
                      </span>
                      <span className="text-[10px] text-[#8c94a4] block font-medium mt-0.5">만기일: {res.dueDate}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 2: Unpaid AP Items */}
          <div className="rounded-2xl bg-white border border-[#eaedf4] p-7 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-[#eaedf4] pb-4">
              <div>
                <h3 className="text-base font-bold text-[#17191e] tracking-tight flex items-center gap-2">
                  <CreditCard className="text-amber-500" size={18} />
                  지급 대상 매입채무 AP ({unpaidPayables.length}건)
                </h3>
                <p className="text-xs text-[#545b69] font-medium mt-0.5">
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
                    className={`flex items-center justify-between p-4 rounded-xl border transition-all cursor-pointer ${
                      isChecked 
                        ? 'bg-amber-50/60 border-amber-300' 
                        : 'bg-[#f7f8fb] border-[#eaedf4] hover:bg-amber-50/30 hover:border-amber-200'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="w-4 h-4 rounded accent-amber-600 cursor-pointer"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-amber-600">{ap.apNumber}</span>
                          <StatusBadge status={ap.status === 'OVERDUE' ? '연체' : '미지급'} variant={ap.status === 'OVERDUE' ? 'error' : 'warning'} />
                        </div>
                        <div className="text-sm font-bold text-[#17191e] mt-1">{ap.vendorName} - {ap.description}</div>
                        <div className="text-xs text-[#545b69] font-medium mt-0.5">계산서: {ap.invoiceNumber}</div>
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-mono font-bold text-[#17191e]">
                        <AmountDisplay amount={ap.balance} />
                      </span>
                      <span className="text-[10px] text-[#8c94a4] block font-medium mt-0.5">만기일: {ap.dueDate}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right Column: Execution Summary Card (1/3 width) */}
        <div className="space-y-6">
          <div className="rounded-2xl bg-white border border-[#eaedf4] p-7 shadow-sm space-y-6 sticky top-6">
            <div className="border-b border-[#eaedf4] pb-4">
              <h3 className="text-base font-bold text-[#17191e] tracking-tight flex items-center gap-2">
                <Zap className="text-[#4262ff]" size={18} />
                지급 배치 실행 요약
              </h3>
              <p className="text-xs text-[#545b69] font-medium mt-0.5">
                펌뱅킹 배치 총 이체 금액 및 잔액 검증
              </p>
            </div>

            <div className="space-y-3.5 text-xs font-medium">
              <div className="flex justify-between py-1.5 border-b border-[#eaedf4] text-[#545b69]">
                <span>출금 계좌:</span>
                <span className="text-[#17191e] font-bold">{currentAccount?.name}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-[#eaedf4] text-[#545b69]">
                <span>선택된 지출결의:</span>
                <span className="text-[#17191e] font-bold">{selectedResolutions.length}건</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-[#eaedf4] text-[#545b69]">
                <span>선택된 매입채무:</span>
                <span className="text-[#17191e] font-bold">{selectedPayables.length}건</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-[#eaedf4] text-[#545b69]">
                <span>이체 수수료(면제):</span>
                <span className="text-emerald-600 font-bold">₩0</span>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-[#f7f8fb] border border-[#eaedf4] text-right space-y-1">
              <span className="text-[10px] text-[#8c94a4] uppercase tracking-wider block font-bold">총 출금 이체 예정액</span>
              <div className="text-2xl font-black text-[#17191e] font-mono tracking-tight">
                <AmountDisplay amount={totalPayout} />
              </div>
            </div>

            <button
              onClick={handleExecutePaymentRun}
              disabled={isExecuting || totalPayout === 0}
              className={`w-full py-3.5 rounded-xl font-bold text-xs transition-all shadow-md flex items-center justify-center gap-2 ${
                isExecuting 
                  ? 'bg-slate-200 text-slate-400 cursor-not-allowed' 
                  : 'bg-[#4262ff] hover:bg-[#3452e6] text-white shadow-blue-600/20 active:scale-98'
              }`}
            >
              {isExecuting ? (
                <>
                  <Clock className="animate-spin" size={16} />
                  펌뱅킹 이체 처리 중...
                </>
              ) : (
                <>
                  <Play size={16} />
                  지급 실행 (펌뱅킹 송금)
                </>
              )}
            </button>

            <div className="text-[11px] text-[#8c94a4] text-center font-medium">
              실행 클릭 시 즉시 펌뱅킹 금융망으로 송금 전문이 송신됩니다.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
