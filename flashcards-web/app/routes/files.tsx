import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import * as fileService from "~/services/file.service";
import { formatBytes, formatDateTime } from "~/lib/utils";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";

export default function FilesPage() {
  const filesQuery = useQuery({
    queryKey: ["files"],
    queryFn: fileService.listFiles,
  });

  const files = filesQuery.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Arquivos"
        subtitle="Materiais enviados e os Cards gerados a partir deles."
        actions={
          <Link to="/generate" className="btn btn-primary btn-sm">
            Enviar arquivo
          </Link>
        }
      />

      <AsyncState
        isLoading={filesQuery.isLoading}
        isError={filesQuery.isError}
        error={filesQuery.error}
        onRetry={() => filesQuery.refetch()}
      >
        {files.length === 0 ? (
          <EmptyState
            message="Nenhum arquivo enviado."
            hint="Envie uma imagem ou PDF para gerar cartões."
            action={
              <Link to="/generate" className="btn btn-primary btn-sm">
                Enviar arquivo
              </Link>
            }
          />
        ) : (
          <ul className="flex flex-col gap-3">
            {files.map((file) => (
              <li key={file.id}>
                <Link
                  to={`/files/${file.id}`}
                  className="card card-border border-base-200 bg-base-100 glow-hover block"
                >
                  <div className="card-body flex-row items-center gap-4 py-4">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-box bg-primary/15 text-primary">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-5 w-5" aria-hidden="true">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                      </svg>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium">
                        {file.originalFilename}
                      </p>
                      <p className="text-xs text-base-content/60">
                        {formatBytes(file.sizeBytes)} ·{" "}
                        {formatDateTime(file.uploadedAt)}
                      </p>
                    </div>
                    <div className="shrink-0 text-right">
                      <span className="badge badge-ghost badge-sm">
                        {file.cardsCount} cartões
                      </span>
                    </div>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </AsyncState>
    </div>
  );
}
