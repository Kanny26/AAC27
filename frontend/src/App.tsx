import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAppStore } from './store/appStore';

const searchSchema = z.object({
  term: z.string().min(2, 'Escribe al menos 2 caracteres'),
});

type SearchForm = z.infer<typeof searchSchema>;

const modules = [
  'Catálogo',
  'Inventario',
  'Compras',
  'Ventas',
  'Apartados',
  'Postventa',
] as const;

export default function App() {
  const selectedModule = useAppStore((state) => state.selectedModule);
  const setSelectedModule = useAppStore((state) => state.setSelectedModule);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SearchForm>({
    resolver: zodResolver(searchSchema),
    defaultValues: { term: '' },
  });

  const onSubmit = (data: SearchForm) => {
    console.info('Búsqueda inicial AAC27:', data.term);
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <section className="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-6 py-8">
        <header className="flex flex-col gap-6 border-b border-white/10 pb-8 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">
              AAC27
            </p>
            <h1 className="mt-3 text-4xl font-bold tracking-tight md:text-5xl">
              Sistema de Gestión de Joyería
            </h1>
            <p className="mt-4 max-w-2xl text-base leading-7 text-slate-300">
              Frontend base con React, TypeScript, TailwindCSS, TanStack Query,
              Zustand, React Hook Form y Zod.
            </p>
          </div>

          <div className="rounded-2xl border border-brand-500/30 bg-brand-500/10 px-5 py-4">
            <p className="text-sm text-slate-300">Backend API</p>
            <p className="mt-1 font-mono text-sm text-brand-100">
              {import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}
            </p>
          </div>
        </header>

        <section className="grid flex-1 gap-6 py-8 lg:grid-cols-[260px_1fr]">
          <aside className="rounded-3xl border border-white/10 bg-white/[0.03] p-4">
            <h2 className="px-3 text-sm font-semibold uppercase tracking-widest text-slate-400">
              Módulos
            </h2>
            <nav className="mt-4 space-y-2">
              {modules.map((moduleName) => (
                <button
                  key={moduleName}
                  type="button"
                  onClick={() => setSelectedModule(moduleName)}
                  className={`w-full rounded-2xl px-4 py-3 text-left text-sm font-medium transition ${
                    selectedModule === moduleName
                      ? 'bg-brand-500 text-slate-950'
                      : 'text-slate-300 hover:bg-white/10'
                  }`}
                >
                  {moduleName}
                </button>
              ))}
            </nav>
          </aside>

          <section className="rounded-3xl border border-white/10 bg-white/[0.03] p-6">
            <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
              <div>
                <p className="text-sm text-slate-400">Módulo seleccionado</p>
                <h2 className="mt-2 text-3xl font-semibold">{selectedModule}</h2>
              </div>
              <span className="rounded-full bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-300">
                Shell compilable
              </span>
            </div>

            <form
              onSubmit={handleSubmit(onSubmit)}
              className="mt-8 rounded-2xl border border-white/10 bg-slate-900/80 p-5"
            >
              <label htmlFor="term" className="text-sm font-medium text-slate-200">
                Búsqueda inicial
              </label>
              <div className="mt-3 flex flex-col gap-3 sm:flex-row">
                <input
                  id="term"
                  type="text"
                  placeholder="Ej: anillo oro"
                  className="min-h-11 flex-1 rounded-xl border border-white/10 bg-slate-950 px-4 text-sm outline-none ring-brand-500/40 transition placeholder:text-slate-500 focus:ring-4"
                  {...register('term')}
                />
                <button
                  type="submit"
                  className="rounded-xl bg-brand-500 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-brand-100"
                >
                  Validar formulario
                </button>
              </div>
              {errors.term && (
                <p className="mt-2 text-sm text-red-300">{errors.term.message}</p>
              )}
            </form>
          </section>
        </section>
      </section>
    </main>
  );
}
