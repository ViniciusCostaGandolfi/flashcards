import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    tailwindcss(),
    reactRouter(),
    VitePWA({
      registerType: "autoUpdate",
      outDir: "build/client",
      injectRegister: null,
      manifest: {
        id: "/",
        name: "Flashcards",
        short_name: "Flashcards",
        description:
          "Flashcards — gere Cards a partir de imagens, PDFs ou textos e estude com repetição espaçada.",
        lang: "pt-BR",
        theme_color: "#2563eb",
        background_color: "#0f172a",
        display: "standalone",
        start_url: "/",
        scope: "/",
        orientation: "portrait",
        categories: ["education", "productivity"],
        icons: [
          {
            src: "pwa-64x64.png",
            sizes: "64x64",
            type: "image/png",
          },
          {
            src: "pwa-192x192.png",
            sizes: "192x192",
            type: "image/png",
          },
          {
            src: "pwa-512x512.png",
            sizes: "512x512",
            type: "image/png",
          },
          {
            src: "maskable-icon-512x512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "any maskable",
          },
          {
            src: "apple-touch-icon-180x180.png",
            sizes: "180x180",
            type: "image/png",
          },
        ],
      },
      workbox: {
        globPatterns: ["**/*.{js,css,html,svg,png,ico,webmanifest,woff2}"],
        // Desativa o `navigateFallback` padrão do vite-plugin-pwa. Com SSR não
        // existe build/client/index.html e o Workbox v7 casa rotas em ordem de
        // registro (FIFO): um NavigationRoute de fallback serviria a página
        // estática em TODA navegação, ignorando o servidor. O offline de
        // navegação é feito pela rota de documentos NetworkFirst abaixo.
        navigateFallback: null,
        // O SW novo assume o controle imediatamente e limpa caches de versões
        // antigas — essencial no iOS Safari, que segura o SW antigo (assets
        // removidos → tela branca no iPhone).
        clientsClaim: true,
        skipWaiting: true,
        cleanupOutdatedCaches: true,
        runtimeCaching: [
          // Navegações (documentos): online serve o HTML renderizado pelo
          // servidor (SSR); offline devolve a última página em cache.
          {
            urlPattern: ({ request }) => request.destination === "document",
            handler: "NetworkFirst",
            options: {
              networkTimeoutSeconds: 4,
              cacheName: "flashcards-pages",
              expiration: {
                maxEntries: 30,
                maxAgeSeconds: 60 * 60 * 24 * 7,
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
          {
            urlPattern: ({ url }) => url.pathname.startsWith("/api/v1"),
            handler: "NetworkFirst",
            options: {
              networkTimeoutSeconds: 10,
              cacheName: "flashcards-api",
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24 * 7,
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
          {
            urlPattern: /^https:\/\/fonts\.(googleapis|gstatic)\.com\/.*/i,
            handler: "StaleWhileRevalidate",
            options: {
              cacheName: "google-fonts",
              expiration: {
                maxEntries: 20,
                maxAgeSeconds: 60 * 60 * 24 * 365,
              },
            },
          },
        ],
      },
      devOptions: {
        enabled: false,
      },
    }),
  ],
  resolve: {
    tsconfigPaths: true,
  },
  server: {
    // Porta fixa 3000: é a origem aceita pelo CORS do backend (FRONTEND_URL).
    port: 3000,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
