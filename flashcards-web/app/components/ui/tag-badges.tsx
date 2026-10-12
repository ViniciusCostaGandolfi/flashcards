import { cn } from "~/lib/utils";

interface TagBadgesProps {
  tags: string[] | null | undefined;
  /** Máximo de badges exibidos; o excedente vira "+N". */
  max?: number;
  size?: "xs" | "sm" | "md";
  className?: string;
  /** Prefixo visual (padrão "#"). */
  prefix?: string;
}

const sizeClass: Record<NonNullable<TagBadgesProps["size"]>, string> = {
  xs: "badge-xs",
  sm: "badge-sm",
  md: "badge-md",
};

/** Lista compacta de tags como badges daisyUI. Não renderiza nada se vazio. */
export function TagBadges({
  tags,
  max,
  size = "sm",
  className,
  prefix = "#",
}: TagBadgesProps) {
  if (!tags || tags.length === 0) return null;

  const shown = typeof max === "number" ? tags.slice(0, max) : tags;
  const rest = typeof max === "number" ? tags.length - shown.length : 0;

  return (
    <ul
      className={cn("flex flex-wrap items-center gap-1.5", className)}
      aria-label="Tags"
    >
      {shown.map((tag) => (
        <li key={tag}>
          <span
            className={cn(
              "badge badge-outline border-primary/40 bg-primary/10 text-primary",
              sizeClass[size],
            )}
          >
            {prefix}
            {tag}
          </span>
        </li>
      ))}
      {rest > 0 ? (
        <li>
          <span className={cn("badge badge-ghost", sizeClass[size])}>
            +{rest}
          </span>
        </li>
      ) : null}
    </ul>
  );
}

export default TagBadges;
