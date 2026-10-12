import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router";
import * as studyService from "~/services/study.service";
import * as cardService from "~/services/card.service";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { TagFilter } from "~/components/ui/tag-filter";
import { StudySession } from "~/components/study/study-session";

export default function StudyPage() {
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

  const dueQuery = useQuery({
    queryKey: ["study", "due", "all", tagFilter ?? "all"],
    queryFn: () => studyService.getDueCards(undefined, tagFilter ?? undefined),
  });

  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: cardService.listTags,
  });

  const cards = dueQuery.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Estudar"
        subtitle={
          tagFilter
            ? `Revisão dos cartões com a tag #${tagFilter}.`
            : "Revisão de todos os Cards."
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
          backTo="/decks"
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
