import {
  defineConfig,
  minimal2023Preset,
} from "@vite-pwa/assets-generator/config";

/**
 * Configuração do `@vite-pwa/assets-generator`.
 * Gera os ícones do manifest a partir de `public/logo.svg`:
 *   npm run generate-pwa-assets
 */
export default defineConfig({
  headLinkOptions: {
    preset: "2023",
  },
  preset: minimal2023Preset,
  images: ["public/logo.svg"],
});
