import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { FlagEmoji } from './FlagEmoji';

describe('FlagEmoji', () => {
  it.each(['🇧🇷', '🇳🇮', '🇸🇹'])('renders %s as a bundled SVG image', (emoji) => {
    render(<FlagEmoji emoji={emoji} />);

    const flag = screen.getByRole('img', { name: emoji });

    expect(flag.tagName).toBe('IMG');
    expect(flag).toHaveAttribute('src');
  });
});
