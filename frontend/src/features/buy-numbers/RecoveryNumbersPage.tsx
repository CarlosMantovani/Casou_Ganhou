import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, Search } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { BrandMark, GoldDivider } from '../../components/brand/BrandMark';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { FlagEmoji } from '../../components/ui/FlagEmoji';
import { TextInput } from '../../components/ui/TextInput';
import { publicMessages } from '../../content/messages';
import { transactionService } from '../../services/transactionService';
import { formatPhoneNumber, normalizePhoneNumber } from '../../utils/phone';
import { PdfDownloadContent, RecoveryCodeContent } from '../payment-return/PaymentReturnPage';
import { recoverySchema, type RecoveryFormData } from './schemas';

export function RecoveryNumbersPage() {
  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
  } = useForm<RecoveryFormData>({
    defaultValues: { phone: '', recoveryCode: '' },
    mode: 'onChange',
    resolver: zodResolver(recoverySchema),
  });

  const recoveryMutation = useMutation({
    mutationFn: (request: RecoveryFormData) =>
      transactionService.recover({
        phone: normalizePhoneNumber(request.phone),
        recoveryCode: request.recoveryCode,
      }),
  });

  return (
    <main className="min-h-screen bg-cream px-6 pb-16 pt-10 text-charcoal">
      <div className="mx-auto flex w-full max-w-[480px] flex-col gap-7">
        <header className="text-center">
          <BrandMark />
          <p className="mx-auto mt-4 max-w-xs text-sm leading-relaxed text-warm-gray">
            Consulte seus números usando o telefone e o código da compra.
          </p>
          <div className="mt-6">
            <GoldDivider />
          </div>
        </header>

        <a
          className="inline-flex w-fit items-center gap-2 rounded-lg border border-green px-4 py-2 text-sm font-bold text-green transition hover:bg-ivory-deep focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green"
          href="/"
        >
          <ArrowLeft aria-hidden="true" className="h-4 w-4" />
          Voltar
        </a>

        <Card className="bg-white/90">
          <form className="space-y-4" onSubmit={handleSubmit((data) => recoveryMutation.mutate(data))}>
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-green">Consultar números</p>
              <h1 className="mt-2 font-serif text-2xl font-bold text-charcoal">Já tenho um código</h1>
            </div>

            <TextInput
              autoComplete="tel"
              error={errors.phone?.message}
              id="recovery-phone"
              inputMode="tel"
              label="Telefone da compra"
              maxLength={15}
              placeholder="(11) 99999-9999"
              type="tel"
              {...register('phone', {
                onChange: (event) => {
                  event.target.value = formatPhoneNumber(event.target.value);
                },
              })}
            />

            <TextInput
              autoComplete="one-time-code"
              error={errors.recoveryCode?.message}
              id="recovery-code"
              inputMode="numeric"
              label="Código de 4 dígitos"
              maxLength={4}
              placeholder="0000"
              {...register('recoveryCode', {
                onChange: (event) => {
                  event.target.value = event.target.value.replace(/\D/g, '').slice(0, 4);
                },
              })}
            />

            {recoveryMutation.isError ? (
              <p className="rounded-lg border border-wine/30 bg-white px-4 py-3 text-sm text-wine" role="alert">
                {publicMessages.recoveryError}
              </p>
            ) : null}

            <Button disabled={!isValid} isLoading={recoveryMutation.isPending} type="submit">
              <Search aria-hidden="true" className="h-5 w-5" />
              Consultar meus números
            </Button>
          </form>

          {recoveryMutation.data ? <RecoveredNumbers transaction={recoveryMutation.data} /> : null}
        </Card>
      </div>
    </main>
  );
}

function RecoveredNumbers({ transaction }: { transaction: Awaited<ReturnType<typeof transactionService.recover>> }) {
  return (
    <div className="mt-5 space-y-4 border-t border-line pt-5">
      <RecoveryCodeContent recoveryCode={transaction.recoveryCode} />
      {transaction.participantFlagEmoji && transaction.participantFlagName ? (
        <div className="rounded-lg border border-[#EEE6DF] bg-white/80 px-4 py-3 text-center shadow-none">
          <p className="text-xs font-bold uppercase tracking-wide text-terracotta">Sua bandeira</p>
          <div className="mt-3 flex items-center justify-center gap-3">
            <span className="grid h-12 w-12 place-items-center rounded-full bg-blush">
              <FlagEmoji className="h-8 w-8" emoji={transaction.participantFlagEmoji} />
            </span>
            <span className="font-serif text-xl font-bold text-charcoal">{transaction.participantFlagName}</span>
          </div>
        </div>
      ) : null}
      {transaction.status === 'APROVADO' && transaction.luckyNumbers.length > 0 ? (
        <>
          <h2 className="text-sm font-bold text-charcoal">Seus números da sorte</h2>
          <div className="flex flex-wrap justify-center gap-3">
            {transaction.luckyNumbers.map((number) => (
              <span className="rounded-lg bg-gold px-4 py-2 text-sm font-bold text-charcoal shadow-sm" key={number}>
                {number}
              </span>
            ))}
          </div>
          <div className="rounded-lg border border-gold bg-gold/10 p-4">
            <PdfDownloadContent externalReference={transaction.externalReference} />
          </div>
        </>
      ) : (
        <p className="rounded-lg bg-ivory-deep px-4 py-3 text-sm leading-relaxed text-warm-gray">
          {transaction.status === 'PENDENTE' ? publicMessages.pending : 'Esta compra ainda não possui números gerados.'}
        </p>
      )}
    </div>
  );
}
