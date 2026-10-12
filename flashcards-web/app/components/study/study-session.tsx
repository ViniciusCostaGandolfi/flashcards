import { useEffect, useState } from "react";
import { Link } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { ReviewGrade } from "~/types/api";
import type { Card as CardModel } from "~/types/api";
import * as cardService from "~/services/card.service";
import { EmptyState } from "~/components/ui/empty-state";
import { StudyCard } from "./study-card";

interface StudySessionProps {
  cards: CardModel[];
  backTo?: string;
  backLabel?: string;
  emptyMessage?: string;
  emptyHint?: string;
}

export function StudySession({
  cards,
  backTo = "/decks",
  backLabel = "Voltar aos Cards",
  emptyMessage = "Nada para estudar agora!",
  emptyHint = "Você não tem cartões devidos. Volte mais tarde ou gere novos Cards.",
}: StudySessionProps) {
  const [index, setIndex] = useState(0);
  const queryClient = useQueryClient();

  // Reinicia a sessão quando a lista de cartões muda.
  useEffect(() => {
    setIndex(0);
  }, [cards]);

  const mutation = useMutation({
    mutationFn: ({ id, grade }: { id: string; grade: ReviewGrade }) =>
      cardService.reviewCard(id, grade),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      queryClient.invalidateQueries({ queryKey: ["study"] });
      queryClient.invalidateQueries({ queryKey: ["deck"] });
    },
  });

  const total = cards.length;
  const current = cards[index];
  const finished = index >= total;

  const handleGrade = (grade: ReviewGrade) => {
    if (!current) return;
    mutation.mutate(
      { id: current.id, grade },
      { onSettled: () => setIndex((value) => value + 1) },
    );
  };

  if (total === 0) {
    return (
      <EmptyState
        message={emptyMessage}
        hint={emptyHint}
        action={
          <Link to="/generate" className="btn btn-primary btn-sm">
            Gerar cartões
          </Link>
        }
      />
    );
  }

  if (finished) {
    return (
      <div className="card card-border border-base-200 bg-base-100 shadow-lg">
        <div className="card-body items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-success/15 text-success">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
              className="h-8 w-8"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
              />
            </svg>
          </div>
          <h2 className="font-display text-2xl font-bold">Sessão concluída!</h2>
          <p className="text-base-content/70">
            Você revisou {total} {total === 1 ? "cartão" : "cartões"}. As datas
            de revisão foram atualizadas.
          </p>
          <div className="card-actions mt-4 flex-col gap-2 sm:flex-row">
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setIndex(0)}
            >
              Estudar de novo
            </button>
            <Link to={backTo} className="btn btn-primary">
              {backLabel}
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const answered = index;
  const progress = Math.round((answered / total) * 100);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <progress
          className="progress progress-primary h-2 flex-1"
          value={answered}
          max={total}
          aria-label="Progresso da sessão"
        />
        <span className="shrink-0 text-sm font-medium text-base-content/70">
          {index + 1}/{total}
        </span>
      </div>
      <p className="sr-only" aria-live="polite">
        {progress}% concluído
      </p>

      <StudyCard
        key={current.id}
        card={current}
        onGrade={handleGrade}
        isSubmitting={mutation.isPending}
      />
    </div>
  );
}

export default StudySession;
