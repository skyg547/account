import React from 'react';
import styles from './Footer.module.css';

/**
 * [시스템 하단 푸터]
 * 저작권 정보 및 현재 실행 환경(DEV/PROD)을 표시합니다.
 */
export default function Footer() {
  const isDev = process.env.NODE_ENV === 'development';

  return (
    <footer className={styles.footer}>
      <div className={styles.left}>
        <p>&copy; 2026 Account.AI Project. All Rights Reserved.</p>
      </div>
      
      <div className={styles.right}>
        <div className={styles.envTag}>
          <span className={styles.dot}></span>
          <span className={styles.envText}>SYSTEM STATUS: </span>
          <span className={isDev ? styles.dev : styles.prod}>
            {isDev ? 'DEVELOPMENT' : 'PRODUCTION'}
          </span>
        </div>
      </div>
    </footer>
  );
}
