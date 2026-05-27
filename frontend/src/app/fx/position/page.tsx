"use client";

import React, { useState, useEffect } from 'react';
import styles from './FxPosition.module.css';
import { fxService, FxDashboardData } from '@/services/fxService';

export default function FxPositionPage() {
  const [data, setData] = useState<FxDashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const fetchFxData = async () => {
      try {
        const res = await fxService.getDashboardData();
        if (active) {
          setData(res);
          setLoading(false);
        }
      } catch (e) {
        console.error(e);
        if (active) setLoading(false);
      }
    };
    fetchFxData();
    return () => { active = false; };
  }, []);

  const formatCurrency = (val: number) => new Intl.NumberFormat('en-US').format(val);

  if (loading || !data) {
    return (
      <div className={styles.container}>
        <div className="flex justify-center items-center py-20 text-slate-500 font-bold">
          Loading FX Position Data...
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>외환(FX) 포지션 관리</h2>
          <p>통화별 보유 자원 및 리스크 포지션을 실시간으로 모니터링합니다.</p>
        </div>
        <div className={styles.exchangeRate}>
          {data.rates.map((r, idx) => (
            <div key={idx} className={styles.rateItem}>
              <span>{r.pair}</span>
              <strong>{r.rate.toFixed(2)}</strong>
              <span className={r.changeAmount >= 0 ? styles.up : styles.down}>
                {r.changeAmount >= 0 ? '+' : ''}{r.changeAmount} ({r.changePercent >= 0 ? '+' : ''}{r.changePercent}%)
              </span>
            </div>
          ))}
        </div>
      </header>

      <div className={styles.positionSummary}>
        <div className={styles.card + " glass-card"}>
          <h4>전체 외화 순포지션</h4>
          <h3>${formatCurrency(data.totalNetPosition)}</h3>
          <p className={styles.subText}>환산금액 ₩{formatCurrency(data.totalKrwAmount)}</p>
        </div>
        <div className={styles.card + " glass-card"}>
          <h4>당일 환평가 손익</h4>
          <h3 className={data.dailyValuationGainLoss >= 0 ? styles.up : styles.down}>
            {data.dailyValuationGainLoss >= 0 ? '+' : ''}₩{formatCurrency(data.dailyValuationGainLoss)}
          </h3>
          <p className={styles.subText}>전일 대비 {data.gainLossPercent}% 상승</p>
        </div>
      </div>

      <section className={styles.gridSection + " glass-card"}>
        <h3>통화별 상세 포지션</h3>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>통화</th>
              <th>장부금액 (외화)</th>
              <th>평균단가</th>
              <th>평가금액 (KRW)</th>
              <th>평가손익</th>
              <th>리밋 준수</th>
            </tr>
          </thead>
          <tbody>
            {data.positions.map((p, idx) => (
              <tr key={idx}>
                <td><span className={styles.currencyCode}>{p.currencyCode}</span> {p.currencyName}</td>
                <td className={styles.bold}>{p.currencyCode === 'JPY' ? '¥' : '$'}{formatCurrency(p.foreignAmount)}</td>
                <td>{p.averageRate.toFixed(2)}</td>
                <td>₩{formatCurrency(p.krwAmount)}</td>
                <td className={p.valuationGainLoss >= 0 ? styles.up : styles.down}>
                  {p.valuationGainLoss >= 0 ? '+' : ''}₩{formatCurrency(p.valuationGainLoss)}
                </td>
                <td>
                  <span className={p.limitStatus === 'SAFE' ? styles.statusOk : styles.statusWarning}>
                    {p.limitStatus}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
