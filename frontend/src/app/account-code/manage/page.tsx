"use client";

import React, { useState } from 'react';
import { 
  FileEdit, 
  PlusCircle, 
  Save, 
  RotateCcw, 
  CheckCircle, 
  Scale, 
  Hash, 
  Upload, 
  FileSpreadsheet
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockAccounts } from '@/mocks/master';

export default function AccountManagePage() {
  const [activeTab, setActiveTab] = useState('single');
  const [submitted, setSubmitted] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    englishName: '',
    category: 'SUBJECT',
    parentCode: '1110000',
    balanceType: 'DEBIT',
    level: 4,
    status: 'ACTIVE',
    usePartner: true,
    useDept: true,
    useBudget: false,
    carryForward: true,
    description: '',
  });

  const tabItems: TabItem[] = [
    { id: 'single', label: '단건 등록 및 속성 편집', icon: PlusCircle },
    { id: 'batch', label: '일괄 엑셀 업로드', icon: FileSpreadsheet },
  ];

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setFormData((prev) => ({ ...prev, [name]: checked }));
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setTimeout(() => {
      setSubmitted(false);
    }, 4000);
  };

  const handleReset = () => {
    setFormData({
      code: '',
      name: '',
      englishName: '',
      category: 'SUBJECT',
      parentCode: '1110000',
      balanceType: 'DEBIT',
      level: 4,
      status: 'ACTIVE',
      usePartner: true,
      useDept: true,
      useBudget: false,
      carryForward: true,
      description: '',
    });
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="계정과목 등록/수정"
        description="신규 기준정보 생성 및 속성 편집"
        breadcrumbs={[
          { label: '기준정보 마스터' },
          { label: '계정과목 관리' },
          { label: '계정과목 등록/수정' },
        ]}
        icon={FileEdit}
        actions={
          <Tabs
            tabs={tabItems}
            activeTab={activeTab}
            onChange={setActiveTab}
          />
        }
      />

      {submitted && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 font-bold flex items-center gap-3 animate-fade-in">
          <CheckCircle size={20} className="text-emerald-400 shrink-0" />
          <span>계정과목 기준정보가 성공적으로 등록/수정 요청되었습니다 (승인대기 번호: REQ-2026-0511).</span>
        </div>
      )}

      {activeTab === 'single' ? (
        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Card 1: Basic Information */}
          <div className="glass-panel p-8 rounded-[2.5rem] border border-white/10 relative overflow-hidden">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
                <Hash size={20} />
              </div>
              <div>
                <h3 className="text-xl font-black text-white italic tracking-tight">
                  기본 식별 정보
                </h3>
                <p className="text-xs text-slate-500 font-medium">
                  계정 코드, 명칭 및 식별용 범주 정보를 입력합니다.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {/* 계정 코드 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  계정 코드 <span className="text-rose-400">*</span>
                </label>
                <input
                  type="text"
                  name="code"
                  required
                  placeholder="예: 1110300"
                  value={formData.code}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-mono font-bold text-white outline-none transition-all placeholder:text-slate-700"
                />
              </div>

              {/* 계정명 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  계정명 (한글) <span className="text-rose-400">*</span>
                </label>
                <input
                  type="text"
                  name="name"
                  required
                  placeholder="예: 정기예금"
                  value={formData.name}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all placeholder:text-slate-700"
                />
              </div>

              {/* 계정 영문명 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  계정명 (영문)
                </label>
                <input
                  type="text"
                  name="englishName"
                  placeholder="예: Time Deposits"
                  value={formData.englishName}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all placeholder:text-slate-700"
                />
              </div>

              {/* 계정 구분 Dropdown */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  계정 구분 <span className="text-rose-400">*</span>
                </label>
                <select
                  name="category"
                  value={formData.category}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all cursor-pointer"
                >
                  <option value="GROUP">GROUP (분류/그룹계정)</option>
                  <option value="SUBJECT">SUBJECT (세목/실계정)</option>
                </select>
              </div>

              {/* 상위 계정 선택 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  상위 계정 선택
                </label>
                <select
                  name="parentCode"
                  value={formData.parentCode}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all cursor-pointer"
                >
                  <option value="">(최상위 계정)</option>
                  {mockAccounts
                    .filter((a) => a.category === 'GROUP')
                    .map((acc) => (
                      <option key={acc.code} value={acc.code}>
                        [{acc.code}] {acc.name} (L{acc.level})
                      </option>
                    ))}
                </select>
              </div>

              {/* 계층 레벨 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  계층 레벨 (Level)
                </label>
                <select
                  name="level"
                  value={formData.level}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all cursor-pointer"
                >
                  <option value={1}>Level 1 (대분류)</option>
                  <option value={2}>Level 2 (중분류)</option>
                  <option value={3}>Level 3 (소분류)</option>
                  <option value={4}>Level 4 (세분류)</option>
                  <option value={5}>Level 5 (세목)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Card 2: Accounting Attributes */}
          <div className="glass-panel p-8 rounded-[2.5rem] border border-white/10 relative overflow-hidden">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-2xl bg-emerald-600/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                <Scale size={20} />
              </div>
              <div>
                <h3 className="text-xl font-black text-white italic tracking-tight">
                  회계 성격 및 통제 속성
                </h3>
                <p className="text-xs text-slate-500 font-medium">
                  본차변 구분, 사용 상태 및 전표 통제 정책을 지정합니다.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {/* 차대 구분 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  잔액 본차변 구분
                </label>
                <select
                  name="balanceType"
                  value={formData.balanceType}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all cursor-pointer"
                >
                  <option value="DEBIT">DEBIT (차변 잔액)</option>
                  <option value="CREDIT">CREDIT (대변 잔액)</option>
                </select>
              </div>

              {/* 사용 상태 */}
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  사용 상태
                </label>
                <select
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all cursor-pointer"
                >
                  <option value="ACTIVE">ACTIVE (사용 중)</option>
                  <option value="INACTIVE">INACTIVE (사용 중지)</option>
                </select>
              </div>

              {/* 설명 / 비고 */}
              <div className="md:col-span-2 lg:col-span-1 space-y-2">
                <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
                  계정 설명 / 관리 목적
                </label>
                <input
                  type="text"
                  name="description"
                  placeholder="계정과목 관리 목적 및 사용 기준"
                  value={formData.description}
                  onChange={handleChange}
                  className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-xl py-3 px-4 text-sm font-bold text-white outline-none transition-all placeholder:text-slate-700"
                />
              </div>
            </div>

            {/* Checkbox Attributes */}
            <div className="mt-8 pt-6 border-t border-white/5">
              <span className="text-xs font-black text-slate-400 uppercase tracking-widest block mb-4">
                세부 입력 및 이월 통제 옵션
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                <label className="flex items-center gap-3 p-4 rounded-xl bg-slate-950/60 border border-white/5 cursor-pointer hover:border-white/20 transition-all">
                  <input
                    type="checkbox"
                    name="usePartner"
                    checked={formData.usePartner}
                    onChange={handleChange}
                    className="w-4 h-4 rounded bg-slate-900 border-white/20 text-blue-600 focus:ring-0"
                  />
                  <span className="text-xs font-bold text-slate-200">거래처 필수 입력</span>
                </label>

                <label className="flex items-center gap-3 p-4 rounded-xl bg-slate-950/60 border border-white/5 cursor-pointer hover:border-white/20 transition-all">
                  <input
                    type="checkbox"
                    name="useDept"
                    checked={formData.useDept}
                    onChange={handleChange}
                    className="w-4 h-4 rounded bg-slate-900 border-white/20 text-blue-600 focus:ring-0"
                  />
                  <span className="text-xs font-bold text-slate-200">귀속부서 필수 입력</span>
                </label>

                <label className="flex items-center gap-3 p-4 rounded-xl bg-slate-950/60 border border-white/5 cursor-pointer hover:border-white/20 transition-all">
                  <input
                    type="checkbox"
                    name="useBudget"
                    checked={formData.useBudget}
                    onChange={handleChange}
                    className="w-4 h-4 rounded bg-slate-900 border-white/20 text-blue-600 focus:ring-0"
                  />
                  <span className="text-xs font-bold text-slate-200">예산 통제 적용</span>
                </label>

                <label className="flex items-center gap-3 p-4 rounded-xl bg-slate-950/60 border border-white/5 cursor-pointer hover:border-white/20 transition-all">
                  <input
                    type="checkbox"
                    name="carryForward"
                    checked={formData.carryForward}
                    onChange={handleChange}
                    className="w-4 h-4 rounded bg-slate-900 border-white/20 text-blue-600 focus:ring-0"
                  />
                  <span className="text-xs font-bold text-slate-200">차기 이월 적용</span>
                </label>
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-4">
            <button
              type="button"
              onClick={handleReset}
              className="px-6 py-3.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 font-bold text-sm transition-all flex items-center gap-2"
            >
              <RotateCcw size={16} /> 초기화
            </button>
            <button
              type="submit"
              className="px-8 py-3.5 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl font-black text-sm transition-all shadow-lg shadow-blue-600/25 flex items-center gap-2"
            >
              <Save size={16} /> 계정과목 저장 요청
            </button>
          </div>
        </form>
      ) : (
        /* Batch Upload View */
        <div className="glass-panel p-12 rounded-[2.5rem] border border-white/10 text-center space-y-6">
          <div className="w-20 h-20 rounded-3xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400 mx-auto">
            <Upload size={36} />
          </div>
          <div className="max-w-md mx-auto space-y-2">
            <h3 className="text-xl font-black text-white italic tracking-tight">
              대량 계정과목 엑셀 파일 일괄 업로드
            </h3>
            <p className="text-xs text-slate-400 font-medium">
              표준 양식 엑셀 파일(.xlsx)을 드래그하거나 선택하여 여러 계정과목을 한 번에 생성합니다.
            </p>
          </div>

          <div className="max-w-xl mx-auto p-10 border-2 border-dashed border-white/10 hover:border-blue-500/50 rounded-3xl bg-slate-950/40 cursor-pointer transition-all space-y-4">
            <FileSpreadsheet size={48} className="text-slate-600 mx-auto" />
            <div>
              <p className="text-sm font-bold text-slate-200">
                파일을 이곳에 드롭하거나 <span className="text-blue-400">클릭하여 선택</span>하세요.
              </p>
              <p className="text-xs text-slate-500 mt-1">최대 10MB, XLSX / CSV 지원</p>
            </div>
          </div>

          <div className="flex justify-center gap-4">
            <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-xs font-bold text-slate-300">
              엑셀 표준 양식 다운로드
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
