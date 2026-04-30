import React from 'react';
import { Settings2, Plus, Code, Play, CheckCircle } from 'lucide-react';
import styles from './JournalRules.module.css';

/**
 * [자동 분개 설정 화면]
 * 외부 시스템에서 유입되는 이벤트에 대해 자동으로 분개 전표를 생성하는 규칙을 정의합니다.
 * 설계서 파트 2-④ 기반.
 */
export default function JournalRulesPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>자동 분개 룰 설정</h2>
          <p>비즈니스 이벤트 발생 시 전표가 생성되는 규칙을 관리합니다.</p>
        </div>
        <button className={styles.addBtn}><Plus size={18} /> 신규 규칙 생성</button>
      </header>

      <div className={styles.mainLayout}>
        {/* 룰 목록 */}
        <aside className={`glass-card ${styles.ruleListSection}`}>
          <h3>등록된 룰 목록</h3>
          <div className={styles.ruleItems}>
            <div className={`${styles.ruleItem} ${styles.active}`}>
              <h4>매입 세금계산서 확정</h4>
              <span>Event: PURCHASE_INVOICE_CONFIRMED</span>
            </div>
            <div className={styles.ruleItem}>
              <h4>매출 채권 수금</h4>
              <span>Event: SALES_RECEIPT_COLLECTED</span>
            </div>
            <div className={styles.ruleItem}>
              <h4>법인카드 승인 내역</h4>
              <span>Event: CARD_APPROVE_SYNCCED</span>
            </div>
          </div>
        </aside>

        {/* 룰 상세 편집 (Condition Builder Interface) */}
        <main className={`glass-card ${styles.detailEditor}`}>
          <div className={styles.editorHeader}>
            <div className={styles.ruleTitle}>
              <Settings2 size={20} />
              <h3>매입 세금계산서 확정 룰 편집</h3>
            </div>
            <div className={styles.editorActions}>
              <button className={styles.testBtn}><Play size={14} /> 룰 테스트</button>
              <button className={styles.saveBtn}>설정 저장</button>
            </div>
          </div>

          <div className={styles.editorBody}>
            {/* 조건부 (Condition) Section */}
            <div className={styles.section}>
              <div className={styles.sectionHeader}>
                <Code size={16} /> <h4>실행 조건 (SpEL Conditions)</h4>
              </div>
              <div className={styles.conditionField}>
                <code>#amount &gt; 1000000 &amp;&amp; #vendorCode.startsWith(&apos;BP-&apos;)</code>
                <p className={styles.hint}>금액이 1,000,000원 이상이고 거래처 코드가 &apos;BP-&apos;로 시작하는 경우</p>
              </div>
            </div>

            {/* 분개 템플릿 (Template) Section */}
            <div className={styles.section}>
              <div className={styles.sectionHeader}>
                <CheckCircle size={16} /> <h4>자동 분개 템플릿</h4>
              </div>
              <table className={styles.templateTable}>
                <thead>
                  <tr>
                    <th>차/대</th>
                    <th>계정 과목</th>
                    <th>금액 수식</th>
                    <th>적요 템플릿</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td><span className={styles.drBadge}>차변</span></td>
                    <td>5101 상품매입</td>
                    <td><code>#amount</code></td>
                    <td><code>[매입] ${'{#vendorName}'}</code></td>
                  </tr>
                  <tr>
                    <td><span className={styles.crBadge}>대변</span></td>
                    <td>2101 외상매입금</td>
                    <td><code>#amount</code></td>
                    <td><code>[채무] ${'{#vendorName}'}</code></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
