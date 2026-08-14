import { BuyNumbersPage } from './features/buy-numbers/BuyNumbersPage';
import { PaymentReturnPage } from './features/payment-return/PaymentReturnPage';

export function App() {
  const path = window.location.pathname;

  if (path.startsWith('/payment-return/')) {
    return <PaymentReturnPage />;
  }

  return <BuyNumbersPage />;
}
