import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, Check, Gift, Loader2 } from 'lucide-react';
import type { ReactNode } from 'react';

import { BrandMark, GoldDivider } from '../../components/brand/BrandMark';
import { Card } from '../../components/ui/Card';
import { publicMessages } from '../../content/messages';
import { transactionService } from '../../services/transactionService';

function getExternalReference(searchParams: URLSearchParams) {
  return searchParams.get('external_reference') ?? searchParams.get('externalReference') ?? '';
}

export function PaymentReturnPage() {
  const searchParams = new URLSearchParams(window.location.search);
  const externalReference = getExternalReference(searchParams);

  const statusQuery = useQuery({
    enabled: Boolean(externalReference),
    queryKey: ['transaction-status', externalReference],
    queryFn: () => transactionService.getStatus(externalReference),
    refetchInterval: (query) => (query.state.data?.status === 'PENDING' ? 5000 : false),
  });

  if (!externalReference) {
    return <PaymentState title="Não foi possível localizar sua compra" message={publicMessages.missingReference} tone="error" />;
  }

  if (statusQuery.isLoading) {
    return (
      <PaymentState
        icon={<Loader2 aria-hidden="true" className="h-10 w-10 animate-spin text-terracotta" />}
        message="Estamos confirmando o status real do seu pagamento."
        title="Consultando pagamento"
        tone="neutral"
      />
    );
  }

  if (statusQuery.isError || !statusQuery.data) {
    return <PaymentState title="Não foi possível confirmar o pagamento" message={publicMessages.genericError} tone="error" />;
  }

  const transaction = statusQuery.data;

  if (transaction.status === 'APPROVED' && transaction.luckyNumbers.length > 0) {
    return (
      <main className="min-h-screen bg-cream px-6 pb-16 pt-10 text-charcoal">
        <div className="mx-auto flex w-full max-w-[440px] flex-col gap-6 text-center">
          <div>
            <Gift aria-hidden="true" className="mx-auto mb-5 h-14 w-14 text-terracotta" />
            <h1 className="font-serif text-4xl font-bold leading-tight">
              Muito obrigado pela <span className="italic text-terracotta">sua gentileza!</span>
            </h1>
            <p className="mx-auto mt-4 max-w-xs text-sm leading-relaxed text-warm-gray">
              Sua participação foi confirmada. Boa sorte no sorteio!
            </p>
            <div className="mt-6">
              <GoldDivider />
            </div>
          </div>

          <Card className="border border-gold/30 text-center">
            <h2 className="font-serif text-lg font-semibold">Seus números da sorte</h2>
            <div className="mt-5 flex flex-wrap justify-center gap-3">
              {transaction.luckyNumbers.map((number) => (
                <span className="rounded-lg bg-gold px-4 py-2 text-sm font-bold text-charcoal shadow-sm" key={number}>
                  {number}
                </span>
              ))}
            </div>
          </Card>

          <Card className="border border-[#EEE6DF] bg-cream text-left shadow-none">
            <div className="flex gap-4">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-blush text-terracotta">
                <Check aria-hidden="true" className="h-5 w-5" />
              </span>
              <div>
                <h2 className="text-sm font-bold">Confirmação enviada por e-mail</h2>
                <p className="mt-1 text-sm leading-relaxed text-warm-gray">
                  Seus números também foram enviados para o e-mail informado na compra.
                </p>
              </div>
            </div>
          </Card>

          <p className="font-serif text-sm italic leading-relaxed text-terracotta">
            Que este número te traga a alegria de celebrar junto ao casal neste dia tão especial.
          </p>

          <a className="text-sm font-semibold text-warm-gray underline underline-offset-4" href="/">
            Voltar ao início
          </a>
        </div>
      </main>
    );
  }

  if (transaction.status === 'PENDING') {
    return <PaymentState title="Pagamento pendente" message={publicMessages.pending} tone="pending" />;
  }

  if (transaction.status === 'CANCELLED') {
    return <PaymentState title="Pagamento cancelado" message={publicMessages.cancelled} tone="error" />;
  }

  return <PaymentState title="Pagamento recusado" message={publicMessages.rejected} tone="error" />;
}

interface PaymentStateProps {
  icon?: ReactNode;
  message: string;
  title: string;
  tone: 'error' | 'neutral' | 'pending';
}

function PaymentState({ icon, message, title, tone }: PaymentStateProps) {
  const iconColor = tone === 'pending' ? 'text-gold' : tone === 'neutral' ? 'text-terracotta' : 'text-terracotta-dark';

  return (
    <main className="min-h-screen bg-cream px-6 pb-16 pt-16 text-charcoal">
      <div className="mx-auto flex w-full max-w-[420px] flex-col items-center gap-7 text-center">
        <BrandMark />
        <div className="grid h-20 w-20 place-items-center rounded-full bg-blush">
          {icon ?? <AlertTriangle aria-hidden="true" className={`h-10 w-10 ${iconColor}`} />}
        </div>
        <div>
          <h1 className="font-serif text-3xl font-bold">{title}</h1>
          <p className="mx-auto mt-4 max-w-xs text-sm leading-relaxed text-warm-gray">{message}</p>
        </div>
        <div className="flex w-full flex-col gap-3">
          {tone !== 'pending' ? (
            <a
              className="inline-flex min-h-12 w-full items-center justify-center rounded-lg bg-terracotta px-5 py-3 text-sm font-semibold text-white shadow-button transition hover:bg-terracotta-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-terracotta"
              href="/"
            >
              Tentar novamente
            </a>
          ) : null}
          <a
            className="inline-flex min-h-12 w-full items-center justify-center rounded-lg border border-terracotta bg-transparent px-5 py-3 text-sm font-semibold text-terracotta transition hover:bg-blush focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-terracotta"
            href="/"
          >
            Voltar ao início
          </a>
        </div>
      </div>
    </main>
  );
}
