"use client";

import React, { useState } from 'react';
import { 
  Plus, 
  Search, 
  FileText, 
  Calculator, 
  History, 
  ArrowUpRight,
  ShieldCheck,
  Landmark,
  X
} from 'lucide-react';
import styles from './LeaseManagement.module.css';

// [Mock Data] 리스 계약 목록
const mockLeases = [
  { id: 1, no: 'L-2026-001', name: '영등포 지점 본동 임대차', lessor: '서울빌딩매니지먼트', startDate: '2026-01-01', endDate: '2028-12-31', payment: 4500000, ifrs16: true },
  { id: 2, no: 'L-2026-002', name: '업무용 복합기 리스 (HP)', lessor: '한국리스금융', startDate: '2026-03-01', endDate: '2029-02-28', payment: 1200000, ifrs16: false },
  { id: 3, no: 'L-2025-084', name: '판교 R&D 센터 데이터룸', lessor: '판교테크노홀딩스', startDate: '2025-06-01', endDate: '2030-05-31', payment: 8500000, ifrs16: true },
];

export default function LeaseManagementPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className="titleArea">
          <h2 className="text-2xl font-bold">리스 계약 관리 (IFRS 16)</h2>
          <p className="text-sm text-gray-400">금융리스 및 운용리스 계약 정보를 관리하고 사용권자산 상각을 자동화합니다.</p>
        </div>
        <div className="flex gap-3">
          <button className="icon-btn-secondary"><History size={18} /> 재측정 이력</button>
          <button className="btn-primary flex items-center gap-2" onClick={() => setIsModalOpen(true)}>
            <Plus size={18} /> 리스 계약 신규 등록
          </button>
        </div>
      </header>

      {/* 2. 요약 지표 */}
      <section className={styles.summary}>
        <div className={styles.summaryItem}>
          <div className={styles.label}>총 사용권자산(ROU)</div>
          <div className={styles.value}>4,520,000,000</div>
        </div>
        <div className={styles.summaryItem}>
          <div className={styles.label}>리스부채 잔액</div>
          <div className={styles.value}>3,842,000,000</div>
        </div>
        <div className={styles.summaryItem}>
          <div className={styles.label}>평균 유효이자율</div>
          <div className={styles.value}>4.20 %</div>
        </div>
        <div className={styles.summaryItem}>
          <div className={styles.label}>당월 리스 비용 (상각+이자)</div>
          <div className={styles.value}>125,400,000</div>
        </div>
      </section>

      {/* 3. 계약 목록 */}
      <section className={styles.card + " glass-card"}>
        <div className="flex justify-between items-center mb-6">
          <h3 className="font-semibold flex items-center gap-2"><Landmark size={18} /> 리스 계약 원장</h3>
          <div className="relative">
            <Search className="absolute left-3 top-2 text-gray-500" size={14} />
            <input 
              type="text" 
              placeholder="계약번호, 리스이용자 검색..." 
              className="pl-8 pr-4 py-1.5 bg-white/5 border border-white/10 rounded-lg text-sm outline-none" 
            />
          </div>
        </div>

        <table className={styles.table}>
          <thead>
            <tr>
              <th>계약번호</th>
              <th>계약명</th>
              <th>리스제공자</th>
              <th>계약기간</th>
              <th>월리스료</th>
              <th>IFRS 16</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {mockLeases.map(lease => (
              <tr key={lease.id}>
                <td className="font-mono text-blue-400">{lease.no}</td>
                <td>{lease.name}</td>
                <td>{lease.lessor}</td>
                <td className="text-xs">{lease.startDate} ~ {lease.endDate}</td>
                <td className="text-right font-semibold">{lease.payment.toLocaleString()}</td>
                <td>{lease.ifrs16 ? <span className={styles.ifrsBadge}>적용</span> : <span className="text-gray-600 text-xs">-</span>}</td>
                <td>
                  <div className="flex gap-2">
                    <button title="상세보기" className="p-1 hover:text-blue-400"><FileText size={16} /></button>
                    <button title="재측정" className="p-1 hover:text-amber-400"><Calculator size={16} /></button>
                    <button title="상각표" className="p-1 hover:text-green-400"><ArrowUpRight size={16} /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* 4. 신규 등록 모달 */}
      {isModalOpen && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal + " glass-card"}>
            <div className="flex justify-between items-center border-b border-white/10 pb-4">
               <h3 className="text-xl font-bold">IFRS 16 리스 계약 등록</h3>
               <button onClick={() => setIsModalOpen(false)}><X size={20} /></button>
            </div>

            <form className={styles.formGrid}>
              <div className={styles.sectionTitle}>기본정보</div>
              <div className={styles.inputGroup}>
                <label>계약번호</label>
                <input type="text" placeholder="L-2026-XXX" />
              </div>
              <div className={`${styles.inputGroup} ${styles.fullWidth}`}>
                <label>계약명</label>
                <input type="text" placeholder="리스 계약 명칭 입력" />
              </div>
              <div className={styles.inputGroup}>
                <label>리스제공자(거래처)</label>
                <select><option>선택하세요...</option></select>
              </div>
              <div className={styles.inputGroup}>
                <label>계약시작일</label>
                <input type="date" />
              </div>
              <div className={styles.inputGroup}>
                <label>계약종료일</label>
                <input type="date" />
              </div>

              <div className={styles.sectionTitle}>금융조건 (IFRS 16)</div>
              <div className={styles.inputGroup}>
                <label>월 리스료 (VAT 제외)</label>
                <input type="number" placeholder="0" />
              </div>
              <div className={styles.inputGroup}>
                <label>할인율 (%)</label>
                <input type="number" placeholder="4.5" step="0.1" />
              </div>
              <div className={styles.inputGroup}>
                <label>지급일 (매월)</label>
                <input type="number" placeholder="25" min="1" max="31" />
              </div>
              <div className={styles.inputGroup}>
                <label>사용권자산 최초가액</label>
                <input type="number" placeholder="자동 계산됨" disabled />
              </div>
              <div className={styles.inputGroup}>
                <label>리스부채 최초가액</label>
                <input type="number" placeholder="자동 계산됨" disabled />
              </div>
              <div className={styles.inputGroup + " flex-row gap-2 items-center"}>
                <input type="checkbox" id="ifrs16" defaultChecked />
                <label htmlFor="ifrs16">IFRS 16 비면제 대상</label>
              </div>
            </form>

            <div className="flex justify-end gap-3 mt-4">
               <button className="btn-secondary" onClick={() => setIsModalOpen(false)}>취소</button>
               <button className="btn-primary flex items-center gap-2"><ShieldCheck size={18} /> 계약 등록 및 가치평가</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
