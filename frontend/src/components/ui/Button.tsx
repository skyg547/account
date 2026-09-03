'use client';

import React from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'outline' | 'ghost' | 'success';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  isLoading?: boolean;
  loadingText?: string;
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: React.ElementType;
  iconPosition?: 'left' | 'right';
  fullWidth?: boolean;
}

const variantStyles: Record<ButtonVariant, string> = {
  primary:
    'bg-[#4262ff] hover:bg-[#3452e6] active:bg-[#2b44d4] text-white shadow-md shadow-blue-600/20 border border-transparent',
  secondary:
    'bg-[#f7f8fb] hover:bg-white dark:bg-slate-800 dark:hover:bg-slate-700 text-[#17191e] dark:text-slate-100 border border-[#eaedf4] dark:border-slate-700',
  danger:
    'bg-rose-600 hover:bg-rose-500 active:bg-rose-700 text-white shadow-md shadow-rose-600/20 border border-transparent',
  outline:
    'bg-transparent hover:bg-slate-50 dark:hover:bg-slate-800 text-[#545b69] dark:text-slate-300 hover:text-[#17191e] dark:hover:text-white border border-[#eaedf4] dark:border-slate-700',
  ghost:
    'bg-transparent hover:bg-slate-100 dark:hover:bg-slate-800 text-[#545b69] dark:text-slate-300 hover:text-[#17191e] dark:hover:text-white border border-transparent',
  success:
    'bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white shadow-md shadow-emerald-600/20 border border-transparent',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'py-1.5 px-3 text-xs rounded-lg gap-1.5',
  md: 'py-2.5 px-4 text-xs font-bold rounded-xl gap-2',
  lg: 'py-3.5 px-6 text-sm font-bold rounded-2xl gap-2.5',
};

export default function Button({
  children,
  isLoading = false,
  loadingText,
  disabled = false,
  variant = 'primary',
  size = 'md',
  icon: Icon,
  iconPosition = 'left',
  fullWidth = false,
  className = '',
  type = 'button',
  ...props
}: ButtonProps) {
  const isDisabled = disabled || isLoading;

  return (
    <button
      type={type}
      disabled={isDisabled}
      aria-busy={isLoading}
      className={`inline-flex items-center justify-center font-bold transition-all duration-200 select-none ${
        fullWidth ? 'w-full' : ''
      } ${sizeStyles[size]} ${variantStyles[variant]} ${
        isDisabled ? 'opacity-60 cursor-not-allowed pointer-events-none' : 'cursor-pointer active:scale-[0.99]'
      } ${className}`}
      {...props}
    >
      {isLoading ? (
        <>
          <Loader2 className="w-4 h-4 animate-spin shrink-0" />
          <span>{loadingText || children}</span>
        </>
      ) : (
        <>
          {Icon && iconPosition === 'left' && <Icon size={size === 'sm' ? 14 : size === 'lg' ? 18 : 16} className="shrink-0" />}
          <span>{children}</span>
          {Icon && iconPosition === 'right' && <Icon size={size === 'sm' ? 14 : size === 'lg' ? 18 : 16} className="shrink-0" />}
        </>
      )}
    </button>
  );
}
