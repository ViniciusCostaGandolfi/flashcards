import { Link, useParams, useSearchParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import * as studyService from "~/services/study.service";
import * as deckService from "~/services/deck.service";
import * as cardService from "~/services/card.service";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { TagFilter } from "~/components/ui/tag-filter";
import { StudySession } from "~/components/study/study-session";

export default function StudyDeckPage() {
  const { deckId } = useParams<{ deckId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const tagFilter = searchParams.get("tag");

  const setTagFilter = (tag: string | null) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (tag) next.set("tag", tag);
        else next.delete("tag");
        return next;
      },
      { replace: true },
    );
  };

  const deckQuery = useQuery({
    queryKey: ["deck", deckId],
    queryFn: () => deckService.getDeck(deckId as string),
    enabled: !!deckId,
  });

  const dueQuery = useQuery({
    queryKey: ["study", "due", deckId, tagFilter ?? "all"],
    queryFn: () =>
      studyService.getDueCards(deckId as string, tagFilter ?? undefined),
    enabled: !!deckId,
  });

  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: cardService.listTags,
  });

  const cards = dueQuery.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-2 text-sm">
        <Link to="/decks" className="link link-hover text-base-content/60">
          Baralhos
        </Link>
        <span aria-hidden="true" className="text-base-content/40">
          /
        </span>
        <Link
          to={`/decks/${deckId}`}
          className="link link-hover text-base-content/60"
        >
          {deckQuery.data?.title ?? "..."}
        </Link>
        <span aria-hidden="true" className="text-base-content/40">
          /
        </span>
        <span className="font-medium">Estudar</span>
      </div>

      <PageHeader
        title={deckQuery.data?.title ?? "Estudar"}
        subtitle={
          tagFilter
            ? `Cartões devidos com a tag #${tagFilter}.`
            : "Cartões devidos deste baralho."
        }
      />

      <TagFilter
        tags={tagsQuery.data ?? []}
        selected={tagFilter}
        onSelect={setTagFilter}
      />

      <AsyncState
        isLoading={dueQuery.isLoading}
        isError={dueQuery.isError}
        error={dueQuery.error}
        onRetry={() => dueQuery.refetch()}
      >
        <StudySession
          cards={cards}
          backTo={`/decks/${deckId}`}
          backLabel="Voltar ao baralho"
          emptyMessage={
            tagFilter
              ? `Nenhum cartão devido com a tag #${tagFilter}.`
              : undefined
          }
        />
      </AsyncState>
    </div>
  );
}
