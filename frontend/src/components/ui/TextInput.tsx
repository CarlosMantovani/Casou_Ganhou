import { forwardRef, type InputHTMLAttributes } from 'react';

interface TextInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  helper?: string;
}

export const TextInput = forwardRef<HTMLInputElement, TextInputProps>(function TextInput(
  { error, helper, id, label, className = '', ...props },
  ref,
) {
  const descriptionId = error ? `${id}-error` : helper ? `${id}-helper` : undefined;

  return (
    <div className="space-y-2">
      <label className="block text-sm font-semibold text-charcoal" htmlFor={id}>
        {label}
      </label>
      <input
        aria-describedby={descriptionId}
        aria-invalid={Boolean(error)}
        className={`min-h-12 w-full rounded-lg border bg-white px-4 text-base text-charcoal outline-none transition placeholder:text-warm-gray/70 focus:border-gold focus:ring-2 focus:ring-gold/20 ${
          error ? 'border-terracotta-dark' : 'border-line'
        } ${className}`}
        id={id}
        ref={ref}
        {...props}
      />
      {helper && !error ? (
        <p className="text-xs leading-relaxed text-warm-gray" id={`${id}-helper`}>
          {helper}
        </p>
      ) : null}
      {error ? (
        <p className="text-xs font-medium text-terracotta-dark" id={`${id}-error`} role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
});
