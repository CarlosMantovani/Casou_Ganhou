import { FlagEmoji } from '../../components/ui/FlagEmoji';
import type { FlagRankingItem } from '../../types/home';

interface FlagRankingListProps {
  isLoading: boolean;
  ranking: FlagRankingItem[];
}

export function FlagRankingList({ isLoading, ranking }: FlagRankingListProps) {
  return (
    <>
      <div className="grid gap-2 sm:hidden">
        {isLoading ? (
          <p className="rounded-lg bg-ivory-deep px-4 py-6 text-center text-sm text-warm-gray">
            Carregando ranking...
          </p>
        ) : null}

        {!isLoading && ranking.length === 0 ? (
          <p className="rounded-lg bg-ivory-deep px-4 py-6 text-center text-sm text-warm-gray">
            Nenhuma bandeira pontuou ainda.
          </p>
        ) : null}

        {!isLoading
          ? ranking.map((item, index) => (
              <div
                className="flex min-w-0 items-center gap-3 rounded-lg border border-line bg-white px-3 py-3"
                key={item.code}
              >
                <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-ivory-deep text-xs font-bold text-green">
                  {index + 1}
                </span>
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-ivory-deep">
                  <FlagEmoji className="h-6 w-6" emoji={item.emoji} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-bold text-charcoal">{item.name}</span>
                  <span className="block truncate text-xs text-warm-gray">{item.code}</span>
                </span>
                <span className="shrink-0 text-right">
                  <span className="block font-bold text-green">{item.totalNumbers}</span>
                  <span className="block text-[11px] font-semibold uppercase text-warm-gray">números</span>
                </span>
              </div>
            ))
          : null}
      </div>

      <div className="hidden overflow-hidden rounded-lg border border-line sm:block">
        <table className="w-full table-fixed text-left text-sm">
          <caption className="sr-only">Ranking das bandeiras por números aprovados</caption>
          <thead className="bg-ivory-deep text-xs uppercase text-warm-gray">
            <tr>
              <th className="w-16 px-4 py-3 font-bold">Pos.</th>
              <th className="px-4 py-3 font-bold">Bandeira</th>
              <th className="w-24 px-4 py-3 text-right font-bold">Números</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line bg-white">
            {isLoading ? (
              <tr>
                <td className="px-4 py-6 text-center text-warm-gray" colSpan={3}>
                  Carregando ranking...
                </td>
              </tr>
            ) : null}

            {!isLoading && ranking.length === 0 ? (
              <tr>
                <td className="px-4 py-6 text-center text-warm-gray" colSpan={3}>
                  Nenhuma bandeira pontuou ainda.
                </td>
              </tr>
            ) : null}

            {!isLoading
              ? ranking.map((item, index) => (
                  <tr key={item.code}>
                    <td className="px-4 py-3 font-bold text-charcoal">{index + 1}</td>
                    <td className="min-w-0 px-4 py-3">
                      <span className="flex items-center gap-3">
                        <span className="grid h-10 w-10 place-items-center rounded-full bg-ivory-deep">
                          <FlagEmoji className="h-6 w-6" emoji={item.emoji} />
                        </span>
                        <span className="min-w-0">
                          <span className="block truncate font-bold text-charcoal">{item.name}</span>
                          <span className="block truncate text-xs text-warm-gray">{item.code}</span>
                        </span>
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-green">{item.totalNumbers}</td>
                  </tr>
                ))
              : null}
          </tbody>
        </table>
      </div>
    </>
  );
}
