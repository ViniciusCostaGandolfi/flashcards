import { useEffect, useRef } from "react";
import { useInstallPrompt } from "~/hooks/use-install-prompt";
import { cn } from "~/lib/utils";

interface InstallPwaButtonProps {
  className?: string;
  size?: "sm" | "md" | "lg";
  label?: string;
}

function DownloadIcon({ className = "h-5 w-5" }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3"
      />
    </svg>
  );
}

function IosInstructionsDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog ref={dialogRef} className="modal" onCancel={onClose}>
      <div className="modal-box">
        <h3 className="text-lg font-bold">Instalar no iPhone / iPad</h3>
        <ol className="mt-4 flex flex-col gap-3 text-sm text-base-content/80">
          <li className="flex items-start gap-3">
            <span className="badge badge-primary badge-sm mt-0.5">1</span>
            <span>
              Toque no botão <strong>Compartilhar</strong>{" "}
              <span aria-hidden="true">（□↑）</span> na barra do Safari.
            </span>
          </li>
          <li className="flex items-start gap-3">
            <span className="badge badge-primary badge-sm mt-0.5">2</span>
            <span>
              Role e toque em <strong>Adicionar à Tela de Início</strong>.
            </span>
          </li>
          <li className="flex items-start gap-3">
            <span className="badge badge-primary badge-sm mt-0.5">3</span>
            <span>
              Toque em <strong>Adicionar</strong>. O ícone aparecerá na sua
              tela inicial.
            </span>
          </li>
        </ol>
        <div className="modal-action">
          <button type="button" className="btn btn-primary" onClick={onClose}>
            Entendi
          </button>
        </div>
      </div>
      <form method="dialog" className="modal-backdrop">
        <button type="submit" aria-label="Fechar">
          fechar
        </button>
      </form>
    </dialog>
  );
}

/**
 * Botão de instalação do PWA.
 * - Android/Chrome: usa o evento `beforeinstallprompt` capturado.
 * - iOS Safari: não há evento → abre instruções manuais.
 * - Instalado (standalone): não renderiza nada.
 */
export function InstallPwaButton({
  className,
  size = "md",
  label = "Instalar app",
}: InstallPwaButtonProps) {
  const {
    canInstall,
    isInstalled,
    isIos,
    showIosInstructions,
    openIosInstructions,
    closeIosInstructions,
    promptInstall,
  } = useInstallPrompt();

  if (isInstalled) return null;

  const sizeClass =
    size === "sm" ? "btn-sm" : size === "lg" ? "btn-lg" : "btn-md";

  // iOS: sempre oferecemos as instruções (o evento nunca dispara no iOS).
  if (isIos) {
    return (
      <>
        <button
          type="button"
          className={cn("btn btn-outline", sizeClass, className)}
          onClick={openIosInstructions}
        >
          <DownloadIcon />
          {label}
        </button>
        <IosInstructionsDialog
          open={showIosInstructions}
          onClose={closeIosInstructions}
        />
      </>
    );
  }

  // Android/Chrome/desktop com suporte: botão que dispara o prompt nativo.
  if (canInstall) {
    return (
      <button
        type="button"
        className={cn("btn btn-primary", sizeClass, className)}
        onClick={() => {
          void promptInstall();
        }}
      >
        <DownloadIcon />
        {label}
      </button>
    );
  }

  // Navegador sem suporte a instalação: não renderiza.
  return null;
}

export default InstallPwaButton;
