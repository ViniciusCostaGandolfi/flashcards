import clsx from "clsx";
import type {
  CardType,
  GenerationStatus,
  ReviewGrade,
  User,
} from "~/types/api";

export function cn(
  ...inputs: Array<string | false | null | undefined>
): string {
  return clsx(...inputs);
}

/** Nome de exibição curto a partir do usuário autenticado. */
export function getUserDisplayName(user: User | null): string {
  if (!user) return "";
  return user.name?.trim() || user.email;
}

export function getInitials(name: string | null | undefined): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

/** Formata bytes em unidade legível (pt-BR). */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  const index = Math.min(
    Math.floor(Math.log(bytes) / Math.log(1024)),
    units.length - 1,
  );
  const value = bytes / 1024 ** index;
  const formatted = value.toLocaleString("pt-BR", {
    maximumFractionDigits: value >= 100 || index === 0 ? 0 : 1,
  });
  return `${formatted} ${units[index]}`;
}

/** Converte um valor ISO para Date local (datas puras sem deslocamento de dia). */
function toLocalDate(iso: string): Date {
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    const [year, month, day] = iso.split("-").map(Number);
    return new Date(year, month - 1, day);
  }
  return new Date(iso);
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  const date = toLocalDate(iso);
  if (Number.isNaN(date.getTime())) return "";
  const day = date.getDate().toString().padStart(2, "0");
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  return `${day}/${month}/${date.getFullYear()}`;
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "";
  const date = toLocalDate(iso);
  if (Number.isNaN(date.getTime())) return "";
  const day = date.getDate().toString().padStart(2, "0");
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const hours = date.getHours().toString().padStart(2, "0");
  const minutes = date.getMinutes().toString().padStart(2, "0");
  return `${day}/${month}/${date.getFullYear()} às ${hours}:${minutes}`;
}

/* ─────────────────────── Rótulos (pt-BR) ─────────────────────── */

export const CARD_TYPE_LABEL: Record<CardType, string> = {
  CLOZE: "Completar",
  TRUE_FALSE: "Verdadeiro ou falso",
  MULTIPLE_CHOICE: "Múltipla escolha",
};

export const CARD_TYPE_BADGE_CLASS: Record<CardType, string> = {
  CLOZE: "badge-info",
  TRUE_FALSE: "badge-warning",
  MULTIPLE_CHOICE: "badge-accent",
};

export const GENERATION_STATUS_LABEL: Record<GenerationStatus, string> = {
  PENDING: "Na fila",
  READING: "Lendo o material",
  GENERATING: "Gerando os cartões",
  DONE: "Concluído",
  FAILED: "Falhou",
};

export const GENERATION_STATUS_BADGE_CLASS: Record<GenerationStatus, string> = {
  PENDING: "badge-ghost",
  READING: "badge-info",
  GENERATING: "badge-primary",
  DONE: "badge-success",
  FAILED: "badge-error",
};

export const GENERATION_STEPS: GenerationStatus[] = [
  "PENDING",
  "READING",
  "GENERATING",
  "DONE",
];

export const REVIEW_GRADE_LABEL: Record<ReviewGrade, string> = {
  AGAIN: "Errei",
  HARD: "Difícil",
  EASY: "Fácil",
};

/** Índice da etapa atual no fluxo de geração (0..3, -1 se falhou). */
export function generationStepIndex(status: GenerationStatus): number {
  return GENERATION_STEPS.indexOf(status);
}
