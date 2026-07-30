import type { HTMLAttributes, ReactNode } from 'react';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ children, className = '', ...props }: CardProps) {
  return (
    <section className={`rounded-lg bg-white p-6 shadow-soft ${className}`} {...props}>
      {children}
    </section>
  );
}
