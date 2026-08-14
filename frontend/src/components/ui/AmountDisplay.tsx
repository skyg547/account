import React from 'react';

interface AmountDisplayProps {
  amount: number;
  currency?: string;
  className?: string;
  showSign?: boolean;
}

export default function AmountDisplay({ amount, currency = '₩', className = '', showSign = false }: AmountDisplayProps) {
  const isNegative = amount < 0;
  const formattedAmount = Math.abs(amount).toLocaleString();
  const sign = isNegative ? '-' : (showSign && amount > 0 ? '+' : '');

  return (
    <span className={`font-mono font-bold tracking-tighter ${isNegative ? 'text-rose-400' : 'text-white'} ${className}`}>
      <span className="text-[10px] text-slate-500 mr-1">{currency}</span>
      {sign}{formattedAmount}
    </span>
  );
}
