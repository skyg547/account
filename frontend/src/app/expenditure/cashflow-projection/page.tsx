'use client';

import React, { useState } from 'react';

interface CashflowEvent {
  id: string;
  dueDate: string;
  category: 'AR_COLLECTION' | 'AP_PAYMENT' | 'TAX_PAYMENT' | 'SALARY';
  title: string;
  partnerName: string;
  inflowAmount: number;
  outflowAmount: number;
  approvalStatus: 'APPROVED' | 'PENDING' | 'REJECTED';
}

export default function CashflowProjectionPage() {
  const [events, setEvents] = useState<CashflowEvent[]>([
    {
      id: 'CF-2026-001',
      dueDate: '2026-07-30',
      category: 'AR_COLLECTION',
      title: '(주) 테크노솔루션 외상매출금 수금 예정',
      partnerName: '(주) 테크노솔루션',
      inflowAmount: 45000000,
      outflowAmount: 0,
      approvalStatus: 'APPROVED',
    },
    {
      id: 'CF-2026-002',
      dueDate: '2026-07-31',
      category: 'AP_PAYMENT',
      title: '서버 클라우드 이용료 지출결의 지급',
      partnerName: 'AWS Korea',
      inflowAmount: 0,
      outflowAmount: 18500000,
      approvalStatus: 'PENDING',
    },
    {
      id: 'CF-2026-003',
      dueDate: '2026-08-05',
      category: 'TAX_PAYMENT',
      title: '2분기 부가가치세 납부',
      partnerName: '마포세무서',
      inflowAmount: 0,
      outflowAmount: 12000000,
      approvalStatus: 'PENDING',
    },
  ]);

  const handleApprove = (id: string) => {
    setEvents((prev) =>
      prev.map((e) => (e.id === id ? { ...e, approvalStatus: 'APPROVED' } : e))
    );
  };

  const handleReject = (id: string) => {
    setEvents((prev) =>
      prev.map((e) => (e.id === id ? { ...e, approvalStatus: 'REJECTED' } : e))
    );
  };

  const totalInflow = events.reduce((sum, e) => sum + e.inflowAmount, 0);
  const totalOutflow = events.reduce((sum, e) => sum + e.outflowAmount, 0);
  const netCashflow = totalInflow - totalOutflow;

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          💰 AR/AP 기반 자금 수지 추계 (Cashflow Projection) (GH-18)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          매출채권(AR) 수금 예정액 및 매입채무(AP) 지출결의 데이터를 기반으로 미래 자금 수지를 추계합니다.
        </p>
      </div>

      {/* 요약 카드 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.5rem', marginBottom: '2rem' }}>
        <div style={{ background: 'rgba(34, 197, 94, 0.1)', border: '1px solid rgba(34, 197, 94, 0.3)', borderRadius: '12px', padding: '1.25rem' }}>
          <h3 style={{ fontSize: '0.9rem', color: '#86efac', marginBottom: '0.5rem' }}>총 수금 예정액 (Inflow)</h3>
          <p style={{ fontSize: '1.6rem', fontWeight: 700, color: '#4ade80' }}>+ {totalInflow.toLocaleString()} 원</p>
        </div>
        <div style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '12px', padding: '1.25rem' }}>
          <h3 style={{ fontSize: '0.9rem', color: '#fca5a5', marginBottom: '0.5rem' }}>총 지급 예정액 (Outflow)</h3>
          <p style={{ fontSize: '1.6rem', fontWeight: 700, color: '#f87171' }}>- {totalOutflow.toLocaleString()} 원</p>
        </div>
        <div style={{ background: 'rgba(59, 130, 246, 0.1)', border: '1px solid rgba(59, 130, 246, 0.3)', borderRadius: '12px', padding: '1.25rem' }}>
          <h3 style={{ fontSize: '0.9rem', color: '#93c5fd', marginBottom: '0.5rem' }}>순 수지 밸런스 (Net Cash)</h3>
          <p style={{ fontSize: '1.6rem', fontWeight: 700, color: netCashflow >= 0 ? '#60a5fa' : '#f87171' }}>
            {netCashflow >= 0 ? '+' : ''}{netCashflow.toLocaleString()} 원
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
        <h2 style={{ fontSize: '1.2rem', fontWeight: 600, marginBottom: '1.5rem' }}>일자별 자금 입출금 이벤트 및 승인</h2>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>예정 일자</th>
              <th style={{ padding: '1rem' }}>구분</th>
              <th style={{ padding: '1rem' }}>내역 적요</th>
              <th style={{ padding: '1rem' }}>거래처</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>입금액</th>
              <th style={{ padding: '1rem', textAlign: 'right' }}>출금액</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>집행 승인</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <tr key={e.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 700 }}>{e.dueDate}</td>
                <td style={{ padding: '1rem' }}>
                  {e.category === 'AR_COLLECTION' && <span style={{ color: '#4ade80' }}>수금 (AR)</span>}
                  {e.category === 'AP_PAYMENT' && <span style={{ color: '#f87171' }}>지출 (AP)</span>}
                  {e.category === 'TAX_PAYMENT' && <span style={{ color: '#f59e0b' }}>세금 납부</span>}
                </td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{e.title}</td>
                <td style={{ padding: '1rem', color: '#94a3b8' }}>{e.partnerName}</td>
                <td style={{ padding: '1rem', textAlign: 'right', color: e.inflowAmount > 0 ? '#4ade80' : '#64748b' }}>
                  {e.inflowAmount > 0 ? `+${e.inflowAmount.toLocaleString()} 원` : '-'}
                </td>
                <td style={{ padding: '1rem', textAlign: 'right', color: e.outflowAmount > 0 ? '#f87171' : '#64748b' }}>
                  {e.outflowAmount > 0 ? `-${e.outflowAmount.toLocaleString()} 원` : '-'}
                </td>
                <td style={{ padding: '1rem', textAlign: 'center' }}>
                  {e.approvalStatus === 'PENDING' ? (
                    <div style={{ display: 'flex', gap: '6px', justifyContent: 'center' }}>
                      <button
                        onClick={() => handleApprove(e.id)}
                        style={{ background: '#22c55e', color: '#fff', border: 'none', padding: '4px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '0.85rem' }}
                      >
                        승인
                      </button>
                      <button
                        onClick={() => handleReject(e.id)}
                        style={{ background: '#ef4444', color: '#fff', border: 'none', padding: '4px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '0.85rem' }}
                      >
                        반려
                      </button>
                    </div>
                  ) : e.approvalStatus === 'APPROVED' ? (
                    <span style={{ color: '#4ade80', fontSize: '0.85rem' }}>✅ 승인됨</span>
                  ) : (
                    <span style={{ color: '#ef4444', fontSize: '0.85rem' }}>❌ 반려됨</span>
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
