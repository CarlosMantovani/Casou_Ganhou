export function toDateTimeLocalValue(value: string | null) {
  if (!value) return '';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

export function fromDateTimeLocalValue(value: string) {
  return new Date(value).toISOString();
}

export function getCountdownParts(target: string, now = new Date()) {
  const diffMs = getCountdownMs(target, now);
  const totalSeconds = Math.floor(diffMs / 1000);

  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;

  return { days, hours, minutes, seconds };
}

export function getCountdownMs(target: string, now = new Date()) {
  const targetDate = new Date(target);
  if (Number.isNaN(targetDate.getTime())) return 0;

  return Math.max(0, targetDate.getTime() - now.getTime());
}

export function isPastDateTime(value: string | null, now = new Date()) {
  if (!value) return false;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return false;

  return date.getTime() <= now.getTime();
}

export function isValidDateTime(value: string) {
  return !Number.isNaN(new Date(value).getTime());
}
