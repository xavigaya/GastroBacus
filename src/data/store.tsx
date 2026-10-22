/*
 * Magatzem de dades.
 * Aquesta primera versió desa tot al navegador (mode demostració amb dades fictícies).
 * Totes les accions són asíncrones i passen per aquest fitxer, de manera que
 * es poden substituir per crides a Supabase sense tocar les pantalles.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import type { Apat, Config, Consum, Correu, DB, Origen, Producte, Reserva, Soci } from './types';
import { crearDadesProva } from './seed';
import { arrodonir, euros, hash, mesDe, nomComplet, nomMes, uid } from './util';
import { estatMes, informeMes } from './informes';

const CLAU = 'gastrobacus-db-v1';

function llegir(): DB | null {
  try {
    const t = localStorage.getItem(CLAU);
    return t ? (JSON.parse(t) as DB) : null;
  } catch {
    return null;
  }
}
function desar(db: DB) {
  try {
    localStorage.setItem(CLAU, JSON.stringify(db));
  } catch {
    /* sense emmagatzematge disponible: es manté en memòria */
  }
}

export interface ItemApunt {
  producteId: string;
  quantitat: number;
  convidat: string | null;
}

export class ErrorApp extends Error {}

function crearAccions(get: () => DB, set: (db: DB) => void) {
  const mutar = (fn: (d: DB) => void) => {
    const d = structuredClone(get());
    fn(d);
    set(d);
    return d;
  };
  const mesBloquejat = (mes: string) => estatMes(get(), mes) === 'tancat';

  async function enviarCorreu(c: Correu): Promise<Correu> {
    const cfg = get().config;
    if (!cfg.urlEnviamentCorreu) return c; // queda a la bústia de sortida
    try {
      await fetch(cfg.urlEnviamentCorreu, {
        method: 'POST',
        mode: 'no-cors',
        headers: { 'Content-Type': 'text/plain;charset=utf-8' },
        body: JSON.stringify({ token: cfg.tokenEnviamentCorreu, to: c.per, subject: c.assumpte, body: c.cos }),
      });
      return { ...c, estat: 'enviat' };
    } catch (e) {
      return { ...c, estat: 'error', error: String(e) };
    }
  }

  return {
    async verificarPin(sociId: string, pin: string) {
      const s = get().socis.find((x) => x.id === sociId);
      return !!s && s.actiu && s.pinHash === (await hash(pin));
    },

    async entrarAmbCorreu(email: string, contrasenya: string): Promise<Soci | null> {
      const s = get().socis.find((x) => x.email.toLowerCase() === email.trim().toLowerCase() && x.actiu);
      if (!s) return null;
      return s.contrasenyaHash === (await hash(contrasenya)) ? s : null;
    },

    async apuntar(sociId: string, items: ItemApunt[], origen: Origen, ts = Date.now()): Promise<Consum[]> {
      if (mesBloquejat(mesDe(ts))) throw new ErrorApp('Aquest mes ja està tancat.');
      const nous: Consum[] = [];
      mutar((d) => {
        for (const it of items) {
          const p = d.productes.find((x) => x.id === it.producteId);
          if (!p || it.quantitat <= 0) continue;
          const preu = it.convidat && p.preuConvidat != null ? p.preuConvidat : p.preu;
          const c: Consum = {
            id: uid(),
            sociId,
            producteId: p.id,
            nomProducte: p.nom,
            quantitat: it.quantitat,
            preuUnitari: preu,
            convidat: it.convidat,
            ts,
            origen,
            anulat: null,
          };
          if (p.controlaEstoc) p.estoc = Math.max(0, p.estoc - it.quantitat);
          d.consums.push(c);
          nous.push(c);
        }
      });
      return nous;
    },

    async anular(consumId: string, perId: string, esJunta: boolean) {
      const d0 = get();
      const c = d0.consums.find((x) => x.id === consumId);
      if (!c || c.anulat) return;
      if (mesBloquejat(mesDe(c.ts))) throw new ErrorApp('Aquest mes ja està tancat i no es pot modificar.');
      if (!esJunta) {
        if (c.sociId !== perId) throw new ErrorApp('Només pots desfer les teves consumicions.');
        if (Date.now() - c.ts > d0.config.minutsDesfer * 60000)
          throw new ErrorApp(`Han passat més de ${d0.config.minutsDesfer} minuts. Demana-ho a la junta.`);
      }
      mutar((d) => {
        const x = d.consums.find((y) => y.id === consumId)!;
        x.anulat = { ts: Date.now(), perId };
        const p = d.productes.find((y) => y.id === x.producteId);
        if (p?.controlaEstoc) p.estoc += x.quantitat;
      });
    },

    async guardarProducte(p: Producte) {
      mutar((d) => {
        const i = d.productes.findIndex((x) => x.id === p.id);
        if (i >= 0) d.productes[i] = p;
        else d.productes.push(p);
      });
    },

    async guardarSoci(s: Soci, pin?: string, contrasenya?: string) {
      const d0 = get();
      if (d0.socis.some((x) => x.id !== s.id && x.email.toLowerCase() === s.email.toLowerCase()))
        throw new ErrorApp('Ja hi ha un soci amb aquest correu.');
      const pinHash = pin ? await hash(pin) : s.pinHash;
      const contrasenyaHash = contrasenya ? await hash(contrasenya) : s.contrasenyaHash;
      mutar((d) => {
        const nou = { ...s, pinHash, contrasenyaHash };
        const i = d.socis.findIndex((x) => x.id === s.id);
        if (i >= 0) d.socis[i] = nou;
        else d.socis.push(nou);
      });
    },

    async canviarCredencials(sociId: string, pin?: string, contrasenya?: string) {
      const s = get().socis.find((x) => x.id === sociId);
      if (s) await this.guardarSoci(s, pin, contrasenya);
    },

    async reposar(producteId: string, quantitat: number, perId: string, nota?: string) {
      mutar((d) => {
        const p = d.productes.find((x) => x.id === producteId);
        if (!p) return;
        p.estoc += quantitat;
        d.reposicions.push({ id: uid(), producteId, quantitat, ts: Date.now(), perId, nota });
      });
    },

    async guardarApat(a: Apat) {
      if (mesBloquejat(mesDe(a.data))) throw new ErrorApp('El mes d’aquest àpat ja està tancat.');
      mutar((d) => {
        const i = d.apats.findIndex((x) => x.id === a.id);
        if (i >= 0) d.apats[i] = a;
        else d.apats.push(a);
      });
    },

    async eliminarApat(id: string) {
      const a = get().apats.find((x) => x.id === id);
      if (a && mesBloquejat(mesDe(a.data))) throw new ErrorApp('El mes d’aquest àpat ja està tancat.');
      mutar((d) => {
        d.apats = d.apats.filter((x) => x.id !== id);
      });
    },

    async crearReserva(r: Omit<Reserva, 'id' | 'creada'>) {
      const conflicte = get().reserves.find(
        (x) =>
          x.data === r.data &&
          x.espai === r.espai &&
          (x.franja === r.franja || x.franja === 'Tot el dia' || r.franja === 'Tot el dia'),
      );
      if (conflicte) {
        const qui = get().socis.find((s) => s.id === conflicte.sociId);
        throw new ErrorApp(`${r.espai} ja està reservat aquell dia (${conflicte.franja.toLowerCase()}) per ${qui ? nomComplet(qui) : 'un altre soci'}.`);
      }
      mutar((d) => {
        d.reserves.push({ ...r, id: uid(), creada: Date.now() });
      });
    },

    async eliminarReserva(id: string) {
      mutar((d) => {
        d.reserves = d.reserves.filter((x) => x.id !== id);
      });
    },

    /** Pas 1 del tancament: envia l'informe a cada soci i al tresorer, i obre el termini de reclamacions. */
    async enviarInformes(mes: string) {
      const db = get();
      const cfg = db.config;
      const informes = informeMes(db, mes).filter((i) => i.linies.length > 0);
      const nm = nomMes(mes);
      const desti = (real: string) => (cfg.modeDesenvolupament ? cfg.correuProves : real);
      const limit = new Date(Date.now() + cfg.diesReclamacio * 86400000).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long' });

      const correus: Correu[] = informes.map((inf) => {
        const linies = inf.linies
          .map((l) => `  ${new Date(l.ts).toLocaleDateString('ca-ES', { day: '2-digit', month: '2-digit' })}  ${l.quantitat > 1 ? l.quantitat + ' × ' : ''}${l.concepte}${l.detall ? ' (' + l.detall + ')' : ''}  ${euros(l.import)}`)
          .join('\n');
        return {
          id: uid(),
          ts: Date.now(),
          per: desti(inf.soci.email),
          destinatariOriginal: inf.soci.email,
          assumpte: `${cfg.nomSocietat}: el teu consum ${nm.llarg}`,
          cos:
            `Hola, ${inf.soci.nom}:\n\nAquest és el resum del teu consum a ${cfg.nomSocietat} ${nm.llarg}.\n\n${linies}\n\n` +
            `Consumicions: ${euros(inf.totalConsum)}\n` +
            (inf.totalApats ? `Àpats compartits: ${euros(inf.totalApats)}\n` : '') +
            (inf.totalAbonaments ? `Despeses que vas avançar: ${euros(inf.totalAbonaments)}\n` : '') +
            `\nTotal a carregar al teu compte: ${euros(inf.total)}\n\n` +
            `Si hi veus algun error, digues-ho a la junta abans del ${limit}. Després es passarà el rebut al banc.\n\nJunta de ${cfg.nomSocietat}`,
          estat: 'pendent',
        };
      });
      const total = arrodonir(informes.reduce((s, i) => s + i.total, 0));
      correus.push({
        id: uid(),
        ts: Date.now(),
        per: desti(cfg.correuSocietat),
        destinatariOriginal: cfg.correuSocietat,
        assumpte: `${cfg.nomSocietat}: informe de consum ${nm.llarg} per a tresoreria`,
        cos:
          `Resum ${nm.llarg}\n\n` +
          informes.map((i) => `  ${nomComplet(i.soci).padEnd(32)} ${i.soci.refBanc.padEnd(10)} ${euros(i.total)}`).join('\n') +
          `\n\nTotal: ${euros(total)} (${informes.length} socis)\nTermini de reclamacions fins al ${limit}.`,
        estat: 'pendent',
      });

      const enviats = await Promise.all(correus.map(enviarCorreu));
      mutar((d) => {
        d.correus.unshift(...enviats);
        d.tancaments = d.tancaments.filter((t) => t.mes !== mes);
        d.tancaments.push({ mes, estat: 'informat', informatTs: Date.now(), tancatTs: null });
      });
      return enviats.length;
    },

    /** Pas 2: tanca el mes (ja no es pot modificar) i deixa a punt la remesa per al banc. */
    async tancarMes(mes: string) {
      mutar((d) => {
        const t = d.tancaments.find((x) => x.mes === mes);
        if (t) {
          t.estat = 'tancat';
          t.tancatTs = Date.now();
        }
      });
    },

    async reobrirMes(mes: string) {
      mutar((d) => {
        d.tancaments = d.tancaments.filter((x) => x.mes !== mes);
      });
    },

    async reintentarCorreu(id: string) {
      const c = get().correus.find((x) => x.id === id);
      if (!c) return;
      const r = await enviarCorreu(c);
      mutar((d) => {
        const i = d.correus.findIndex((x) => x.id === id);
        d.correus[i] = r;
      });
    },

    async marcarCorreuEnviat(id: string) {
      mutar((d) => {
        const c = d.correus.find((x) => x.id === id);
        if (c) c.estat = 'enviat';
      });
    },

    async guardarConfig(c: Config) {
      mutar((d) => {
        d.config = c;
      });
    },

    async restablirDadesProva() {
      set(await crearDadesProva());
    },
  };
}

export type Accions = ReturnType<typeof crearAccions>;

interface Ctx {
  db: DB;
  accions: Accions;
}
const DBContext = createContext<Ctx | null>(null);

export function DBProvider({ children }: { children: ReactNode }) {
  const [db, setDb] = useState<DB | null>(() => llegir());
  const ref = useRef(db);
  ref.current = db;

  useEffect(() => {
    if (!db) crearDadesProva().then((d) => {
      desar(d);
      setDb(d);
    });
  }, [db]);

  // Sincronitza entre pestanyes del mateix navegador
  useEffect(() => {
    const f = (e: StorageEvent) => {
      if (e.key === CLAU) {
        const d = llegir();
        if (d) setDb(d);
      }
    };
    window.addEventListener('storage', f);
    return () => window.removeEventListener('storage', f);
  }, []);

  const set = useCallback((d: DB) => {
    ref.current = d;
    desar(d);
    setDb(d);
  }, []);
  const accions = useMemo(() => crearAccions(() => ref.current!, set), [set]);

  if (!db) return <div className="carregant"><img src="logo.svg" alt="" width={64} height={64} /></div>;
  return <DBContext.Provider value={{ db, accions }}>{children}</DBContext.Provider>;
}

export function useDB() {
  const c = useContext(DBContext);
  if (!c) throw new Error('useDB fora del DBProvider');
  return c;
}
