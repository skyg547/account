'use client';

import React, { useState } from 'react';

interface ClosingStep {
  stepNo: number;
  title: string;
  category: 'PREPARATION' | 'JOURNAL_CLOSING' | 'PL_CLOSING' | 'STATEMENTS';
  status: 'COMPLETED' | 'IN_PROGRESS' | 'NOT_STARTED';
  reconciliationDiff: number;
  completedAt?: string;
}

export default function ClosingChecklistPage() {
  const [steps, setSteps] = useState<ClosingStep[]>([
    {
      stepNo: 1,
      title: '미승인 전표 일괄 처리 및 필수 속성 검증',
      category: 'PREPARATION',
      status: 'COMPLETED',
      reconciliationDiff: 0,
      completedAt: '2026-07-28 09:30',
    },
    {
      stepNo: 2,
      title: '총계정원장 - 보조원장 간 대사 (Reconciliation)',
      category: 'JOURNAL_CLOSING',
      status: 'IN_PROGRESS',
      reconciliationDiff: 0,
    },
    {
      stepNo: 3,
      title: '감가상각비 및 기말 손익 결산 전표 자동 생성',
      category: 'PL_CLOSING',
      status: 'NOT_STARTED',
      reconciliationDiff: 0,
    },
    {
      stepNo: 4,
      title: '재무상태표 및 손익계산서 최종 확정 마감',
      category: 'STATEMENTS',
      status: 'NOT_STARTED',
      reconciliationDiff: 0,
    },
  ]);

  const handleCompleteStep = (stepNo: number) => {
    setSteps((prev) =>
      prev.map((s) =>
        s.stepNo === stepNo
          ? { ...s, status: 'COMPLETED', completedAt: new Date().toISOString().slice(0, 16).replace('T', ' ') }
          : s
      )
    );
  };

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          🔒 월말/연말 결산 마감 체크리스트 & 원장 대사 (GH-20)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          전표 마감, 보조원장-총계정원장 데이터 대사, 결산 전표 자동 생성을 단계별로 실행하여 마감 정합성을 확보합니다.
        </p>
      </div>

      <div
        style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(10px)',
          borderRadius: '16px',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          padding: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>2026년 7월 월말 결산 마감 프로세스</h2>
          <button
            style={{
              background: '#ef4444',
              color: '#fff',
              border: 'none',
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            🔒 최종 결산 락(Lock) 실행
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {steps.map((step) => (
            <div
              key={step.stepNo}
              style={{
                background: 'rgba(255, 255, 255, 0.03)',
                borderRadius: '12px',
                padding: '1.25rem',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div
                  style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '50%',
                    background: step.status === 'COMPLETED' ? '#22c55e' : step.status === 'IN_PROGRESS' ? '#3b82f6' : '#64748b',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                  }}
                >
                  {step.stepNo}
                </div>
                <div>
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.25rem' }}>{step.title}</h3>
                  <p style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
                    대사 차액: <span style={{ color: step.reconciliationDiff === 0 ? '#4ade80' : '#f87171', fontWeight: 700 }}>{step.reconciliationDiff} 원</span>
                    {step.completedAt && ` • 완료일시: ${step.completedAt}`}
                  </p>
                </div>
              </div>

              <div>
                {step.status === 'COMPLETED' ? (
                  <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '6px 12px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
                    ✅ 마감 완료
                  </span>
                ) : step.status === 'IN_PROGRESS' ? (
                  <button
                    onClick={() => handleCompleteStep(step.stepNo)}
                    style={{
                      background: '#3b82f6',
                      color: '#fff',
                      border: 'none',
                      padding: '6px 14px',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      fontWeight: 600,
                    }}
                  >
                    ▶ 마감 처리 실행
                  </button>
                ) : (
                  <span style={{ color: '#64748b', fontSize: '0.85rem' }}>대기중</span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
