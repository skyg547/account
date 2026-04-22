import React from 'react';
import { Bell, Search, User, Moon, Sun } from 'lucide-react';
import styles from './Navbar.module.css';

export default function Navbar() {
  return (
    <header className={styles.navbar}>
      <div className={styles.searchBar}>
        <Search size={18} className={styles.searchIcon} />
        <input type="text" placeholder="통합 검색 (전표, 거래처, 계정...)" className={styles.searchInput} />
      </div>
      
      <div className={styles.actions}>
        <button className={styles.iconBtn}>
          <Moon size={20} />
        </button>
        <button className={styles.iconBtn}>
          <Bell size={20} />
          <span className={styles.badge} />
        </button>
        <div className={styles.profile}>
          <div className={styles.avatar}>
            <User size={20} />
          </div>
          <div className={styles.userInfo}>
            <span className={styles.userName}>김재무 팀장</span>
            <span className={styles.userRole}>Master Admin</span>
          </div>
        </div>
      </div>
    </header>
  );
}
