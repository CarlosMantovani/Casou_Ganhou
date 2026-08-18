import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { App } from './App';
import { adminTransactionService } from './services/adminTransactionService';
import { createAdminSession, storeAdminSession } from './services/adminSession';
import { authService } from './services/authService';
import { homeService } from './services/homeService';
import { raffleService } from './services/raffleService';
import { transactionService } from './services/transactionService';

vi.mock('./services/transactionService', () => ({
  transactionService: {
    create: vi.fn(),
    getLuckyNumbersPdfUrl: vi.fn(),
    getStatus: vi.fn(),
    quote: vi.fn(),
  },
}));

vi.mock('./services/homeService', () => ({
  homeService: {
    getSummary: vi.fn(),
  },
}));

vi.mock('./services/authService', () => ({
  authService: {
    login: vi.fn(),
  },
}));

vi.mock('./services/adminTransactionService', () => ({
  adminTransactionService: {
    createCashTransaction: vi.fn(),
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
const mockedHomeService = vi.mocked(homeService);
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
      name: 'Guest User',
      phone: '11999999999',
      quantity: 1,
      unitPrice: '10.00',
      totalAmount: '10.00',
    });
    mockedTransactionService.getLuckyNumbersPdfUrl.mockReturnValue(
      'http://localhost:8080/transactions/external-reference/lucky-numbers.pdf',
    );
    mockedHomeService.getSummary.mockResolvedValue({
      scheduledDrawAt: null,
      flagRanking: [
        {
          code: 'BRAZIL',
          emoji: '🇧🇷',
          name: 'Brasil',
          totalNumbers: 12,
        },
      ],
    });
    mockedAdminTransactionService.list.mockResolvedValue({
      content: [
        {
          createdAt: '2026-08-14T18:00:00-03:00',
          email: 'guest@example.com',
          externalReference: 'external-reference',
          luckyNumbers: ['00001', '00002'],
          name: 'Guest User',
          paymentMethod: 'MERCADO_PAGO',
          phone: '11999999999',
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

    await user.type(screen.getByLabelText('Nome'), 'Guest User');
    await user.type(screen.getByLabelText('Telefone'), '(11) 99999-9999');
    await user.type(screen.getByLabelText('E-mail (opcional)'), 'invalid');

    expect(await screen.findByText('Informe um e-mail valido.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
    expect(screen.queryByText('Quantos numeros voce quer?')).not.toBeInTheDocument();
  });

  it('renders the public flag ranking on the purchase page', async () => {
    renderApp();

    expect(await screen.findByText('Ranking de bandeiras')).toBeInTheDocument();
    expect(screen.getByText(/primeiro lugar tambem ganhara um premio/i)).toBeInTheDocument();
    expect(await screen.findByText('Brasil')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '🇧🇷' })).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  it('requires name and phone before the quantity step', async () => {
    const user = userEvent.setup();
    renderApp();

    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();

    await user.type(screen.getByLabelText('Nome'), 'Guest User');

    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
    expect(screen.queryByText('Quantos numeros voce quer?')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('Telefone'), '44988549696');

    expect(screen.getByLabelText('Telefone')).toHaveValue('(44) 98854-9696');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled();
  });

  it('shows quote values when quantity changes', async () => {
    const user = userEvent.setup();
    mockedTransactionService.quote
      .mockResolvedValueOnce({
        email: 'guest@example.com',
        name: 'Guest User',
        phone: '11999999999',
        quantity: 1,
        unitPrice: '10.00',
        totalAmount: '10.00',
      })
      .mockResolvedValueOnce({
        email: 'guest@example.com',
        name: 'Guest User',
        phone: '11999999999',
        quantity: 2,
        unitPrice: '10.00',
        totalAmount: '20.00',
      });

    renderApp();

    await user.type(screen.getByLabelText('Nome'), 'Guest User');
    await user.type(screen.getByLabelText('Telefone'), '11999999999');
    await user.type(screen.getByLabelText('E-mail (opcional)'), 'guest@example.com');
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

    await user.type(screen.getByLabelText('Nome'), 'Guest User');
    await user.type(screen.getByLabelText('Telefone'), '(11) 99999-9999');
    await user.type(screen.getByLabelText('E-mail (opcional)'), 'guest@example.com');
    await user.click(screen.getByRole('button', { name: 'Continuar' }));
    await screen.findAllByText('R$ 10,00');

    await user.click(screen.getByRole('button', { name: /Pagar com Mercado Pago/i }));
    await waitFor(() => expect(mockedTransactionService.create).toHaveBeenCalledTimes(1));

    expect(mockedTransactionService.create).toHaveBeenCalledWith({
      email: 'guest@example.com',
      name: 'Guest User',
      phone: '11999999999',
      quantity: 1,
    });
    expect(assign).toHaveBeenCalledWith('https://checkout.example.com');
  });

  it('renders approved payment numbers from backend status', async () => {
    mockedTransactionService.getStatus.mockResolvedValue({
      externalReference: 'external-reference',
      emailProvided: true,
      luckyNumbers: ['00042', '12345'],
      participantFlagEmoji: '🇧🇷',
      participantFlagName: 'Brasil',
      quantity: 2,
      status: 'APPROVED',
      totalAmount: '20.00',
    });

    renderApp('/payment-return/success?external_reference=external-reference');

    expect(await screen.findByText('00042')).toBeInTheDocument();
    expect(screen.getByText('12345')).toBeInTheDocument();
    expect(screen.getByText('Sua bandeira')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '🇧🇷' })).toBeInTheDocument();
    expect(screen.getByText('Brasil')).toBeInTheDocument();
    expect(screen.getByText('Confirmacao enviada por e-mail')).toBeInTheDocument();
  });

  it('renders pdf download when approved payment has no email', async () => {
    mockedTransactionService.getStatus.mockResolvedValue({
      externalReference: 'external-reference',
      emailProvided: false,
      luckyNumbers: ['00042'],
      participantFlagEmoji: '🇧🇷',
      participantFlagName: 'Brasil',
      quantity: 1,
      status: 'APPROVED',
      totalAmount: '10.00',
    });

    renderApp('/payment-return/success?external_reference=external-reference');

    expect(await screen.findByText('Baixe seus numeros agora')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Baixar PDF/i })).toHaveAttribute(
      'href',
      'http://localhost:8080/transactions/external-reference/lucky-numbers.pdf',
    );
  });

  it('renders pending payment message', async () => {
    mockedTransactionService.getStatus.mockResolvedValue({
      externalReference: 'external-reference',
      emailProvided: false,
      luckyNumbers: [],
      participantFlagEmoji: '🇧🇷',
      participantFlagName: 'Brasil',
      quantity: 1,
      status: 'PENDING',
      totalAmount: '10.00',
    });

    renderApp('/payment-return/pending?external_reference=external-reference');

    expect(await screen.findByText('Pagamento pendente')).toBeInTheDocument();
    expect(screen.getByText(/numeros serao gerados assim que a confirmacao/i)).toBeInTheDocument();
  });

  it('renders a friendly error when external reference is missing', () => {
    renderApp('/payment-return/success');

    expect(screen.getByText('Nao foi possivel localizar sua compra')).toBeInTheDocument();
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
    expect(screen.getByText('14/08/2026, 18:00')).toBeInTheDocument();
    expect(screen.getByText('00001')).toBeInTheDocument();
    expect(screen.getByText('(11) 99999-9999')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Buscar por nome ou e-mail'), 'guest');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    await waitFor(() =>
      expect(mockedAdminTransactionService.list).toHaveBeenLastCalledWith({ query: 'guest', page: 0, size: 20 }),
    );
  });

  it('expands and collapses transaction lucky numbers above the initial limit', async () => {
    const user = userEvent.setup();
    const luckyNumbers = Array.from({ length: 10 }, (_, index) => String(index + 1).padStart(5, '0'));
    mockedAdminTransactionService.list.mockResolvedValue({
      content: [
        {
          createdAt: '2026-08-14T18:00:00-03:00',
          email: 'guest@example.com',
          externalReference: 'external-reference',
          luckyNumbers,
          name: 'Guest User',
          paymentMethod: 'MERCADO_PAGO',
          phone: '11999999999',
          quantity: 10,
          status: 'APPROVED',
          totalAmount: '100.00',
        },
      ],
      first: true,
      last: true,
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));

    renderApp('/admin');

    const transactionRow = await screen.findByRole('button', { name: /Guest User/ });
    expect(screen.getByText('00008')).toBeInTheDocument();
    expect(screen.queryByText('00009')).not.toBeInTheDocument();

    await user.click(transactionRow);

    expect(screen.getByText('00009')).toBeInTheDocument();
    expect(screen.getByText('00010')).toBeInTheDocument();

    await user.click(transactionRow);

    expect(screen.queryByText('00009')).not.toBeInTheDocument();
  });

  it('registers an admin cash payment and shows pdf link', async () => {
    const user = userEvent.setup();
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));
    mockedAdminTransactionService.createCashTransaction.mockResolvedValue({
      email: null,
      externalReference: 'cash-reference',
      luckyNumbers: ['00077'],
      name: 'Cash Guest',
      paymentMethod: 'CASH',
      phone: '11999999999',
      quantity: 1,
      status: 'APPROVED',
      totalAmount: '10.00',
    });
    mockedTransactionService.getLuckyNumbersPdfUrl.mockReturnValue(
      'http://localhost:8080/transactions/cash-reference/lucky-numbers.pdf',
    );

    renderApp('/admin/cash-payment');

    await user.type(await screen.findByLabelText('Nome'), 'Cash Guest');
    await user.type(screen.getByLabelText('Telefone'), '(11) 99999-9999');
    await user.clear(screen.getByLabelText('Quantidade'));
    await user.type(screen.getByLabelText('Quantidade'), '1');
    await user.click(screen.getByRole('button', { name: /Confirmar pagamento/i }));

    expect(await screen.findByText('00077')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Baixar PDF/i })).toHaveAttribute(
      'href',
      'http://localhost:8080/transactions/cash-reference/lucky-numbers.pdf',
    );
  });

  it('renders existing raffle result without drawing again', async () => {
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));
    mockedRaffleService.getResult.mockResolvedValue({
      drawnAt: '2026-07-30T12:00:00Z',
      winnerName: 'Winner Guest',
      winningNumber: '00042',
    });

    renderApp('/admin/draw');

    expect(await screen.findByText('00042')).toBeInTheDocument();
    expect(screen.getByText('Winner Guest')).toBeInTheDocument();
    expect(mockedRaffleService.draw).not.toHaveBeenCalled();
  });

  it('confirms and runs raffle draw when no result exists', async () => {
    const user = userEvent.setup();
    storeAdminSession(createAdminSession({ accessToken: 'jwt-token', expiresIn: 3600, tokenType: 'Bearer' }));
    mockedRaffleService.getResult.mockRejectedValue({ status: 404 });
    mockedRaffleService.draw.mockResolvedValue({
      drawnAt: '2026-07-30T12:00:00Z',
      winnerName: 'Winner Guest',
      winningNumber: '00042',
    });

    renderApp('/admin/draw');

    await user.click(await screen.findByRole('button', { name: 'Sortear vencedor' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar' }));

    await waitFor(() => expect(mockedRaffleService.draw).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('00042')).toBeInTheDocument();
  });
});
