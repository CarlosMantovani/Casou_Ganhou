import { lazy, Suspense } from 'react';

import { BuyNumbersPage } from './features/buy-numbers/BuyNumbersPage';
import { FlagRankingPage } from './features/flag-ranking/FlagRankingPage';
import { PaymentReturnPage } from './features/payment-return/PaymentReturnPage';

const AdminApp = lazy(() => import('./features/admin/AdminApp'));

export function App() {
  const path = window.location.pathname;

  if (path.startsWith('/admin')) {
    return (
      <Suspense
        fallback={
          <main className="grid min-h-screen place-items-center bg-cream px-6 text-charcoal">
            <p className="text-sm font-semibold text-warm-gray">Carregando área administrativa...</p>
          </main>
        }
      >
        <AdminApp />
      </Suspense>
    );
  }

  if (path.startsWith('/payment-return/')) {
    return <PaymentReturnPage />;
  }

  if (path === '/flag-ranking') {
    return <FlagRankingPage />;
  }

  return <BuyNumbersPage />;
}
