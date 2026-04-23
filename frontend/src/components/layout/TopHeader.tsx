"use client";

import React from 'react';
import { Bell, Search, User, Moon, Sun, LayoutGrid } from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';
import styles from './TopHeader.module.css';

/**
 * [최상단 시스템 헤더]
 * 시스템 이름, 글로벌 메뉴, 로그인 세션 정보를 통합 관리하는 최상위 바입니다.
 */
export default function TopHeader() {
  const { activeCategory, setActiveCategory } = useNav();

  const handleCategoryClick = (category: NavCategory) => {
    setActiveCategory(category);
  };

  return (
    <header className={styles.header}>
      {/* 1. 좌측: 시스템 로고 및 명칭 */}
      <div className={styles.logoArea}>
        <div className={styles.iconBox}>
          <LayoutGrid size={24} color="#fff" />
        </div>
        <div className={styles.systemName}>
          <h1>Account.AI</h1>
          <span>Modern Enterprise Resource Planning</span>
        </div>
      </div>

      {/* 2. 중앙: 글로벌 메뉴 바 (카테고리 필터) */}
      <nav className={styles.menuBar}>
        <button 
          className={activeCategory === 'DASHBOARD' ? styles.active : ''} 
          onClick={() => handleCategoryClick('DASHBOARD')}
        >대시보드</button>
        <button 
          className={activeCategory === 'ACCOUNTING' ? styles.active : ''} 
          onClick={() => handleCategoryClick('ACCOUNTING')}
        >재무업무</button>
        <button 
          className={activeCategory === 'OPERATIONS' ? styles.active : ''} 
          onClick={() => handleCategoryClick('OPERATIONS')}
        >재무운영</button>
        <button 
          className={activeCategory === 'BANKING' ? styles.active : ''} 
          onClick={() => handleCategoryClick('BANKING')}
        >은행업무</button>
        <button 
          className={activeCategory === 'RISK' ? styles.active : ''} 
          onClick={() => handleCategoryClick('RISK')}
        >리스크관리</button>
        <button 
          className={activeCategory === 'MASTER' ? styles.active : ''} 
          onClick={() => handleCategoryClick('MASTER')}
        >기준관리</button>
        <button 
          className={activeCategory === 'ADMIN' ? styles.active : ''} 
          onClick={() => handleCategoryClick('ADMIN')}
        >시스템관리</button>
      </nav>

      {/* 3. 우측: 검색 및 로그인 정보 영역 */}
      <div className={styles.rightArea}>
        <div className={styles.searchBox}>
          <Search size={16} />
          <input type="text" placeholder="Global Search..." />
        </div>
        
        <div className={styles.actionIcons}>
          <button className={styles.iconBtn}><Moon size={20} /></button>
          <button className={styles.iconBtn}>
            <Bell size={20} />
            <span className={styles.notiBadge} />
          </button>
        </div>

        <div className={styles.userInfo}>
          <div className={styles.userText}>
            <span className={styles.userName}>김재무 팀장</span>
            <span className={styles.userDept}>재무회계본부</span>
          </div>
          <div className={styles.avatar}>
            <User size={20} />
          </div>
        </div>
      </div>
    </header>
  );
}
