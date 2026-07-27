"use client";

import React, { useState } from 'react';
import { 
  ClipboardCheck, 
  CheckCircle2, 
  Clock, 
  AlertCircle, 
  UserCheck, 
  RefreshCw,
  FileCheck,
  Award
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface ControlItem {
  id: string;
  category: string;
  title: string;
  description: string;
  verified: boolean;
  lastVerifiedAt: string;
  verifier: string;
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
}

const initialControls: ControlItem[] = [
  {
    id: 'CTRL-01',
    category: '회계 결산 통제 (Closing Controls)',
    title: '월말 미승인 전표 잔여 여부 전수 검증',
    description: '결산 마감 전 임시 저장 및 미승인 상태 전표가 존재하는지 전수 대조합니다.',
    verified: true,
    lastVerifiedAt: '2026-07-28 08:30:12',
    verifier: '김회계 (재무기획실)',
    riskLevel: 'HIGH'
  },
  {
    id: 'CTRL-02',
    category: '회계 결산 통제 (Closing Controls)',
    title: '총계정원장(GL)과 보조원장(SL) 잔액 대사 (Reconciliation)',
    description: '매출채권, 매입채무, 유형자산의 GL 총액과 Sub-ledger 명세 일치 여부를 검증합니다.',
    verified: true,
    lastVerifiedAt: '2026-07-28 08:45:00',
    verifier: '김회계 (재무기획실)',
    riskLevel: 'HIGH'
  },
  {
    id: 'CTRL-03',
    category: '회계 결산 통제 (Closing Controls)',
    title: '외화 자산/부채 기말 매매기준율 평가 검증',
    description: '한국은행 매매기준율 동기화 및 외화 평가손익 계상 정확성을 체크합니다.',
    verified: false,
    lastVerifiedAt: '2026-06-30 18:00:00',
    verifier: '이외환 (외환운용팀)',
    riskLevel: 'MEDIUM'
  },
  {
    id: 'CTRL-04',
    category: '자금 및 금융자산 통제 (Treasury Controls)',
    title: '시중은행 예금 잔액증명서 대조',
    description: '주거래 은행 잔액증명서와 사내 예금 장부 잔액 간 차이 내역을 확인합니다.',
    verified: true,
    lastVerifiedAt: '2026-07-27 17:20:40',
    verifier: '박자금 (자금팀)',
    riskLevel: 'HIGH'
  },
  {
    id: 'CTRL-05',
    category: '자금 및 금융자산 통제 (Treasury Controls)',
    title: '미결제 외환 포지션 한도 점검',
    description: '딜러별 FX 오버나잇 한도 및 스왑 포지션 이행 여부를 확인합니다.',
    verified: true,
    lastVerifiedAt: '2026-07-28 09:00:00',
    verifier: '이위험 (리스크관리부)',
    riskLevel: 'HIGH'
  },
  {
    id: 'CTRL-06',
    category: 'IFRS9 대손충당금 통제 (IFRS9 ECL Controls)',
    title: 'PD/LGD 파라미터 변동성 및 부도 시나리오 검토',
    description: '거시경제 예측 변수 반영 비율 및 LGD 파라미터 타당성을 점검합니다.',
    verified: false,
    lastVerifiedAt: '2026-07-15 14:10:00',
    verifier: '이위험 (리스크관리부)',
    riskLevel: 'HIGH'
  },
  {
    id: 'CTRL-07',
    category: 'IFRS9 대손충당금 통제 (IFRS9 ECL Controls)',
    title: 'Stage 3 부실채권 개별 평가 적정성 검토',
    description: '연체 90일 이상 여신 및 개별 손상 평가 대상 금액의 담보 가치를 Re-eval 합니다.',
    verified: true,
    lastVerifiedAt: '2026-07-26 11:30:00',
    verifier: '박감사 (내부감사팀)',
    riskLevel: 'HIGH'
  },
  {
    id: 'CTRL-08',
    category: '시스템 및 보안 통제 (System Security Controls)',
    title: '특권 계정(SYSTEM_ADMIN) 작업 감사 로그 점검',
    description: '시스템 최고 관리자 계정의 DB 직연결 및 권한 변경 이력을 주기적으로 점검합니다.',
    verified: true,
    lastVerifiedAt: '2026-07-28 09:10:00',
    verifier: '박감사 (내부감사팀)',
    riskLevel: 'MEDIUM'
  },
  {
    id: 'CTRL-09',
    category: '시스템 및 보안 통제 (System Security Controls)',
    title: '퇴직/전출 임직원 계정 접근 권한 회수 검증',
    description: '인사 발령 데이터와 시스템 접근 권한 자동 차단 여부를 검증합니다.',
    verified: false,
    lastVerifiedAt: '2026-07-01 09:00:00',
    verifier: '시스템관리자 (IT본부)',
    riskLevel: 'MEDIUM'
  }
];

export default function ControlsPage() {
  const [controls, setControls] = useState<ControlItem[]>(initialControls);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  const toggleVerify = (id: string) => {
    setControls(prev => prev.map(item => {
      if (item.id === id) {
        const nextState = !item.verified;
        const now = new Date();
        const timestamp = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
        return {
          ...item,
          verified: nextState,
          lastVerifiedAt: nextState ? timestamp : item.lastVerifiedAt,
          verifier: nextState ? '김회계 (현재 사용자)' : item.verifier
        };
      }
      return item;
    }));
  };

  const categories = Array.from(new Set(controls.map(c => c.category)));

  const filteredControls = controls.filter(c => 
    selectedCategory === 'ALL' || c.category === selectedCategory
  );

  const verifiedCount = controls.filter(c => c.verified).length;
  const totalCount = controls.length;
  const completionPercent = Math.round((verifiedCount / totalCount) * 100);

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="내부통제 체크리스트"
        description="회계 결산 통제 절차 및 점검 이력"
        breadcrumbs={[
          { label: 'Governance', href: '/governance/controls' },
          { label: '내부통제 체크리스트' }
        ]}
        icon={ClipboardCheck}
        actions={
          <button
            onClick={() => setControls(initialControls)}
            className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 text-sm font-bold transition-all flex items-center gap-2"
          >
            <RefreshCw size={16} /> 초기화
          </button>
        }
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">전체 통제 항목</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">{totalCount}개 절차</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <FileCheck size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">검증 완료 (Verified)</p>
              <h3 className="text-3xl font-black text-emerald-400 mt-2 tracking-tight">{verifiedCount}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <CheckCircle2 size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">미검증/지연 (Pending)</p>
              <h3 className="text-3xl font-black text-amber-400 mt-2 tracking-tight">{totalCount - verifiedCount}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <AlertCircle size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">통제 준수율 (Compliance)</p>
              <h3 className="text-3xl font-black text-purple-400 mt-2 tracking-tight">{completionPercent}%</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Award size={22} />
            </div>
          </div>
        </div>
      </div>

      {/* Progress Bar Container */}
      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-3">
        <div className="flex items-center justify-between text-xs font-bold">
          <span className="text-slate-300">월말 결산 내부통제 수행 진척도</span>
          <span className="text-emerald-400">{verifiedCount} / {totalCount} 항목 완료 ({completionPercent}%)</span>
        </div>
        <div className="w-full h-3 rounded-full bg-slate-800 overflow-hidden p-0.5 border border-white/5">
          <div 
            className="h-full rounded-full bg-gradient-to-r from-blue-500 to-emerald-400 transition-all duration-500 shadow-lg shadow-emerald-500/20"
            style={{ width: `${completionPercent}%` }}
          />
        </div>
      </div>

      {/* Main Checklist View */}
      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6">
        {/* Category Selector Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-white/10">
          <button
            onClick={() => setSelectedCategory('ALL')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
              selectedCategory === 'ALL'
                ? 'bg-blue-600 text-white shadow-md'
                : 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white'
            }`}
          >
            전체 영역 ({controls.length})
          </button>
          {categories.map(cat => {
            const catCount = controls.filter(c => c.category === cat).length;
            return (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
                  selectedCategory === cat
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white'
                }`}
              >
                {cat} ({catCount})
              </button>
            );
          })}
        </div>

        {/* Checklist Cards */}
        <div className="space-y-4">
          {filteredControls.map((item) => (
            <div
              key={item.id}
              onClick={() => toggleVerify(item.id)}
              className={`p-5 rounded-2xl border cursor-pointer transition-all flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 ${
                item.verified
                  ? 'bg-slate-900/80 border-emerald-500/30 hover:border-emerald-500/50'
                  : 'bg-slate-900/40 border-amber-500/20 hover:border-amber-500/40'
              }`}
            >
              <div className="flex items-start gap-4">
                {/* Custom Checkbox */}
                <div className={`w-6 h-6 rounded-lg flex items-center justify-center shrink-0 mt-1 sm:mt-0 transition-all ${
                  item.verified 
                    ? 'bg-emerald-500 text-slate-950 font-bold shadow-lg shadow-emerald-500/20' 
                    : 'bg-white/10 text-transparent border border-white/20'
                }`}>
                  <CheckCircle2 size={18} />
                </div>

                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-bold text-slate-400 bg-white/5 px-2 py-0.5 rounded border border-white/5">
                      {item.id}
                    </span>
                    <span className="text-xs font-bold text-blue-400">
                      {item.category}
                    </span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                      item.riskLevel === 'HIGH' ? 'bg-rose-500/10 text-rose-400 border-rose-500/20' : 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                    }`}>
                      위험도: {item.riskLevel}
                    </span>
                  </div>

                  <h4 className={`text-base font-black tracking-tight ${item.verified ? 'text-white' : 'text-slate-200'}`}>
                    {item.title}
                  </h4>

                  <p className="text-xs text-slate-400 leading-relaxed font-medium">
                    {item.description}
                  </p>
                </div>
              </div>

              {/* Status and Verification Meta */}
              <div className="flex flex-col sm:items-end gap-1.5 shrink-0 pl-10 sm:pl-0 border-t sm:border-t-0 border-white/5 pt-3 sm:pt-0 w-full sm:w-auto">
                <StatusBadge 
                  status={item.verified ? '점검 완료 (Verified)' : '점검 필요 (Pending)'} 
                  variant={item.verified ? 'success' : 'warning'} 
                />
                
                <div className="text-[11px] text-slate-400 font-mono flex items-center gap-1 mt-1">
                  <Clock size={12} className="text-slate-500" />
                  최종 검증: {item.lastVerifiedAt}
                </div>
                
                <div className="text-[11px] text-slate-400 flex items-center gap-1">
                  <UserCheck size={12} className="text-slate-500" />
                  검증자: <span className="text-slate-300 font-bold">{item.verifier}</span>
                </div>
              </div>
            </div>
          ))}

          {filteredControls.length === 0 && (
            <div className="py-12 text-center text-slate-500 font-medium">
              해당 카테고리에 정의된 통제 절차가 없습니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
