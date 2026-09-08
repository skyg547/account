'use client';

import React, { useState } from 'react';

interface PartnerDoc {
  id: string;
  partnerName: string;
  docType: 'BUSINESS_LICENSE' | 'BANK_PASSBOOK' | 'SEAL_CERTIFICATE';
  docName: string;
  uploadedAt: string;
  dueDate: string;
  status: 'SUBMITTED' | 'EXPIRED' | 'MISSING';
}

export default function PartnerDocumentsPage() {
  const [docs] = useState<PartnerDoc[]>([
    {
      id: 'DOC-101',
      partnerName: '(주) 테크노솔루션',
      docType: 'BUSINESS_LICENSE',
      docName: '사업자등록증_2026.pdf',
      uploadedAt: '2026-01-15',
      dueDate: '2026-12-31',
      status: 'SUBMITTED',
    },
    {
      id: 'DOC-102',
      partnerName: '(주) 테크노솔루션',
      docType: 'BANK_PASSBOOK',
      docName: '신한은행_통장사본.pdf',
      uploadedAt: '2026-01-15',
      dueDate: '2026-12-31',
      status: 'SUBMITTED',
    },
    {
      id: 'DOC-103',
      partnerName: '한빛글로벌 유한회사',
      docType: 'SEAL_CERTIFICATE',
      docName: '법인인감증명서.pdf',
      uploadedAt: '2025-06-01',
      dueDate: '2026-06-01',
      status: 'EXPIRED',
    },
  ]);

  const getDocTypeName = (type: string) => {
    switch (type) {
      case 'BUSINESS_LICENSE':
        return '사업자등록증';
      case 'BANK_PASSBOOK':
        return '통장 사본';
      case 'SEAL_CERTIFICATE':
        return '인감증명서';
      default:
        return type;
    }
  };

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          📁 거래처 첨부서류 및 유효기간 관리 (GH-14)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          사업자등록증, 통장사본, 법인인감증명서 등 필수 서류의 제출 상태 및 마감/만료 일자를 관리합니다.
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
          <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>제출 서류 현황 목록</h2>
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
            + 새 서류 등록
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#cbd5e1' }}>
              <th style={{ padding: '1rem' }}>서류 ID</th>
              <th style={{ padding: '1rem' }}>거래처명</th>
              <th style={{ padding: '1rem' }}>서류 구분</th>
              <th style={{ padding: '1rem' }}>파일명</th>
              <th style={{ padding: '1rem' }}>제출일자</th>
              <th style={{ padding: '1rem' }}>유효/마감일자</th>
              <th style={{ padding: '1rem' }}>상태</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>다운로드</th>
            </tr>
          </thead>
          <tbody>
            {docs.map((doc) => (
              <tr key={doc.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace' }}>{doc.id}</td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{doc.partnerName}</td>
                <td style={{ padding: '1rem', color: '#93c5fd' }}>{getDocTypeName(doc.docType)}</td>
                <td style={{ padding: '1rem', color: '#cbd5e1' }}>{doc.docName}</td>
                <td style={{ padding: '1rem', color: '#94a3b8' }}>{doc.uploadedAt}</td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{doc.dueDate}</td>
                <td style={{ padding: '1rem' }}>
                  {doc.status === 'SUBMITTED' && (
                    <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      유효함
                    </span>
                  )}
                  {doc.status === 'EXPIRED' && (
                    <span style={{ color: '#f87171', background: 'rgba(248, 113, 113, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      ⚠️ 기간 만료 (재제출 필요)
                    </span>
                  )}
                  {doc.status === 'MISSING' && (
                    <span style={{ color: '#f59e0b', background: 'rgba(245, 158, 11, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      미제출
                    </span>
                  )}
                </td>
                <td style={{ padding: '1rem', textAlign: 'center' }}>
                  <button
                    style={{
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#fff',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      padding: '4px 10px',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                    }}
                  >
                    📥 다운로드
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
