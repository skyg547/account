"use client";

import React, { useState } from 'react';
import { 
  Plus, 
  Search, 
  Trash2, 
  FileOutput, 
  Filter, 
  Calendar,
  Package,
  History,
  X
} from 'lucide-react';
import styles from './AssetManagement.module.css';

// [Mock Data] 자산 목록
const mockAssets = [
  { id: 1, code: 'AST-2026-0001', name: '서버 하드웨어 (Core)', acqDate: '2026-01-15', acqCost: 45000000, life: 5, method: 'STRAIGHT_LINE', status: 'ACTIVE', dept: 'IT개발팀' },
  { id: 2, code: 'AST-2026-0002', name: '사무동 업무용 PC (30대)', acqDate: '2026-02-10', acqCost: 36000000, life: 3, method: 'DECLINING_BALANCE', status: 'ACTIVE', dept: '경영지원팀' },
  { id: 3, code: 'AST-2025-0152', name: '업무용 법인차량 (G80)', acqDate: '2025-11-20', acqCost: 65000000, life: 5, method: 'STRAIGHT_LINE', status: 'DISPOSED', dept: '영업본부' },
];

export default function AssetManagementPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className={styles.container}>
      {/* 1. 헤더 및 컨트롤 바 */}
      <header className={styles.header}>
        <div className="titleArea">
          <h2 className="text-2xl font-bold">고정자산 마스터</h2>
          <p className="text-sm text-gray-400">기업이 보유한 고정자산의 원장 관리 및 상각 내역을 추적합니다.</p>
        </div>
        <div className={styles.actionPanel}>
          <button className="icon-btn-secondary"><Filter size={18} /> 필터</button>
          <button className="icon-btn-secondary"><FileOutput size={18} /> 엑셀 내보내기</button>
          <button className="btn-primary flex items-center gap-2" onClick={() => setIsModalOpen(true)}>
            <Plus size={18} /> 신규 자산 등록
          </button>
        </div>
      </header>

      {/* 2. 자산 현황 요약 카드 (상단) */}
      <section className="grid grid-cols-4 gap-4">
        <div className="glass-card p-6 border-l-4 border-blue-500">
          <div className="text-xs text-gray-400 mb-1">총 자산 취득가액</div>
          <div className="text-xl font-bold">1.28 B</div>
        </div>
        <div className="glass-card p-6 border-l-4 border-green-500">
          <div className="text-xs text-gray-400 mb-1">당월 감가상각액</div>
          <div className="text-xl font-bold">12.5 M</div>
        </div>
        <div className="glass-card p-6 border-l-4 border-amber-500">
          <div className="text-xs text-gray-400 mb-1">미상각 잔액</div>
          <div className="text-xl font-bold">845.2 M</div>
        </div>
        <div className="glass-card p-6 border-l-4 border-purple-500">
          <div className="text-xs text-gray-400 mb-1">운용 중인 자산</div>
          <div className="text-xl font-bold">142 건</div>
        </div>
      </section>

      {/* 3. 자산 그리드 영역 */}
      <section className={styles.gridSection}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-semibold flex items-center gap-2"><Package size={18} /> 자산 관리 대장</h3>
          <div className="relative">
            <Search className="absolute left-3 top-2.5 text-gray-500" size={16} />
            <input 
              type="text" 
              placeholder="자산명, 자산코드 검색..." 
              className="pl-10 pr-4 py-2 bg-white/5 border border-white/10 rounded-lg text-sm outline-none focus:border-blue-500" 
            />
          </div>
        </div>

        <div className={styles.tableContainer}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>자산코드</th>
                <th>자산명</th>
                <th>취득일자</th>
                <th>취득원가</th>
                <th>내용연수/방법</th>
                <th>관리부서</th>
                <th>상태</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {mockAssets.map(asset => (
                <tr key={asset.id}>
                  <td className="font-mono text-blue-400">{asset.code}</td>
                  <td>{asset.name}</td>
                  <td>{asset.acqDate}</td>
                  <td className="text-right font-semibold">{asset.acqCost.toLocaleString()}</td>
                  <td>{asset.life}년 / {asset.method === 'STRAIGHT_LINE' ? '정액법' : '정률법'}</td>
                  <td>{asset.dept}</td>
                  <td>
                    <span className={`${styles.statusTag} ${asset.status === 'ACTIVE' ? styles.active : styles.disposed}`}>
                      {asset.status}
                    </span>
                  </td>
                  <td>
                    <div className="flex gap-2">
                       <button title="이력조회" className="p-1 hover:text-blue-400 transition-colors"><History size={16} /></button>
                       <button title="처분/폐기" className="p-1 hover:text-red-400 transition-colors"><Trash2 size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* 4. 신규 등록 모달 */}
      {isModalOpen && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal + " glass-card"}>
            <div className="flex justify-between items-center border-b border-white/10 pb-4">
              <h3 className="text-xl font-bold">신규 고정자산 등록</h3>
              <button onClick={() => setIsModalOpen(false)}><X size={20} /></button>
            </div>
            
            <form className={styles.formGrid}>
              <div className={styles.inputGroup}>
                <label>자산 코드 (자동채번)</label>
                <input type="text" placeholder="AST-2026-XXXX" disabled />
              </div>
              <div className={styles.inputGroup}>
                <label>자산명</label>
                <input type="text" placeholder="자산 명칭 입력" />
              </div>
              <div className={styles.inputGroup}>
                <label>자산 계정</label>
                <select>
                  <option>대기 중...</option>
                  <option>1001-01 기계장치</option>
                  <option>1001-02 비품</option>
                  <option>1001-03 차량운반구</option>
                </select>
              </div>
              <div className={styles.inputGroup}>
                <label>관리 부서</label>
                <select>
                  <option>전체 부서</option>
                  <option>IT개발팀</option>
                  <option>경영지원팀</option>
                </select>
              </div>
              <div className={styles.inputGroup}>
                <label>취득 일자</label>
                <input type="date" />
              </div>
              <div className={styles.inputGroup}>
                <label>취득 원가</label>
                <input type="number" placeholder="0" />
              </div>
              <div className={styles.inputGroup}>
                <label>내용 연수 (년)</label>
                <input type="number" placeholder="5" />
              </div>
              <div className={styles.inputGroup}>
                <label>상각 방법</label>
                <select>
                  <option value="STRAIGHT_LINE">정액법</option>
                  <option value="DECLINING_BALANCE">정률법</option>
                </select>
              </div>
            </form>

            <div className={styles.modalFooter}>
               <button className="btn-secondary" onClick={() => setIsModalOpen(false)}>취소</button>
               <button className="btn-primary px-8">자산 등록 실행</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
