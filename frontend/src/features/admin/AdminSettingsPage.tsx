import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, CalendarClock, Heart, Save, Settings } from 'lucide-react';
import { useEffect } from 'react';
import { useForm, useWatch } from 'react-hook-form';

import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { raffleConfigService } from '../../services/raffleConfigService';
import { fromDateTimeLocalValue, toDateTimeLocalValue } from '../../utils/dateTime';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import {
  raffleConfigSchema,
  scheduledDrawSchema,
  type RaffleConfigFormData,
  type ScheduledDrawFormData,
  type WeddingProfileFormData,
  weddingProfileSchema,
} from './schemas';

const DEFAULT_WEDDING_PROFILE: WeddingProfileFormData = {
  groomName: 'Jose Carlos',
  brideName: 'Paula',
  palette: {
    ivory: '#F7F1E6',
    ivoryDeep: '#F0E8D8',
    ink: '#2B2419',
    inkSoft: '#5B5140',
    green: '#24402E',
    greenDeep: '#152A1D',
    wine: '#7A2E33',
    gold: '#B8935A',
    goldSoft: '#DCC79A',
    line: '#D9CBAA',
  },
};

const COLOR_FIELDS = [
  { label: 'Fundo claro', name: 'ivory' },
  { label: 'Fundo destaque', name: 'ivoryDeep' },
  { label: 'Texto principal', name: 'ink' },
  { label: 'Texto suave', name: 'inkSoft' },
  { label: 'Verde', name: 'green' },
  { label: 'Verde escuro', name: 'greenDeep' },
  { label: 'Vinho', name: 'wine' },
  { label: 'Dourado', name: 'gold' },
  { label: 'Dourado suave', name: 'goldSoft' },
  { label: 'Linhas', name: 'line' },
] as const;

export function AdminSettingsPage() {
  const queryClient = useQueryClient();
  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
    reset,
  } = useForm<RaffleConfigFormData>({
    defaultValues: { unitPrice: 0 },
    mode: 'onChange',
    resolver: zodResolver(raffleConfigSchema),
  });
  const {
    formState: { errors: scheduledDrawErrors, isValid: isScheduledDrawValid },
    handleSubmit: handleScheduledDrawSubmit,
    register: registerScheduledDraw,
    reset: resetScheduledDraw,
  } = useForm<ScheduledDrawFormData>({
    defaultValues: { scheduledDrawAt: '' },
    mode: 'onChange',
    resolver: zodResolver(scheduledDrawSchema),
  });
  const {
    control: weddingProfileControl,
    formState: { errors: weddingProfileErrors, isValid: isWeddingProfileValid },
    handleSubmit: handleWeddingProfileSubmit,
    register: registerWeddingProfile,
    reset: resetWeddingProfile,
  } = useForm<WeddingProfileFormData>({
    defaultValues: DEFAULT_WEDDING_PROFILE,
    mode: 'onChange',
    resolver: zodResolver(weddingProfileSchema),
  });

  const configQuery = useQuery({
    queryKey: ['admin-raffle-config'],
    queryFn: raffleConfigService.getConfig,
  });

  useEffect(() => {
    if (configQuery.data) {
      reset({ unitPrice: Number(configQuery.data.unitPrice) });
      resetScheduledDraw({ scheduledDrawAt: toDateTimeLocalValue(configQuery.data.scheduledDrawAt) });
      resetWeddingProfile(configQuery.data.weddingProfile);
    }
  }, [configQuery.data, reset, resetScheduledDraw, resetWeddingProfile]);

  const updateUnitPriceMutation = useMutation({
    mutationFn: (data: RaffleConfigFormData) =>
      raffleConfigService.updateUnitPrice({ unitPrice: data.unitPrice.toFixed(2) }),
    onSuccess: (data) => {
      queryClient.setQueryData(['admin-raffle-config'], data);
      reset({ unitPrice: Number(data.unitPrice) });
    },
  });

  const updateScheduledDrawMutation = useMutation({
    mutationFn: (data: ScheduledDrawFormData) =>
      raffleConfigService.updateScheduledDrawAt({
        scheduledDrawAt: fromDateTimeLocalValue(data.scheduledDrawAt),
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['admin-raffle-config'], data);
      resetScheduledDraw({ scheduledDrawAt: toDateTimeLocalValue(data.scheduledDrawAt) });
    },
  });

  const updateWeddingProfileMutation = useMutation({
    mutationFn: (data: WeddingProfileFormData) =>
      raffleConfigService.updateWeddingProfile({
        groomName: data.groomName.trim(),
        brideName: data.brideName.trim(),
        palette: data.palette,
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['admin-raffle-config'], data);
      resetWeddingProfile(data.weddingProfile);
    },
  });

  const previewProfile = useWatch({
    control: weddingProfileControl,
    defaultValue: DEFAULT_WEDDING_PROFILE,
  }) as WeddingProfileFormData;

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
        <div className="space-y-6">
          <Card>
            <form className="space-y-5" onSubmit={handleSubmit((data) => updateUnitPriceMutation.mutate(data))}>
              <div>
                <h1 className="font-serif text-2xl font-bold">Preço unitário</h1>
                <p className="mt-1 text-sm text-warm-gray">
                  O novo valor passa a valer apenas para novas cotações e novas transações.
                </p>
              </div>

              <TextInput
                id="raffle-unit-price"
                label="Valor por número"
                min="0.01"
                step="0.01"
                type="number"
                error={errors.unitPrice?.message}
                {...register('unitPrice')}
              />

              {updateUnitPriceMutation.isSuccess ? (
                <p
                  className="rounded-lg border border-olive/20 bg-olive/10 px-4 py-3 text-sm font-semibold text-olive"
                  role="status"
                >
                  Preço atualizado com sucesso.
                </p>
              ) : null}

              {updateUnitPriceMutation.isError ? (
                <p
                  className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark"
                  role="alert"
                >
                  Não foi possível atualizar o preço.
                </p>
              ) : null}

              <Button
                disabled={!isValid || configQuery.isLoading}
                isLoading={updateUnitPriceMutation.isPending}
                type="submit"
              >
                <Save aria-hidden="true" className="h-5 w-5" />
                Salvar preço
              </Button>
            </form>
          </Card>

          <Card>
            <form
              className="space-y-5"
              onSubmit={handleScheduledDrawSubmit((data) => updateScheduledDrawMutation.mutate(data))}
            >
              <div>
                <h2 className="font-serif text-2xl font-bold">Data do sorteio</h2>
                <p className="mt-1 text-sm text-warm-gray">
                  Essa data alimenta a contagem regressiva exibida na tela inicial.
                </p>
              </div>

              <TextInput
                id="raffle-scheduled-draw-at"
                label="Data e horário"
                type="datetime-local"
                error={scheduledDrawErrors.scheduledDrawAt?.message}
                {...registerScheduledDraw('scheduledDrawAt')}
              />

              {updateScheduledDrawMutation.isSuccess ? (
                <p
                  className="rounded-lg border border-olive/20 bg-olive/10 px-4 py-3 text-sm font-semibold text-olive"
                  role="status"
                >
                  Data do sorteio atualizada com sucesso.
                </p>
              ) : null}

              {updateScheduledDrawMutation.isError ? (
                <p
                  className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark"
                  role="alert"
                >
                  Não foi possível atualizar a data do sorteio.
                </p>
              ) : null}

              <Button
                disabled={!isScheduledDrawValid || configQuery.isLoading}
                isLoading={updateScheduledDrawMutation.isPending}
                type="submit"
              >
                <CalendarClock aria-hidden="true" className="h-5 w-5" />
                Salvar data
              </Button>
            </form>
          </Card>

          <Card>
            <form
              className="space-y-5"
              onSubmit={handleWeddingProfileSubmit((data) => updateWeddingProfileMutation.mutate(data))}
            >
              <div>
                <h2 className="font-serif text-2xl font-bold">Identidade dos noivos</h2>
                <p className="mt-1 text-sm text-warm-gray">
                  Esses dados aparecem na tela inicial e definem a paleta visual do evento.
                </p>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <TextInput
                  id="wedding-groom-name"
                  label="Nome do noivo"
                  error={weddingProfileErrors.groomName?.message}
                  {...registerWeddingProfile('groomName')}
                />
                <TextInput
                  id="wedding-bride-name"
                  label="Nome da noiva"
                  error={weddingProfileErrors.brideName?.message}
                  {...registerWeddingProfile('brideName')}
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                {COLOR_FIELDS.map((field) => (
                  <div className="grid grid-cols-[3rem_1fr] items-end gap-3" key={field.name}>
                    <input
                      aria-label={field.label}
                      className="h-12 w-12 rounded-lg border border-line bg-white p-1"
                      type="color"
                      {...registerWeddingProfile(`palette.${field.name}`)}
                    />
                    <TextInput
                      id={`wedding-color-${field.name}`}
                      label={field.label}
                      maxLength={7}
                      error={weddingProfileErrors.palette?.[field.name]?.message}
                      {...registerWeddingProfile(`palette.${field.name}`)}
                    />
                  </div>
                ))}
              </div>

              {updateWeddingProfileMutation.isSuccess ? (
                <p
                  className="rounded-lg border border-olive/20 bg-olive/10 px-4 py-3 text-sm font-semibold text-olive"
                  role="status"
                >
                  Identidade atualizada com sucesso.
                </p>
              ) : null}

              {updateWeddingProfileMutation.isError ? (
                <p
                  className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark"
                  role="alert"
                >
                  Não foi possível atualizar a identidade dos noivos.
                </p>
              ) : null}

              <Button
                disabled={!isWeddingProfileValid || configQuery.isLoading}
                isLoading={updateWeddingProfileMutation.isPending}
                type="submit"
              >
                <Save aria-hidden="true" className="h-5 w-5" />
                Salvar identidade
              </Button>
            </form>
          </Card>
        </div>

        <Card className="bg-blush shadow-none">
          {configQuery.isLoading ? (
            <p className="py-10 text-center text-sm text-warm-gray">Carregando configurações...</p>
          ) : null}

          {configQuery.isError ? (
            <p className="rounded-lg border border-terracotta/30 bg-white px-4 py-3 text-sm text-terracotta-dark" role="alert">
              Não foi possível carregar o preço atual.
            </p>
          ) : null}

          {configQuery.data ? (
            <div className="space-y-5">
              <div>
                <Settings aria-hidden="true" className="h-10 w-10 text-terracotta" />
                <p className="mt-4 text-xs font-bold uppercase tracking-wide text-warm-gray">Preço vigente</p>
                <p className="mt-2 font-serif text-4xl font-bold text-charcoal">
                  {formatCurrency(configQuery.data.unitPrice)}
                </p>
              </div>

              <div className="rounded-lg bg-white/70 px-4 py-3 text-sm leading-relaxed text-warm-gray">
                Transações já criadas mantêm o valor com que nasceram. Esta configuração só altera o preço usado daqui em diante.
              </div>

              {configQuery.data.updatedAt ? (
                <p className="text-xs text-warm-gray">
                  Última atualização: {formatDateTime(configQuery.data.updatedAt)}
                </p>
              ) : null}

              <div className="rounded-lg bg-white/70 px-4 py-3">
                <p className="text-xs font-bold uppercase tracking-wide text-warm-gray">Sorteio</p>
                <p className="mt-2 text-sm font-semibold text-charcoal">
                  {configQuery.data.scheduledDrawAt
                    ? formatDateTime(configQuery.data.scheduledDrawAt)
                    : 'Ainda não configurado'}
                </p>
              </div>

              <div
                className="rounded-lg border px-4 py-5 text-center"
                style={{
                  backgroundColor: previewProfile.palette.ivory,
                  borderColor: previewProfile.palette.line,
                  color: previewProfile.palette.ink,
                }}
              >
                <p className="text-xs font-bold uppercase tracking-wide" style={{ color: previewProfile.palette.gold }}>
                  Prévia da tela inicial
                </p>
                <p className="mt-3 font-serif text-3xl font-bold leading-tight" style={{ color: previewProfile.palette.green }}>
                  {previewProfile.groomName}
                  <Heart
                    aria-hidden="true"
                    className="mx-2 mb-1 inline-block h-6 w-6"
                    style={{ color: previewProfile.palette.wine }}
                    strokeWidth={1.5}
                  />
                  {previewProfile.brideName}
                </p>
              </div>
            </div>
          ) : null}
        </Card>
      </section>
    </main>
  );
}
