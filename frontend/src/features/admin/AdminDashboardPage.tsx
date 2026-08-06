import { useQuery } from '@tanstack/react-query';
import { Gift, LogOut, ReceiptText, Search, Settings, Ticket } from 'lucide-react';
import { useMemo, useState } from 'react';

import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { adminTransactionService } from '../../services/adminTransactionService';
import type { AdminTransactionResponse } from '../../types/admin';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { useAuth } from './AuthContext';

const PAGE_SIZE = 20;
const EMPTY_TRANSACTIONS: AdminTransactionResponse[] = [];

export function AdminDashboardPage() {
  const { logout } = useAuth();
  const [queryFilter, setQueryFilter] = useState('');
  const [submittedQueryFilter, setSubmittedQueryFilter] = useState('');
  const [page, setPage] = useState(0);

  const transactionsQuery = useQuery({
    queryKey: ['admin-transactions', submittedQueryFilter, page],
    queryFn: () => adminTransactionService.list({ query: submittedQueryFilter, page, size: PAGE_SIZE }),
  });

  const transactions = transactionsQuery.data?.content ?? EMPTY_TRANSACTIONS;
  const approvedTransactions = useMemo(
    () => transactions.filter((transaction) => transaction.status === 'APPROVED'),
    [transactions],
  );
  const soldNumbersCount = approvedTransactions.reduce((total, transaction) => total + transaction.luckyNumbers.length, 0);
  const approvedAmount = approvedTransactions.reduce((total, transaction) => total + Number(transaction.totalAmount), 0);

  const submitFilter = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setSubmittedQueryFilter(queryFilter.trim());
  };

  return (
    <main className="min-h-screen bg-cream text-charcoal">
      <header className="bg-charcoal px-6 py-4 text-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4">
          <div>
            <p className="font-serif text-2xl font-bold">
              Presente <span className="italic text-gold">Premiado</span>
            </p>
            <p className="mt-1 text-xs text-white/55">Painel administrativo</p>
          </div>
          <div className="flex flex-wrap items-center justify-end gap-3">
            <a
              className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-white/10 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/15"
              href="/admin/cash-payment"
            >
              <ReceiptText aria-hidden="true" className="h-4 w-4" />
              Dinheiro
            </a>
            <a
              className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-white/10 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/15"
              href="/admin/settings"
            >
              <Settings aria-hidden="true" className="h-4 w-4" />
              Configurações
            </a>
            <a
              className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-gold px-4 py-2 text-sm font-bold text-charcoal transition hover:bg-gold/90"
              href="/admin/draw"
            >
              <Gift aria-hidden="true" className="h-4 w-4" />
              Sorteio
            </a>
            <button
              className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-white/20 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/10"
              onClick={logout}
              type="button"
            >
              <LogOut aria-hidden="true" className="h-4 w-4" />
              Sair
            </button>
          </div>
        </div>
      </header>

      <section className="mx-auto max-w-6xl px-6 py-8">
        <div className="grid gap-4 md:grid-cols-3">
          <MetricCard label="Transacoes nesta pagina" value={String(transactions.length)} />
          <MetricCard label="Numeros aprovados nesta pagina" value={String(soldNumbersCount)} />
          <MetricCard label="Receita aprovada nesta pagina" value={formatCurrency(approvedAmount)} />
        </div>

        <Card className="mt-6 overflow-hidden">
          <form className="mb-6 flex flex-col gap-3 md:flex-row md:items-end" onSubmit={submitFilter}>
            <div className="flex-1">
              <TextInput
                id="admin-query-filter"
                label="Buscar por nome ou e-mail"
                onChange={(event) => setQueryFilter(event.target.value)}
                placeholder="nome ou email@exemplo.com"
                value={queryFilter}
              />
            </div>
            <div className="md:w-44">
              <Button type="submit">
                <Search aria-hidden="true" className="h-4 w-4" />
                Buscar
              </Button>
            </div>
          </form>

          {transactionsQuery.isLoading ? <p className="py-10 text-center text-sm text-warm-gray">Carregando transacoes...</p> : null}

          {transactionsQuery.isError ? (
            <p className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark" role="alert">
              Nao foi possivel carregar as transacoes.
            </p>
          ) : null}

          {!transactionsQuery.isLoading && !transactionsQuery.isError && transactions.length === 0 ? (
            <p className="py-10 text-center text-sm text-warm-gray">Nenhuma transacao encontrada.</p>
          ) : null}

          {transactions.length > 0 ? <TransactionTable transactions={transactions} /> : null}

          {transactionsQuery.data ? (
            <div className="mt-6 flex items-center justify-between gap-4 border-t border-[#EEE6DF] pt-4">
              <p className="text-sm text-warm-gray">
                Pagina {transactionsQuery.data.number + 1} de {Math.max(transactionsQuery.data.totalPages, 1)}
              </p>
              <div className="flex gap-2">
                <button
                  className="rounded-lg border border-[#DDD2CB] px-4 py-2 text-sm font-semibold text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
                  disabled={transactionsQuery.data.first}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  type="button"
                >
                  Anterior
                </button>
                <button
                  className="rounded-lg border border-[#DDD2CB] px-4 py-2 text-sm font-semibold text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
                  disabled={transactionsQuery.data.last}
                  onClick={() => setPage((current) => current + 1)}
                  type="button"
                >
                  Proxima
                </button>
              </div>
            </div>
          ) : null}
        </Card>
      </section>
    </main>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <Card className="text-center">
      <p className="text-xs font-bold uppercase tracking-wide text-warm-gray">{label}</p>
      <p className="mt-3 font-serif text-3xl font-bold text-charcoal">{value}</p>
    </Card>
  );
}

function TransactionTable({ transactions }: { transactions: AdminTransactionResponse[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="border-b border-[#E7DDD6] text-xs uppercase text-warm-gray">
          <tr>
            <th className="px-3 py-3 font-bold">Nome</th>
            <th className="px-3 py-3 font-bold">Contato</th>
            <th className="px-3 py-3 font-bold">Metodo</th>
            <th className="px-3 py-3 font-bold">Compra</th>
            <th className="px-3 py-3 font-bold">Qtd.</th>
            <th className="px-3 py-3 font-bold">Total</th>
            <th className="px-3 py-3 font-bold">Status</th>
            <th className="px-3 py-3 font-bold">Numeros</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-[#EEE6DF]">
          {transactions.map((transaction) => (
            <tr key={transaction.externalReference}>
              <td className="px-3 py-4 font-medium text-charcoal">{transaction.name}</td>
              <td className="px-3 py-4 text-warm-gray">
                <span className="block">{transaction.phone}</span>
                <span className="block text-xs">{transaction.email || '-'}</span>
              </td>
              <td className="px-3 py-4 text-warm-gray">{transaction.paymentMethod === 'CASH' ? 'Dinheiro' : 'Mercado Pago'}</td>
              <td className="px-3 py-4 text-warm-gray">{formatDateTime(transaction.createdAt)}</td>
              <td className="px-3 py-4 text-warm-gray">{transaction.quantity}</td>
              <td className="px-3 py-4 text-warm-gray">{formatCurrency(transaction.totalAmount)}</td>
              <td className="px-3 py-4">
                <StatusBadge status={transaction.status} />
              </td>
              <td className="px-3 py-4">
                {transaction.luckyNumbers.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {transaction.luckyNumbers.map((number) => (
                      <span className="inline-flex items-center gap-1 rounded-md bg-gold/20 px-2 py-1 text-xs font-bold text-charcoal" key={number}>
                        <Ticket aria-hidden="true" className="h-3 w-3" />
                        {number}
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className="text-warm-gray">-</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function StatusBadge({ status }: { status: AdminTransactionResponse['status'] }) {
  const styles = {
    APPROVED: 'bg-olive/15 text-olive',
    PENDING: 'bg-gold/15 text-[#8A6A00]',
    REJECTED: 'bg-terracotta/15 text-terracotta-dark',
    CANCELLED: 'bg-terracotta/15 text-terracotta-dark',
  };

  return <span className={`rounded-full px-3 py-1 text-xs font-bold ${styles[status]}`}>{status}</span>;
}
