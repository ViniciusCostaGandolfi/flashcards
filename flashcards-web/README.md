# Flashcards Web

Frontend do **Flashcards** — gere baralhos a partir de imagens, PDFs ou textos
e estude com repetição espaçada. PWA instalável (Android e iOS), mobile-first,
em português (pt-BR).

## Stack

- React Router v8 (framework mode, SSR) + React 19 + TypeScript
- Vite 8 + Tailwind 4 (`@tailwindcss/vite`) + daisyUI 5
- `vite-plugin-pwa` + `@vite-pwa/assets-generator`
- TanStack Query, axios (JWT + refresh), react-hook-form + zod, clsx

## Desenvolvimento

```bash
npm install
npm run dev        # http://localhost:3000 (proxy /api -> localhost:8080)
```

## Build de produção

```bash
npm run build      # gera build/client e build/server
npm run start      # react-router-serve ./build/server/index.js
```

## Variáveis de ambiente

`VITE_API_URL` define a base da API:

- vazio → `/api/v1` (mesmo host; em dev o Vite faz proxy de `/api` para `:8080`)
- `https://api-flashcards.vgandolfi.dev` → `https://api-flashcards.vgandolfi.dev/api/v1`

Veja `.env.example`.

## PWA

- `vite.config.ts` gera `manifest.webmanifest` + `sw.js` (`build/client`).
- O service worker é registrado em `app/root.tsx` apenas em produção, com
  `updateViaCache: "none"` (evita SW antigo travado no iOS Safari).
- Botão de instalação em `app/components/ui/install-pwa-button.tsx`:
  Android/Chrome usa `beforeinstallprompt`; iOS Safari mostra instruções manuais.
- Para regenerar os ícones a partir de `public/logo.svg`:
  `npm run generate-pwa-assets`.

### Nota sobre `navigateFallback` (desvio consciente)

O app roda em **SSR** (`react-router.config.ts` → `ssr: true`), então não existe
`build/client/index.html` — o HTML vem do servidor. O `navigateFallback` padrão
do `vite-plugin-pwa` foi **desativado** (`navigateFallback: null`), porque o
Workbox v7 casa rotas em ordem de registro (FIFO): um `NavigationRoute` de
fallback serviria a página estática em **toda** navegação, ignorando o SSR (e,
sem um `index.html` precacheado, o `createHandlerBoundToURL` quebraria a
instalação do service worker). No lugar, o suporte offline de navegação é feito
por uma rota `NetworkFirst` para documentos (`request.destination ===
"document"`): online serve o HTML do servidor, offline devolve a última página
em cache.

