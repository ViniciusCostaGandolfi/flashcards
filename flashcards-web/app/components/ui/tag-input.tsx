import { useId, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as cardService from "~/services/card.service";
import { cn } from "~/lib/utils";

interface TagInputProps {
  value: string[];
  onChange: (tags: string[]) => void;
  /** Habilita a criação inline de tags novas (POST /tags). */
  allowCreate?: boolean;
  placeholder?: string;
  maxTags?: number;
  disabled?: boolean;
  label?: string;
  describedBy?: string;
  invalid?: boolean;
  className?: string;
}

function normalize(raw: string): string {
  return raw.trim().replace(/\s+/g, " ");
}

const MAX_SUGGESTIONS = 8;

/**
 * Entrada de tags em "chips": digite e pressione Enter (ou vírgula) para
 * adicionar, clique no × para remover. Sugestões vêm de `GET /tags`; com
 * `allowCreate` também permite criar uma tag nova na hora.
 *
 * Controlado (value/onChange) — use com `Controller` do react-hook-form.
 */
export function TagInput({
  value,
  onChange,
  allowCreate = false,
  placeholder = "Digite e pressione Enter...",
  maxTags,
  disabled = false,
  label = "Tags",
  describedBy,
  invalid = false,
  className,
}: TagInputProps) {
  const [input, setInput] = useState("");
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();
  const baseId = useId();
  const listId = `${baseId}-listbox`;

  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: cardService.listTags,
    staleTime: 60_000,
  });

  const allTags = tagsQuery.data ?? [];

  const selectedLower = useMemo(
    () => new Set(value.map((tag) => tag.toLowerCase())),
    [value],
  );

  const suggestions = useMemo(() => {
    const term = input.trim().toLowerCase();
    return allTags
      .filter(
        (tag) =>
          !selectedLower.has(tag.name.toLowerCase()) &&
          (term === "" || tag.name.toLowerCase().includes(term)),
      )
      .slice(0, MAX_SUGGESTIONS);
  }, [allTags, input, selectedLower]);

  const existingLower = useMemo(
    () => new Set(allTags.map((tag) => tag.name.toLowerCase())),
    [allTags],
  );

  const createMutation = useMutation({
    mutationFn: (name: string) => cardService.createTag(name),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["tags"] });
    },
    // 409 (tag já existe) não deve travar o fluxo — a tag é adicionada mesmo assim.
    onError: () => {
      void queryClient.invalidateQueries({ queryKey: ["tags"] });
    },
  });

  const atLimit = typeof maxTags === "number" && value.length >= maxTags;

  const commit = (raw: string) => {
    const name = normalize(raw);
    if (!name || atLimit || disabled) return;
    if (selectedLower.has(name.toLowerCase())) {
      setInput("");
      setHighlight(-1);
      return;
    }
    if (allowCreate && !existingLower.has(name.toLowerCase())) {
      createMutation.mutate(name);
    }
    onChange([...value, name]);
    setInput("");
    setHighlight(-1);
    inputRef.current?.focus();
  };

  const removeAt = (index: number) => {
    if (disabled) return;
    onChange(value.filter((_, i) => i !== index));
    inputRef.current?.focus();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter") {
      event.preventDefault();
      const highlighted = highlight >= 0 ? suggestions[highlight] : undefined;
      commit(highlighted ? highlighted.name : input);
    } else if (event.key === ",") {
      event.preventDefault();
      commit(input);
    } else if (event.key === "Backspace" && input === "" && value.length > 0) {
      event.preventDefault();
      removeAt(value.length - 1);
    } else if (event.key === "ArrowDown") {
      event.preventDefault();
      setOpen(true);
      setHighlight((current) =>
        suggestions.length === 0
          ? -1
          : Math.min(current + 1, suggestions.length - 1),
      );
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setHighlight((current) => Math.max(current - 1, 0));
    } else if (event.key === "Escape") {
      setOpen(false);
      setHighlight(-1);
    }
  };

  const showCreate =
    allowCreate &&
    !atLimit &&
    input.trim() !== "" &&
    !existingLower.has(input.trim().toLowerCase()) &&
    !selectedLower.has(input.trim().toLowerCase());

  const showList = open && (suggestions.length > 0 || showCreate);

  return (
    <div className={cn("relative", className)}>
      <div
        className={cn(
          "flex min-h-11 flex-wrap items-center gap-1.5 rounded-box border bg-base-100 px-2 py-1.5 transition-colors",
          invalid ? "border-error" : "border-base-300",
          "focus-within:border-primary focus-within:ring-1 focus-within:ring-primary/30",
          disabled && "opacity-60",
        )}
        onBlur={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget as Node)) {
            setOpen(false);
            setHighlight(-1);
          }
        }}
      >
        {value.map((tag, index) => (
          <span
            key={`${tag}-${index}`}
            className="badge badge-primary badge-sm gap-1 pr-1"
          >
            <span className="max-w-40 truncate">#{tag}</span>
            <button
              type="button"
              className="rounded-full p-0.5 transition-colors hover:bg-primary-content/25 disabled:cursor-not-allowed"
              aria-label={`Remover tag ${tag}`}
              onClick={() => removeAt(index)}
              disabled={disabled}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
                className="h-3.5 w-3.5"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M4.293 4.293a1 1 0 0 1 1.414 0L10 8.586l4.293-4.293a1 1 0 1 1 1.414 1.414L11.414 10l4.293 4.293a1 1 0 0 1-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 0 1-1.414-1.414L8.586 10 4.293 5.707a1 1 0 0 1 0-1.414Z"
                  clipRule="evenodd"
                />
              </svg>
            </button>
          </span>
        ))}

        <input
          ref={inputRef}
          id={`${baseId}-input`}
          type="text"
          role="combobox"
          className="h-7 min-w-24 flex-1 bg-transparent text-sm outline-none placeholder:text-base-content/40 disabled:cursor-not-allowed"
          value={input}
          placeholder={value.length === 0 ? placeholder : ""}
          aria-label={label}
          aria-expanded={showList}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={
            highlight >= 0 && suggestions[highlight]
              ? `${baseId}-option-${highlight}`
              : undefined
          }
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          disabled={disabled || atLimit}
          onChange={(event) => {
            setInput(event.target.value);
            setOpen(true);
            setHighlight(-1);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
        />
      </div>

      {atLimit ? (
        <span className="mt-1 block text-xs text-base-content/60">
          Limite de {maxTags} tags atingido.
        </span>
      ) : null}

      {showList ? (
        <ul
          id={listId}
          role="listbox"
          aria-label="Sugestões de tags"
          className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-box border border-base-200 bg-base-100 p-1 shadow-lg"
        >
          {suggestions.map((tag, index) => (
            <li key={tag.id} role="presentation">
              <button
                type="button"
                id={`${baseId}-option-${index}`}
                role="option"
                aria-selected={highlight === index}
                className={cn(
                  "flex w-full items-center justify-between gap-2 rounded-field px-3 py-2 text-left text-sm transition-colors hover:bg-base-200",
                  highlight === index && "bg-base-200",
                )}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => commit(tag.name)}
              >
                <span className="truncate">#{tag.name}</span>
                <span className="shrink-0 text-xs text-base-content/50">
                  {tag.cardsCount} {tag.cardsCount === 1 ? "cartão" : "cartões"}
                </span>
              </button>
            </li>
          ))}

          {showCreate ? (
            <li role="presentation">
              <button
                type="button"
                className="flex w-full items-center gap-2 rounded-field px-3 py-2 text-left text-sm text-primary transition-colors hover:bg-primary/10"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => commit(input)}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  className="h-4 w-4"
                  aria-hidden="true"
                >
                  <path d="M10.75 4.75a.75.75 0 0 0-1.5 0v4.5h-4.5a.75.75 0 0 0 0 1.5h4.5v4.5a.75.75 0 0 0 1.5 0v-4.5h4.5a.75.75 0 0 0 0-1.5h-4.5v-4.5Z" />
                </svg>
                <span className="truncate">
                  Criar tag &ldquo;{normalize(input)}&rdquo;
                </span>
              </button>
            </li>
          ) : null}
        </ul>
      ) : null}
    </div>
  );
}

export default TagInput;
