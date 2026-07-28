'use client';

import React, { useState } from 'react';

interface AssetFairValue {
  id: string;
  assetName: string;
  category: 'LOAN_RECEIVABLE' | 'BOND' | 'SWAP_DERIVATIVE';
  bookValue: number;
  fairValue: number;
  level: 'LEVEL_1' | 'LEVEL_2' | 'LEVEL_3';
  evaluatedAt: string;
}

export default function LoanFairValuePage() {
  const [assets, setAssets] = useState<AssetFairValue[]>([
    {
      id: 'LFA-2026-01',
      assetName: '기업 대출 채권 (A등급 중소기업)',
      category: 'LOAN_RECEIVABLE',
      bookValue: 500000000,
      fairValue: 508500000,
      level: 'LEVEL_2',
      evaluatedAt: '2026-07-28',
    },
    {
      id: 'LFA-2026-02',
      assetName: '국공채 3년물 파생결합증권',
      category: 'BOND',
      bookValue: 300000000,
      fairValue: 298000000,
      level: 'LEVEL_1',
      evaluatedAt: '2026-07-28',
    },
    {
      id: 'LFA-2026-03',
      assetName: '고정금리 헤지용 이자율 스왑(IRS)',
      category: 'SWAP_DERIVATIVE',
      bookValue: 0,
      fairValue: 4200000,
      level: 'LEVEL_2',
      evaluatedAt: '2026-07-28',
    },
  ]);

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          📈 여수신 & 금융자산 공정가치 평가 (GH-21)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          여수신 채권, 국공채 및 파생상품의 Fair Value(Level 1~3) 평가손익 및 평가액을 산출합니다.
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>공정가치 평가 자산 현황</h2>
          <button
            style={{
              background: '#3b82f6',
              color: '#fff',
              border: 'none',
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            📊 시가 평가 재산출 실행
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>자산 ID</th>
              <th style={{ padding: '1rem' }}>금융 자산명</th>
              <th style={{ padding: '1rem' }}>공정가치 서열 (Level)</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>장부가액 (Book Value)</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>공정가치 (Fair Value)</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>평가 손익</th>
            </tr>
          </thead>
          <tbody>
            {assets.map((item) => {
              const diff = item.fairValue - item.bookValue;
              return (
                <tr key={item.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                  <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 700 }}>{item.id}</td>
                  <td style={{ padding: '1rem', fontWeight: 600 }}>{item.assetName}</td>
                  <td style={{ padding: '1rem' }}>
                    <span style={{ background: 'rgba(255,255,255,0.1)', padding: '4px 8px', borderRadius: '4px', fontSize: '0.85rem', fontWeight: 700 }}>
                      {item.level}
                    </span>
                  </td>
                  <td style={{ padding: '1rem', textAlign: 'right', color: '#cbd5e1' }}>
                    {item.bookValue.toLocaleString()} 원
                  </td>
                  <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 700, color: '#93c5fd' }}>
                    {item.fairValue.toLocaleString()} 원
                  </td>
                  <td
                    style={{
                      padding: '1rem',
                      textAlign: 'right',
                      fontWeight: 700,
                      color: diff >= 0 ? '#4ade80' : '#f87171',
                    }}
                  >
                    {diff >= 0 ? '+' : ''}{diff.toLocaleString()} 원
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
