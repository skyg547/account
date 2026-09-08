'use client';

import React, { useState } from 'react';

interface SodRule {
  id: string;
  roleA: string;
  roleB: string;
  description: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM';
  conflictStatus: 'SAFE' | 'VIOLATED';
}

export default function SodMatrixPage() {
  const [rules] = useState<SodRule[]>([
    {
      id: 'SOD-001',
      roleA: '전표 작성자 (Journal Creator)',
      roleB: '전표 승인자 (Journal Approver)',
      description: '동일인이 전표를 기표함과 동시에 승인할 수 없음 (자기 승인 금지)',
      severity: 'CRITICAL',
      conflictStatus: 'SAFE',
    },
    {
      id: 'SOD-002',
      roleA: '지출결의 등록자 (Payment Creator)',
      roleB: '자금 집행자 (Cash Disburser)',
      description: '지출결의를 등록한 자가 직접 자금 이체를 실행할 수 없음',
      severity: 'HIGH',
      conflictStatus: 'VIOLATED',
    },
    {
      id: 'SOD-003',
      roleA: '거래처 등록자 (Partner Registrar)',
      roleB: '거래처 심사자 (Partner Auditor)',
      description: '신규 거래처 등록자와 신용/실명 심사 승인자는 겸직 불가',
      severity: 'HIGH',
      conflictStatus: 'SAFE',
    },
  ]);

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          🛡️ 권한 분리 및 직무 통제 (SOD Matrix) (GH-15)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          Segregation of Duties(SOD) 규정에 의거하여 상충하는 직무 권한의 겸직 및 내부통제 위반 사항을 관리합니다.
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>직무 분리 규칙 및 검증 현황</h2>
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
            ⚡ SOD 위반 탐지 스캔
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>규칙 ID</th>
              <th style={{ padding: '1rem' }}>금지 직무 A</th>
              <th style={{ padding: '1rem' }}>상충 직무 B</th>
              <th style={{ padding: '1rem' }}>통제 설명</th>
              <th style={{ padding: '1rem' }}>위험도</th>
              <th style={{ padding: '1rem' }}>현재 상태</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule) => (
              <tr key={rule.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 700 }}>{rule.id}</td>
                <td style={{ padding: '1rem', color: '#fca5a5', fontWeight: 600 }}>{rule.roleA}</td>
                <td style={{ padding: '1rem', color: '#93c5fd', fontWeight: 600 }}>{rule.roleB}</td>
                <td style={{ padding: '1rem', color: '#cbd5e1', fontSize: '0.9rem' }}>{rule.description}</td>
                <td style={{ padding: '1rem' }}>
                  {rule.severity === 'CRITICAL' && <span style={{ color: '#ef4444', fontWeight: 700 }}>🔴 심각</span>}
                  {rule.severity === 'HIGH' && <span style={{ color: '#f59e0b', fontWeight: 700 }}>🟠 높음</span>}
                  {rule.severity === 'MEDIUM' && <span style={{ color: '#eab308' }}>🟡 보통</span>}
                </td>
                <td style={{ padding: '1rem' }}>
                  {rule.conflictStatus === 'SAFE' ? (
                    <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      ✅ 정상 통제됨
                    </span>
                  ) : (
                    <span style={{ color: '#ef4444', background: 'rgba(239, 68, 68, 0.15)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 700 }}>
                      ⚠️ 겸직 위반 탐지됨
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
