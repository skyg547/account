'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  Sliders,
  Plus,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  Clock,
  TrendingUp,
  Percent,
  Search,
  Edit,
  History,
  FileSpreadsheet,
  Zap,
  X
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockParameters, EclParameterDto } from '@/mocks/ecl';

const emptySubscribe = () => () => {};

export default function EclParametersPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [parameters, setParameters] = useState<EclParameterDto[]>(mockParameters);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [editingParam, setEditingParam] = useState<EclParameterDto | null>(null);
  const [editValue, setEditValue] = useState<number>(0);
  const [notification, setNotification] = useState<string | null>(null);

  const activeParams = parameters.filter(p => p.status === 'ACTIVE');
  const pdParams = activeParams.filter(p => p.type === 'PD');
  const lgdParams = activeParams.filter(p => p.type === 'LGD');
  const ccfParams = activeParams.filter(p => p.type === 'CCF');

  const avgPd = pdParams.length > 0 ? (pdParams.reduce((sum, p) => sum + p.value, 0) / pdParams.length).toFixed(2) : '0';
  const avgLgd = lgdParams.length > 0 ? (lgdParams.reduce((sum, p) => sum + p.value, 0) / lgdParams.length).toFixed(2) : '0';
  const avgCcf = ccfParams.length > 0 ? (ccfParams.reduce((sum, p) => sum + p.value, 0) / ccfParams.length).toFixed(2) : '0';

  const categoryTabs: TabItem[] = [
    { id: 'ALL', label: '전체 파라미터' },
    { id: 'PD', label: 'PD (부도확률)' },
    { id: 'LGD', label: 'LGD (부도시손실률)' },
    { id: 'CCF', label: 'CCF (신용전환율)' },
    { id: 'FLI', label: 'FLI (거시경제전망)' },
  ];

  const filteredParameters = parameters.filter(p => {
    const matchesTab = activeTab === 'ALL' || p.type === activeTab;
    const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
    const matchesSearch =
      p.parameterCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.parameterName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.portfolio.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesStatus && matchesSearch;
  });

  const handleEditClick = (param: EclParameterDto) => {
    setEditingParam(param);
    setEditValue(param.value);
  };

  const handleSaveEdit = () => {
    if (!editingParam) return;
    setParameters(prev => prev.map(p => p.id === editingParam.id ? { ...p, value: editValue, updatedAt: '2026-07-28' } : p));
    setNotification(`[${editingParam.parameterCode}] 파라미터 수치가 ${editValue}${editingParam.unit}(으)로 업데이트되었습니다.`);
    setEditingParam(null);
    setTimeout(() => setNotification(null), 4000);
  };

  const handleCalibrationRun = () => {
    setNotification('모형 보정(Calibration) 배치 프로세스가 트리거되었습니다.');
    setTimeout(() => {
      setNotification('파라미터 보정 완료: 2026 상반기 리스크 추정치가 재적용 되었습니다.');
      setTimeout(() => setNotification(null), 4000);
    }, 1500);
  };

  const getTypeTagColor = (type: EclParameterDto['type']) => {
    switch (type) {
      case 'PD': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      case 'LGD': return 'bg-purple-500/20 text-purple-400 border-purple-500/30';
      case 'CCF': return 'bg-amber-500/20 text-amber-400 border-amber-500/30';
      case 'FLI': return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
    }
  };

  const getStatusVariant = (status: EclParameterDto['status']) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'PENDING': return 'warning';
      case 'EXPIRED': return 'neutral';
      default: return 'neutral';
    }
  };

  if (!mounted) {
    return <div className="p-8 text-slate-400">Loading 모델 파라미터 Page...</div>;
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Page Header */}
      <PageHeader
        title="모델 파라미터"
        description="부도확률(PD), 부도시손실률(LGD), 신용부합율(CCF) 및 거시경제 전망(FLI) 파라미터 관리"
        breadcrumbs={[
          { label: 'ECL' },
          { label: '모델 파라미터' }
        ]}
        icon={Sliders}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleCalibrationRun}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-white/10 font-bold text-sm transition-all"
            >
              <RotateCcw size={16} />
              보정(Calibration) 실행
            </button>
            <button
              onClick={() => {
                setNotification('신규 파라미터 등록 폼 모달 준비 중입니다.');
                setTimeout(() => setNotification(null), 3000);
              }}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/30 transition-all"
            >
              <Plus size={16} />
              파라미터 신규 등록
            </button>
          </div>
        }
      />

      {/* Notification Banner */}
      {notification && (
        <div className="p-4 rounded-2xl bg-blue-500/10 border border-blue-500/30 text-blue-400 font-medium flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Zap size={18} />
            <span>{notification}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-slate-400 hover:text-white text-xs">닫기</button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>활성 파라미터</span>
            <Sliders size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {activeParams.length} <span className="text-sm font-normal text-slate-400">/ {parameters.length} 개</span>
          </div>
          <p className="text-xs text-emerald-400 pt-1">최근 보정: 2026-06-01</p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 PD 추정치</span>
            <Percent size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {avgPd} <span className="text-sm font-normal text-slate-400">%</span>
          </div>
          <p className="text-xs text-slate-400 pt-1">Stage 1/2 우량~중위 평준치</p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 LGD 추정치</span>
            <TrendingUp size={18} className="text-purple-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {avgLgd} <span className="text-sm font-normal text-slate-400">%</span>
          </div>
          <p className="text-xs text-slate-400 pt-1">담보유형 및 회수율 반영</p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 CCF (신용전환율)</span>
            <CheckCircle2 size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {avgCcf} <span className="text-sm font-normal text-slate-400">%</span>
          </div>
          <p className="text-xs text-slate-400 pt-1">한도대출 및 카드 미사용 잔액</p>
        </div>
      </div>

      {/* Tabs & Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <Tabs tabs={categoryTabs} activeTab={activeTab} onChange={setActiveTab} />

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2.5 bg-slate-900/50 backdrop-blur-md border border-white/5 rounded-xl text-sm font-bold text-slate-300 focus:outline-none focus:border-blue-500 w-full sm:w-auto"
          >
            <option value="ALL">전체 상태</option>
            <option value="ACTIVE">사용 중 (ACTIVE)</option>
            <option value="PENDING">검증 대기 (PENDING)</option>
            <option value="EXPIRED">만료됨 (EXPIRED)</option>
          </select>

          <div className="relative w-full sm:w-64">
            <Search size={16} className="absolute left-3.5 top-3 text-slate-500" />
            <input
              type="text"
              placeholder="파라미터 코드/명칭..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-900/50 border border-white/5 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Parameters Table */}
      <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-white/5 bg-slate-900/80 text-slate-400 text-xs font-black uppercase tracking-wider">
                <th className="px-6 py-4">구분 (Type)</th>
                <th className="px-6 py-4">파라미터 코드 / 명칭</th>
                <th className="px-6 py-4">대상 포트폴리오</th>
                <th className="px-6 py-4 text-right">설정 수치 (Value)</th>
                <th className="px-6 py-4 text-center">유효 기간</th>
                <th className="px-6 py-4 text-center">상태</th>
                <th className="px-6 py-4">최종 수정자</th>
                <th className="px-6 py-4 text-center">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-slate-300">
              {filteredParameters.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-slate-500 font-medium">
                    조건에 해당하는 파라미터가 없습니다.
                  </td>
                </tr>
              ) : (
                filteredParameters.map(param => (
                  <tr key={param.id} className="hover:bg-white/5 transition-all">
                    <td className="px-6 py-4">
                      <span className={`inline-block px-2.5 py-1 rounded-lg text-xs font-black border ${getTypeTagColor(param.type)}`}>
                        {param.type}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-white">{param.parameterCode}</div>
                      <div className="text-xs text-slate-400 font-medium">{param.parameterName}</div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-bold text-slate-200">{param.portfolio}</div>
                      {param.stage && <div className="text-xs text-slate-500">{param.stage}</div>}
                    </td>
                    <td className="px-6 py-4 text-right font-mono">
                      <span className="text-lg font-black text-white">{param.value}</span>
                      <span className="text-xs text-slate-400 ml-1">{param.unit}</span>
                    </td>
                    <td className="px-6 py-4 text-center text-xs font-mono text-slate-400">
                      {param.validFrom} ~ {param.validTo}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <StatusBadge status={param.status} variant={getStatusVariant(param.status)} />
                    </td>
                    <td className="px-6 py-4 text-xs">
                      <div className="text-slate-300 font-medium">{param.updatedBy}</div>
                      <div className="text-slate-500 font-mono">{param.updatedAt}</div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <button
                        onClick={() => handleEditClick(param)}
                        className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
                        title="값 수정"
                      >
                        <Edit size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Parameter Edit Modal */}
      {editingParam && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-2xl max-w-md w-full p-6 space-y-6 animate-scale-up">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div>
                <span className="text-xs font-bold text-blue-400 uppercase tracking-widest">EDIT PARAMETER</span>
                <h3 className="text-lg font-bold text-white">{editingParam.parameterCode}</h3>
              </div>
              <button
                onClick={() => setEditingParam(null)}
                className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-400 mb-1">파라미터 명칭</label>
                <input
                  type="text"
                  disabled
                  value={editingParam.parameterName}
                  className="w-full px-4 py-2 bg-slate-950/60 border border-white/5 rounded-xl text-sm text-slate-300 font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 mb-1">설정 값 ({editingParam.unit})</label>
                <input
                  type="number"
                  step="0.01"
                  value={editValue}
                  onChange={(e) => setEditValue(parseFloat(e.target.value) || 0)}
                  className="w-full px-4 py-2.5 bg-slate-950 border border-blue-500/50 rounded-xl text-lg font-black text-white font-mono focus:outline-none focus:border-blue-400"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 mb-1">설명 및 보정 근거</label>
                <p className="text-xs text-slate-400 p-3 bg-slate-950/40 rounded-xl border border-white/5">
                  {editingParam.description}
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setEditingParam(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-sm"
              >
                취소
              </button>
              <button
                onClick={handleSaveEdit}
                className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/30"
              >
                수정 반영
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
