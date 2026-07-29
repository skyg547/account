'use client';

import React, { useState } from 'react';

interface AuditRequest {
  id: string;
  partnerName: string;
  bizNo: string;
  ownerName: string;
  hometaxStatus: 'REGULAR' | 'SUSPENDED' | 'CLOSED' | 'CHECKING';
  creditGrade: string;
  requestedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}

export default function PartnerAuditPage() {
  const [requests, setRequests] = useState<AuditRequest[]>([
    {
      id: 'AUD-2026-001',
      partnerName: '(주) 테크노솔루션',
      bizNo: '123-86-00192',
      ownerName: '홍길동',
      hometaxStatus: 'REGULAR',
      creditGrade: 'AA',
      requestedAt: '2026-07-28',
      status: 'PENDING',
    },
    {
      id: 'AUD-2026-002',
      partnerName: '한빛글로벌 유한회사',
      bizNo: '214-88-99210',
      ownerName: '김철수',
      hometaxStatus: 'CHECKING',
      creditGrade: 'BBB+',
      requestedAt: '2026-07-27',
      status: 'PENDING',
    },
  ]);

  const [loadingId, setLoadingId] = useState<string | null>(null);

  // 국세청 휴폐업 실시간 조회 모의 함수
  const checkHometaxStatus = (id: string) => {
    setLoadingId(id);
    setTimeout(() => {
      setRequests((prev) =>
        prev.map((req) =>
          req.id === id ? { ...req, hometaxStatus: 'REGULAR' } : req
        )
      );
      setLoadingId(null);
    }, 1000);
  };

  const handleApprove = (id: string) => {
    setRequests((prev) =>
      prev.map((req) => (req.id === id ? { ...req, status: 'APPROVED' } : req))
    );
  };

  const handleReject = (id: string) => {
    setRequests((prev) =>
      prev.map((req) => (req.id === id ? { ...req, status: 'REJECTED' } : req))
    );
  };

  return (
    <div style={{ padding: '2rem', color: '#fff', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          📑 거래처 심사 승인 워크플로 (GH-14)
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>
          신규 거래처 등록 요청에 대해 국세청 휴폐업 실시간 검증 및 신용등급에 기초하여 최종 승인을 진행합니다.
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
              <th style={{ padding: '1rem' }}>요청 번호</th>
              <th style={{ padding: '1rem' }}>거래처명</th>
              <th style={{ padding: '1rem' }}>사업자번호</th>
              <th style={{ padding: '1rem' }}>대표자</th>
              <th style={{ padding: '1rem' }}>국세청 상태</th>
              <th style={{ padding: '1rem' }}>신용등급</th>
              <th style={{ padding: '1rem' }}>진행 상태</th>
              <th style={{ padding: '1rem', textAlign: 'center' }}>심사 액션</th>
            </tr>
          </thead>
          <tbody>
            {requests.map((req) => (
              <tr key={req.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <td style={{ padding: '1rem', fontFamily: 'monospace' }}>{req.id}</td>
                <td style={{ padding: '1rem', fontWeight: 600 }}>{req.partnerName}</td>
                <td style={{ padding: '1rem', color: '#94a3b8' }}>{req.bizNo}</td>
                <td style={{ padding: '1rem' }}>{req.ownerName}</td>
                <td style={{ padding: '1rem' }}>
                  {req.hometaxStatus === 'REGULAR' && (
                    <span style={{ color: '#4ade80', background: 'rgba(74, 222, 128, 0.1)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85rem' }}>
                      정상 (계속사업자)
                    </span>
                  )}
                  {req.hometaxStatus === 'CHECKING' && (
                    <button
                      onClick={() => checkHometaxStatus(req.id)}
                      disabled={loadingId === req.id}
                      style={{
                        background: '#3b82f6',
                        color: '#fff',
                        border: 'none',
                        padding: '4px 10px',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        fontSize: '0.85rem',
                      }}
                    >
                      {loadingId === req.id ? '조회중...' : '🔍 실시간 조회'}
                    </button>
                  )}
                </td>
                <td style={{ padding: '1rem' }}>
                  <span style={{ background: 'rgba(255,255,255,0.1)', padding: '2px 8px', borderRadius: '4px', fontWeight: 700 }}>
                    {req.creditGrade}
                  </span>
                </td>
                <td style={{ padding: '1rem' }}>
                  {req.status === 'PENDING' && <span style={{ color: '#f59e0b' }}>⏳ 심사 대기</span>}
                  {req.status === 'APPROVED' && <span style={{ color: '#4ade80' }}>✅ 승인 완료</span>}
                  {req.status === 'REJECTED' && <span style={{ color: '#ef4444' }}>❌ 반려</span>}
                </td>
                <td style={{ padding: '1rem', textAlign: 'center' }}>
                  {req.status === 'PENDING' ? (
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                      <button
                        onClick={() => handleApprove(req.id)}
                        style={{ background: '#22c55e', color: '#fff', border: 'none', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer' }}
                      >
                        승인
                      </button>
                      <button
                        onClick={() => handleReject(req.id)}
                        style={{ background: '#ef4444', color: '#fff', border: 'none', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer' }}
                      >
                        반려
                      </button>
                    </div>
                  ) : (
                    <span style={{ color: '#64748b', fontSize: '0.85rem' }}>완료됨</span>
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
