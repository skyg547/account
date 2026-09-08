'use client';

import React, { useState } from 'react';
import {
  Receipt,
  Plus,
  Trash2,
  Send,
  Save,
  RotateCcw,
  UserCheck,
  Building2,
  CreditCard,
  HelpCircle,
  FileText
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import AmountDisplay from '@/components/ui/AmountDisplay';import { ResolutionLineItem } from '@/mocks/expenditure';

export default function ResolutionPage() {
  // Form State
  const [title, setTitle] = useState('');
  const [department, setDepartment] = useState('IT개발팀');
  const [vendorName, setVendorName] = useState('');
  const [vendorBank, setVendorBank] = useState('');
  const [vendorAccount, setVendorAccount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'BANK_TRANSFER' | 'CORPORATE_CARD' | 'CASH'>('BANK_TRANSFER');
  const [dueDate, setDueDate] = useState('2026-08-15');

  // Line items state
  const [lineItems, setLineItems] = useState<ResolutionLineItem[]>([
    {
      id: 'line-1',
      accountCode: '51400',
      accountName: '지급임차료/서버사용료',
      description: '클라우드 인프라 8월 기본 사용료',
      amount: 15000000,
      taxAmount: 1500000,
      costCenter: 'CC-101 (IT실)',
      remark: '월 정기 청구'
    }
  ]);

  // Approval Line state
  const [approvalLine] = useState([
    { step: 1, name: '김민준', position: '대리 (작성자)', dept: 'IT개발팀', role: 'DRAFTER' },
    { step: 2, name: '박서준', position: '팀장 (1차승인)', dept: 'IT개발팀', role: 'APPROVER' },
    { step: 3, name: '이현우', position: '재무이사 (2차승인)', dept: '재무기획본부', role: 'APPROVER' },
    { step: 4, name: '정하은', position: '대표이사 (최종승인)', dept: '임원실', role: 'FINAL' }
  ]);

  // Calculated totals
  const totalSupplyAmount = lineItems.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
  const totalTaxAmount = lineItems.reduce((sum, item) => sum + (Number(item.taxAmount) || 0), 0);
  const totalGrandAmount = totalSupplyAmount + totalTaxAmount;

  const handleAddLine = () => {
    const newId = `line-${Date.now()}`;
    setLineItems([
      ...lineItems,
      {
        id: newId,
        accountCode: '51600',
        accountName: '지급수수료',
        description: '',
        amount: 0,
        taxAmount: 0,
        costCenter: 'CC-101 (IT실)',
        remark: ''
      }
    ]);
  };

  const handleRemoveLine = (id: string) => {
    if (lineItems.length === 1) {
      alert('최소 1개 이상의 결의 항목이 필요합니다.');
      return;
    }
    setLineItems(lineItems.filter(item => item.id !== id));
  };

  const handleLineChange = (id: string, field: keyof ResolutionLineItem, value: ResolutionLineItem[keyof ResolutionLineItem]) => {
    setLineItems(lineItems.map(item => {
      if (item.id !== id) return item;
      const updated = { ...item, [field]: value };
      // auto calculate 10% tax if amount is updated
      if (field === 'amount') {
        const amt = Number(value) || 0;
        updated.taxAmount = Math.round(amt * 0.1);
      }
      return updated;
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      alert('지출결의 제목을 입력해 주세요.');
      return;
    }
    alert(`[지출결의서 제출 완료]\n제목: ${title}\n총 결의금액: ₩${totalGrandAmount.toLocaleString()}\n결의 승인 요청이 상신되었습니다.`);
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="지출결의서 작성"
        description="법인 경비 및 거래처 지급 건에 대한 지출결의서를 작성하고 승인 라인을 지정하여 상신합니다."
        breadcrumbs={[
          { label: '지출관리' },
          { label: '지출결의서 작성' }
        ]}
        icon={Receipt}
        actions={
          <div className="flex items-center gap-3">
            <button 
              type="button"
              onClick={() => {
                setTitle('');
                setVendorName('');
                setRemarks('');
              }}
              className="px-4 py-2.5 bg-slate-900/50 hover:bg-slate-800 border border-white/5 rounded-xl text-slate-400 hover:text-white text-xs font-black transition-all flex items-center gap-2"
            >
              <RotateCcw size={14} /> 초기화
            </button>
            <button 
              type="button"
              onClick={() => alert('작성 중인 내용이 임시저장 되었습니다.')}
              className="px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-slate-300 text-xs font-black transition-all flex items-center gap-2"
            >
              <Save size={14} /> 임시저장
            </button>
            <button 
              type="button"
              onClick={handleSubmit}
              className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2"
            >
              <Send size={14} /> 결의 상신하기
            </button>
          </div>
        }
      />

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Main Header Form Card */}
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          <div className="flex items-center justify-between border-b border-white/5 pb-4">
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <FileText className="text-blue-400" size={18} />
              지출결의 기본 정보
            </h3>
            <span className="text-xs font-mono text-slate-500 bg-white/5 px-3 py-1 rounded-full border border-white/5">
              임시문서번호: EXP-2026-DRAFT
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="md:col-span-2 space-y-2">
              <label className="text-xs font-black uppercase tracking-wider text-slate-400">결의 제목 <span className="text-rose-500">*</span></label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: 8월 클라우드 서버 사용료 및 소프트웨어 라이선스 구매 결의"
                className="w-full bg-slate-950/80 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500 transition-all font-medium"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-wider text-slate-400">기안 부서</label>
              <select
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                className="w-full bg-slate-950/80 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500 transition-all font-medium"
              >
                <option value="IT개발팀">IT개발팀</option>
                <option value="마케팅팀">마케팅팀</option>
                <option value="경영지원실">경영지원실</option>
                <option value="영업본부">영업본부</option>
                <option value="재무팀">재무팀</option>
              </select>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-wider text-slate-400">지급 방식</label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'BANK_TRANSFER', label: '계좌이체', icon: Building2 },
                  { id: 'CORPORATE_CARD', label: '법인카드', icon: CreditCard },
                  { id: 'CASH', label: '현금', icon: Receipt },
                ].map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setPaymentMethod(item.id as 'BANK_TRANSFER' | 'CORPORATE_CARD' | 'CASH')}
                    className={`flex items-center justify-center gap-1.5 py-3 rounded-xl text-xs font-black border transition-all ${
                      paymentMethod === item.id 
                        ? 'bg-blue-600/20 text-blue-400 border-blue-500/40' 
                        : 'bg-slate-950/50 text-slate-400 border-white/5 hover:border-white/10'
                    }`}
                  >
                    <item.icon size={14} />
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-wider text-slate-400">지급처 (수령인)</label>
              <input
                type="text"
                value={vendorName}
                onChange={(e) => setVendorName(e.target.value)}
                placeholder="예: (주)메가존클라우드"
                className="w-full bg-slate-950/80 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500 transition-all font-medium"
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-wider text-slate-400">지급 희망일</label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full bg-slate-950/80 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500 transition-all font-medium"
              />
            </div>

            {paymentMethod === 'BANK_TRANSFER' && (
              <>
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-wider text-slate-400">입금 은행</label>
                  <input
                    type="text"
                    value={vendorBank}
                    onChange={(e) => setVendorBank(e.target.value)}
                    placeholder="예: 신한은행"
                    className="w-full bg-slate-950/80 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500 transition-all font-medium"
                  />
                </div>

                <div className="md:col-span-2 space-y-2">
                  <label className="text-xs font-black uppercase tracking-wider text-slate-400">입금 계좌번호</label>
                  <input
                    type="text"
                    value={vendorAccount}
                    onChange={(e) => setVendorAccount(e.target.value)}
                    placeholder="예: 110-384-992019 (예금주명)"
                    className="w-full bg-slate-950/80 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500 transition-all font-medium"
                  />
                </div>
              </>
            )}
          </div>
        </div>

        {/* Line Items Detail Card */}
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
                <Receipt className="text-emerald-400" size={18} />
                지출 명세 내역 (Line Items)
              </h3>
              <p className="text-xs text-slate-400 font-medium mt-1">
                계정과목 및 코스트센터별 공급가액과 부가세를 명시합니다.
              </p>
            </div>

            <button
              type="button"
              onClick={handleAddLine}
              className="px-4 py-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 rounded-xl text-xs font-black transition-all flex items-center gap-1.5"
            >
              <Plus size={14} /> 항목 추가
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3 px-3 w-32">계정과목</th>
                  <th className="py-3 px-3">적요 및 상세내용</th>
                  <th className="py-3 px-3 w-40">코스트센터</th>
                  <th className="py-3 px-3 w-36 text-right">공급가액(원)</th>
                  <th className="py-3 px-3 w-32 text-right">부가세(원)</th>
                  <th className="py-3 px-3 w-36 text-right">합계금액(원)</th>
                  <th className="py-3 px-3 w-12 text-center">삭제</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {lineItems.map((item) => {
                  const itemTotal = (Number(item.amount) || 0) + (Number(item.taxAmount) || 0);
                  return (
                    <tr key={item.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3 px-2">
                        <select
                          value={item.accountCode}
                          onChange={(e) => {
                            const code = e.target.value;
                            const nameMap: Record<string, string> = {
                              '51400': '지급임차료/서버',
                              '51500': '소프트웨어구독료',
                              '52000': '광고선전비',
                              '51300': '접대비',
                              '51600': '지급수수료',
                              '21100': '비품(자산)'
                            };
                            handleLineChange(item.id, 'accountCode', code);
                            handleLineChange(item.id, 'accountName', nameMap[code] || '소모품비');
                          }}
                          className="w-full bg-slate-950/80 border border-white/10 rounded-xl px-2 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                        >
                          <option value="51400">51400 (서버임차)</option>
                          <option value="51500">51500 (SW구독료)</option>
                          <option value="52000">52000 (광고선전비)</option>
                          <option value="51300">51300 (접대비)</option>
                          <option value="51600">51600 (지급수수료)</option>
                          <option value="21100">21100 (비품)</option>
                        </select>
                      </td>
                      <td className="py-3 px-2">
                        <input
                          type="text"
                          value={item.description}
                          onChange={(e) => handleLineChange(item.id, 'description', e.target.value)}
                          placeholder="상세 품목/서비스 내용"
                          className="w-full bg-slate-950/80 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                        />
                      </td>
                      <td className="py-3 px-2">
                        <select
                          value={item.costCenter}
                          onChange={(e) => handleLineChange(item.id, 'costCenter', e.target.value)}
                          className="w-full bg-slate-950/80 border border-white/10 rounded-xl px-2 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                        >
                          <option value="CC-101 (IT실)">CC-101 (IT실)</option>
                          <option value="CC-202 (마케팅본부)">CC-202 (마케팅)</option>
                          <option value="CC-301 (경영지원실)">CC-301 (경영지원)</option>
                          <option value="CC-401 (영업본부)">CC-401 (영업본부)</option>
                        </select>
                      </td>
                      <td className="py-3 px-2">
                        <input
                          type="number"
                          value={item.amount || ''}
                          onChange={(e) => handleLineChange(item.id, 'amount', e.target.value)}
                          className="w-full bg-slate-950/80 border border-white/10 rounded-xl px-3 py-2 text-xs text-right text-white font-mono focus:outline-none focus:border-blue-500"
                          placeholder="0"
                        />
                      </td>
                      <td className="py-3 px-2">
                        <input
                          type="number"
                          value={item.taxAmount || ''}
                          onChange={(e) => handleLineChange(item.id, 'taxAmount', e.target.value)}
                          className="w-full bg-slate-950/80 border border-white/10 rounded-xl px-3 py-2 text-xs text-right text-slate-300 font-mono focus:outline-none focus:border-blue-500"
                          placeholder="0"
                        />
                      </td>
                      <td className="py-3 px-3 text-right font-mono font-bold text-white italic">
                        <AmountDisplay amount={itemTotal} />
                      </td>
                      <td className="py-3 px-2 text-center">
                        <button
                          type="button"
                          onClick={() => handleRemoveLine(item.id)}
                          className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-all"
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Line Summary */}
          <div className="flex flex-col md:flex-row items-center justify-between p-4 rounded-2xl bg-white/[0.02] border border-white/5 gap-4">
            <div className="text-xs text-slate-400 font-medium flex items-center gap-2">
              <HelpCircle size={14} className="text-blue-400" />
              <span>공급가액 입력 시 부가세 10%가 자동 산출됩니다.</span>
            </div>
            <div className="flex items-center gap-8 font-mono">
              <div className="text-right">
                <span className="text-[10px] text-slate-500 uppercase tracking-widest block">공급가액 총계</span>
                <span className="text-sm font-bold text-slate-300"><AmountDisplay amount={totalSupplyAmount} /></span>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-slate-500 uppercase tracking-widest block">부가세 총계</span>
                <span className="text-sm font-bold text-slate-400"><AmountDisplay amount={totalTaxAmount} /></span>
              </div>
              <div className="text-right pl-6 border-l border-white/10">
                <span className="text-[10px] text-blue-400 uppercase tracking-widest font-black block">최종 결의 합계</span>
                <span className="text-2xl font-black text-emerald-400 italic tracking-tight"><AmountDisplay amount={totalGrandAmount} /></span>
              </div>
            </div>
          </div>
        </div>

        {/* Approval Line Section */}
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          <div className="flex items-center justify-between border-b border-white/5 pb-4">
            <div>
              <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
                <UserCheck className="text-indigo-400" size={18} />
                결의 승인 라인 설정 (Approval Workflow)
              </h3>
              <p className="text-xs text-slate-400 font-medium mt-1">
                결의 금액 기준에 맞춰 적정 결권자의 승인을 거칩니다. (1,000만원 이상 대표이사 결재 필수)
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            {approvalLine.map((step) => (
              <div 
                key={step.step}
                className="relative rounded-2xl bg-white/[0.02] border border-white/5 p-4 flex flex-col justify-between group hover:border-indigo-500/30 transition-all"
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="w-6 h-6 rounded-lg bg-indigo-500/10 text-indigo-400 text-xs font-mono font-black flex items-center justify-center border border-indigo-500/20">
                    {step.step}
                  </span>
                  <span className="text-[10px] font-black uppercase text-slate-500 tracking-wider">
                    {step.role === 'DRAFTER' ? '기안자' : step.role === 'FINAL' ? '최종결재' : '중간결재'}
                  </span>
                </div>

                <div className="space-y-1">
                  <div className="text-base font-black text-white">{step.name}</div>
                  <div className="text-xs font-bold text-slate-400">{step.position}</div>
                  <div className="text-[11px] text-slate-500">{step.dept}</div>
                </div>

                <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-[11px]">
                  <span className="text-slate-500 font-medium">상태:</span>
                  <span className={`font-black ${step.role === 'DRAFTER' ? 'text-emerald-400' : 'text-slate-400'}`}>
                    {step.role === 'DRAFTER' ? '작성중' : '대기'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </form>
    </div>
  );
}
