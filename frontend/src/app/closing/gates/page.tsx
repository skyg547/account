"use client";

import React, { useState } from 'react';
import { 
  ShieldCheck, 
  CheckCircle2, 
  AlertTriangle, 
  Zap
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface GateRule {
  id: string;
  name: string;
  description: string;
  status: 'PASS' | 'FAIL' | 'IN_REVIEW' | 'PENDING';
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  lastChecked: string;
  metric: string;
}

interface GateStage {
  stageId: number;
  stageName: string;
  subtitle: string;
  rules: GateRule[];
}

export default function ClosingGatesPage() {
  const [stages, setStages] = useState<GateStage[]>([
    {
      stageId: 1,
      stageName: 'Gate 1: 전표 동결 & 기초 정산',
      subtitle: '일반전표 작성 정지 및 미승인 전표 0건 검증',
      rules: [
        {
          id: 'RULE-101',
          name: '미승인 임시 전표 존재 여부',
          description: '승인 대기 중이거나 DRAFT 상태인 전표가 0건이어야 합니다.',
          status: 'PASS',
          severity: 'CRITICAL',
          lastChecked: '2026-07-28 09:30',
          metric: 'DRAFT/PENDING 0건',
        },
        {
          id: 'RULE-102',
          name: '은행 실시간 입출금 내역 대사',
          description: '전 계좌 금융기관 통장 잔액과 계정 원장 잔액 산출 일치',
          status: 'PASS',
          severity: 'CRITICAL',
          lastChecked: '2026-07-28 09:32',
          metric: '차액 ₩0원 (100% 대사 완료)',
        },
        {
          id: 'RULE-103',
          name: '임시계정 (가지급금/가수금) 정리',
          description: '미정산 가지급금 및 가수금 계정 잔액 정리 완료',
          status: 'IN_REVIEW',
          severity: 'WARNING',
          lastChecked: '2026-07-28 10:15',
          metric: '잔액 ₩1,200,000 (검토 중)',
        },
      ],
    },
    {
      stageId: 2,
      stageName: 'Gate 2: 자동 평가 배치 실행',
      subtitle: '외화 환평가, 감가상각, 선급비용 안분 배치 실행 여부',
      rules: [
        {
          id: 'RULE-201',
          name: '외화 평가 배치 정상 종료',
          description: '기말 고시환율 적용 외화 자산/부채 미실현 평가 완료',
          status: 'PASS',
          severity: 'CRITICAL',
          lastChecked: '2026-07-28 11:00',
          metric: '배치 ID: FX-202607-01 (성공)',
        },
        {
          id: 'RULE-202',
          name: '유/무형 자산 감가상각 전표 계상',
          description: '당월 감가상각 상각비 자동 계산 및 전표 생성 완료',
          status: 'PASS',
          severity: 'CRITICAL',
          lastChecked: '2026-07-28 11:10',
          metric: '총 342건 자산 상각 완료',
        },
        {
          id: 'RULE-203',
          name: '이연수익 및 선급비용 안분전표',
          description: '기간 안분 대상 비용의 당월 경과분 전표 발행',
          status: 'FAIL',
          severity: 'CRITICAL',
          lastChecked: '2026-07-28 11:30',
          metric: '소프트웨어 라이선스 1건 누락',
        },
      ],
    },
    {
      stageId: 3,
      stageName: 'Gate 3: 시산표 대차 대사 & 세무 추산',
      subtitle: '차변/대변 총액 일치 및 법인세 추산액 계상',
      rules: [
        {
          id: 'RULE-301',
          name: '합계잔액시산표 차대변 균형',
          description: '시산표 차변 합계와 대변 합계 차액 0원 검증',
          status: 'PASS',
          severity: 'CRITICAL',
          lastChecked: '2026-07-28 11:45',
          metric: '차액 ₩0원 (차대 완벽 대칭)',
        },
        {
          id: 'RULE-302',
          name: '법인세 비용 추산액 정산',
          description: '당월 누계 손익 기반 추산 법인세 비용 계상',
          status: 'PENDING',
          severity: 'WARNING',
          lastChecked: '미실행',
          metric: 'Gate 2 선결 조건 미완료',
        },
      ],
    },
    {
      stageId: 4,
      stageName: 'Gate 4: 최종 잠금 및 CFO 승인',
      subtitle: '회계 기간 잠금 마감 및 재무제표 확정 권한 승인',
      rules: [
        {
          id: 'RULE-401',
          name: 'CFO 최종 서명 및 기간 잠금 권한',
          description: '선행 게이트 100% Pass 완료 후 기간 잠금 실행',
          status: 'PENDING',
          severity: 'CRITICAL',
          lastChecked: '미실행',
          metric: '최종 승인 대기',
        },
      ],
    },
  ]);

  const [isRechecking, setIsRechecking] = useState(false);

  const allRules = stages.flatMap(s => s.rules);
  const totalRules = allRules.length;
  const passRules = allRules.filter(r => r.status === 'PASS').length;
  const failRules = allRules.filter(r => r.status === 'FAIL').length;
  const inReviewRules = allRules.filter(r => r.status === 'IN_REVIEW').length;
  const passPercentage = Math.round((passRules / totalRules) * 100);

  const handleRunAllChecks = () => {
    setIsRechecking(true);
    setTimeout(() => {
      setStages(prev =>
        prev.map(stage => ({
          ...stage,
          rules: stage.rules.map(rule => {
            if (rule.id === 'RULE-203') {
              return {
                ...rule,
                status: 'PASS',
                lastChecked: new Date().toISOString().slice(0, 16).replace('T', ' '),
                metric: '누락 건 정산 완료 (100% Pass)',
              };
            }
            if (rule.status === 'PENDING' && stage.stageId === 3) {
              return {
                ...rule,
                status: 'IN_REVIEW',
                lastChecked: new Date().toISOString().slice(0, 16).replace('T', ' '),
                metric: '추산액 산출 진행중',
              };
            }
            return {
              ...rule,
              lastChecked: new Date().toISOString().slice(0, 16).replace('T', ' '),
            };
          }),
        }))
      );
      setIsRechecking(false);
    }, 1200);
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="게이트 점검"
        description="회계 마감 및 기간 잠금 전 필수 선행 조건(Pre-requisite Rules)을 다각도로 검증하는 매트릭스입니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '게이트 점검' },
        ]}
        icon={ShieldCheck}
        actions={
          <button
            onClick={handleRunAllChecks}
            disabled={isRechecking}
            className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/20 transition-all disabled:opacity-50"
          >
            <Zap size={18} className={isRechecking ? 'animate-spin' : ''} />
            <span>{isRechecking ? '게이트 실시간 자동 검증 중...' : '전체 게이트 다시 검증'}</span>
          </button>
        }
      />

      {/* Overview Metric Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 items-center">
          <div className="md:col-span-2 space-y-3">
            <div className="flex items-center gap-3">
              <span className="text-xs font-black tracking-widest text-emerald-400 uppercase bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
                Gate Readiness Rate
              </span>
              {failRules > 0 ? (
                <StatusBadge status="차단 항목 존재" variant="error" />
              ) : (
                <StatusBadge status="양호" variant="success" />
              )}
            </div>
            <div className="flex items-baseline gap-4">
              <span className="text-5xl font-black text-white italic tracking-tighter">{passPercentage}%</span>
              <span className="text-sm text-slate-400 font-medium">
                총 {totalRules}개 규칙 중 {passRules}개 통과 (Pass)
              </span>
            </div>
            <div className="w-full h-3 bg-slate-800 rounded-full overflow-hidden border border-white/5 p-0.5">
              <div 
                className="h-full bg-gradient-to-r from-blue-500 via-indigo-500 to-emerald-400 rounded-full transition-all duration-700"
                style={{ width: `${passPercentage}%` }}
              />
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-1">
            <span className="text-xs font-bold text-slate-400">통과 (Pass)</span>
            <p className="text-3xl font-black text-emerald-400">{passRules} <span className="text-xs font-normal text-slate-500">규칙</span></p>
            <p className="text-[11px] text-slate-400">결산 진행 조건 충족</p>
          </div>

          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-1">
            <span className="text-xs font-bold text-slate-400">미달 (Fail) / 검토중</span>
            <p className="text-3xl font-black text-rose-400">{failRules + inReviewRules} <span className="text-xs font-normal text-slate-500">규칙</span></p>
            <p className="text-[11px] text-rose-400 font-medium">조치 후 결산 가능</p>
          </div>
        </div>
      </div>

      {/* Gate Matrix Boards */}
      <div className="space-y-6">
        {stages.map((stage) => {
          const stagePasses = stage.rules.filter(r => r.status === 'PASS').length;
          const stageTotal = stage.rules.length;
          const stageComplete = stagePasses === stageTotal;

          return (
            <div
              key={stage.stageId}
              className={`p-6 rounded-3xl backdrop-blur-md border transition-all ${
                stageComplete
                  ? 'bg-slate-900/40 border-white/5'
                  : 'bg-slate-900/70 border-white/10'
              }`}
            >
              <div className="flex flex-col md:flex-row md:items-center justify-between pb-4 mb-4 border-b border-white/5 gap-2">
                <div className="space-y-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-black text-white italic">{stage.stageName}</h3>
                    <span className="text-xs font-mono font-bold px-2.5 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
                      {stagePasses} / {stageTotal} PASS
                    </span>
                  </div>
                  <p className="text-xs text-slate-400">{stage.subtitle}</p>
                </div>

                <div className="flex items-center gap-2">
                  {stageComplete ? (
                    <span className="flex items-center gap-1.5 text-xs font-black text-emerald-400 bg-emerald-500/10 px-3 py-1.5 rounded-xl border border-emerald-500/20">
                      <CheckCircle2 size={14} /> 게이트 통과 (Clear)
                    </span>
                  ) : (
                    <span className="flex items-center gap-1.5 text-xs font-black text-amber-400 bg-amber-500/10 px-3 py-1.5 rounded-xl border border-amber-500/20">
                      <AlertTriangle size={14} /> 미완료 조건 존재
                    </span>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {stage.rules.map((rule) => {
                  const isPass = rule.status === 'PASS';
                  const isFail = rule.status === 'FAIL';
                  const isInReview = rule.status === 'IN_REVIEW';

                  return (
                    <div
                      key={rule.id}
                      className={`p-5 rounded-2xl border transition-all flex flex-col justify-between space-y-4 ${
                        isPass
                          ? 'bg-white/[0.02] border-white/5 hover:border-emerald-500/30'
                          : isFail
                          ? 'bg-rose-500/5 border-rose-500/20 hover:border-rose-500/40'
                          : 'bg-amber-500/5 border-amber-500/20'
                      }`}
                    >
                      <div className="space-y-2">
                        <div className="flex items-start justify-between gap-2">
                          <span className="text-[10px] font-mono font-bold text-slate-500">{rule.id}</span>
                          <StatusBadge
                            status={
                              isPass ? 'PASS' :
                              isFail ? 'FAIL' :
                              isInReview ? '검토중' : '대기'
                            }
                            variant={
                              isPass ? 'success' :
                              isFail ? 'error' :
                              isInReview ? 'warning' : 'neutral'
                            }
                          />
                        </div>

                        <h4 className="text-sm font-bold text-white">{rule.name}</h4>
                        <p className="text-xs text-slate-400 leading-relaxed">{rule.description}</p>
                      </div>

                      <div className="space-y-2 pt-3 border-t border-white/5">
                        <div className="p-2.5 rounded-xl bg-slate-950/40 border border-white/5 text-xs">
                          <span className="text-[10px] text-slate-500 font-bold block mb-0.5">지표 / 결과</span>
                          <span className="font-mono font-bold text-slate-200">{rule.metric}</span>
                        </div>
                        <div className="flex items-center justify-between text-[10px] text-slate-500">
                          <span>최종검증: {rule.lastChecked}</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
