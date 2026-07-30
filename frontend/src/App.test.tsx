import { render, screen } from '@testing-library/react';

import { App } from './App';

describe('App', () => {
  it('renders the application root', () => {
    render(<App />);

    expect(screen.getByRole('main', { name: 'Application root' })).toBeInTheDocument();
  });
});
