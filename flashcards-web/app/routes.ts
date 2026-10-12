import {
  type RouteConfig,
  index,
  layout,
  route,
} from "@react-router/dev/routes";

export default [
  // Públicas (fora do layout autenticado)
  index("routes/landing.tsx"),
  route("login", "routes/login.tsx"),

  // Layout autenticado (AppLayout + guarda de rota)
  layout("routes/_layout.tsx", [
    route("generate", "routes/generate.tsx"),
    route("decks", "routes/decks.tsx"),
    route("decks/:id", "routes/deck-detail.tsx"),
    route("files", "routes/files.tsx"),
    route("files/:id", "routes/file-detail.tsx"),
    route("settings", "routes/settings.tsx"),
    route("study", "routes/study.tsx"),
    route("study/:deckId", "routes/study-deck.tsx"),
  ]),
] satisfies RouteConfig;
