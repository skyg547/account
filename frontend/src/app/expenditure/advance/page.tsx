'use client';

import React, { useState } from 'react';
import { 
  Wallet, 
  Plus, 
  ArrowRightLeft, 
  Building2, 
  Calendar, 
  CheckCircle2, 
  Clock, 
  FileText, 
  Search, 
  ArrowRight,
  ShieldAlert
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockPayables } from '@/mocks/expenditure';

interface AdvancePayment {
  id: string;
  advNumber: string;
  vendorName: string;
  paymentDate: string;
  totalAmount: number;
  offsetAmount: number;
  balance: number;
  status: 'UNSETTLED' | 'PARTIAL' | 'SETTLED';
  description: string;
}

export default function AdvancePage() {
  const [advances, setAdvances] = useState<AdvancePayment[]>([
    {
      id: 'adv-001',
      advNumber: 'ADV-2026-0701',
      vendorName: '(주)삼성SDS',
      paymentDate: '2026-06-01',
      totalAmount: 20000000,
      offsetAmount: 5000000,
      balance: 15000000,
      status: 'PARTIAL',
      description: 'ERP 구축 프로젝트 1차 선수 선급금'
    },
    {
      id: 'adv-002',
      advNumber: 'ADV-2026-0615',
      vendorName: '(주)한샘인테리어',
      paymentDate: '2026-05-15',
      totalAmount: 10000000,
      offsetAmount: 0,
      balance: 10000000,
      status: 'UNSETTLED',
      description: '본사 인테리어 공사 사전 착수금 선급'
    },
    {
      id: 'adv-003',
      advNumber: 'ADV-2026-0501',
      vendorName: '오라클코리아 유한회사',
      paymentDate: '2026-04-10',
      totalAmount: 15000000,
      offsetAmount: 15000000,
      balance: 0,
      status: 'SETTLED',
      description: 'DBMS 라이선스 사전 예약 정산금'
    }
  ]);

  const [activeTab, setActiveTab] = useState('LIST');
  const [selectedVendor, setSelectedVendor] = useState('(주)삼성SDS');
  const [selectedAdvId, setSelectedAdvId] = useState('adv-001');
  const [selectedApId, setSelectedApId] = useState('ap-001');
  const [offsetInput, setOffsetInput] = useState<number>(10000000);

  // New Advance form state
  const [newVendor, setNewVendor] = useState('');
  const [newAmount, setNewAmount] = useState<number>(0);
  const [newDesc, setNewDesc] = useState('');
  const [newDate, setNewDate] = useState('2026-07-28');

  const mainTabs: TabItem[] = [
    { id: 'LIST', label: '선급금 현황 목록', icon: Wallet },
    { id: 'OFFSET', label: '매입채무(AP) 상계 정산', icon: ArrowRightLeft },
    { id: 'NEW', label: '신규 선급금 등록', icon: Plus },
  ];

  const totalAdvanceAmount = advances.reduce((sum, a) => sum + a.totalAmount, 0);
  const totalBalanceAmount = advances.reduce((sum, a) => sum + a.balance, 0);
  const totalOffsetAmount = advances.reduce((sum, a) => sum + a.offsetAmount, 0);

  const handleRegisterNewAdvance = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newVendor || newAmount <= 0) {
      alert('거래처명과 금액을 입력해 주세요.');
      return;
    }
    const newEntry: AdvancePayment = {
      id: `adv-${Date.now()}`,
      advNumber: `ADV-2026-0${advances.length + 1}`,
      vendorName: newVendor,
      paymentDate: newDate,
      totalAmount: Number(newAmount),
      offsetAmount: 0,
      balance: Number(newAmount),
      status: 'UNSETTLED',
      description: newDesc || '선급금 지급'
    };
    setAdvances([newEntry, ...advances]);
    alert(`[선급금 등록 완료] ${newVendor} 대상 ₩${Number(newAmount).toLocaleString()} 선급금이 등록되었습니다.`);
    setNewVendor('');
    setNewAmount(0);
    setNewDesc('');
    setActiveTab('LIST');
  };

  const handleExecuteOffset = () => {
    const targetAdv = advances.find(a => a.id === selectedAdvId);
    if (!targetAdv) return;
    if (offsetInput <= 0 || offsetInput > targetAdv.balance) {
      alert('유효한 상계 정산 금액을 입력해 주세요 (잔액 범위 내).');
      return;
    }

    setAdvances(advances.map(a => {
      if (a.id !== selectedAdvId) return a;
      const newOffset = a.offsetAmount + offsetInput;
      const newBal = a.totalAmount - newOffset;
      return {
        ...a,
        offsetAmount: newOffset,
        balance: newBal,
        status: newBal === 0 ? 'SETTLED' : 'PARTIAL'
      };
    }));

    alert(`[AP 상계 처리 완료] ${targetAdv.vendorName} 선급금 ₩${offsetInput.toLocaleString()} 이 매입채무와 성공적으로 상계 처리되었습니다.`);
    setActiveTab('LIST');
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="선급금 관리"
        description="사전 지급 선급금(Prepayments)을 등록하고 매입채무(AP) 발생 시 상계 정산 처리합니다."
        breadcrumbs={[
          { label: '지출관리' },
          { label: '선급금 관리' }
        ]}
        icon={Wallet}
      />

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>총 등록 선급금</span>
            <Wallet size={18} className="text-blue-400" />
          </div>
          <div className="text-3xl font-black text-white italic tracking-tight font-mono">
            <AmountDisplay amount={totalAdvanceAmount} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            전체 선급 건수: <span className="text-white font-black">{advances.length}건</span>
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>미상계 잔액</span>
            <Clock size={18} className="text-amber-400" />
          </div>
          <div className="text-3xl font-black text-amber-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalBalanceAmount} />
          </div>
          <div className="text-xs text-amber-400/80 font-medium">
            정산 대기중인 선급금 현황
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>누적 상계 완료액</span>
            <CheckCircle2 size={18} className="text-emerald-400" />
          </div>
          <div className="text-3xl font-black text-emerald-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalOffsetAmount} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            매입채무와 정상 대체된 누적 금액
          </div>
        </div>
      </div>

      <Tabs tabs={mainTabs} activeTab={activeTab} onChange={setActiveTab} />

      {/* Tab Content 1: LIST */}
      {activeTab === 'LIST' && (
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
          <div className="flex items-center justify-between border-b border-white/5 pb-4">
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <FileText className="text-blue-400" size={18} />
              선급금 등록 및 정산 현황
            </h3>
          </div>

          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                <th className="py-3.5 px-4">선급 번호</th>
                <th className="py-3.5 px-4">거래처명</th>
                <th className="py-3.5 px-4">지급일자</th>
                <th className="py-3.5 px-4">적요</th>
                <th className="py-3.5 px-4 text-right">총 선급금액</th>
                <th className="py-3.5 px-4 text-right">상계 정산액</th>
                <th className="py-3.5 px-4 text-right">미정산 잔액</th>
                <th className="py-3.5 px-4 text-center">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {advances.map((adv) => (
                <tr key={adv.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-4 px-4 font-mono font-bold text-blue-400">{adv.advNumber}</td>
                  <td className="py-4 px-4 font-black text-white">{adv.vendorName}</td>
                  <td className="py-4 px-4 text-slate-400 font-mono">{adv.paymentDate}</td>
                  <td className="py-4 px-4 text-slate-300">{adv.description}</td>
                  <td className="py-4 px-4 text-right font-mono font-bold text-white"><AmountDisplay amount={adv.totalAmount} /></td>
                  <td className="py-4 px-4 text-right font-mono text-emerald-400"><AmountDisplay amount={adv.offsetAmount} /></td>
                  <td className="py-4 px-4 text-right font-mono font-black text-amber-400 italic text-sm"><AmountDisplay amount={adv.balance} /></td>
                  <td className="py-4 px-4 text-center">
                    <StatusBadge 
                      status={adv.status === 'SETTLED' ? '완료' : adv.status === 'PARTIAL' ? '부분상계' : '미정산'}
                      variant={adv.status === 'SETTLED' ? 'success' : adv.status === 'PARTIAL' ? 'info' : 'warning'}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Tab Content 2: OFFSET */}
      {activeTab === 'OFFSET' && (
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          <div className="border-b border-white/5 pb-4">
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <ArrowRightLeft className="text-emerald-400" size={18} />
              선급금 - 매입채무(AP) 상계 맞교환 매칭
            </h3>
            <p className="text-xs text-slate-400 font-medium mt-1">
              기지급된 선급 잔액으로 도래한 매입채무 세금계산서를 차감 상계합니다.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* Source Advance Selection */}
            <div className="p-6 rounded-2xl bg-white/[0.02] border border-white/5 space-y-4">
              <h4 className="text-xs font-black uppercase text-blue-400 tracking-wider">1. 차감 대상 선급금 선택</h4>
              <select
                value={selectedAdvId}
                onChange={(e) => setSelectedAdvId(e.target.value)}
                className="w-full bg-slate-950 border border-white/10 rounded-xl px-3 py-2.5 text-xs text-white"
              >
                {advances.filter(a => a.balance > 0).map(a => (
                  <option key={a.id} value={a.id}>
                    {a.advNumber} - {a.vendorName} (잔액: ₩{a.balance.toLocaleString()})
                  </option>
                ))}
              </select>
            </div>

            {/* Target AP Invoice Selection */}
            <div className="p-6 rounded-2xl bg-white/[0.02] border border-white/5 space-y-4">
              <h4 className="text-xs font-black uppercase text-emerald-400 tracking-wider">2. 상계 처리할 매입채무(AP) 선택</h4>
              <select
                value={selectedApId}
                onChange={(e) => setSelectedApId(e.target.value)}
                className="w-full bg-slate-950 border border-white/10 rounded-xl px-3 py-2.5 text-xs text-white"
              >
                {mockPayables.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.apNumber} - {p.vendorName} (채무 잔액: ₩{p.balance.toLocaleString()})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="p-6 rounded-2xl bg-slate-950 border border-white/5 space-y-4">
            <label className="text-xs font-black text-slate-300 uppercase tracking-wider block">3. 상계 적용 금액 입력 (원)</label>
            <input
              type="number"
              value={offsetInput}
              onChange={(e) => setOffsetInput(Number(e.target.value))}
              className="w-full bg-slate-900 border border-white/10 rounded-xl px-4 py-3 text-lg font-mono font-bold text-emerald-400"
            />

            <div className="flex justify-end pt-4">
              <button
                onClick={handleExecuteOffset}
                className="px-8 py-3 bg-emerald-600 hover:bg-emerald-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-emerald-600/20 flex items-center gap-2"
              >
                <ArrowRight size={16} /> 상계 전표 발행 확정
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tab Content 3: NEW */}
      {activeTab === 'NEW' && (
        <form onSubmit={handleRegisterNewAdvance} className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          <div className="border-b border-white/5 pb-4">
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <Plus className="text-blue-400" size={18} />
              신규 선급금 지급 등록
            </h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">거래처명</label>
              <input
                type="text"
                value={newVendor}
                onChange={(e) => setNewVendor(e.target.value)}
                placeholder="예: (주)LG CNS"
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white font-medium"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">선급 지급 금액 (원)</label>
              <input
                type="number"
                value={newAmount || ''}
                onChange={(e) => setNewAmount(Number(e.target.value))}
                placeholder="0"
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm font-mono text-white font-bold"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">지급 일자</label>
              <input
                type="date"
                value={newDate}
                onChange={(e) => setNewDate(e.target.value)}
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white font-medium"
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">지급 사유 및 적요</label>
              <input
                type="text"
                value={newDesc}
                onChange={(e) => setNewDesc(e.target.value)}
                placeholder="예: 차세대 플랫폼 구축 착수 선급금"
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white font-medium"
              />
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button
              type="submit"
              className="px-8 py-3 bg-blue-600 hover:bg-blue-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20"
            >
              선급금 등록 완료
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
