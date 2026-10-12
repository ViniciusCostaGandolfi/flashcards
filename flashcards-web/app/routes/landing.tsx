import { Link, Navigate } from "react-router";
import { useAuth } from "~/hooks/use-auth";
import { InstallPwaButton } from "~/components/ui/install-pwa-button";
import { Spinner } from "~/components/ui/spinner";

function IconArrowRight() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-5 w-5" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3" />
    </svg>
  );
}

function IconSparkles() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715 18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 0 0-2.456 2.456Z" />
    </svg>
  );
}

function IconPhoto() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
    </svg>
  );
}

function IconBolt() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" />
    </svg>
  );
}

function IconBrain() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 18v-5.25m0 0a6.01 6.01 0 0 0 1.5-.189m-1.5.189a6.01 6.01 0 0 1-1.5-.189m3.75 7.478a12.06 12.06 0 0 1-4.5 0m3.75 2.383a14.406 14.406 0 0 1-3 0M14.25 18v-.192c0-.983.658-1.823 1.508-2.316a7.5 7.5 0 1 0-7.517 0c.85.493 1.509 1.333 1.509 2.316V18" />
    </svg>
  );
}

function IconDevicePhone() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 1.5H8.25A2.25 2.25 0 0 0 6 3.75v16.5a2.25 2.25 0 0 0 2.25 2.25h7.5A2.25 2.25 0 0 0 18 20.25V3.75a2.25 2.25 0 0 0-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3" />
    </svg>
  );
}

const FEATURES = [
  {
    icon: <IconSparkles />,
    title: "Gere em segundos",
    description:
      "Envie uma foto, um PDF ou cole um texto. A IA transforma o material em cartões prontos para estudar.",
  },
  {
    icon: <IconPhoto />,
    title: "Imagem, PDF ou texto",
    description:
      "Apostilas, slides e anotações viram Cards no mesmo fluxo, sem trabalho manual.",
  },
  {
    icon: <IconBolt />,
    title: "Três tipos de cartão",
    description:
      "Completar lacunas, verdadeiro/falso e múltipla escolha — variando o estímulo do estudo.",
  },
  {
    icon: <IconBrain />,
    title: "Repetição espaçada",
    description:
      "O algoritmo decide quando revisar cada cartão, focando no que você ainda não domina.",
  },
  {
    icon: <IconDevicePhone />,
    title: "Instale como app",
    description:
      "Funciona no celular e pode ser instalado na tela inicial, com uso offline do conteúdo.",
  },
  {
    icon: <IconArrowRight />,
    title: "Modelos configuráveis",
    description:
      "Escolha os modelos de leitura e geração e ajuste o prompt base do seu jeito.",
  },
];

export default function LandingPage() {
  const { isAuthenticated, isReady } = useAuth();

  if (!isReady) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-base-100">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/generate" replace />;
  }

  const year = new Date().getFullYear();

  return (
    <div className="min-h-screen overflow-x-hidden bg-base-100">
      {/* Navbar */}
      <header className="navbar sticky top-0 z-40 border-b border-base-200 bg-base-100/80 backdrop-blur-md">
        <div className="navbar-start">
          <Link to="/" className="flex items-center gap-2 px-2">
            <img src="/logo.svg" alt="Logo Flashcards" className="h-9 w-9 rounded-box" />
            <span className="font-display text-lg font-bold tracking-tight">
              Flashcards
            </span>
          </Link>
        </div>
        <div className="navbar-end gap-2">
          <InstallPwaButton size="sm" label="Instalar" className="hidden sm:inline-flex" />
          <Link to="/login" className="btn btn-primary btn-sm">
            Entrar
          </Link>
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-grid" />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -top-32 right-[-10%] h-96 w-96 animate-blob rounded-full bg-primary/25 blur-3xl"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute bottom-[-25%] left-[-10%] h-96 w-96 animate-blob rounded-full bg-accent/20 blur-3xl [animation-delay:2s]"
        />

        <div className="hero relative">
          <div className="hero-content mx-auto flex w-full max-w-7xl flex-col items-center gap-12 px-4 py-20 text-center md:py-28">
            <span className="badge badge-primary badge-lg animate-fade-in-down">
              Estudo com IA + repetição espaçada
            </span>
            <h1 className="mt-2 animate-fade-in-up text-balance font-display text-4xl font-black leading-tight tracking-tight md:text-6xl">
              Transforme qualquer material em{" "}
              <span className="gradient-text">flashcards</span>
            </h1>
            <p className="max-w-xl animate-fade-in-up text-lg text-base-content/70 delay-200">
              Envie uma imagem, um PDF ou um texto e receba um Card pronto.
              Estude no celular com revisões que se adaptam ao seu ritmo.
            </p>
            <div className="animate-fade-in-up delay-300 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
              <Link to="/login" className="btn btn-primary btn-lg w-full sm:w-auto">
                Começar agora
                <IconArrowRight />
              </Link>
              <InstallPwaButton size="lg" className="w-full sm:w-auto" />
            </div>
            <p className="animate-fade-in text-sm text-base-content/50 delay-500">
              Funciona no celular · Instalável como app · Uso offline
            </p>
          </div>
        </div>
      </section>

      {/* Recursos */}
      <section id="recursos" className="scroll-mt-24 py-16 md:py-24">
        <div className="mx-auto max-w-7xl px-4">
          <div className="mx-auto max-w-2xl text-center">
            <span className="badge badge-soft badge-primary">Recursos</span>
            <h2 className="mt-4 text-balance font-display text-3xl font-black tracking-tight md:text-4xl">
              Do material bruto ao cartão, sem esforço
            </h2>
            <p className="mt-3 text-base-content/70">
              Tudo o que você precisa para estudar melhor, em um só lugar.
            </p>
          </div>

          <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((feature) => (
              <div
                key={feature.title}
                className="card card-border border-base-200 bg-base-100 glow-hover"
              >
                <div className="card-body">
                  <div className="flex h-11 w-11 items-center justify-center rounded-box bg-primary/15 text-primary">
                    {feature.icon}
                  </div>
                  <h3 className="card-title mt-3 font-display text-lg">
                    {feature.title}
                  </h3>
                  <p className="text-sm text-base-content/70">
                    {feature.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Como funciona */}
      <section
        id="como-funciona"
        className="scroll-mt-24 bg-base-200/60 py-16 md:py-24"
      >
        <div className="mx-auto max-w-7xl px-4">
          <div className="mx-auto max-w-2xl text-center">
            <span className="badge badge-soft badge-accent">Como funciona</span>
            <h2 className="mt-4 text-balance font-display text-3xl font-black tracking-tight md:text-4xl">
              Três passos e pronto
            </h2>
          </div>

          <ol className="steps steps-vertical mt-12 lg:steps-horizontal">
            <li className="step step-primary" data-content="1">
              <div className="text-left">
                <p className="font-semibold">Envie o material</p>
                <p className="text-sm text-base-content/60">
                  Imagem, PDF ou um texto colado — você escolhe.
                </p>
              </div>
            </li>
            <li className="step step-primary" data-content="2">
              <div className="text-left">
                <p className="font-semibold">A IA gera os cartões</p>
                <p className="text-sm text-base-content/60">
                  Um Card completo, formatado e pronto para revisão.
                </p>
              </div>
            </li>
            <li className="step step-primary" data-content="3">
              <div className="text-left">
                <p className="font-semibold">Estude e revise</p>
                <p className="text-sm text-base-content/60">
                  Responda, avalie a dificuldade e deixe o algoritmo agendar.
                </p>
              </div>
            </li>
          </ol>
        </div>
      </section>

      {/* CTA final */}
      <section className="mx-auto max-w-7xl px-4 py-16 md:py-24">
        <div className="card bg-gradient-to-br from-primary via-primary to-accent shadow-xl">
          <div className="card-body items-center py-12 text-center md:py-16">
            <h2 className="text-balance font-display text-3xl font-black tracking-tight text-primary-content md:text-4xl">
              Pronto para estudar melhor?
            </h2>
            <p className="mt-2 max-w-xl text-primary-content/80">
              Crie seu primeiro Card em menos de um minuto.
            </p>
            <div className="card-actions mt-6 flex-col gap-3 sm:flex-row">
              <Link to="/login" className="btn btn-lg bg-base-100 text-base-content">
                Entrar
              </Link>
              <InstallPwaButton
                size="lg"
                label="Instalar o app"
                className="border-primary-content/40 bg-transparent text-primary-content hover:border-primary-content hover:bg-primary-content/10"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-base-200 bg-base-200">
        <div className="footer footer-vertical mx-auto max-w-7xl px-4 py-12 sm:footer-horizontal">
          <aside className="max-w-sm">
            <img src="/logo.svg" alt="Logo Flashcards" className="h-10 w-10 rounded-box" />
            <p className="text-sm text-base-content/70">
              Flashcards — gere e estude Cards com inteligência artificial e
              repetição espaçada.
            </p>
          </aside>
          <nav aria-label="Navegação">
            <h6 className="footer-title">Página</h6>
            <a href="#recursos" className="link link-hover">
              Recursos
            </a>
            <a href="#como-funciona" className="link link-hover">
              Como funciona
            </a>
          </nav>
          <nav aria-label="Acesso">
            <h6 className="footer-title">Acesso</h6>
            <Link to="/login" className="link link-hover">
              Entrar
            </Link>
          </nav>
        </div>
        <div className="border-t border-base-300/60">
          <div className="mx-auto flex max-w-7xl flex-col gap-1 px-4 py-4 text-sm text-base-content/60 sm:flex-row sm:items-center sm:justify-between">
            <p>© {year} Flashcards. Todos os direitos reservados.</p>
            <p>Feito para estudar de verdade.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
