import React from 'react';
import Link from 'next/link';
import { 
  LayoutDashboard, 
  FileText, 
  BookOpen, 
  Settings, 
  BarChart3, 
  Wallet,
  Activity
} from 'lucide-react';
import styles from './Sidebar.module.css';

const menuItems = [
  { icon: LayoutDashboard, label: '대시보드', href: '/' },
  { icon: FileText, label: '전표 관리', href: '/journal' },
  { icon: BookOpen, label: '원장 조회', href: '/ledger' },
  { icon: Wallet, label: '자산/리스', href: '/assets' },
  { icon: BarChart3, label: '결산/보고서', href: '/reports' },
  { icon: Activity, label: '시스템 모니터링', href: '/monitor' },
];

export default function Sidebar() {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.logo}>
        <div className={styles.logoIcon}>A</div>
        <span className={styles.logoText}>Account.AI</span>
      </div>
      
      <nav className={styles.nav}>
        <ul className={styles.menuList}>
          {menuItems.map((item) => (
            <li key={item.href} className={styles.menuItem}>
              <Link href={item.href} className={styles.menuLink}>
                <item.icon size={20} className={styles.icon} />
                <span>{item.label}</span>
              </Link>
            </li>
          ))}
        </ul>
      </nav>
      
      <div className={styles.footer}>
        <Link href="/settings" className={styles.settingsLink}>
          <Settings size={20} />
          <span>환경 설정</span>
        </Link>
      </div>
    </aside>
  );
}
