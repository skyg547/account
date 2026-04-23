"use client";

import React, { useState } from 'react';
import { 
  Play, 
  RotateCcw, 
  CheckCircle2, 
  Clock, 
  AlertCircle,
  Calendar,
  Layers,
  Database,
  ArrowRight
} from 'lucide-react';
import styles from './AssetClosing.module.css';

export default function AssetClosingPage() {
  const [processDate, setProcessDate] = useState('2026-04-30');
  const [assetStatus, setAssetStatus] = useState('READY'); // READY, RUNNING, DONE
  const [leaseStatus, setLeaseStatus] = useState('READY');

  const runAssetDepreciation = () => {
    setAssetStatus('RUNNING');
    setTimeout(() => setAssetStatus('DONE'), 2500);
  };

  const runLeaseClosing = () => {
    setLeaseStatus('RUNNING');
    setTimeout(() => setLeaseStatus('DONE'), 3000);
  };

  return (
    <div className={styles.container}>
      <header className="flex justify-between items-end border-b border-white/10 pb-6">
        <div className="titleArea">
          <h2 className="text-2xl font-bold">자산/리스 결산 컨트롤 타워</h2>
          <p className="text-sm text-gray-400">월말 감가상각 및 리스크 회계 결산 프로세스를 통합 제어합니다.</p>
        </div>
        <div className="flex gap-4 items-center bg-white/5 p-4 rounded-2xl border border-white/10">
          <div className="flex flex-col">
            <span className="text-[10px] text-gray-500 uppercase font-bold">결산 대상 년월</span>
            <div className="flex items-center gap-2 font-bold text-blue-400">
               <Calendar size={16} />
               <input 
                 type="date" 
                 value={processDate} 
                 onChange={(e) => setProcessDate(e.target.value)}
                 className="bg-transparent border-none outline-none text-blue-400 cursor-pointer"
               />
            </div>
          </div>
        </div>
      </header>

      <main className={styles.processGrid}>
        {/* 1. 고정자산 감가상각 */}
        <div className={styles.processCard + " glass-card"}>
          <div className={`${styles.iconWrapper} ${styles.assetIcon}`}>
            <Layers size={28} />
          </div>
          <div>
            <h3 className="text-lg font-bold">고정자산 감가상각 실행</h3>
            <p className="text-xs text-gray-500 mt-1">등록된 모든 활성 자산에 대해 월할 감가상각비를 자동 계상하고 전표를 생성합니다.</p>
          </div>
          
          <div className="flex-1 py-4">
             <div className="flex justify-between text-sm mb-2">
                <span>대상 자산 수</span>
                <span className="font-bold">142 건</span>
             </div>
             <div className="flex justify-between text-sm">
                <span>예상 상각 총액</span>
                <span className="font-bold">12,450,000 KRW</span>
             </div>
          </div>

          <div className={styles.statusArea}>
            <div className={`${styles.dot} ${assetStatus === 'DONE' ? styles.activeDot : ''}`}></div>
            <span>{assetStatus === 'READY' ? '대기 중' : assetStatus === 'RUNNING' ? '처리 중...' : '결산 완료 (04.23 15:40)'}</span>
          </div>

          <button 
            className={`${styles.actionBtn} ${styles.assetBtn}`}
            onClick={runAssetDepreciation}
            disabled={assetStatus === 'RUNNING'}
          >
            {assetStatus === 'RUNNING' ? <RotateCcw className="animate-spin" size={20} /> : <Play size={20} />}
            상각 프로세스 실행
          </button>
        </div>

        {/* 2. IFRS 16 리스 결산 */}
        <div className={styles.processCard + " glass-card"}>
          <div className={`${styles.iconWrapper} ${styles.leaseIcon}`}>
             <Database size={28} />
          </div>
          <div>
            <h3 className="text-lg font-bold">IFRS 16 리스 회계 결산</h3>
            <p className="text-xs text-gray-500 mt-1">리스 계약별 사용권자산 상각 및 리스부채 이자 비용을 산출하여 기말 잔액을 확정합니다.</p>
          </div>

          <div className="flex-1 py-4">
             <div className="flex justify-between text-sm mb-2">
                <span>대상 리스 계약</span>
                <span className="font-bold">28 건</span>
             </div>
             <div className="flex justify-between text-sm">
                <span>이자비용 추정액</span>
                <span className="font-bold">4,280,000 KRW</span>
             </div>
          </div>

          <div className={styles.statusArea}>
             <div className={`${styles.dot} ${leaseStatus === 'DONE' ? styles.activeDot : ''}`}></div>
             <span>{leaseStatus === 'READY' ? '대기 중' : leaseStatus === 'RUNNING' ? '처리 중...' : '결산 완료 (04.23 15:42)'}</span>
          </div>

          <button 
            className={`${styles.actionBtn} ${styles.leaseBtn}`}
            onClick={runLeaseClosing}
            disabled={leaseStatus === 'RUNNING'}
          >
            {leaseStatus === 'RUNNING' ? <RotateCcw className="animate-spin" size={20} /> : <Play size={20} />}
            리스 결산 프로세스 실행
          </button>
        </div>
      </main>

      {/* 3. 최근 결산 이력 */}
      <section className={styles.historySection}>
        <h4 className="text-sm font-bold mb-4 flex items-center gap-2">
           <Clock size={16} className="text-gray-500" /> 최근 결산 수행 이력
        </h4>
        <div className="flex flex-col gap-1">
           {[
             { date: '2026-03-31', type: '고정자산 상각', count: 138, amount: '12,100,000', status: 'SUCCESS' },
             { date: '2026-03-31', type: '리스 회계 결산', count: 26, amount: '4,150,000', status: 'SUCCESS' },
             { date: '2026-02-28', type: '정기 결산 통합', count: 160, amount: '16,200,000', status: 'SUCCESS' }
           ].map((item, i) => (
             <div key={i} className={styles.historyItem}>
                <div className="flex gap-4 items-center">
                   <div className="text-xs text-gray-500 font-mono">{item.date}</div>
                   <div className="text-sm font-medium">{item.type}</div>
                   <div className="text-[10px] bg-white/5 px-2 py-0.5 rounded border border-white/10">{item.count}건</div>
                </div>
                <div className="flex items-center gap-6">
                   <div className="text-sm font-bold">{item.amount} KRW</div>
                   <div className="text-green-400"><CheckCircle2 size={16} /></div>
                </div>
             </div>
           ))}
        </div>
        <button className="w-full mt-4 py-2 text-xs text-gray-500 flex justify-center items-center gap-2 hover:text-white transition-colors">
          전체 이력 보기 <ArrowRight size={14} />
        </button>
      </section>

      {/* 4. 주의 사항 */}
      <div className="bg-amber-500/5 border border-amber-500/20 p-4 rounded-xl flex gap-3 text-amber-500">
         <AlertCircle size={20} />
         <div className="text-xs">
            <p className="font-bold">결산 실행 전 확인 사항</p>
            <p className="mt-1 opacity-80">당월 취득 또는 처분된 자산의 정보가 모두 확정되었는지 확인하십시오. 결산 완료 후 전표가 생성되면 마스터 정보 수정이 제한됩니다.</p>
         </div>
      </div>
    </div>
  );
}
