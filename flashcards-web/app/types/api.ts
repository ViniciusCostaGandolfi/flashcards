/* ───────────────────────── Auth ───────────────────────── */

export interface User {
  id: string;
  name: string;
  email: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

/* ─────────────────────── Settings ─────────────────────── */

export interface Settings {
  readerModel: string;
  generatorModel: string;
  basePrompt: string;
  cardsPerGeneration: number;
  language: string;
  pdfEngine: string;
}

export interface ModelInfo {
  id: string;
  name: string;
  inputModalities: string[];
}

/* ─────────────────────── Generation ───────────────────── */

export type GenerationStatus =
  | "PENDING"
  | "READING"
  | "GENERATING"
  | "DONE"
  | "FAILED";

export interface GenerationJob {
  id: string;
  status: GenerationStatus;
  errorMessage: string | null;
  deckId: string | null;
  cardsCount: number | null;
}

export interface CreateGenerationResponse {
  jobId: string;
}

export interface CreateGenerationInput {
  file?: File | null;
  text?: string | null;
  title?: string | null;
  /** Tags aplicadas a todos os cartões gerados (enviadas como CSV). */
  tags?: string[] | null;
}

/* ───────────────────────── Tags ───────────────────────── */

export interface Tag {
  id: string;
  name: string;
  cardsCount: number;
}

/* ───────────────────────── Files ──────────────────────── */

export interface FileSummary {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
  deckId: string | null;
  cardsCount: number;
}

/* ───────────────────────── Decks ──────────────────────── */

export interface Deck {
  id: string;
  title: string;
  sourceType: string;
  createdAt: string;
  cardsCount: number;
  dueCount: number;
}

/* ───────────────────────── Cards ──────────────────────── */

export type CardType = "CLOZE" | "TRUE_FALSE" | "MULTIPLE_CHOICE";

export interface Card {
  id: string;
  deckId: string;
  type: CardType;
  question: string;
  answer: string;
  options: string[];
  explanation: string | null;
  hint: string | null;
  tags: string[];
  position: number;
  easeFactor: number;
  intervalDays: number;
  repetitions: number;
  dueDate: string | null;
  lastReviewedAt: string | null;
}

export interface CardInput {
  deckId: string;
  type: CardType;
  question: string;
  answer: string;
  options: string[];
  explanation?: string | null;
  hint?: string | null;
  tags?: string[];
}

export type ReviewGrade = "AGAIN" | "HARD" | "EASY";

/* ───────────────────── File detail ────────────────────── */

export interface FileDetail extends FileSummary {
  deck: Deck | null;
  cards: Card[];
}

/* ─────────────────────── Misc ─────────────────────────── */

export interface MessageResponse {
  message: string;
}
