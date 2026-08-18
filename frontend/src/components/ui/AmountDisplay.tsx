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
    <span className={`font-mono font-bold tracking-tight ${isNegative ? 'text-rose-600' : 'text-[#17191e]'} ${className}`}>
      <span className="text-xs text-[#8c94a4] font-medium mr-1">{currency}</span>
      {sign}{formattedAmount}
    </span>
  );
}
