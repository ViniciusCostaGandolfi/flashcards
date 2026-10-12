import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import * as deckService from "~/services/deck.service";
import { formatDate } from "~/lib/utils";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";

export default function DecksPage() {
  const decksQuery = useQuery({
    queryKey: ["decks"],
    queryFn: deckService.listDecks,
  });

  const decks = decksQuery.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Cards"
        subtitle="Todos os seus Cards de estudo."
        actions={
          <Link to="/generate" className="btn btn-primary btn-sm">
            Novo Card
          </Link>
        }
      />

      <AsyncState
        isLoading={decksQuery.isLoading}
        isError={decksQuery.isError}
        error={decksQuery.error}
        onRetry={() => decksQuery.refetch()}
      >
        {decks.length === 0 ? (
          <EmptyState
            message="Você ainda não tem Cards."
            hint="Gere o primeiro a partir de uma imagem, PDF ou texto."
            action={
              <Link to="/generate" className="btn btn-primary btn-sm">
                Gerar cartões
              </Link>
            }
          />
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2">
            {decks.map((deck) => (
              <li key={deck.id}>
                <Link
                  to={`/decks/${deck.id}`}
                  className="card card-border border-base-200 bg-base-100 glow-hover block h-full"
                >
                  <div className="card-body gap-3">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="card-title font-display text-base">
                        {deck.title}
                      </h3>
                      {deck.dueCount > 0 ? (
                        <span className="badge badge-primary badge-sm">
                          {deck.dueCount} p/ revisar
                        </span>
                      ) : null}
                    </div>
                    <div className="flex flex-wrap items-center gap-2 text-xs text-base-content/60">
                      <span className="badge badge-ghost badge-sm">
                        {deck.sourceType}
                      </span>
                      <span>{deck.cardsCount} cartões</span>
                      <span aria-hidden="true">·</span>
                      <span>{formatDate(deck.createdAt)}</span>
                    </div>
                    <div className="card-actions mt-1 justify-end">
                      <span className="btn btn-ghost btn-xs">Abrir</span>
                    </div>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </AsyncState>
    </div>
  );
}
