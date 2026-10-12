import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import type { Card, CardInput, CardType } from "~/types/api";
import * as deckService from "~/services/deck.service";
import * as cardService from "~/services/card.service";
import {
  CARD_TYPE_BADGE_CLASS,
  CARD_TYPE_LABEL,
  cn,
  formatDate,
} from "~/lib/utils";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";
import { FormField } from "~/components/ui/form-field";
import { ErrorAlert } from "~/components/ui/error-alert";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { TagInput } from "~/components/ui/tag-input";
import { TagFilter } from "~/components/ui/tag-filter";
import { TagBadges } from "~/components/ui/tag-badges";

const cardFormSchema = z.object({
  type: z.enum(["CLOZE", "TRUE_FALSE", "MULTIPLE_CHOICE"]),
  question: z.string().min(1, "Informe a pergunta."),
  answer: z.string().min(1, "Informe a resposta."),
  optionsText: z.string(),
  explanation: z.string(),
  hint: z.string(),
  tags: z.array(z.string()),
});

type CardFormValues = z.infer<typeof cardFormSchema>;

const EMPTY_FORM: CardFormValues = {
  type: "CLOZE",
  question: "",
  answer: "",
  optionsText: "",
  explanation: "",
  hint: "",
  tags: [],
};

function parseOptions(optionsText: string): string[] {
  return optionsText
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
}

export default function DeckDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
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

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Card | null>(null);
  const [cardToDelete, setCardToDelete] = useState<Card | null>(null);
  const [deleteDeckOpen, setDeleteDeckOpen] = useState(false);

  const deckQuery = useQuery({
    queryKey: ["deck", id],
    queryFn: () => deckService.getDeck(id as string),
    enabled: !!id,
  });

  const cardsQuery = useQuery({
    queryKey: ["deck", id, "cards", tagFilter ?? "all"],
    queryFn: () => deckService.getDeckCards(id as string, tagFilter ?? undefined),
    enabled: !!id,
  });

  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: cardService.listTags,
  });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ["deck", id] });
    void queryClient.invalidateQueries({ queryKey: ["decks"] });
    void queryClient.invalidateQueries({ queryKey: ["tags"] });
  };

  const deleteDeckMutation = useMutation({
    mutationFn: () => deckService.deleteDeck(id as string),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["decks"] });
      navigate("/decks", { replace: true });
    },
  });

  const deleteCardMutation = useMutation({
    mutationFn: (cardId: string) => cardService.deleteCard(cardId),
    onSuccess: () => {
      setCardToDelete(null);
      invalidate();
    },
  });

  const deck = deckQuery.data;
  const cards = cardsQuery.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-2 text-sm">
        <Link to="/decks" className="link link-hover text-base-content/60">
          Baralhos
        </Link>
        <span aria-hidden="true" className="text-base-content/40">
          /
        </span>
        <span className="truncate font-medium">{deck?.title ?? "..."}</span>
      </div>

      <PageHeader
        title={deck?.title ?? "Baralho"}
        subtitle={
          deck
            ? `${deck.cardsCount} cartões · ${deck.dueCount} para revisar hoje`
            : undefined
        }
        actions={
          <>
            <button
              type="button"
              className="btn btn-ghost btn-sm text-error"
              onClick={() => setDeleteDeckOpen(true)}
            >
              Excluir
            </button>
            <Link
              to={`/study/${id}`}
              className={cn(
                "btn btn-primary btn-sm",
                deck && deck.cardsCount === 0 && "btn-disabled",
              )}
              aria-disabled={deck?.cardsCount === 0}
              onClick={(event) => {
                if (deck?.cardsCount === 0) event.preventDefault();
              }}
            >
              Estudar
            </Link>
          </>
        }
      />

      <TagFilter
        tags={tagsQuery.data ?? []}
        selected={tagFilter}
        onSelect={setTagFilter}
      />

      <AsyncState
        isLoading={cardsQuery.isLoading}
        isError={cardsQuery.isError}
        error={cardsQuery.error}
        onRetry={() => cardsQuery.refetch()}
      >
        {cards.length === 0 ? (
          tagFilter ? (
            <EmptyState
              message={`Nenhum cartão com a tag #${tagFilter}.`}
              hint="Tente outra tag ou limpe o filtro."
              action={
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  onClick={() => setTagFilter(null)}
                >
                  Limpar filtro
                </button>
              }
            />
          ) : (
            <EmptyState
              message="Nenhum cartão neste baralho."
              hint="Adicione um cartão manualmente para começar."
              action={
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => {
                    setEditing(null);
                    setFormOpen(true);
                  }}
                >
                  Adicionar cartão
                </button>
              }
            />
          )
        ) : (
          <>
            <div className="flex justify-end">
              <button
                type="button"
                className="btn btn-outline btn-sm"
                onClick={() => {
                  setEditing(null);
                  setFormOpen(true);
                }}
              >
                Adicionar cartão
              </button>
            </div>
            <ul className="flex flex-col gap-3">
              {cards.map((card, index) => (
                <li
                  key={card.id}
                  className="card card-border border-base-200 bg-base-100"
                >
                  <div className="card-body gap-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-base-content/50">
                          #{index + 1}
                        </span>
                        <span
                          className={cn(
                            "badge badge-sm",
                            CARD_TYPE_BADGE_CLASS[card.type],
                          )}
                        >
                          {CARD_TYPE_LABEL[card.type]}
                        </span>
                      </div>
                      <div className="flex gap-1">
                        <button
                          type="button"
                          className="btn btn-ghost btn-xs"
                          onClick={() => {
                            setEditing(card);
                            setFormOpen(true);
                          }}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="btn btn-ghost btn-xs text-error"
                          onClick={() => setCardToDelete(card)}
                        >
                          Excluir
                        </button>
                      </div>
                    </div>
                    <p className="font-medium">{card.question}</p>
                    <p className="text-sm text-base-content/70">
                      <span className="font-semibold">Resposta: </span>
                      {card.answer}
                    </p>
                    {card.options.length > 0 ? (
                      <ul className="flex flex-wrap gap-1.5">
                        {card.options.map((option) => (
                          <li
                            key={option}
                            className="badge badge-outline badge-sm"
                          >
                            {option}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                    <TagBadges tags={card.tags} size="sm" />
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}
      </AsyncState>

      <CardFormDialog
        open={formOpen}
        deckId={id ?? ""}
        card={editing}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          invalidate();
        }}
      />

      <ConfirmDialog
        open={!!cardToDelete}
        title="Excluir cartão"
        message="Tem certeza que deseja excluir este cartão? Esta ação não pode ser desfeita."
        confirmLabel="Excluir"
        danger
        isLoading={deleteCardMutation.isPending}
        onConfirm={() => {
          if (cardToDelete) deleteCardMutation.mutate(cardToDelete.id);
        }}
        onCancel={() => setCardToDelete(null)}
      />

      <ConfirmDialog
        open={deleteDeckOpen}
        title="Excluir baralho"
        message="Excluir o baralho apaga todos os seus cartões. Deseja continuar?"
        confirmLabel="Excluir baralho"
        danger
        isLoading={deleteDeckMutation.isPending}
        onConfirm={() => deleteDeckMutation.mutate()}
        onCancel={() => setDeleteDeckOpen(false)}
      />
    </div>
  );
}

function CardFormDialog({
  open,
  deckId,
  card,
  onClose,
  onSaved,
}: {
  open: boolean;
  deckId: string;
  card: Card | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [formError, setFileError] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    control,
    formState: { errors },
  } = useForm<CardFormValues>({
    resolver: zodResolver(cardFormSchema),
    defaultValues: EMPTY_FORM,
  });

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    else if (!open && dialog.open) dialog.close();
  }, [open]);

  useEffect(() => {
    if (!open) return;
    if (card) {
      reset({
        type: card.type,
        question: card.question,
        answer: card.answer,
        optionsText: card.options.join("\n"),
        explanation: card.explanation ?? "",
        hint: card.hint ?? "",
        tags: card.tags ?? [],
      });
    } else {
      reset(EMPTY_FORM);
    }
  }, [open, card, reset]);

  const formType = watch("type");

  const mutation = useMutation({
    mutationFn: (input: CardInput) =>
      card
        ? cardService.updateCard(card.id, input)
        : cardService.createCard(input),
    onSuccess: () => {
      onSaved();
    },
    onError: () => setFileError("Não foi possível salvar o cartão."),
  });

  const submit = (values: CardFormValues) => {
    const options = parseOptions(values.optionsText);
    if (
      (values.type === "MULTIPLE_CHOICE" || values.type === "CLOZE") &&
      options.length < 2
    ) {
      setFileError("Informe pelo menos 2 opções (uma por linha).");
      return;
    }
    setFileError(null);
    mutation.mutate({
      deckId,
      type: values.type as CardType,
      question: values.question.trim(),
      answer: values.answer.trim(),
      options,
      explanation: values.explanation.trim() || null,
      hint: values.hint.trim() || null,
      tags: values.tags,
    });
  };

  return (
    <dialog
      ref={dialogRef}
      className="modal"
      onCancel={onClose}
      onClose={onClose}
    >
      <div className="modal-box max-w-lg">
        <h3 className="font-display text-lg font-bold">
          {card ? "Editar cartão" : "Novo cartão"}
        </h3>

        <form
          className="mt-4 flex flex-col gap-4"
          onSubmit={handleSubmit(submit)}
          noValidate
        >
          {formError ? (
            <div role="alert" className="alert alert-warning">
              {formError}
            </div>
          ) : null}
          {mutation.isError ? <ErrorAlert error={mutation.error} /> : null}

          <FormField label="Tipo" required>
            <select className="select  w-full" {...register("type")}>
              <option value="CLOZE">{CARD_TYPE_LABEL.CLOZE}</option>
              <option value="TRUE_FALSE">{CARD_TYPE_LABEL.TRUE_FALSE}</option>
              <option value="MULTIPLE_CHOICE">
                {CARD_TYPE_LABEL.MULTIPLE_CHOICE}
              </option>
            </select>
          </FormField>

          <FormField
            label="Pergunta"
            required
            error={errors.question?.message}
            hint={
              formType === "CLOZE"
                ? "Use ___ (três sublinhados ou mais) para marcar a lacuna."
                : undefined
            }
          >
            <textarea
              rows={3}
              className="textarea  w-full"
              {...register("question")}
            />
          </FormField>

          <FormField label="Resposta" required error={errors.answer?.message}>
            <input
              type="text"
              className="input  w-full"
              {...register("answer")}
            />
          </FormField>

          {formType !== "TRUE_FALSE" ? (
            <FormField
              label="Opções"
              hint="Uma por linha. Para CLOZE, as opções preenchem a lacuna."
            >
              <textarea
                rows={4}
                className="textarea  w-full"
                {...register("optionsText")}
              />
            </FormField>
          ) : null}

          <FormField label="Explicação (opcional)">
            <textarea
              rows={2}
              className="textarea  w-full"
              {...register("explanation")}
            />
          </FormField>

          <FormField label="Dica (opcional)">
            <input
              type="text"
              className="input  w-full"
              {...register("hint")}
            />
          </FormField>

          <FormField
            label="Tags (opcional)"
            hint="Organize o cartão. Pressione Enter para adicionar; digite para criar uma nova."
          >
            <Controller
              control={control}
              name="tags"
              render={({ field }) => (
                <TagInput
                  value={field.value}
                  onChange={field.onChange}
                  allowCreate
                  label="Tags do cartão"
                />
              )}
            />
          </FormField>

          <div className="modal-action">
            <button
              type="button"
              className="btn"
              onClick={onClose}
              disabled={mutation.isPending}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? (
                <span className="loading loading-spinner loading-xs" />
              ) : null}
              Salvar
            </button>
          </div>
        </form>
      </div>
      <form method="dialog" className="modal-backdrop">
        <button type="submit" aria-label="Fechar" onClick={onClose}>
          fechar
        </button>
      </form>
    </dialog>
  );
}
