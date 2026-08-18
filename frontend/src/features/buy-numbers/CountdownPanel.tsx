import { useEffect, useState } from 'react';

import { Card } from '../../components/ui/Card';
import { getCountdownParts } from '../../utils/dateTime';

export function CountdownPanel({ scheduledDrawAt }: { scheduledDrawAt: string | null }) {
  const [, setTick] = useState(0);

  useEffect(() => {
    if (!scheduledDrawAt) return undefined;

    const intervalId = window.setInterval(() => setTick((current) => current + 1), 1000);
    return () => window.clearInterval(intervalId);
  }, [scheduledDrawAt]);

  if (!scheduledDrawAt) return null;

  const countdown = getCountdownParts(scheduledDrawAt);

  return (
    <Card className="bg-charcoal text-center text-white shadow-none">
      <p className="text-xs font-bold uppercase tracking-wide text-gold">Contagem para o sorteio</p>
      <div className="mt-4 grid grid-cols-4 gap-2">
        <CountdownItem label="Dias" value={countdown.days} />
        <CountdownItem label="Horas" value={countdown.hours} />
        <CountdownItem label="Min." value={countdown.minutes} />
        <CountdownItem label="Seg." value={countdown.seconds} />
      </div>
    </Card>
  );
}

function CountdownItem({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-white/10 px-2 py-3">
      <span className="block font-serif text-2xl font-bold leading-none">{String(value).padStart(2, '0')}</span>
      <span className="mt-1 block text-[11px] font-semibold uppercase text-white/60">{label}</span>
    </div>
  );
}
