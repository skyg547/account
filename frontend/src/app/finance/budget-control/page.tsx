'use client';

import React, { useState } from 'react';

interface BudgetItem {
  id: string;
  deptName: string;
  accountName: string;
  allocatedAmount: number;
  usedAmount: number;
  status: 'SAFE' | 'WARNING' | 'EXCEEDED';
}

export default function BudgetControlPage() {
  const [budgets, setBudgets] = useState<BudgetItem[]>([
    {
      id: 'BDG-2026-01',
      deptName: 'IT개발팀',
      accountName: '도서인쇄/소프트웨어구독비',
      allocatedAmount: 15000000,
      usedAmount: 12400000,
      status: 'SAFE',
    },
    {
      id: 'BDG-2026-02',
      deptName: '마케팅팀',
      accountName: '광고선전비',
      allocatedAmount: 50000000,
      usedAmount: 48500000,
      status: 'WARNING',
    },
    {
      id: 'BDG-2026-03',
      deptName: '경영지원팀',
      accountName: '복리후생비',
      allocatedAmount: 20000000,
      usedAmount: 21500000,
      status: 'EXCEEDED',
    },
  ]);

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          📊 예산 통제 & 실적 대비 차이 분석 (GH-17)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          부서별/계정별 편성 예산 대비 실적 집행률을 모니터링하고 초과 집행 전표를 통제합니다.
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>부서별 예산 집행 현황</h2>
          <button
            style={{
              background: '#22c55e',
              color: '#fff',
              border: 'none',
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            + 추가 예산 배정
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>예산 코드</th>
              <th style={{ padding: '1rem' }}>부서명</th>
              <th style={{ padding: '1rem' }}>예산 계정과목</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>편성 예산액</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>현재 집행액</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>잔여 예산액</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>집행율/상태</th>
            </tr>
          </thead>
          <tbody>
            {budgets.map((item) => {
              const remaining = item.allocatedAmount - item.usedAmount;
              const usageRate = ((item.usedAmount / item.allocatedAmount) * 100).toFixed(1);
              return (
                <tr key={item.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                  <td style={{ padding: '1rem', fontFamily: 'monospace' }}>{item.id}</td>
                  <td style={{ padding: '1rem', fontWeight: 600 }}>{item.deptName}</td>
                  <td style={{ padding: '1rem', color: '#93c5fd' }}>{item.accountName}</td>
                  <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 600 }}>
                    {item.allocatedAmount.toLocaleString()} 원
                  </td>
                  <td style={{ padding: '1rem', textAlign: 'right', color: '#cbd5e1' }}>
                    {item.usedAmount.toLocaleString()} 원
                  </td>
                  <td
                    style={{
                      padding: '1rem',
                      textAlign: 'right',
                      fontWeight: 700,
                      color: remaining < 0 ? '#f87171' : '#4ade80',
                    }}
                  >
                    {remaining.toLocaleString()} 원
                  </td>
                  <td style={{ padding: '1rem', textAlign: 'center' }}>
                    {item.status === 'SAFE' && (
                      <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                        정상 ({usageRate}%)
                      </span>
                    )}
                    {item.status === 'WARNING' && (
                      <span style={{ color: '#f59e0b', background: 'rgba(245, 158, 11, 0.15)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
                        ⚠️ 경고 ({usageRate}%)
                      </span>
                    )}
                    {item.status === 'EXCEEDED' && (
                      <span style={{ color: '#ef4444', background: 'rgba(239, 68, 68, 0.2)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 700 }}>
                        🚫 한도초과 ({usageRate}%)
                      </span>
                    )}
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
