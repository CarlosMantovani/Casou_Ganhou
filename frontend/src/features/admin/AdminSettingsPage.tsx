import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Save, Settings } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { raffleConfigService } from '../../services/raffleConfigService';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { raffleConfigSchema, type RaffleConfigFormData } from './schemas';

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

  const configQuery = useQuery({
    queryKey: ['admin-raffle-config'],
    queryFn: raffleConfigService.getConfig,
  });

  useEffect(() => {
    if (configQuery.data) {
      reset({ unitPrice: Number(configQuery.data.unitPrice) });
    }
  }, [configQuery.data, reset]);

  const updateUnitPriceMutation = useMutation({
    mutationFn: (data: RaffleConfigFormData) =>
      raffleConfigService.updateUnitPrice({ unitPrice: data.unitPrice.toFixed(2) }),
    onSuccess: (data) => {
      queryClient.setQueryData(['admin-raffle-config'], data);
      reset({ unitPrice: Number(data.unitPrice) });
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
            <p className="mt-1 text-xs text-white/55">Configuracoes da rifa</p>
          </div>
          <a className="inline-flex items-center gap-2 text-sm font-semibold text-white/70 hover:text-white" href="/admin">
            <ArrowLeft aria-hidden="true" className="h-4 w-4" />
            Voltar
          </a>
        </div>
      </header>

      <section className="mx-auto grid max-w-4xl gap-6 px-6 py-8 md:grid-cols-[1fr_0.9fr]">
        <Card>
          <form className="space-y-5" onSubmit={handleSubmit((data) => updateUnitPriceMutation.mutate(data))}>
            <div>
              <h1 className="font-serif text-2xl font-bold">Preco unitario</h1>
              <p className="mt-1 text-sm text-warm-gray">
                O novo valor passa a valer apenas para novas cotacoes e novas transacoes.
              </p>
            </div>

            <TextInput
              id="raffle-unit-price"
              label="Valor por numero"
              min="0.01"
              step="0.01"
              type="number"
              error={errors.unitPrice?.message}
              {...register('unitPrice')}
            />

            {updateUnitPriceMutation.isSuccess ? (
              <p className="rounded-lg border border-olive/20 bg-olive/10 px-4 py-3 text-sm font-semibold text-olive" role="status">
                Preco atualizado com sucesso.
              </p>
            ) : null}

            {updateUnitPriceMutation.isError ? (
              <p className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark" role="alert">
                Nao foi possivel atualizar o preco.
              </p>
            ) : null}

            <Button disabled={!isValid || configQuery.isLoading} isLoading={updateUnitPriceMutation.isPending} type="submit">
              <Save aria-hidden="true" className="h-5 w-5" />
              Salvar preco
            </Button>
          </form>
        </Card>

        <Card className="bg-blush shadow-none">
          {configQuery.isLoading ? (
            <p className="py-10 text-center text-sm text-warm-gray">Carregando configuracoes...</p>
          ) : null}

          {configQuery.isError ? (
            <p className="rounded-lg border border-terracotta/30 bg-white px-4 py-3 text-sm text-terracotta-dark" role="alert">
              Nao foi possivel carregar o preco atual.
            </p>
          ) : null}

          {configQuery.data ? (
            <div className="space-y-5">
              <div>
                <Settings aria-hidden="true" className="h-10 w-10 text-terracotta" />
                <p className="mt-4 text-xs font-bold uppercase tracking-wide text-warm-gray">Preco vigente</p>
                <p className="mt-2 font-serif text-4xl font-bold text-charcoal">
                  {formatCurrency(configQuery.data.unitPrice)}
                </p>
              </div>

              <div className="rounded-lg bg-white/70 px-4 py-3 text-sm leading-relaxed text-warm-gray">
                Transacoes ja criadas mantem o valor com que nasceram. Esta configuracao so altera o preco usado daqui em diante.
              </div>

              {configQuery.data.updatedAt ? (
                <p className="text-xs text-warm-gray">Ultima atualizacao: {formatDateTime(configQuery.data.updatedAt)}</p>
              ) : null}
            </div>
          ) : null}
        </Card>
      </section>
    </main>
  );
}
