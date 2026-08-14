import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

type ButtonVariant = 'primary' | 'secondary';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  isLoading?: boolean;
  variant?: ButtonVariant;
}

const variants: Record<ButtonVariant, string> = {
  primary:
    'bg-terracotta text-white shadow-button hover:bg-terracotta-dark focus-visible:outline-terracotta',
  secondary:
    'border border-terracotta bg-transparent text-terracotta hover:bg-blush focus-visible:outline-terracotta',
};

export function Button({ children, className = '', disabled, isLoading = false, variant = 'primary', ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-lg px-5 py-3 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 ${variants[variant]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" /> : null}
      {children}
    </button>
  );
}
