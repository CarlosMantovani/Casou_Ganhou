import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, CalendarClock, Save, Settings } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { raffleConfigService } from '../../services/raffleConfigService';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { raffleSettingsSchema, type RaffleSettingsFormData } from './schemas';

export function AdminSettingsPage() {
  const queryClient = useQueryClient();
  const configQuery = useQuery({
    queryKey: ['raffle-config'],
    queryFn: raffleConfigService.getConfig,
  });

  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
    reset,
  } = useForm<RaffleSettingsFormData>({
    defaultValues: { scheduledDrawAt: '', unitPrice: 10 },
    mode: 'onChange',
    resolver: zodResolver(raffleSettingsSchema),
  });

  useEffect(() => {
    if (!configQuery.data) return;

    reset({
      unitPrice: Number(configQuery.data.unitPrice),
      scheduledDrawAt: toDateTimeLocalValue(configQuery.data.scheduledDrawAt),
    });
  }, [configQuery.data, reset]);

  const updateSettingsMutation = useMutation({
    mutationFn: async (data: RaffleSettingsFormData) => {
      const unitPriceResponse = await raffleConfigService.updateUnitPrice({
        unitPrice: Number(data.unitPrice).toFixed(2),
      });
      return raffleConfigService.updateScheduledDrawAt({
        scheduledDrawAt: data.scheduledDrawAt ? new Date(data.scheduledDrawAt).toISOString() : null,
      }).then(() => unitPriceResponse);
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['raffle-config'] }),
        queryClient.invalidateQueries({ queryKey: ['public-home-summary'] }),
        queryClient.invalidateQueries({ queryKey: ['transaction-quote'] }),
      ]);
    },
  });

  return (
    <main className="min-h-screen bg-cream text-charcoal">
      <header className="bg-charcoal px-6 py-4 text-white">
        <div className="mx-auto flex max-w-4xl items-center justify-between gap-4">
          <div>
            <p className="font-serif text-2xl font-bold">
              Presente <span className="italic text-gold">Premiado</span>
            </p>
            <p className="mt-1 text-xs text-white/55">Configurações da rifa</p>
          </div>
          <a className="inline-flex items-center gap-2 text-sm font-semibold text-white/70 hover:text-white" href="/admin">
            <ArrowLeft aria-hidden="true" className="h-4 w-4" />
            Voltar
          </a>
        </div>
      </header>

      <section className="mx-auto grid max-w-4xl gap-6 px-6 py-8 md:grid-cols-[1fr_0.9fr]">
        <Card>
          <form className="space-y-5" onSubmit={handleSubmit((data) => updateSettingsMutation.mutate(data))}>
            <div>
              <h1 className="flex items-center gap-2 font-serif text-2xl font-bold">
                <Settings aria-hidden="true" className="h-5 w-5 text-terracotta" />
                Dados vigentes
              </h1>
              <p className="mt-1 text-sm text-warm-gray">As alterações valem apenas para novas compras.</p>
            </div>

            <TextInput
              id="raffle-unit-price"
              label="Preço unitário"
              min="0.01"
              step="0.01"
              type="number"
              error={errors.unitPrice?.message}
              {...register('unitPrice')}
            />

            <TextInput
              id="raffle-scheduled-draw-at"
              label="Data e hora do sorteio"
              type="datetime-local"
              error={errors.scheduledDrawAt?.message}
              {...register('scheduledDrawAt')}
            />

            {configQuery.isError || updateSettingsMutation.isError ? (
              <p className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark" role="alert">
                Não foi possível salvar as configurações.
              </p>
            ) : null}

            {updateSettingsMutation.isSuccess ? (
              <p className="rounded-lg border border-olive/20 bg-olive/10 px-4 py-3 text-sm font-semibold text-olive" role="status">
                Configurações salvas.
              </p>
            ) : null}

            <Button disabled={!isValid || configQuery.isLoading} isLoading={updateSettingsMutation.isPending} type="submit">
              <Save aria-hidden="true" className="h-5 w-5" />
              Salvar configurações
            </Button>
          </form>
        </Card>

        <Card className="bg-blush shadow-none">
          <div className="space-y-5">
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-warm-gray">Configuração atual</p>
              <p className="mt-3 font-serif text-3xl font-bold text-charcoal">
                {configQuery.data ? formatCurrency(configQuery.data.unitPrice) : '-'}
              </p>
              <p className="mt-1 text-sm text-warm-gray">Valor unitário dos próximos números.</p>
            </div>

            <div className="rounded-lg border border-[#E7DDD6] bg-white/70 p-4">
              <div className="flex items-center gap-2 text-sm font-bold text-charcoal">
                <CalendarClock aria-hidden="true" className="h-4 w-4 text-terracotta" />
                Sorteio
              </div>
              <p className="mt-2 text-sm text-warm-gray">{formatDateTime(configQuery.data?.scheduledDrawAt)}</p>
            </div>
          </div>
        </Card>
      </section>
    </main>
  );
}

function toDateTimeLocalValue(value: string | null): string {
  if (!value) return '';

  const date = new Date(value);
  const offsetMs = date.getTimezoneOffset() * 60 * 1000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}
