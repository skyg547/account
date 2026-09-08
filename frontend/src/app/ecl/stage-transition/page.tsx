'use client';

import React, { useState } from 'react';

interface EclExposure {
  id: string;
  customerName: string;
  exposureAmount: number;
  stage: 'STAGE_1' | 'STAGE_2' | 'STAGE_3';
  pd: number; // 부도확률 (%)
  lgd: number; // 손실률 (%)
  eclAmount: number; // 기대신용손실
  previousStage: 'STAGE_1' | 'STAGE_2' | 'STAGE_3';
}

export default function EclStageTransitionPage() {
  const [exposures] = useState<EclExposure[]>([
    {
      id: 'ECL-2026-001',
      customerName: '(주) 테크노솔루션',
      exposureAmount: 500000000,
      stage: 'STAGE_1',
      pd: 0.5,
      lgd: 45.0,
      eclAmount: 1125000,
      previousStage: 'STAGE_1',
    },
    {
      id: 'ECL-2026-002',
      customerName: '한빛글로벌 유한회사',
      exposureAmount: 300000000,
      stage: 'STAGE_2',
      pd: 4.2,
      lgd: 50.0,
      eclAmount: 6300000,
      previousStage: 'STAGE_1',
    },
    {
      id: 'ECL-2026-003',
      customerName: '(주) 대우산업개발',
      exposureAmount: 150000000,
      stage: 'STAGE_3',
      pd: 100.0,
      lgd: 75.0,
      eclAmount: 112500000,
      previousStage: 'STAGE_2',
    },
  ]);

  const totalExposure = exposures.reduce((sum, e) => sum + e.exposureAmount, 0);
  const totalEcl = exposures.reduce((sum, e) => sum + e.eclAmount, 0);

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          📉 IFRS 9 Stage 전이 & 대손충당금(ECL) 산출 (GH-22)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          신용위험 변화에 따른 Stage 1/2/3 단계 전이를 분류하고 PD, LGD, EAD 수식에 기반하여 대손충당금을 자동 산출합니다.
        </p>
      </div>

      {/* 요약 현황 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.5rem', marginBottom: '2rem' }}>
        <div style={{ background: 'rgba(255, 255, 255, 0.05)', borderRadius: '12px', padding: '1.25rem', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
          <h3 style={{ fontSize: '0.9rem', color: '#94a3b8', marginBottom: '0.5rem' }}>총 익스포저 (EAD)</h3>
          <p style={{ fontSize: '1.6rem', fontWeight: 700, color: '#fff' }}>{totalExposure.toLocaleString()} 원</p>
        </div>
        <div style={{ background: 'rgba(239, 68, 68, 0.1)', borderRadius: '12px', padding: '1.25rem', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
          <h3 style={{ fontSize: '0.9rem', color: '#fca5a5', marginBottom: '0.5rem' }}>총 대손충당금 (ECL)</h3>
          <p style={{ fontSize: '1.6rem', fontWeight: 700, color: '#f87171' }}>{totalEcl.toLocaleString()} 원</p>
        </div>
        <div style={{ background: 'rgba(245, 158, 11, 0.1)', borderRadius: '12px', padding: '1.25rem', border: '1px solid rgba(245, 158, 11, 0.3)' }}>
          <h3 style={{ fontSize: '0.9rem', color: '#fcd34d', marginBottom: '0.5rem' }}>평균 대손 적립률</h3>
          <p style={{ fontSize: '1.6rem', fontWeight: 700, color: '#fbbf24' }}>
            {((totalEcl / totalExposure) * 100).toFixed(2)} %
          </p>
        </div>
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>익스포저별 Stage 단계 전이 및 ECL 상세</h2>
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
            ⚡ ECL 배치 산출 실행
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>익스포저 ID</th>
              <th style={{ padding: '1rem' }}>차주명 (고객명)</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>EAD (익스포저액)</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>Stage 단계</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>PD (부도확률)</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>LGD (손실률)</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>산출 ECL (충당금)</th>
            </tr>
          </thead>
          <tbody>
            {exposures.map((item) => (
              <tr key={item.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 700 }}>{item.id}</td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{item.customerName}</td>
                <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 600 }}>
                  {item.exposureAmount.toLocaleString()} 원
                </td>
                <td style={{ padding: '1rem', textAlign: 'center' }}>
                  {item.stage === 'STAGE_1' && (
                    <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      Stage 1 (12개월)
                    </span>
                  )}
                  {item.stage === 'STAGE_2' && (
                    <span style={{ color: '#f59e0b', background: 'rgba(245, 158, 11, 0.15)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
                      ⚠️ Stage 2 (전생애)
                    </span>
                  )}
                  {item.stage === 'STAGE_3' && (
                    <span style={{ color: '#ef4444', background: 'rgba(239, 68, 68, 0.2)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 700 }}>
                      🚨 Stage 3 (손상)
                    </span>
                  )}
                </td>
                <td style={{ padding: '1rem', textAlign: 'right' }}>{item.pd}%</td>
                <td style={{ padding: '1rem', textAlign: 'right' }}>{item.lgd}%</td>
                <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 700, color: '#f87171' }}>
                  {item.eclAmount.toLocaleString()} 원
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
