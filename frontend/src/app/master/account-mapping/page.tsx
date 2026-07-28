'use client';

import React, { useState } from 'react';

interface AccountMapping {
  accountCode: string;
  accountName: string;
  accountType: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
  isControlAccount: boolean;
  budgetCategory: string;
  requiredPartner: boolean;
  requiredDept: boolean;
}

export default function AccountMappingPage() {
  const [mappings, setMappings] = useState<AccountMapping[]>([
    {
      accountCode: '11100',
      accountName: '보통예금',
      accountType: 'ASSET',
      isControlAccount: true,
      budgetCategory: '자금/예금관리비',
      requiredPartner: true,
      requiredDept: true,
    },
    {
      accountCode: '21100',
      accountName: '외상매입금',
      accountType: 'LIABILITY',
      isControlAccount: true,
      budgetCategory: '채무/매입관리',
      requiredPartner: true,
      requiredDept: false,
    },
    {
      accountCode: '81100',
      accountName: '복리후생비',
      accountType: 'EXPENSE',
      isControlAccount: false,
      budgetCategory: '인원/복리후생예산',
      requiredPartner: false,
      requiredDept: true,
    },
  ]);

  const toggleControlAccount = (code: string) => {
    setMappings((prev) =>
      prev.map((item) =>
        item.accountCode === code ? { ...item, isControlAccount: !item.isControlAccount } : item
      )
    );
  };

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          ⚙️ 계정과목 체계 & 예산 매핑 관리 (GH-14)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          재무회계 계정과목별 통제 계정(Control Account) 지정, 예산 항목 맵핑, 전표 입력 시 차대변 필수 속성을 정의합니다.
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
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>계정 코드</th>
              <th style={{ padding: '1rem' }}>계정과목명</th>
              <th style={{ padding: '1rem' }}>계정 구분</th>
              <th style={{ padding: '1rem' }}>통제계정 여부</th>
              <th style={{ padding: '1rem' }}>매핑 예산항목</th>
              <th style={{ padding: '1rem' }}>필수 필수 속성</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>설정</th>
            </tr>
          </thead>
          <tbody>
            {mappings.map((item) => (
              <tr key={item.accountCode} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 700 }}>{item.accountCode}</td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{item.accountName}</td>
                <td style={{ padding: '1rem' }}>
                  <span style={{ background: 'rgba(255,255,255,0.08)', padding: '4px 8px', borderRadius: '4px', fontSize: '0.85rem' }}>
                    {item.accountType}
                  </span>
                </td>
                <td style={{ padding: '1rem' }}>
                  {item.isControlAccount ? (
                    <span style={{ color: '#38bdf8', fontWeight: 600 }}>🔒 통제계정 (전용)</span>
                  ) : (
                    <span style={{ color: '#94a3b8' }}>일반계정</span>
                  )}
                </td>
                <td style={{ padding: '1rem', color: '#a7f3d0' }}>{item.budgetCategory}</td>
                <td style={{ padding: '1rem', fontSize: '0.85rem' }}>
                  {item.requiredPartner && <span style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#f87171', padding: '2px 6px', borderRadius: '4px', marginRight: '4px' }}>거래처 필수</span>}
                  {item.requiredDept && <span style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa', padding: '2px 6px', borderRadius: '4px' }}>부서 필수</span>}
                </td>
                <td style={{ padding: '1rem', textAlign: 'center' }}>
                  <button
                    onClick={() => toggleControlAccount(item.accountCode)}
                    style={{
                      background: item.isControlAccount ? 'rgba(239, 68, 68, 0.2)' : 'rgba(59, 130, 246, 0.2)',
                      color: item.isControlAccount ? '#f87171' : '#60a5fa',
                      border: '1px solid rgba(255,255,255,0.1)',
                      padding: '4px 10px',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                    }}
                  >
                    {item.isControlAccount ? '통제 해제' : '통제 지정'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
