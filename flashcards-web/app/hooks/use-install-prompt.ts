import { useCallback, useEffect, useState } from "react";

/** Evento não-padronizado de instalação do Chrome/Android. */
interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{
    outcome: "accepted" | "dismissed";
    platform: string;
  }>;
  prompt: () => Promise<void>;
}

interface NavigatorStandalone extends Navigator {
  standalone?: boolean;
}

function detectStandalone(): boolean {
  if (typeof window === "undefined") return false;
  const nav = window.navigator as NavigatorStandalone;
  return (
    window.matchMedia?.("(display-mode: standalone)").matches === true ||
    nav.standalone === true
  );
}

function detectIos(): boolean {
  if (typeof window === "undefined") return false;
  const ua = window.navigator.userAgent;
  const isIosDevice = /iPad|iPhone|iPod/.test(ua);
  // iPadOS 13+ se identifica como Mac; usa touch points para diferenciar.
  const isIpadOs =
    ua.includes("Macintosh") && window.navigator.maxTouchPoints > 1;
  return isIosDevice || isIpadOs;
}

export interface InstallPromptState {
  /** Há um evento `beforeinstallprompt` disponível (Android/Chrome desktop). */
  canInstall: boolean;
  /** App já instalado (standalone) — esconder o botão. */
  isInstalled: boolean;
  /** iOS Safari: sem evento, precisa de instruções manuais. */
  isIos: boolean;
  /** Modal de instruções do iOS visível. */
  showIosInstructions: boolean;
  openIosInstructions: () => void;
  closeIosInstructions: () => void;
  /** Dispara o prompt nativo. Resolve com o outcome. */
  promptInstall: () => Promise<"accepted" | "dismissed" | "unavailable">;
}

/**
 * Captura o `beforeinstallprompt` (Android/Chrome) e detecta iOS para exibir
 * instruções manuais. Esconde tudo quando o app já está instalado.
 */
export function useInstallPrompt(): InstallPromptState {
  const [deferredPrompt, setDeferredPrompt] =
    useState<BeforeInstallPromptEvent | null>(null);
  const [isInstalled, setIsInstalled] = useState<boolean>(() =>
    detectStandalone(),
  );
  const [isIos, setIsIos] = useState<boolean>(() => detectIos());
  const [showIosInstructions, setShowIosInstructions] = useState(false);

  useEffect(() => {
    setIsIos(detectIos());
    setIsInstalled(detectStandalone());

    const handleBeforeInstallPrompt = (event: Event) => {
      // Impede o mini-infobar do Chrome e guarda para o clique do botão.
      event.preventDefault();
      setDeferredPrompt(event as BeforeInstallPromptEvent);
    };

    const handleInstalled = () => {
      setIsInstalled(true);
      setDeferredPrompt(null);
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    window.addEventListener("appinstalled", handleInstalled);

    // Detecta instalação via display-mode changes (Android).
    const media = window.matchMedia("(display-mode: standalone)");
    const handleDisplayMode = (event: MediaQueryListEvent) => {
      if (event.matches) setIsInstalled(true);
    };
    media.addEventListener?.("change", handleDisplayMode);

    return () => {
      window.removeEventListener(
        "beforeinstallprompt",
        handleBeforeInstallPrompt,
      );
      window.removeEventListener("appinstalled", handleInstalled);
      media.removeEventListener?.("change", handleDisplayMode);
    };
  }, []);

  const openIosInstructions = useCallback(() => setShowIosInstructions(true), []);
  const closeIosInstructions = useCallback(
    () => setShowIosInstructions(false),
    [],
  );

  const promptInstall = useCallback(async () => {
    if (!deferredPrompt) return "unavailable" as const;
    await deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    setDeferredPrompt(null);
    return outcome;
  }, [deferredPrompt]);

  return {
    canInstall: !!deferredPrompt,
    isInstalled,
    isIos,
    showIosInstructions,
    openIosInstructions,
    closeIosInstructions,
    promptInstall,
  };
}

export default useInstallPrompt;
