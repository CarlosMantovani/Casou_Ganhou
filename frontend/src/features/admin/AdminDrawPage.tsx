import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, PartyPopper, Sparkles } from 'lucide-react';
import { useState } from 'react';

import { Button } from '../../components/ui/Button';
import { raffleService } from '../../services/raffleService';
import type { ApiError } from '../../types/api';
import type { RaffleDrawResponse } from '../../types/admin';

const REVEAL_DURATION_MS = 4500;
const REVEAL_TICK_MS = 75;

export function AdminDrawPage() {
  const [isConfirming, setIsConfirming] = useState(false);
  const [revealNumber, setRevealNumber] = useState<string | null>(null);

  const resultQuery = useQuery<RaffleDrawResponse, ApiError>({
    queryKey: ['raffle-result'],
    queryFn: raffleService.getResult,
    retry: false,
  });

  const drawMutation = useMutation<RaffleDrawResponse, ApiError>({
    mutationFn: async () => {
      const eligibleNumbers = await raffleService.getEligibleNumbers();
      await runRevealAnimation(eligibleNumbers, setRevealNumber);
      return raffleService.draw();
    },
    onMutate: () => {
      setRevealNumber(null);
    },
    onSuccess: () => {
      setIsConfirming(false);
      void resultQuery.refetch();
    },
    onSettled: () => {
      setRevealNumber(null);
    },
  });

  const result = drawMutation.data ?? resultQuery.data;
  const drawError = drawMutation.error;
  const isRevealing = drawMutation.isPending;

  return (
    <main className="min-h-screen bg-[#1B1714] px-6 py-8 text-white">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-3xl flex-col">
        <a className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-white/70 hover:text-white" href="/admin">
          <ArrowLeft aria-hidden="true" className="h-4 w-4" />
          Voltar ao painel
        </a>

        <section className="grid flex-1 place-items-center text-center">
          <div className="w-full max-w-xl">
            <p className="font-serif text-2xl italic text-gold">Presente Premiado</p>

            {isRevealing ? (
              <RevealStage number={revealNumber} />
            ) : result ? (
              <WinnerResult result={result} />
            ) : (
              <div className="mt-10">
                <h1 className="font-serif text-6xl font-bold leading-none">
                  Grande <span className="block text-gold">Sorteio</span>
                </h1>
                <div className="mx-auto mt-10 grid h-48 w-48 place-items-center rounded-full border border-gold/35">
                  <div className="grid h-32 w-32 place-items-center rounded-full border border-gold/20">
                    <Sparkles aria-hidden="true" className="h-16 w-16 text-gold" />
                  </div>
                </div>

                {resultQuery.isLoading ? <p className="mt-8 text-sm text-white/60">Consultando resultado...</p> : null}

                {drawError ? (
                  <p className="mx-auto mt-6 max-w-sm rounded-lg border border-gold/20 bg-white/5 px-4 py-3 text-sm text-white/80" role="alert">
                    {drawError.status === 409
                      ? 'Ainda nao ha numeros aprovados suficientes para realizar o sorteio.'
                      : 'Nao foi possivel realizar o sorteio agora.'}
                  </p>
                ) : null}
              </div>
            )}

            {!isRevealing && !resultQuery.isLoading ? (
              <div className="mx-auto mt-10 max-w-sm">
                <Button onClick={() => setIsConfirming(true)} type="button">
                  {result ? 'Sortear novamente' : 'Sortear vencedor'}
                </Button>
              </div>
            ) : null}

            {!isRevealing && result && drawError ? (
              <p className="mx-auto mt-6 max-w-sm rounded-lg border border-gold/20 bg-white/5 px-4 py-3 text-sm text-white/80" role="alert">
                {drawError.status === 409
                  ? 'Ainda nao ha numeros aprovados suficientes para realizar o sorteio.'
                  : 'Nao foi possivel realizar o sorteio agora.'}
              </p>
            ) : null}
          </div>
        </section>
      </div>

      {isConfirming ? (
        <div className="fixed inset-0 grid place-items-center bg-black/60 px-6">
          <div className="w-full max-w-sm rounded-lg bg-cream p-6 text-charcoal shadow-soft">
            <h2 className="font-serif text-2xl font-bold">Confirmar sorteio?</h2>
            <p className="mt-3 text-sm leading-relaxed text-warm-gray">
              A tela vai passar pelos numeros concorrentes antes de revelar o vencedor.
            </p>
            <div className="mt-6 flex gap-3">
              <button
                className="min-h-11 flex-1 rounded-lg border border-[#DDD2CB] px-4 py-2 text-sm font-semibold"
                onClick={() => setIsConfirming(false)}
                type="button"
              >
                Cancelar
              </button>
              <button
                className="min-h-11 flex-1 rounded-lg bg-gold px-4 py-2 text-sm font-bold text-charcoal disabled:opacity-60"
                disabled={drawMutation.isPending}
                onClick={() => drawMutation.mutate()}
                type="button"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

function RevealStage({ number }: { number: string | null }) {
  return (
    <div className="mt-16">
      <p className="text-sm font-bold uppercase tracking-[0.28em] text-gold">
        <Sparkles aria-hidden="true" className="mr-2 inline h-4 w-4" />
        Sorteando entre os numeros
      </p>
      <div className="mx-auto mt-8 grid h-52 w-52 place-items-center rounded-full border border-gold/35 bg-white/5 shadow-[0_0_60px_rgba(201,162,39,0.16)]">
        <span className="font-serif text-6xl font-bold text-gold drop-shadow-[0_0_30px_rgba(201,162,39,0.35)]">
          {number ?? '-----'}
        </span>
      </div>
      <p className="mt-8 text-sm font-semibold uppercase tracking-[0.28em] text-white/55">Preparando a revelacao</p>
    </div>
  );
}

function WinnerResult({ result }: { result: RaffleDrawResponse }) {
  return (
    <div className="mt-16">
      <p className="text-sm font-bold uppercase tracking-[0.28em] text-gold">
        <PartyPopper aria-hidden="true" className="mr-2 inline h-4 w-4" />
        Numero vencedor
      </p>
      <h1 className="mt-8 font-serif text-[clamp(5rem,18vw,11rem)] font-bold leading-none text-gold drop-shadow-[0_0_30px_rgba(201,162,39,0.35)]">
        {result.winningNumber}
      </h1>
      <p className="mt-8 font-serif text-4xl font-bold">{result.winnerName}</p>
      <p className="mt-3 text-sm text-white/50">
        Sorteado em {new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(result.drawnAt))}
      </p>
    </div>
  );
}

function runRevealAnimation(numbers: string[], setNumber: (number: string) => void) {
  if (numbers.length === 0) return Promise.resolve();

  return new Promise<void>((resolve) => {
    let index = 0;
    setNumber(numbers[index]);

    const intervalId = window.setInterval(() => {
      index = (index + 1) % numbers.length;
      setNumber(numbers[index]);
    }, REVEAL_TICK_MS);

    window.setTimeout(() => {
      window.clearInterval(intervalId);
      resolve();
    }, REVEAL_DURATION_MS);
  });
}
