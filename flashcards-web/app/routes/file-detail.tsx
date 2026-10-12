import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import * as fileService from "~/services/file.service";
import {
  CARD_TYPE_BADGE_CLASS,
  CARD_TYPE_LABEL,
  cn,
  formatBytes,
  formatDateTime,
} from "~/lib/utils";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";
import { TagBadges } from "~/components/ui/tag-badges";

/** Busca o conteúdo binário e devolve uma object URL (revogada ao desmontar). */
function useFilePreview(id: string | undefined): string | null {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    let revoked = false;
    let objectUrl: string | null = null;

    fileService
      .getFileContent(id)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        if (!revoked) setUrl(objectUrl);
      })
      .catch(() => {
        if (!revoked) setUrl(null);
      });

    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [id]);

  return url;
}

export default function FileDetailPage() {
  const { id } = useParams<{ id: string }>();

  const fileQuery = useQuery({
    queryKey: ["file", id],
    queryFn: () => fileService.getFile(id as string),
    enabled: !!id,
  });

  const file = fileQuery.data;
  const previewUrl = useFilePreview(file?.id);

  const isImage = file?.contentType.startsWith("image/") ?? false;
  const isPdf = file?.contentType === "application/pdf";

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-2 text-sm">
        <Link to="/files" className="link link-hover text-base-content/60">
          Arquivos
        </Link>
        <span aria-hidden="true" className="text-base-content/40">
          /
        </span>
        <span className="truncate font-medium">
          {file?.originalFilename ?? "..."}
        </span>
      </div>

      <AsyncState
        isLoading={fileQuery.isLoading}
        isError={fileQuery.isError}
        error={fileQuery.error}
        onRetry={() => fileQuery.refetch()}
      >
        {file ? (
          <>
            <PageHeader
              title={file.originalFilename}
              subtitle={`${formatBytes(file.sizeBytes)} · ${formatDateTime(
                file.uploadedAt,
              )}`}
              actions={
                file.deckId ? (
                  <Link to={`/decks/${file.deckId}`} className="btn btn-primary btn-sm">
                    Ver baralho
                  </Link>
                ) : null
              }
            />

            {/* Pré-visualização do conteúdo */}
            {previewUrl && isImage ? (
              <div className="overflow-hidden rounded-box border border-base-200 bg-base-200/40">
                <img
                  src={previewUrl}
                  alt={`Conteúdo de ${file.originalFilename}`}
                  className="mx-auto max-h-[70vh] w-auto object-contain"
                  loading="lazy"
                />
              </div>
            ) : previewUrl && isPdf ? (
              <div className="overflow-hidden rounded-box border border-base-200">
                <iframe
                  src={previewUrl}
                  title={`PDF ${file.originalFilename}`}
                  className="h-[70vh] w-full"
                />
              </div>
            ) : null}

            {/* Baralho gerado */}
            {file.deck ? (
              <div className="card card-border border-base-200 bg-base-100">
                <div className="card-body gap-2">
                  <h2 className="font-display text-lg font-bold">
                    Baralho gerado
                  </h2>
                  <p className="text-sm text-base-content/70">
                    {file.deck.title} · {file.deck.cardsCount} cartões
                  </p>
                  <div className="card-actions mt-1">
                    <Link
                      to={`/study/${file.deck.id}`}
                      className="btn btn-outline btn-sm"
                    >
                      Estudar
                    </Link>
                  </div>
                </div>
              </div>
            ) : null}

            {/* Cartões */}
            <section className="flex flex-col gap-3">
              <h2 className="font-display text-lg font-bold">Cartões</h2>
              {file.cards.length === 0 ? (
                <EmptyState
                  message="Nenhum cartão gerado a partir deste arquivo."
                  hint="A geração pode ainda estar em andamento."
                />
              ) : (
                <ul className="flex flex-col gap-3">
                  {file.cards.map((card) => (
                    <li
                      key={card.id}
                      className="card card-border border-base-200 bg-base-100"
                    >
                      <div className="card-body gap-2">
                        <span
                          className={cn(
                            "badge badge-sm self-start",
                            CARD_TYPE_BADGE_CLASS[card.type],
                          )}
                        >
                          {CARD_TYPE_LABEL[card.type]}
                        </span>
                        <p className="font-medium">{card.question}</p>
                        <p className="text-sm text-base-content/70">
                          <span className="font-semibold">Resposta: </span>
                          {card.answer}
                        </p>
                        <TagBadges tags={card.tags} size="sm" />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        ) : null}
      </AsyncState>
    </div>
  );
}
