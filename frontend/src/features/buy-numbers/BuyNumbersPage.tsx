import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { CreditCard, Minus, Plus, Trophy } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';

import { BrandMark, GoldDivider } from '../../components/brand/BrandMark';
import { StepProgress } from '../../components/brand/StepProgress';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { publicMessages } from '../../content/messages';
import { publicHomeService } from '../../services/publicHomeService';
import { transactionService } from '../../services/transactionService';
import { formatCurrency } from '../../utils/formatters';
import { buyerSchema, type BuyerFormData } from './schemas';

export function BuyNumbersPage() {
  const [buyer, setBuyer] = useState<BuyerFormData | null>(null);
  const [quantity, setQuantity] = useState(1);

  const homeSummaryQuery = useQuery({
    queryKey: ['public-home-summary'],
    queryFn: publicHomeService.getSummary,
  });

  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
  } = useForm<BuyerFormData>({
    defaultValues: { email: '', name: '', phone: '' },
    mode: 'onChange',
    resolver: zodResolver(buyerSchema),
  });

  const quoteQuery = useQuery({
    enabled: Boolean(buyer),
    queryKey: ['transaction-quote', buyer, quantity],
    queryFn: () => transactionService.quote({ ...buyer!, quantity }),
  });

  const createTransactionMutation = useMutation({
    mutationFn: (request: BuyerFormData & { quantity: number }) => transactionService.create(request),
    onSuccess: (response) => {
      window.location.assign(response.checkoutUrl);
    },
  });

  const onSubmitBuyer = (data: BuyerFormData) => {
    setBuyer({
      name: data.name.trim(),
      phone: data.phone.trim(),
      email: data.email?.trim() || undefined,
    });
  };

  const decreaseQuantity = () => setQuantity((current) => Math.max(1, current - 1));
  const increaseQuantity = () => setQuantity((current) => current + 1);

  const handlePay = () => {
    if (!buyer || createTransactionMutation.isPending) return;
    createTransactionMutation.mutate({ ...buyer, quantity });
  };

  const currentStep: 1 | 2 = buyer ? 2 : 1;
  const unitPrice = quoteQuery.data?.unitPrice;
  const totalAmount = quoteQuery.data?.totalAmount;

  return (
    <main className="min-h-screen bg-cream px-6 pb-16 pt-10 text-charcoal">
      <div className="mx-auto flex w-full max-w-[440px] flex-col gap-7">
        <header className="text-center">
          <BrandMark />
          <p className="mx-auto mt-4 max-w-xs text-sm leading-relaxed text-warm-gray">
            Participe do sorteio e faca parte deste presente especial para o casal.
          </p>
          <div className="mt-6">
            <GoldDivider />
          </div>
        </header>

        <StepProgress currentStep={currentStep} />

        {!buyer ? (
          <PublicHomeSummary
            scheduledDrawAt={homeSummaryQuery.data?.scheduledDrawAt ?? null}
            topBuyers={homeSummaryQuery.data?.topBuyers ?? []}
          />
        ) : null}

        {!buyer ? (
          <Card>
            <form className="space-y-5" onSubmit={handleSubmit(onSubmitBuyer)}>
              <div>
                <h1 className="font-serif text-xl font-semibold text-charcoal">Vamos comecar!</h1>
                <p className="mt-1 text-sm text-warm-gray">Informe seus dados para escolher seus numeros.</p>
              </div>

              <TextInput
                autoComplete="name"
                error={errors.name?.message}
                id="buyer-name"
                label="Nome"
                placeholder="Seu nome"
                {...register('name')}
              />

              <TextInput
                autoComplete="tel"
                error={errors.phone?.message}
                helper="Use um telefone com DDD."
                id="buyer-phone"
                inputMode="tel"
                label="Telefone"
                placeholder="(11) 99999-9999"
                type="tel"
                {...register('phone')}
              />

              <TextInput
                autoComplete="email"
                error={errors.email?.message}
                helper="Informe seu e-mail para receber os numeros automaticamente, ou deixe em branco e baixe um PDF ao final."
                id="buyer-email"
                inputMode="email"
                label="E-mail (opcional)"
                placeholder="seu@email.com"
                type="email"
                {...register('email')}
              />

              <Button disabled={!isValid} type="submit">
                Continuar
              </Button>
            </form>
          </Card>
        ) : (
          <section className="space-y-4" aria-labelledby="quantity-title">
            <div className="text-center">
              <h1 className="font-serif text-lg text-charcoal" id="quantity-title">
                Quantos numeros voce quer?
              </h1>
              <button
                className="mt-2 text-xs font-semibold text-terracotta underline underline-offset-4"
                onClick={() => setBuyer(null)}
                type="button"
              >
                Alterar dados
              </button>
            </div>

            <Card>
              <div className="flex items-center justify-center gap-8">
                <button
                  aria-label="Diminuir quantidade"
                  className="grid h-14 w-14 place-items-center rounded-full border-2 border-terracotta text-terracotta transition disabled:cursor-not-allowed disabled:opacity-30"
                  disabled={quantity === 1}
                  onClick={decreaseQuantity}
                  type="button"
                >
                  <Minus className="h-5 w-5" />
                </button>

                <div className="min-w-24 text-center">
                  <span className="block font-serif text-7xl font-bold leading-none text-charcoal">{quantity}</span>
                  <span className="mt-1 block text-xs text-warm-gray">{quantity === 1 ? 'numero' : 'numeros'}</span>
                </div>

                <button
                  aria-label="Aumentar quantidade"
                  className="grid h-14 w-14 place-items-center rounded-full bg-terracotta text-white shadow-button transition hover:bg-terracotta-dark"
                  onClick={increaseQuantity}
                  type="button"
                >
                  <Plus className="h-5 w-5" />
                </button>
              </div>
            </Card>

            <Card className="bg-blush shadow-none">
              <dl className="space-y-3">
                <div className="flex items-center justify-between gap-4">
                  <dt className="text-sm text-warm-gray">Quantidade</dt>
                  <dd className="text-sm font-semibold">
                    {quantity} {quantity === 1 ? 'numero' : 'numeros'}
                  </dd>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <dt className="text-sm text-warm-gray">Valor unitario</dt>
                  <dd className="text-sm font-semibold">
                    {quoteQuery.isLoading ? 'Atualizando...' : unitPrice ? formatCurrency(unitPrice) : '-'}
                  </dd>
                </div>
                <div className="h-px bg-[#DCBFB5]" />
                <div className="flex items-center justify-between gap-4">
                  <dt className="text-base font-bold">Total</dt>
                  <dd className="font-serif text-3xl font-bold text-terracotta">
                    {quoteQuery.isLoading ? '...' : totalAmount ? formatCurrency(totalAmount) : '-'}
                  </dd>
                </div>
              </dl>
            </Card>

            {quoteQuery.isError ? (
              <p className="rounded-lg border border-terracotta/30 bg-white px-4 py-3 text-sm text-terracotta-dark" role="alert">
                {publicMessages.quoteError}
              </p>
            ) : null}

            {createTransactionMutation.isError ? (
              <p className="rounded-lg border border-terracotta/30 bg-white px-4 py-3 text-sm text-terracotta-dark" role="alert">
                {publicMessages.checkoutError}
              </p>
            ) : null}

            <Button
              disabled={!quoteQuery.data || quoteQuery.isFetching}
              isLoading={createTransactionMutation.isPending}
              onClick={handlePay}
              type="button"
            >
              <CreditCard aria-hidden="true" className="h-5 w-5" />
              Pagar com Mercado Pago
            </Button>

            <p className="px-2 text-center text-xs leading-relaxed text-warm-gray">
              Voce sera redirecionado ao Mercado Pago para concluir o pagamento com seguranca.
            </p>
          </section>
        )}
      </div>
    </main>
  );
}

function PublicHomeSummary({
  scheduledDrawAt,
  topBuyers,
}: {
  scheduledDrawAt: string | null;
  topBuyers: Array<{ avatarEmoji: string; avatarColor: string; quantity: number }>;
}) {
  if (!scheduledDrawAt && topBuyers.length === 0) return null;

  return (
    <section className="space-y-4" aria-label="Resumo da rifa">
      {scheduledDrawAt ? <Countdown scheduledDrawAt={scheduledDrawAt} /> : null}
      {topBuyers.length > 0 ? <TopBuyersRank topBuyers={topBuyers} /> : null}
    </section>
  );
}

function Countdown({ scheduledDrawAt }: { scheduledDrawAt: string }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);

  const remainingMs = Math.max(0, new Date(scheduledDrawAt).getTime() - now);
  const totalSeconds = Math.floor(remainingMs / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return (
    <Card className="bg-charcoal text-white shadow-none">
      <p className="text-center text-xs font-bold uppercase tracking-wide text-gold">Sorteio em</p>
      <div className="mt-4 grid grid-cols-4 gap-2 text-center">
        <CountdownPart label="dias" value={days} />
        <CountdownPart label="horas" value={hours} />
        <CountdownPart label="min" value={minutes} />
        <CountdownPart label="seg" value={seconds} />
      </div>
    </Card>
  );
}

function CountdownPart({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-white/10 px-2 py-3">
      <span className="block font-serif text-2xl font-bold leading-none">{String(value).padStart(2, '0')}</span>
      <span className="mt-1 block text-[11px] font-semibold uppercase text-white/60">{label}</span>
    </div>
  );
}

function TopBuyersRank({
  topBuyers,
}: {
  topBuyers: Array<{ avatarEmoji: string; avatarColor: string; quantity: number }>;
}) {
  return (
    <Card className="shadow-none">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-wide text-warm-gray">Top presentes</p>
          <h2 className="font-serif text-lg font-bold text-charcoal">Maiores compradores</h2>
        </div>
        <Trophy aria-hidden="true" className="h-5 w-5 text-gold" />
      </div>

      <ol className="space-y-3">
        {topBuyers.map((buyer, index) => (
          <li className="flex items-center justify-between gap-3" key={`${buyer.avatarEmoji}-${buyer.avatarColor}-${index}`}>
            <div className="flex items-center gap-3">
              <span
                className="grid h-10 w-10 place-items-center rounded-full text-lg"
                style={{ backgroundColor: buyer.avatarColor }}
                aria-hidden="true"
              >
                {buyer.avatarEmoji}
              </span>
              <span className="text-sm font-bold text-charcoal">#{index + 1}</span>
            </div>
            <span className="text-sm font-semibold text-warm-gray">
              {buyer.quantity} {buyer.quantity === 1 ? 'numero' : 'numeros'}
            </span>
          </li>
        ))}
      </ol>
    </Card>
  );
}
