import { useRef, useState } from "react";
import { Link } from "react-router";
import { Controller, useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as generationService from "~/services/generation.service";
import type { CreateGenerationInput, GenerationJob } from "~/types/api";
import {
  GENERATION_STATUS_BADGE_CLASS,
  GENERATION_STATUS_LABEL,
  GENERATION_STEPS,
  cn,
  generationStepIndex,
} from "~/lib/utils";
import { PageHeader } from "~/components/ui/page-header";
import { FormField } from "~/components/ui/form-field";
import { ErrorAlert } from "~/components/ui/error-alert";
import { TagInput } from "~/components/ui/tag-input";

type Mode = "file" | "text";

interface FormValues {
  title: string;
  text: string;
  tags: string[];
}

const ACCEPTED = "image/*,application/pdf";

export default function GeneratePage() {
  const [mode, setMode] = useState<Mode>("file");
  const [file, setFile] = useState<File | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const [jobId, setJobId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<FormValues>({ defaultValues: { title: "", text: "", tags: [] } });

  const createMutation = useMutation({
    mutationFn: (input: CreateGenerationInput) =>
      generationService.createGeneration(input),
    onSuccess: (data) => {
      setJobId(data.jobId);
      setLocalError(null);
    },
  });

  const jobQuery = useQuery({
    queryKey: ["generation", jobId],
    queryFn: () => generationService.getGeneration(jobId as string),
    enabled: !!jobId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "DONE" || status === "FAILED" ? false : 1500;
    },
  });

  const job: GenerationJob | undefined = jobQuery.data;

  // Quando concluir, atualiza Cards, arquivos e tags.
  if (job?.status === "DONE") {
    void queryClient.invalidateQueries({ queryKey: ["decks"] });
    void queryClient.invalidateQueries({ queryKey: ["files"] });
    void queryClient.invalidateQueries({ queryKey: ["tags"] });
  }

  const onSubmit = (values: FormValues) => {
    if (mode === "file" && !file) {
      setLocalError("Selecione uma imagem ou PDF.");
      return;
    }
    if (mode === "text" && !values.text.trim()) {
      setLocalError("Cole um texto para gerar os cartões.");
      return;
    }
    setLocalError(null);
    createMutation.mutate({
      file: mode === "file" ? file : null,
      text: mode === "text" ? values.text : null,
      title: values.title,
      tags: values.tags,
    });
  };

  const resetAll = () => {
    setJobId(null);
    setFile(null);
    reset();
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Gerar cartões"
        subtitle="Envie uma imagem, um PDF ou um texto e a IA cria o Card."
      />

      {jobId ? (
        <GenerationProgress
          job={job}
          isError={jobQuery.isError}
          error={jobQuery.error}
          onRetry={() => jobQuery.refetch()}
          onReset={resetAll}
        />
      ) : (
        <form
          className="card card-border border-base-200 bg-base-100 shadow-sm"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
        >
          <div className="card-body gap-4">
            {localError ? (
              <div role="alert" className="alert alert-warning">
                {localError}
              </div>
            ) : null}
            {createMutation.isError ? (
              <ErrorAlert error={createMutation.error} />
            ) : null}

            {/* Alternância de modo */}
            <div role="tablist" className="tabs tabs-box grid grid-cols-2">
              <button
                type="button"
                role="tab"
                aria-selected={mode === "file"}
                className={cn("tab", mode === "file" && "tab-active")}
                onClick={() => setMode("file")}
              >
                Arquivo
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={mode === "text"}
                className={cn("tab", mode === "text" && "tab-active")}
                onClick={() => setMode("text")}
              >
                Texto
              </button>
            </div>

            {mode === "file" ? (
              <FormField
                label="Imagem ou PDF"
                required
                hint="Formatos aceitos: JPG, PNG, WEBP, PDF."
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPTED}
                  className="file-input  w-full"
                  onChange={(event) => {
                    setFile(event.target.files?.[0] ?? null);
                    setLocalError(null);
                  }}
                />
                {file ? (
                  <span className="text-xs text-base-content/60">
                    {file.name}
                  </span>
                ) : null}
              </FormField>
            ) : (
              <FormField
                label="Texto"
                required
                error={errors.text?.message}
                hint="Cole anotações, resumos ou trechos de estudo."
              >
                <textarea
                  rows={8}
                  placeholder="Cole aqui o conteúdo que deve virar flashcards..."
                  className="textarea  w-full"
                  {...register("text")}
                />
              </FormField>
            )}

            <FormField label="Título (opcional)">
              <input
                type="text"
                placeholder="Ex.: Biologia — Sistema nervoso"
                className="input  w-full"
                {...register("title")}
              />
            </FormField>

            <FormField
              label="Tags (opcional)"
              hint="Aplicadas a todos os cartões gerados. Pressione Enter para adicionar."
            >
              <Controller
                control={control}
                name="tags"
                render={({ field }) => (
                  <TagInput
                    value={field.value}
                    onChange={field.onChange}
                    allowCreate
                    label="Tags da geração"
                  />
                )}
              />
            </FormField>

            <div className="card-actions justify-end">
              <button
                type="submit"
                className="btn btn-primary w-full sm:w-auto"
                disabled={createMutation.isPending}
              >
                {createMutation.isPending ? (
                  <span className="loading loading-spinner loading-sm" />
                ) : null}
                {createMutation.isPending ? "Enviando..." : "Gerar cartões"}
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
}

function GenerationProgress({
  job,
  isError,
  error,
  onRetry,
  onReset,
}: {
  job: GenerationJob | undefined;
  isError: boolean;
  error: unknown;
  onRetry: () => void;
  onReset: () => void;
}) {
  const status = job?.status;
  const stepIndex = status ? generationStepIndex(status) : 0;

  return (
    <div className="card card-border border-base-200 bg-base-100 shadow-sm">
      <div className="card-body gap-6">
        <div className="flex items-center justify-between gap-3">
          <h2 className="font-display text-lg font-bold">Progresso da geração</h2>
          {status ? (
            <span className={cn("badge", GENERATION_STATUS_BADGE_CLASS[status])}>
              {GENERATION_STATUS_LABEL[status]}
            </span>
          ) : (
            <span className="badge badge-ghost">Aguardando</span>
          )}
        </div>

        {isError ? <ErrorAlert error={error} onRetry={onRetry} /> : null}

        {status === "FAILED" ? (
          <div role="alert" className="alert alert-error">
            <span className="font-semibold">Não foi possível gerar o Card.</span>
            {job?.errorMessage ? (
              <span className="text-sm">{job.errorMessage}</span>
            ) : null}
          </div>
        ) : (
          <ul className="steps steps-vertical sm:steps-horizontal">
            {GENERATION_STEPS.map((step, index) => (
              <li
                key={step}
                data-content={index < stepIndex ? "✓" : String(index + 1)}
                className={cn(
                  "step",
                  index <= stepIndex && "step-primary",
                )}
              >
                <span className="text-xs sm:text-sm">
                  {GENERATION_STATUS_LABEL[step]}
                </span>
              </li>
            ))}
          </ul>
        )}

        {status === "DONE" ? (
          <div className="alert alert-success">
            <div className="flex flex-col gap-1">
              <span className="font-semibold">Card criado!</span>
              <span className="text-sm">
                {job?.cardsCount ?? 0} cartões gerados.
              </span>
            </div>
          </div>
        ) : (
          <p className="flex items-center gap-2 text-sm text-base-content/60">
            <span className="loading loading-dots loading-sm" />
            Isso pode levar alguns segundos...
          </p>
        )}

        <div className="card-actions justify-end">
          <button type="button" className="btn btn-ghost" onClick={onReset}>
            Gerar outro
          </button>
          {status === "DONE" && job?.deckId ? (
            <Link to={`/decks/${job.deckId}`} className="btn btn-primary">
              Ver Card
            </Link>
          ) : null}
        </div>
      </div>
    </div>
  );
}
