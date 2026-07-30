import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { CreditCard, Minus, Plus } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { BrandMark, GoldDivider } from '../../components/brand/BrandMark';
import { StepProgress } from '../../components/brand/StepProgress';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { publicMessages } from '../../content/messages';
import { transactionService } from '../../services/transactionService';
import { formatCurrency } from '../../utils/formatters';
import { buyerEmailSchema, type BuyerEmailFormData } from './schemas';

export function BuyNumbersPage() {
  const [email, setEmail] = useState('');
  const [quantity, setQuantity] = useState(1);

  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
  } = useForm<BuyerEmailFormData>({
    defaultValues: { email: '' },
    mode: 'onChange',
    resolver: zodResolver(buyerEmailSchema),
  });

  const quoteQuery = useQuery({
    enabled: Boolean(email),
    queryKey: ['transaction-quote', email, quantity],
    queryFn: () => transactionService.quote({ email, quantity }),
  });

  const createTransactionMutation = useMutation({
    mutationFn: (request: { email: string; quantity: number }) => transactionService.create(request),
    onSuccess: (response) => {
      window.location.assign(response.checkoutUrl);
    },
  });

  const onSubmitEmail = (data: BuyerEmailFormData) => {
    setEmail(data.email.trim());
  };

  const decreaseQuantity = () => setQuantity((current) => Math.max(1, current - 1));
  const increaseQuantity = () => setQuantity((current) => current + 1);

  const handlePay = () => {
    if (!email || createTransactionMutation.isPending) return;
    createTransactionMutation.mutate({ email, quantity });
  };

  const currentStep: 1 | 2 = email ? 2 : 1;
  const unitPrice = quoteQuery.data?.unitPrice;
  const totalAmount = quoteQuery.data?.totalAmount;

  return (
    <main className="min-h-screen bg-cream px-6 pb-16 pt-10 text-charcoal">
      <div className="mx-auto flex w-full max-w-[440px] flex-col gap-7">
        <header className="text-center">
          <BrandMark />
          <p className="mx-auto mt-4 max-w-xs text-sm leading-relaxed text-warm-gray">
            Participe do sorteio e faça parte deste presente especial para o casal.
          </p>
          <div className="mt-6">
            <GoldDivider />
          </div>
        </header>

        <StepProgress currentStep={currentStep} />

        {!email ? (
          <Card>
            <form className="space-y-5" onSubmit={handleSubmit(onSubmitEmail)}>
              <div>
                <h1 className="font-serif text-xl font-semibold text-charcoal">Vamos começar!</h1>
                <p className="mt-1 text-sm text-warm-gray">Informe seu e-mail para escolher seus números.</p>
              </div>

              <TextInput
                autoComplete="email"
                error={errors.email?.message}
                helper="Os números serão enviados para este e-mail após a confirmação do pagamento."
                id="buyer-email"
                inputMode="email"
                label="E-mail"
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
                Quantos números você quer?
              </h1>
              <button
                className="mt-2 text-xs font-semibold text-terracotta underline underline-offset-4"
                onClick={() => setEmail('')}
                type="button"
              >
                Trocar e-mail
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
                  <span className="mt-1 block text-xs text-warm-gray">{quantity === 1 ? 'número' : 'números'}</span>
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
                  <dd className="text-sm font-semibold">{quantity} {quantity === 1 ? 'número' : 'números'}</dd>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <dt className="text-sm text-warm-gray">Valor unitário</dt>
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
              Você será redirecionado ao Mercado Pago para concluir o pagamento com segurança.
            </p>
          </section>
        )}
      </div>
    </main>
  );
}
