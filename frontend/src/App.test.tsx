import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { App } from './App';
import { adminTransactionService } from './services/adminTransactionService';
import { createAdminSession, storeAdminSession } from './services/adminSession';
import { authService } from './services/authService';
import { raffleService } from './services/raffleService';
import { transactionService } from './services/transactionService';

vi.mock('./services/transactionService', () => ({
  transactionService: {
    create: vi.fn(),
    getStatus: vi.fn(),
    quote: vi.fn(),
  },
}));

vi.mock('./services/authService', () => ({
  authService: {
    login: vi.fn(),
  },
}));

vi.mock('./services/adminTransactionService', () => ({
  adminTransactionService: {
    list: vi.fn(),
  },
}));

vi.mock('./services/raffleService', () => ({
  raffleService: {
    draw: vi.fn(),
    getResult: vi.fn(),
  },
}));

const mockedTransactionService = vi.mocked(transactionService);
const mockedAuthService = vi.mocked(authService);
const mockedAdminTransactionService = vi.mocked(adminTransactionService);
const mockedRaffleService = vi.mocked(raffleService);
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
    window.sessionStorage.clear();
    mockedTransactionService.quote.mockResolvedValue({
      email: 'guest@example.com',
      quantity: 1,
      unitPrice: '10.00',
      totalAmount: '10.00',
    });
    mockedAdminTransactionService.list.mockResolvedValue({
      content: [
        {
          email: 'guest@example.com',
          externalReference: 'external-reference',
          luckyNumbers: ['00001', '00002'],
          quantity: 2,
          status: 'APPROVED',
          totalAmount: '20.00',
        },
      ],
      first: true,
      last: true,
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
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

  it('redirects protected admin route to login without session', async () => {
    renderApp('/admin');

    expect(await screen.findByText('Área Administrativa')).toBeInTheDocument();
    expect(screen.getByLabelText('Usuário')).toBeInTheDocument();
  });

  it('logs admin in and renders dashboard', async () => {
    const user = userEvent.setup();
    mockedAuthService.login.mockImplementation(async () => {
      const response = { accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' };
      storeAdminSession(createAdminSession(response));
      return response;
    });

    renderApp('/admin/login');

    await user.type(await screen.findByLabelText('Usuário'), 'admin');
    await user.type(screen.getByLabelText('Senha'), 'password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByText('Painel administrativo')).toBeInTheDocument();
    expect(mockedAuthService.login).toHaveBeenCalledWith({ username: 'admin', password: 'password' });
  });

  it('lists admin transactions with email filter', async () => {
    const user = userEvent.setup();
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));

    renderApp('/admin');

    expect(await screen.findByText('guest@example.com')).toBeInTheDocument();
    expect(screen.getByText('00001')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Buscar por e-mail'), 'guest');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    await waitFor(() =>
      expect(mockedAdminTransactionService.list).toHaveBeenLastCalledWith({ email: 'guest', page: 0, size: 20 }),
    );
  });

  it('renders existing raffle result without drawing again', async () => {
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));
    mockedRaffleService.getResult.mockResolvedValue({
      drawnAt: '2026-07-30T12:00:00Z',
      winnerEmail: 'winner@example.com',
      winningNumber: '00042',
    });

    renderApp('/admin/draw');

    expect(await screen.findByText('00042')).toBeInTheDocument();
    expect(screen.getByText('winner@example.com')).toBeInTheDocument();
    expect(mockedRaffleService.draw).not.toHaveBeenCalled();
  });

  it('confirms and runs raffle draw when no result exists', async () => {
    const user = userEvent.setup();
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));
    mockedRaffleService.getResult.mockRejectedValue({ status: 404 });
    mockedRaffleService.draw.mockResolvedValue({
      drawnAt: '2026-07-30T12:00:00Z',
      winnerEmail: 'winner@example.com',
      winningNumber: '00042',
    });

    renderApp('/admin/draw');

    await user.click(await screen.findByRole('button', { name: 'Sortear vencedor' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar' }));

    await waitFor(() => expect(mockedRaffleService.draw).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('00042')).toBeInTheDocument();
  });
});
