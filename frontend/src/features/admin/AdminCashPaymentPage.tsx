import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, Download, ReceiptText, Ticket } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { adminTransactionService } from '../../services/adminTransactionService';
import { transactionService } from '../../services/transactionService';
import { formatCurrency } from '../../utils/formatters';
import { cashPaymentSchema, type CashPaymentFormData } from './schemas';

export function AdminCashPaymentPage() {
  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
  } = useForm<CashPaymentFormData>({
    defaultValues: { email: '', name: '', phone: '', quantity: 1 },
    mode: 'onChange',
    resolver: zodResolver(cashPaymentSchema),
  });

  const createCashMutation = useMutation({
    mutationFn: (data: CashPaymentFormData) =>
      adminTransactionService.createCashTransaction({
        name: data.name.trim(),
        phone: data.phone.trim(),
        email: data.email?.trim() || undefined,
        quantity: data.quantity,
      }),
  });

  return (
    <main className="min-h-screen bg-cream text-charcoal">
      <header className="bg-charcoal px-6 py-4 text-white">
        <div className="mx-auto flex max-w-4xl items-center justify-between gap-4">
          <div>
            <p className="font-serif text-2xl font-bold">
              Presente <span className="italic text-gold">Premiado</span>
            </p>
            <p className="mt-1 text-xs text-white/55">Pagamento em dinheiro</p>
          </div>
          <a className="inline-flex items-center gap-2 text-sm font-semibold text-white/70 hover:text-white" href="/admin">
            <ArrowLeft aria-hidden="true" className="h-4 w-4" />
            Voltar
          </a>
        </div>
      </header>

      <section className="mx-auto grid max-w-4xl gap-6 px-6 py-8 md:grid-cols-[1fr_0.9fr]">
        <Card>
          <form className="space-y-5" onSubmit={handleSubmit((data) => createCashMutation.mutate(data))}>
            <div>
              <h1 className="font-serif text-2xl font-bold">Registrar pagamento</h1>
              <p className="mt-1 text-sm text-warm-gray">Os numeros sao gerados imediatamente apos confirmar.</p>
            </div>

            <TextInput id="cash-name" label="Nome" placeholder="Nome do convidado" error={errors.name?.message} {...register('name')} />

            <TextInput
              id="cash-phone"
              label="Telefone"
              placeholder="(11) 99999-9999"
              inputMode="tel"
              type="tel"
              error={errors.phone?.message}
              {...register('phone')}
            />

            <TextInput
              id="cash-email"
              label="E-mail (opcional)"
              placeholder="convidado@email.com"
              inputMode="email"
              type="email"
              error={errors.email?.message}
              {...register('email')}
            />

            <TextInput
              id="cash-quantity"
              label="Quantidade"
              min={1}
              type="number"
              error={errors.quantity?.message}
              {...register('quantity')}
            />

            {createCashMutation.isError ? (
              <p className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark" role="alert">
                Nao foi possivel registrar o pagamento.
              </p>
            ) : null}

            <Button disabled={!isValid} isLoading={createCashMutation.isPending} type="submit">
              <ReceiptText aria-hidden="true" className="h-5 w-5" />
              Confirmar pagamento
            </Button>
          </form>
        </Card>

        <Card className="bg-blush shadow-none">
          {createCashMutation.data ? (
            <div className="space-y-5">
              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-warm-gray">Pagamento aprovado</p>
                <h2 className="mt-2 font-serif text-2xl font-bold">{createCashMutation.data.name}</h2>
                <p className="mt-1 text-sm text-warm-gray">{formatCurrency(createCashMutation.data.totalAmount)}</p>
              </div>

              <div className="flex flex-wrap gap-2">
                {createCashMutation.data.luckyNumbers.map((number) => (
                  <span className="inline-flex items-center gap-1 rounded-md bg-gold px-3 py-2 text-sm font-bold text-charcoal" key={number}>
                    <Ticket aria-hidden="true" className="h-4 w-4" />
                    {number}
                  </span>
                ))}
              </div>

              <a
                className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-lg bg-terracotta px-4 py-2 text-sm font-semibold text-white shadow-button transition hover:bg-terracotta-dark"
                href={transactionService.getLuckyNumbersPdfUrl(createCashMutation.data.externalReference)}
              >
                <Download aria-hidden="true" className="h-4 w-4" />
                Baixar PDF
              </a>
            </div>
          ) : (
            <div className="grid min-h-72 place-items-center text-center">
              <div>
                <ReceiptText aria-hidden="true" className="mx-auto h-12 w-12 text-terracotta" />
                <p className="mt-4 text-sm leading-relaxed text-warm-gray">
                  Depois de confirmar, os numeros aparecerao aqui para entrega ao convidado.
                </p>
              </div>
            </div>
          )}
        </Card>
      </section>
    </main>
  );
}
