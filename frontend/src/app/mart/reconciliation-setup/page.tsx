'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { 
  Settings, 
  Plus, 
  Search, 
  Sliders, 
  Play, 
  CheckCircle2, 
  Database, 
  Clock, 
  Edit3, 
  Trash2,
  X
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockReconciliationRules, ReconciliationRuleDto } from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function ReconciliationSetupPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [rules, setRules] = useState<ReconciliationRuleDto[]>(mockReconciliationRules);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);

  // New Rule Form State
  const [newRuleName, setNewRuleName] = useState('');
  const [newCategory, setNewCategory] = useState<'원장vs마트' | '계정대계정' | '파생/외화' | '시스템간'>('원장vs마트');
  const [newSourceDataset, setNewSourceDataset] = useState('GL Core (TB_GL_BALANCE)');
  const [newTargetDataset, setNewTargetDataset] = useState('Accounting Mart (DM_ACCOUNT_BALANCE_M)');
  const [newToleranceAmount, setNewToleranceAmount] = useState<number>(10000);
  const [newSchedule, setNewSchedule] = useState<'REALTIME' | 'DAILY' | 'MONTHLY'>('DAILY');

  const categoryTabs: TabItem[] = [
    { id: 'ALL', label: '전체 대사 규칙' },
    { id: '원장vs마트', label: '원장 vs Mart' },
    { id: '파생/외화', label: '파생 / 외화' },
    { id: '계정대계정', label: '계정 대 계정' },
    { id: '시스템간', label: '시스템 간 대사' },
  ];

  const filteredRules = rules.filter(r => {
    const matchesCat = selectedCategory === 'ALL' || r.category === selectedCategory;
    const matchesSearch = 
      r.ruleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.sourceDataset.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.targetDataset.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  const handleAddRule = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRuleName.trim()) return;

    const created: ReconciliationRuleDto = {
      id: `RULE-0${rules.length + 1}`,
      ruleName: newRuleName,
      category: newCategory,
      sourceDataset: newSourceDataset,
      targetDataset: newTargetDataset,
      toleranceAmount: newToleranceAmount,
      tolerancePercent: 0.01,
      matchingKeys: ['POSTING_DATE', 'ACCT_CODE'],
      schedule: newSchedule,
      isActive: true,
      lastRun: '미실행',
      description: '사용자 정의 신규 대사 규칙',
    };

    setRules([created, ...rules]);
    setIsModalOpen(false);
    setNewRuleName('');
    alert(`신규 대사 규칙 [${newRuleName}]이 성공적으로 등록되었습니다.`);
  };

  const toggleRuleActive = (id: string) => {
    setRules(rules.map(r => r.id === id ? { ...r, isActive: !r.isActive } : r));
  };

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="대사 규칙 설정"
        description="원장 vs 마트 간 대사 규칙 (Reconciliation Rules), 허용 오차 (Tolerance), 매칭 키 설정"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '대사 관리' },
          { label: '대사 규칙 설정' },
        ]}
        icon={Settings}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-5 py-2.5 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
          >
            <Plus size={14} />
            신규 대사 규칙 등록
          </button>
        }
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">등록된 규칙 수</div>
          <div className="text-3xl font-black text-white">{rules.length} <span className="text-sm font-normal text-slate-400">개</span></div>
          <div className="text-xs text-emerald-400 flex items-center gap-1">
            <CheckCircle2 size={12} /> 활성 상태 {rules.filter(r => r.isActive).length}개
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">실시간 (Real-time) 규칙</div>
          <div className="text-2xl font-black text-white">
            {rules.filter(r => r.schedule === 'REALTIME').length} <span className="text-sm font-normal text-slate-400">개</span>
          </div>
          <div className="text-xs text-slate-500">전표 입력 시 자동 실시간 검수</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">일별 배치 (Daily Batch)</div>
          <div className="text-2xl font-black text-white">
            {rules.filter(r => r.schedule === 'DAILY').length} <span className="text-sm font-normal text-slate-400">개</span>
          </div>
          <div className="text-xs text-slate-500">매일 장후 05:00 자동 수행</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">평균 허용 오차 (Tolerance)</div>
          <div className="text-2xl font-black text-white">
            ₩10,000 <span className="text-sm font-normal text-slate-400">이내</span>
          </div>
          <div className="text-xs text-slate-500">초과 시 자동 차액 (Diff) 생성</div>
        </div>
      </div>

      {/* Rules Table Section */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <Tabs tabs={categoryTabs} activeTab={selectedCategory} onChange={setSelectedCategory} />

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="규칙명 / 소스 / 타겟 검색..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        {filteredRules.length === 0 ? (
          <EmptyState
            icon={Sliders}
            title="조건에 부합하는 대사 규칙이 없습니다"
            description="새로운 대사 규칙을 생성하거나 필터 검색어를 수정하세요."
          />
        ) : (
          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">규칙 ID</th>
                  <th className="py-3.5 px-4">대사 규칙명</th>
                  <th className="py-3.5 px-4">카테고리</th>
                  <th className="py-3.5 px-4">소스 데이터셋</th>
                  <th className="py-3.5 px-4">타겟 데이터셋</th>
                  <th className="py-3.5 px-4">매칭 키 (Matching Keys)</th>
                  <th className="py-3.5 px-4 text-right">허용 오차</th>
                  <th className="py-3.5 px-4 text-center">스케줄</th>
                  <th className="py-3.5 px-4 text-center">상태</th>
                  <th className="py-3.5 px-4 text-center">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {filteredRules.map((rule) => (
                  <tr key={rule.id} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3.5 px-4 font-mono font-bold text-blue-400">{rule.id}</td>
                    <td className="py-3.5 px-4 font-bold text-white max-w-xs">{rule.ruleName}</td>
                    <td className="py-3.5 px-4">
                      <span className="px-2 py-0.5 rounded bg-white/5 text-slate-300 font-medium">
                        {rule.category}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{rule.sourceDataset}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{rule.targetDataset}</td>
                    <td className="py-3.5 px-4">
                      <div className="flex flex-wrap gap-1">
                        {rule.matchingKeys.map((key) => (
                          <span key={key} className="px-1.5 py-0.5 rounded bg-slate-800 text-[10px] text-slate-400 font-mono">
                            {key}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-white">
                      <AmountDisplay amount={rule.toleranceAmount} />
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-purple-500/10 text-purple-400 border border-purple-500/20">
                        {rule.schedule}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <button 
                        onClick={() => toggleRuleActive(rule.id)} 
                        className="cursor-pointer"
                      >
                        <StatusBadge 
                          status={rule.isActive ? '사용 중' : '비활성'} 
                          variant={rule.isActive ? 'success' : 'neutral'} 
                        />
                      </button>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button 
                          onClick={() => alert(`[${rule.ruleName}] 대사를 즉시 실행합니다.`)}
                          className="p-1.5 rounded-lg bg-blue-600/10 text-blue-400 hover:bg-blue-600/20"
                          title="즉시 실행"
                        >
                          <Play size={13} />
                        </button>
                        <button 
                          onClick={() => alert(`[${rule.ruleName}] 규칙 수정 창`)}
                          className="p-1.5 rounded-lg bg-white/5 text-slate-400 hover:text-white"
                          title="수정"
                        >
                          <Edit3 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Registration Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl bg-slate-900 border border-white/10 p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/5 pb-4">
              <h3 className="text-lg font-black text-white flex items-center gap-2">
                <Settings className="text-blue-400" size={20} />
                신규 대사 규칙 등록
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleAddRule} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-400 font-bold mb-1">대사 규칙명 *</label>
                <input
                  type="text"
                  required
                  placeholder="예: 원장 vs Mart 외화보유액 일치 검증"
                  value={newRuleName}
                  onChange={e => setNewRuleName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white placeholder:text-slate-600 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-slate-400 font-bold mb-1">카테고리</label>
                  <select
                    value={newCategory}
                    onChange={e => setNewCategory(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-blue-500"
                  >
                    <option value="원장vs마트" className="bg-slate-900">원장 vs 마트</option>
                    <option value="계정대계정" className="bg-slate-900">계정 대 계정</option>
                    <option value="파생/외화" className="bg-slate-900">파생 / 외화</option>
                    <option value="시스템간" className="bg-slate-900">시스템 간</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-400 font-bold mb-1">스케줄 방식</label>
                  <select
                    value={newSchedule}
                    onChange={e => setNewSchedule(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-blue-500"
                  >
                    <option value="DAILY" className="bg-slate-900">일별 배치 (DAILY)</option>
                    <option value="REALTIME" className="bg-slate-900">실시간 (REALTIME)</option>
                    <option value="MONTHLY" className="bg-slate-900">월별 (MONTHLY)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-slate-400 font-bold mb-1">소스 데이터셋 (Source)</label>
                <input
                  type="text"
                  value={newSourceDataset}
                  onChange={e => setNewSourceDataset(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-400 font-bold mb-1">타겟 데이터셋 (Target)</label>
                <input
                  type="text"
                  value={newTargetDataset}
                  onChange={e => setNewTargetDataset(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-400 font-bold mb-1">허용 오차 금액 (원)</label>
                <input
                  type="number"
                  value={newToleranceAmount}
                  onChange={e => setNewToleranceAmount(Number(e.target.value))}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white font-mono"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/5">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-white/5 text-slate-400 hover:text-white font-bold"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold shadow-lg shadow-blue-600/20"
                >
                  규칙 등록 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
