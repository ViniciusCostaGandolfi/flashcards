import { useState } from "react";
import type { Card, ReviewGrade } from "~/types/api";
import { cn } from "~/lib/utils";
import { TagBadges } from "~/components/ui/tag-badges";

interface StudyCardProps {
  card: Card;
  onGrade: (grade: ReviewGrade) => void;
  isSubmitting: boolean;
}

function normalize(value: string): string {
  return value.trim().toLowerCase();
}

function answerIsTrue(answer: string): boolean {
  return ["true", "verdadeiro", "verdade", "v", "sim", "1"].includes(
    normalize(answer),
  );
}

/** Divide a questão cloze em [antes, depois] do primeiro espaço em branco. */
function splitCloze(question: string): [string, string] {
  const match = /_{2,}/.exec(question);
  if (!match || match.index === undefined) return [question, ""];
  return [
    question.slice(0, match.index),
    question.slice(match.index + match[0].length),
  ];
}

export function StudyCard({ card, onGrade, isSubmitting }: StudyCardProps) {
  const [selected, setSelected] = useState<string | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [showHint, setShowHint] = useState(false);

  const answer = normalize(card.answer);

  const isCorrect = (() => {
    if (selected === null) return false;
    if (card.type === "TRUE_FALSE") {
      const expected = answerIsTrue(card.answer) ? "true" : "false";
      return selected === expected;
    }
    return normalize(selected) === answer;
  })();

  const handleAnswer = (value: string) => {
    if (revealed) return;
    setSelected(value);
    setRevealed(true);
  };

  return (
    <div className="card card-border border-base-200 bg-base-100 shadow-lg">
      <div className="card-body gap-5">
        {/* Enunciado */}
        {card.type === "CLOZE" ? (
          <ClozeQuestion question={card.question} filled={selected} revealed={revealed} answer={card.answer} />
        ) : (
          <p className="text-balance font-display text-xl font-semibold leading-snug text-base-content">
            {card.question}
          </p>
        )}

        <TagBadges tags={card.tags} size="sm" />

        {card.hint ? (
          showHint || revealed ? (
            <p className="rounded-box bg-base-200 px-3 py-2 text-sm text-base-content/80">
              <span className="font-semibold">Dica:</span> {card.hint}
            </p>
          ) : (
            <button
              type="button"
              className="btn btn-ghost btn-xs self-start"
              onClick={() => setShowHint(true)}
            >
              Mostrar dica
            </button>
          )
        ) : null}

        {/* Respostas por tipo */}
        {card.type === "TRUE_FALSE" ? (
          <div className="grid grid-cols-2 gap-3">
            <TrueFalseButton
              label="Verdadeiro"
              tone="success"
              selected={selected === "true"}
              revealed={revealed}
              onClick={() => handleAnswer("true")}
            />
            <TrueFalseButton
              label="Falso"
              tone="error"
              selected={selected === "false"}
              revealed={revealed}
              onClick={() => handleAnswer("false")}
            />
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {card.options.map((option) => {
              const isSelected = selected === option;
              const isRight = normalize(option) === answer;
              return (
                <li key={option}>
                  <button
                    type="button"
                    disabled={revealed}
                    onClick={() => handleAnswer(option)}
                    className={cn(
                      "w-full rounded-box border px-4 py-3 text-left text-sm transition-colors",
                      "border-base-300 bg-base-100 hover:border-primary hover:bg-primary/5",
                      revealed && isRight && "border-success bg-success/10",
                      revealed && isSelected && !isRight && "border-error bg-error/10",
                      revealed && !isRight && !isSelected && "opacity-60",
                    )}
                    aria-pressed={isSelected}
                  >
                    {option}
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        {/* Feedback */}
        {revealed ? (
          <div
            className={cn(
              "alert",
              isCorrect ? "alert-success" : "alert-error",
            )}
            role="status"
          >
            <span className="font-semibold">
              {isCorrect ? "Resposta correta!" : "Resposta incorreta"}
            </span>
            {!isCorrect ? (
              <span className="text-sm">
                A resposta é: <strong>{card.answer}</strong>
              </span>
            ) : null}
            {card.explanation ? (
              <span className="text-sm opacity-90">{card.explanation}</span>
            ) : null}
          </div>
        ) : null}

        {/* Avaliação (repetição espaçada) */}
        {revealed ? (
          <div className="flex flex-col gap-2">
            <p className="text-center text-sm text-base-content/60">
              Como foi?
            </p>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                className="btn btn-error btn-outline"
                disabled={isSubmitting}
                onClick={() => onGrade("AGAIN")}
              >
                Errei
              </button>
              <button
                type="button"
                className="btn btn-warning btn-outline"
                disabled={isSubmitting}
                onClick={() => onGrade("HARD")}
              >
                Difícil
              </button>
              <button
                type="button"
                className="btn btn-success btn-outline"
                disabled={isSubmitting}
                onClick={() => onGrade("EASY")}
              >
                Fácil
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}

function TrueFalseButton({
  label,
  tone,
  selected,
  revealed,
  onClick,
}: {
  label: string;
  tone: "success" | "error";
  selected: boolean;
  revealed: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={revealed}
      onClick={onClick}
      className={cn(
        "btn h-20 text-lg",
        revealed && selected
          ? tone === "success"
            ? "btn-success"
            : "btn-error"
          : "btn-outline",
      )}
      aria-pressed={selected}
    >
      {label}
    </button>
  );
}

function ClozeQuestion({
  question,
  filled,
  revealed,
  answer,
}: {
  question: string;
  filled: string | null;
  revealed: boolean;
  answer: string;
}) {
  const [before, after] = splitCloze(question);
  const blank = revealed ? answer : filled ?? "_____";
  const correct = normalize(filled ?? "") === normalize(answer);

  return (
    <p className="text-balance font-display text-xl font-semibold leading-relaxed text-base-content">
      {before}
      <span
        className={cn(
          "mx-1 inline-block min-w-16 rounded-box border-b-2 px-2 text-center",
          revealed
            ? correct
              ? "border-success text-success"
              : "border-error text-error"
            : filled
              ? "border-primary text-primary"
              : "border-base-300 text-base-content/50",
        )}
      >
        {blank}
      </span>
      {after}
    </p>
  );
}

export default StudyCard;
