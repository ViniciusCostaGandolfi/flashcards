import { cn } from "~/lib/utils";

type SpinnerSize = "sm" | "md" | "lg";

const sizeClass: Record<SpinnerSize, string> = {
  sm: "loading-sm",
  md: "loading-md",
  lg: "loading-lg",
};

interface SpinnerProps {
  size?: SpinnerSize;
  className?: string;
}

export function Spinner({ size = "md", className }: SpinnerProps) {
  return (
    <span
      role="status"
      aria-label="Carregando"
      className={cn("loading loading-spinner", sizeClass[size], className)}
    />
  );
}

export default Spinner;
