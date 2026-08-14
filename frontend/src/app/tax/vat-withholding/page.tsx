'use client';

import React, { useState } from 'react';

interface TaxDeclaration {
  id: string;
  taxType: 'VAT' | 'WITHHOLDING' | 'CORPORATE';
  periodName: string;
  dueDate: string;
  taxableAmount: number;
  taxAmount: number;
  validationStatus: 'PASSED' | 'WARNING' | 'ERROR';
}

export default function VatWithholdingPage() {
  const [declarations, setDeclarations] = useState<TaxDeclaration[]>([
    {
      id: 'TAX-2026-Q2-VAT',
      taxType: 'VAT',
      periodName: '2026년 2분기 부가가치세 확정신고',
      dueDate: '2026-07-25',
      taxableAmount: 450000000,
      taxAmount: 45000000,
      validationStatus: 'PASSED',
    },
    {
      id: 'TAX-2026-07-WTH',
      taxType: 'WITHHOLDING',
      periodName: '2026년 7월분 원천징수 이행상황신고',
      dueDate: '2026-08-10',
      taxableAmount: 120000000,
      taxAmount: 13200000,
      validationStatus: 'PASSED',
    },
    {
      id: 'TAX-2026-ADJ-VAT',
      taxType: 'VAT',
      periodName: '세금계산서 불부합 검증 (매입누락)',
      dueDate: '2026-08-15',
      taxableAmount: 8500000,
      taxAmount: 850000,
      validationStatus: 'WARNING',
    },
  ]);

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          📑 부가가치세 & 원천세 신고 캘린더 (GH-19)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          매입/매출 세금계산서 불부합 검증, 원천징수 이행상황 신고 서식 및 납부 캘린더를 관리합니다.
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>세무 신고/납부 예정 항목</h2>
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
            📋 전자신고 파일 생성 (홈택스 연동)
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>신고 ID</th>
              <th style={{ padding: '1rem' }}>세목 구분</th>
              <th style={{ padding: '1rem' }}>신고 대상 기간/명칭</th>
              <th style={{ padding: '1rem' }}>신고/납부 기한</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>과세표준액</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>산출 세액</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>검증 상태</th>
            </tr>
          </thead>
          <tbody>
            {declarations.map((item) => (
              <tr key={item.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 700 }}>{item.id}</td>
                <td style={{ padding: '1rem' }}>
                  {item.taxType === 'VAT' && <span style={{ color: '#60a5fa' }}>부가가치세</span>}
                  {item.taxType === 'WITHHOLDING' && <span style={{ color: '#c084fc' }}>원천세</span>}
                  {item.taxType === 'CORPORATE' && <span style={{ color: '#facc15' }}>법인세</span>}
                </td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{item.periodName}</td>
                <td style={{ padding: '1rem', color: '#f87171', fontWeight: 600 }}>{item.dueDate}</td>
                <td style={{ padding: '1rem', textAlign: 'right', color: '#cbd5e1' }}>
                  {item.taxableAmount.toLocaleString()} 원
                </td>
                <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 700, color: '#f87171' }}>
                  {item.taxAmount.toLocaleString()} 원
                </td>
                <td style={{ padding: '1rem', textAlign: 'center' }}>
                  {item.validationStatus === 'PASSED' && (
                    <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      ✅ 이상없음 (검증통과)
                    </span>
                  )}
                  {item.validationStatus === 'WARNING' && (
                    <span style={{ color: '#f59e0b', background: 'rgba(245, 158, 11, 0.15)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
                      ⚠️ 매입 불부합 확인필요
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
