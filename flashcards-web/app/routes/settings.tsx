import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import * as settingsService from "~/services/settings.service";
import type { ModelInfo, Settings } from "~/types/api";
import { PageHeader } from "~/components/ui/page-header";
import { AsyncState } from "~/components/ui/async-state";
import { FormField } from "~/components/ui/form-field";
import { ErrorAlert } from "~/components/ui/error-alert";
import { TagsManager } from "~/components/ui/tags-manager";

const settingsSchema = z.object({
  readerModel: z.string().min(1, "Selecione o modelo de leitura."),
  generatorModel: z.string().min(1, "Selecione o modelo de geração."),
  basePrompt: z.string().min(1, "Informe o prompt base."),
  cardsPerGeneration: z
    .number({ message: "Informe um número." })
    .int("Use um número inteiro.")
    .min(1, "Mínimo de 1 cartão.")
    .max(100, "Máximo de 100 cartões."),
  language: z.string().min(1, "Selecione o idioma."),
  pdfEngine: z.string().min(1, "Informe o motor de PDF."),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

const LANGUAGES = [
  { value: "pt-BR", label: "Português (Brasil)" },
  { value: "en-US", label: "Inglês (EUA)" },
  { value: "es-ES", label: "Espanhol" },
  { value: "fr-FR", label: "Francês" },
];

function modelOptionLabel(model: ModelInfo): string {
  const modalities = model.inputModalities?.join(", ");
  return modalities ? `${model.name} (${modalities})` : model.name;
}

export default function SettingsPage() {
  const queryClient = useQueryClient();

  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: settingsService.getSettings,
  });

  const modelsQuery = useQuery({
    queryKey: ["models"],
    queryFn: settingsService.listModels,
    staleTime: 5 * 60_000,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      readerModel: "",
      generatorModel: "",
      basePrompt: "",
      cardsPerGeneration: 10,
      language: "pt-BR",
      pdfEngine: "",
    },
  });

  useEffect(() => {
    if (settingsQuery.data) {
      reset(settingsQuery.data);
    }
  }, [settingsQuery.data, reset]);

  const mutation = useMutation({
    mutationFn: (values: SettingsFormValues) =>
      settingsService.updateSettings(values as Settings),
    onSuccess: (data) => {
      queryClient.setQueryData(["settings"], data);
      reset(data);
    },
  });

  const models = modelsQuery.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Ajustes"
        subtitle="Modelos de IA e preferências de geração."
      />

      <AsyncState
        isLoading={settingsQuery.isLoading}
        isError={settingsQuery.isError}
        error={settingsQuery.error}
        onRetry={() => settingsQuery.refetch()}
      >
        <form
          className="card card-border border-base-200 bg-base-100 shadow-sm"
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
          noValidate
        >
          <div className="card-body gap-4">
            {mutation.isError ? <ErrorAlert error={mutation.error} /> : null}
            {mutation.isSuccess ? (
              <div role="status" className="alert alert-success">
                Ajustes salvos com sucesso.
              </div>
            ) : null}

            <FormField
              label="Modelo de leitura"
              required
              error={errors.readerModel?.message}
              hint="Usado para extrair o conteúdo de imagens e PDFs."
            >
              <select
                className="select select-bordered w-full"
                {...register("readerModel")}
              >
                <option value="">Selecione...</option>
                {models.map((model) => (
                  <option key={model.id} value={model.id}>
                    {modelOptionLabel(model)}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField
              label="Modelo de geração"
              required
              error={errors.generatorModel?.message}
              hint="Usado para criar as perguntas e respostas."
            >
              <select
                className="select select-bordered w-full"
                {...register("generatorModel")}
              >
                <option value="">Selecione...</option>
                {models.map((model) => (
                  <option key={model.id} value={model.id}>
                    {modelOptionLabel(model)}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField
              label="Prompt base"
              required
              error={errors.basePrompt?.message}
              hint="Instruções enviadas ao modelo ao gerar os cartões."
            >
              <textarea
                rows={6}
                className="textarea textarea-bordered w-full"
                {...register("basePrompt")}
              />
            </FormField>

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                label="Cartões por geração"
                required
                error={errors.cardsPerGeneration?.message}
              >
                <input
                  type="number"
                  min={1}
                  max={100}
                  className="input input-bordered w-full"
                  {...register("cardsPerGeneration", { valueAsNumber: true })}
                />
              </FormField>

              <FormField
                label="Idioma"
                required
                error={errors.language?.message}
              >
                <select
                  className="select select-bordered w-full"
                  {...register("language")}
                >
                  {LANGUAGES.map((language) => (
                    <option key={language.value} value={language.value}>
                      {language.label}
                    </option>
                  ))}
                </select>
              </FormField>
            </div>

            <FormField
              label="Motor de PDF"
              required
              error={errors.pdfEngine?.message}
              hint="Engine responsável por interpretar arquivos PDF."
            >
              <input
                type="text"
                placeholder="Ex.: PDFBOX"
                className="input input-bordered w-full"
                {...register("pdfEngine")}
              />
            </FormField>

            <div className="card-actions justify-end">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={mutation.isPending || !isDirty}
              >
                {mutation.isPending ? (
                  <span className="loading loading-spinner loading-sm" />
                ) : null}
                Salvar ajustes
              </button>
            </div>
          </div>
        </form>
      </AsyncState>

      <TagsManager />
    </div>
  );
}
