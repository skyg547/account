'use client';

import React, { useState } from 'react';

interface MasterSyncItem {
  domain: string;
  totalRecords: number;
  lastSyncedAt: string;
  status: 'SYNCED' | 'SYNCING' | 'FAILED';
}

export default function MasterIntegrationPage() {
  const [items] = useState<MasterSyncItem[]>([
    { domain: '법인/조직 (Company & Dept)', totalRecords: 48, lastSyncedAt: '2026-07-28 10:00:00', status: 'SYNCED' },
    { domain: '계정과목 체계 (Chart of Accounts)', totalRecords: 320, lastSyncedAt: '2026-07-28 10:00:00', status: 'SYNCED' },
    { domain: '거래처 마스터 (Partner Master)', totalRecords: 1420, lastSyncedAt: '2026-07-28 11:30:00', status: 'SYNCED' },
    { domain: '금융기관 계좌 마스터 (Bank Accounts)', totalRecords: 85, lastSyncedAt: '2026-07-28 09:15:00', status: 'SYNCED' },
  ]);

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          🌐 엔터프라이즈 기준정보(Master Data) 통합 관리 (GH-16)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          전사 ERP 및 외부 수신 시스템과의 기준정보(조직, 계정과목, 거래처, 계좌) 동기화 정합성을 관리합니다.
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Master Data 영역별 동기화 상태</h2>
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
            🔄 전체 마스터 동기화 실행
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.5rem' }}>
          {items.map((item) => (
            <div
              key={item.domain}
              style={{
                background: 'rgba(255, 255, 255, 0.03)',
                borderRadius: '12px',
                padding: '1.25rem',
                border: '1px solid rgba(255, 255, 255, 0.08)',
              }}
            >
              <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.5rem', color: '#93c5fd' }}>
                {item.domain}
              </h3>
              <p style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                {item.totalRecords.toLocaleString()} <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>건</span>
              </p>
              <p style={{ fontSize: '0.8rem', color: '#64748b', marginBottom: '1rem' }}>
                최종 동기화: {item.lastSyncedAt}
              </p>
              <div>
                {item.status === 'SYNCED' && (
                  <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '4px', fontSize: '0.8rem' }}>
                    ● 동기화 완료
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
