import twemoji from 'twemoji';

import argentinaFlagUrl from '@twemoji/svg/1f1e6-1f1f7.svg?url';
import brazilFlagUrl from '@twemoji/svg/1f1e7-1f1f7.svg?url';
import canadaFlagUrl from '@twemoji/svg/1f1e8-1f1e6.svg?url';
import chileFlagUrl from '@twemoji/svg/1f1e8-1f1f1.svg?url';
import germanyFlagUrl from '@twemoji/svg/1f1e9-1f1ea.svg?url';
import spainFlagUrl from '@twemoji/svg/1f1ea-1f1f8.svg?url';
import franceFlagUrl from '@twemoji/svg/1f1eb-1f1f7.svg?url';
import italyFlagUrl from '@twemoji/svg/1f1ee-1f1f9.svg?url';
import japanFlagUrl from '@twemoji/svg/1f1ef-1f1f5.svg?url';
import mexicoFlagUrl from '@twemoji/svg/1f1f2-1f1fd.svg?url';
import portugalFlagUrl from '@twemoji/svg/1f1f5-1f1f9.svg?url';
import uruguayFlagUrl from '@twemoji/svg/1f1fa-1f1fe.svg?url';

const flagAssets: Record<string, string> = {
  '1f1e6-1f1f7': argentinaFlagUrl,
  '1f1e7-1f1f7': brazilFlagUrl,
  '1f1e8-1f1e6': canadaFlagUrl,
  '1f1e8-1f1f1': chileFlagUrl,
  '1f1e9-1f1ea': germanyFlagUrl,
  '1f1ea-1f1f8': spainFlagUrl,
  '1f1eb-1f1f7': franceFlagUrl,
  '1f1ee-1f1f9': italyFlagUrl,
  '1f1ef-1f1f5': japanFlagUrl,
  '1f1f2-1f1fd': mexicoFlagUrl,
  '1f1f5-1f1f9': portugalFlagUrl,
  '1f1fa-1f1fe': uruguayFlagUrl,
};

interface FlagEmojiProps {
  className?: string;
  emoji: string;
}

export function FlagEmoji({ className = '', emoji }: FlagEmojiProps) {
  const codePoint = twemoji.convert.toCodePoint(emoji).toLowerCase();
  const assetUrl = flagAssets[codePoint];

  if (!assetUrl) {
    return (
      <span aria-label={emoji} className={className} role="img">
        {emoji}
      </span>
    );
  }

  return <img alt={emoji} className={className} draggable={false} src={assetUrl} />;
}
