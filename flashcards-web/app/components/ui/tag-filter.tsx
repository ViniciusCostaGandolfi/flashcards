import type { Tag } from "~/types/api";
import { cn } from "~/lib/utils";

interface TagFilterProps {
  tags: Tag[];
  selected: string | null;
  onSelect: (tag: string | null) => void;
  allLabel?: string;
  className?: string;
  /** Mostra apenas tags com cartões (padrão true). */
  onlyWithCards?: boolean;
}

/** Fila de chips para filtrar por tag. "Todas" limpa o filtro. */
export function TagFilter({
  tags,
  selected,
  onSelect,
  allLabel = "Todas",
  className,
  onlyWithCards = true,
}: TagFilterProps) {
  const visible = (onlyWithCards
    ? tags.filter((tag) => tag.cardsCount > 0)
    : tags
  ).slice();

  if (visible.length === 0) return null;

  return (
    <div
      role="group"
      aria-label="Filtrar por tag"
      className={cn(
        "-mx-1 flex gap-2 overflow-x-auto px-1 pb-1",
        className,
      )}
    >
      <button
        type="button"
        aria-pressed={selected === null}
        className={cn(
          "btn btn-xs shrink-0 rounded-full",
          selected === null ? "btn-primary" : "btn-outline",
        )}
        onClick={() => onSelect(null)}
      >
        {allLabel}
      </button>

      {visible.map((tag) => {
        const isActive = selected === tag.name;
        return (
          <button
            key={tag.id}
            type="button"
            aria-pressed={isActive}
            className={cn(
              "btn btn-xs shrink-0 rounded-full",
              isActive ? "btn-primary" : "btn-outline",
            )}
            onClick={() => onSelect(isActive ? null : tag.name)}
          >
            #{tag.name}
            <span className="opacity-60">{tag.cardsCount}</span>
          </button>
        );
      })}
    </div>
  );
}

export default TagFilter;
