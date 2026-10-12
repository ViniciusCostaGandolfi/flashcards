import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import type { Tag } from "~/types/api";
import * as cardService from "~/services/card.service";
import { getApiErrorMessage } from "~/lib/api";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";

const tagSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Informe o nome da tag.")
    .max(40, "Máximo de 40 caracteres."),
});

type TagFormValues = z.infer<typeof tagSchema>;

/** Gerenciador de tags: listar, criar e excluir. Usado em /settings. */
export function TagsManager() {
  const queryClient = useQueryClient();
  const [tagToDelete, setTagToDelete] = useState<Tag | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);

  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: cardService.listTags,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TagFormValues>({
    resolver: zodResolver(tagSchema),
    defaultValues: { name: "" },
  });

  const invalidateAll = () => {
    void queryClient.invalidateQueries({ queryKey: ["tags"] });
    void queryClient.invalidateQueries({ queryKey: ["decks"] });
    void queryClient.invalidateQueries({ queryKey: ["deck"] });
    void queryClient.invalidateQueries({ queryKey: ["study"] });
  };

  const createMutation = useMutation({
    mutationFn: (name: string) => cardService.createTag(name),
    onSuccess: () => {
      setCreateError(null);
      reset();
      invalidateAll();
    },
    onError: (error) => setCreateError(getApiErrorMessage(error)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => cardService.deleteTag(id),
    onSuccess: () => {
      setTagToDelete(null);
      invalidateAll();
    },
  });

  const tags = tagsQuery.data ?? [];

  return (
    <section className="card card-border border-base-200 bg-base-100 shadow-sm">
      <div className="card-body gap-4">
        <div>
          <h2 className="font-display text-lg font-bold">Tags</h2>
          <p className="text-sm text-base-content/70">
            Crie e remova tags usadas para organizar e filtrar seus cartões.
          </p>
        </div>

        <form
          className="flex items-start gap-2"
          onSubmit={handleSubmit((values) =>
            createMutation.mutate(values.name.trim()),
          )}
          noValidate
        >
          <div className="flex-1">
            <input
              type="text"
              placeholder="Nova tag (ex.: biologia)"
              className="input input-bordered w-full"
              aria-label="Nome da nova tag"
              aria-invalid={!!errors.name || undefined}
              {...register("name")}
            />
            {errors.name ? (
              <span className="mt-1 block text-sm text-error" role="alert">
                {errors.name.message}
              </span>
            ) : createError ? (
              <span className="mt-1 block text-sm text-error" role="alert">
                {createError}
              </span>
            ) : null}
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={createMutation.isPending}
          >
            {createMutation.isPending ? (
              <span className="loading loading-spinner loading-sm" />
            ) : null}
            Criar
          </button>
        </form>

        <AsyncState
          isLoading={tagsQuery.isLoading}
          isError={tagsQuery.isError}
          error={tagsQuery.error}
          onRetry={() => tagsQuery.refetch()}
        >
          {tags.length === 0 ? (
            <EmptyState
              message="Nenhuma tag ainda."
              hint="Crie a primeira tag acima ou adicione tags a um cartão."
            />
          ) : (
            <ul className="flex flex-wrap gap-2">
              {tags.map((tag) => (
                <li
                  key={tag.id}
                  className="badge badge-outline gap-1.5 py-3 pl-3 pr-1.5"
                >
                  <span className="truncate">#{tag.name}</span>
                  <span className="text-xs text-base-content/50">
                    {tag.cardsCount}
                  </span>
                  <button
                    type="button"
                    className="rounded-full p-0.5 text-base-content/60 transition-colors hover:bg-base-200 hover:text-error"
                    aria-label={`Excluir tag ${tag.name}`}
                    onClick={() => setTagToDelete(tag)}
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
                        d="M8.75 1A2.75 2.75 0 0 0 6 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 1 0 .23 1.482l.149-.022.841 10.518A2.75 2.75 0 0 0 7.596 19h4.807a2.75 2.75 0 0 0 2.742-2.53l.841-10.52.149.023a.75.75 0 0 0 .23-1.482A41.03 41.03 0 0 0 14 4.193V3.75A2.75 2.75 0 0 0 11.25 1h-2.5ZM10 4c.84 0 1.673.025 2.5.075V3.75c0-.69-.56-1.25-1.25-1.25h-2.5c-.69 0-1.25.56-1.25 1.25v.325C8.327 4.025 9.16 4 10 4ZM8.58 7.72a.75.75 0 0 0-1.5.06l.3 7.5a.75.75 0 1 0 1.5-.06l-.3-7.5Zm4.34.06a.75.75 0 1 0-1.5-.06l-.3 7.5a.75.75 0 1 0 1.5.06l.3-7.5Z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </AsyncState>
      </div>

      <ConfirmDialog
        open={!!tagToDelete}
        title="Excluir tag"
        message={`A tag "${tagToDelete?.name ?? ""}" será removida dos cartões. Deseja continuar?`}
        confirmLabel="Excluir"
        danger
        isLoading={deleteMutation.isPending}
        onConfirm={() => {
          if (tagToDelete) deleteMutation.mutate(tagToDelete.id);
        }}
        onCancel={() => setTagToDelete(null)}
      />
    </section>
  );
}

export default TagsManager;
