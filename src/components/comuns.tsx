import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import type { Categoria } from '../data/types';

export const COLOR_CATEGORIA: Record<Categoria, string> = {
  Cervesa: 'var(--cat-cervesa)',
  Vi: 'var(--cat-vi)',
  Refrescos: 'var(--cat-refrescos)',
  Aigua: 'var(--cat-aigua)',
  'Cafè i licors': 'var(--cat-cafe)',
  Picar: 'var(--cat-picar)',
};

const camins: Record<string, ReactNode> = {
  copa: <path d="M8 3h8l-1 6a3 3 0 0 1-6 0L8 3Zm4 9v7m-4 2h8" />,
  llista: <path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01" />,
  calendari: <path d="M4 6h16v14H4zM4 10h16M8 3v4m8-4v4" />,
  olla: <path d="M4 10h16v4a6 6 0 0 1-6 6h-4a6 6 0 0 1-6-6v-4Zm-2 0h20M9 6c0-1 1-1 1-2m4 2c0-1 1-1 1-2" />,
  clau: <path d="M14 10a4 4 0 1 0-3.5 4L12 16h2v2h2v2h4v-3l-5.2-5.2A4 4 0 0 0 14 10Z" />,
  tauleta: <path d="M6 3h12a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Zm5 15h2" />,
  mobil: <path d="M8 2h8a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Zm3 17h2" />,
  sortir: <path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" />,
  mes: <path d="M12 5v14M5 12h14" />,
  menys: <path d="M5 12h14" />,
  creu: <path d="M6 6l12 12M18 6 6 18" />,
  desfer: <path d="M9 14 4 9l5-5M4 9h10a6 6 0 0 1 0 12h-3" />,
  descarrega: <path d="M12 4v11m-5-5 5 5 5-5M5 20h14" />,
  persona: <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0" />,
  enrere: <path d="M15 5l-7 7 7 7" />,
};

export function Icona({ nom, mida = 24 }: { nom: keyof typeof camins | string; mida?: number }) {
  return (
    <svg width={mida} height={mida} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {camins[nom]}
    </svg>
  );
}

/* Avisos breus */
const ToastCtx = createContext<(t: string) => void>(() => {});
export function ToastProvider({ children }: { children: ReactNode }) {
  const [text, setText] = useState<string | null>(null);
  const [clau, setClau] = useState(0);
  useEffect(() => {
    if (!text) return;
    const t = setTimeout(() => setText(null), 3200);
    return () => clearTimeout(t);
  }, [text, clau]);
  const mostra = useCallback((t: string) => {
    setText(t);
    setClau((k) => k + 1);
  }, []);
  return (
    <ToastCtx.Provider value={mostra}>
      {children}
      {text && (
        <div className="toast" role="status" key={clau}>
          {text}
        </div>
      )}
    </ToastCtx.Provider>
  );
}
export const useToast = () => useContext(ToastCtx);

export function Dialeg({ titol, children, onTanca }: { titol: string; children: ReactNode; onTanca: () => void }) {
  useEffect(() => {
    const f = (e: KeyboardEvent) => e.key === 'Escape' && onTanca();
    window.addEventListener('keydown', f);
    return () => window.removeEventListener('keydown', f);
  }, [onTanca]);
  return (
    <div className="fons-dialeg" onMouseDown={(e) => e.target === e.currentTarget && onTanca()}>
      <div className="dialeg" role="dialog" aria-modal="true" aria-label={titol}>
        <div className="fila entre">
          <h2>{titol}</h2>
          <button className="mini" onClick={onTanca} aria-label="Tanca">
            <Icona nom="creu" mida={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Capcalera({ children }: { children?: ReactNode }) {
  return (
    <header className="capcalera">
      <div className="marca">
        <img src="logo.svg" alt="" />
        GastroBacus
      </div>
      <div className="espai" />
      {children}
    </header>
  );
}

export function SelectorMes({ mes, onCanvi, mesos }: { mes: string; onCanvi: (m: string) => void; mesos: string[] }) {
  return (
    <select value={mes} onChange={(e) => onCanvi(e.target.value)} style={{ width: 'auto' }} aria-label="Mes">
      {mesos.map((m) => {
        const [a, n] = m.split('-').map(Number);
        const t = new Date(a, n - 1, 1).toLocaleDateString('ca-ES', { month: 'long', year: 'numeric' });
        return (
          <option key={m} value={m}>
            {t[0].toUpperCase() + t.slice(1)}
          </option>
        );
      })}
    </select>
  );
}

export function useEsperaAccio() {
  const toast = useToast();
  return useCallback(
    async <T,>(fn: () => Promise<T>, ok?: string): Promise<T | true | undefined> => {
      // Retorna el resultat (o true) si ha anat bé, i undefined si hi ha hagut un error
      try {
        const r = await fn();
        if (ok) toast(ok);
        return r === undefined ? true : r;
      } catch (e) {
        toast(e instanceof Error ? e.message : String(e));
        return undefined;
      }
    },
    [toast],
  );
}
