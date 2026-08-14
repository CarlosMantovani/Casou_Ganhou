import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { App } from './App';
import { transactionService } from './services/transactionService';

vi.mock('./services/transactionService', () => ({
  transactionService: {
    create: vi.fn(),
    getStatus: vi.fn(),
    quote: vi.fn(),
  },
}));

const mockedTransactionService = vi.mocked(transactionService);
const originalLocation = window.location;

function renderApp(path = '/') {
  window.history.pushState({}, '', path);

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>,
  );
}

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
    window.history.pushState({}, '', '/');
    mockedTransactionService.quote.mockResolvedValue({
      email: 'guest@example.com',
      quantity: 1,
      unitPrice: '10.00',
      totalAmount: '10.00',
    });
  });

  it('blocks invalid email before the quantity step', async () => {
    const user = userEvent.setup();
    renderApp();

    await user.type(screen.getByLabelText('E-mail'), 'invalid');

    expect(await screen.findByText('Informe um e-mail válido.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
    expect(screen.queryByText('Quantos números você quer?')).not.toBeInTheDocument();
  });

  it('shows quote values when quantity changes', async () => {
    const user = userEvent.setup();
    mockedTransactionService.quote
      .mockResolvedValueOnce({
        email: 'guest@example.com',
        quantity: 1,
        unitPrice: '10.00',
        totalAmount: '10.00',
      })
      .mockResolvedValueOnce({
        email: 'guest@example.com',
        quantity: 2,
        unitPrice: '10.00',
        totalAmount: '20.00',
      });

    renderApp();

    await user.type(screen.getByLabelText('E-mail'), 'guest@example.com');
    await user.click(screen.getByRole('button', { name: 'Continuar' }));
    await screen.findAllByText('R$ 10,00');

    await user.click(screen.getByRole('button', { name: 'Aumentar quantidade' }));

    expect(await screen.findByText('R$ 20,00')).toBeInTheDocument();
  });

  it('creates transaction once and redirects to Mercado Pago checkout', async () => {
    const user = userEvent.setup();
    const assign = vi.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, assign },
    });
    mockedTransactionService.create.mockResolvedValue({
      checkoutUrl: 'https://checkout.example.com',
      externalReference: 'external-reference',
      preferenceId: 'preference-id',
    });

    renderApp();

    await user.type(screen.getByLabelText('E-mail'), 'guest@example.com');
    await user.click(screen.getByRole('button', { name: 'Continuar' }));
    await screen.findAllByText('R$ 10,00');

    await user.click(screen.getByRole('button', { name: /Pagar com Mercado Pago/i }));
    await waitFor(() => expect(mockedTransactionService.create).toHaveBeenCalledTimes(1));

    expect(mockedTransactionService.create).toHaveBeenCalledWith({ email: 'guest@example.com', quantity: 1 });
    expect(assign).toHaveBeenCalledWith('https://checkout.example.com');
  });

  it('renders approved payment numbers from backend status', async () => {
    mockedTransactionService.getStatus.mockResolvedValue({
      externalReference: 'external-reference',
      luckyNumbers: ['00042', '12345'],
      quantity: 2,
      status: 'APPROVED',
      totalAmount: '20.00',
    });

    renderApp('/payment-return/success?external_reference=external-reference');

    expect(await screen.findByText('00042')).toBeInTheDocument();
    expect(screen.getByText('12345')).toBeInTheDocument();
    expect(screen.getByText('Confirmação enviada por e-mail')).toBeInTheDocument();
  });

  it('renders pending payment message', async () => {
    mockedTransactionService.getStatus.mockResolvedValue({
      externalReference: 'external-reference',
      luckyNumbers: [],
      quantity: 1,
      status: 'PENDING',
      totalAmount: '10.00',
    });

    renderApp('/payment-return/pending?external_reference=external-reference');

    expect(await screen.findByText('Pagamento pendente')).toBeInTheDocument();
    expect(screen.getByText(/números serão gerados assim que a confirmação/i)).toBeInTheDocument();
  });

  it('renders a friendly error when external reference is missing', () => {
    renderApp('/payment-return/success');

    expect(screen.getByText('Não foi possível localizar sua compra')).toBeInTheDocument();
  });
});
