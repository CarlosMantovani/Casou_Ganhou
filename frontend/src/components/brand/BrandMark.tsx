export function BrandMark() {
  return (
    <div className="text-center">
      <svg aria-hidden="true" className="mx-auto mb-4 h-16 w-28" viewBox="0 0 130 82" fill="none">
        <circle cx="45" cy="41" r="30" stroke="#B8935A" strokeWidth="5.5" fill="none" opacity="0.88" />
        <circle cx="85" cy="41" r="30" stroke="#B8935A" strokeWidth="5.5" fill="none" opacity="0.88" />
        <path d="M65 15 L69 23 L65 31 L61 23 Z" fill="#B8935A" opacity="0.72" />
      </svg>
      <p className="font-serif text-3xl font-bold leading-tight text-charcoal">
        Presente <span className="italic text-terracotta">Premiado</span>
      </p>
    </div>
  );
}

export function GoldDivider() {
  return (
    <div aria-hidden="true" className="mx-auto flex max-w-56 items-center gap-3">
      <div className="h-px flex-1 bg-gradient-to-r from-transparent to-gold/60" />
      <div className="h-1.5 w-1.5 rotate-45 bg-gold" />
      <div className="h-px flex-1 bg-gradient-to-l from-transparent to-gold/60" />
    </div>
  );
}
