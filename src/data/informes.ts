import type { Apat, DB, Soci } from './types';
import { arrodonir, mesDe } from './util';

export interface Quota {
  sociId: string;
  unitats: number; // persones (iguals) o parts (proporcional)
  quota: number; // el que li toca pagar
  pagat: number; // el que ha avançat en despeses
  saldo: number; // quota - pagat (positiu: ho paga; negatiu: se li abona)
}

export function repartiment(apat: Apat): { total: number; perUnitat: number; quotes: Quota[] } {
  const total = arrodonir(apat.despeses.reduce((a, d) => a + d.import, 0));
  const unitatsDe = (a: Apat['assistents'][number]) =>
    apat.mode === 'iguals' ? 1 + a.convidats : Math.max(0, a.parts);
  const totalUnitats = apat.assistents.reduce((a, x) => a + unitatsDe(x), 0);
  const perUnitat = totalUnitats > 0 ? total / totalUnitats : 0;

  const ids = new Set<string>([...apat.assistents.map((a) => a.sociId), ...apat.despeses.map((d) => d.pagadorId)]);
  const quotes: Quota[] = [...ids].map((sociId) => {
    const a = apat.assistents.find((x) => x.sociId === sociId);
    const unitats = a ? unitatsDe(a) : 0;
    const quota = arrodonir(perUnitat * unitats);
    const pagat = arrodonir(apat.despeses.filter((d) => d.pagadorId === sociId).reduce((s, d) => s + d.import, 0));
    return { sociId, unitats, quota, pagat, saldo: 0 };
  });
  // Ajust de cèntims perquè la suma de quotes sigui exactament el total
  const dif = arrodonir(total - quotes.reduce((s, q) => s + q.quota, 0));
  if (dif !== 0 && totalUnitats > 0) {
    const q = quotes.find((x) => x.unitats > 0);
    if (q) q.quota = arrodonir(q.quota + dif);
  }
  quotes.forEach((q) => (q.saldo = arrodonir(q.quota - q.pagat)));
  return { total, perUnitat, quotes };
}

export interface LiniaInforme {
  ts: number;
  concepte: string;
  detall: string;
  quantitat: number;
  preu: number;
  import: number;
  tipus: 'consum' | 'apat' | 'abonament';
}

export interface InformeSoci {
  soci: Soci;
  linies: LiniaInforme[];
  totalConsum: number;
  totalApats: number;
  totalAbonaments: number;
  total: number;
}

export function informeSoci(db: DB, soci: Soci, mes: string): InformeSoci {
  const linies: LiniaInforme[] = [];
  for (const c of db.consums) {
    if (c.sociId !== soci.id || c.anulat || mesDe(c.ts) !== mes) continue;
    linies.push({
      ts: c.ts,
      concepte: c.nomProducte,
      detall: c.convidat ? `Convidat: ${c.convidat}` : '',
      quantitat: c.quantitat,
      preu: c.preuUnitari,
      import: arrodonir(c.quantitat * c.preuUnitari),
      tipus: 'consum',
    });
  }
  for (const a of db.apats) {
    if (a.estat !== 'tancat' || mesDe(a.data) !== mes) continue;
    const q = repartiment(a).quotes.find((x) => x.sociId === soci.id);
    if (!q) continue;
    const ts = new Date(a.data + 'T21:00:00').getTime();
    if (q.quota > 0)
      linies.push({
        ts,
        concepte: `Àpat: ${a.titol}`,
        detall: a.mode === 'iguals' ? `${q.unitats} ${q.unitats === 1 ? 'persona' : 'persones'}` : `${q.unitats} parts`,
        quantitat: 1,
        preu: q.quota,
        import: q.quota,
        tipus: 'apat',
      });
    if (q.pagat > 0)
      linies.push({
        ts,
        concepte: `Àpat: ${a.titol}`,
        detall: 'Despeses avançades',
        quantitat: 1,
        preu: -q.pagat,
        import: -q.pagat,
        tipus: 'abonament',
      });
  }
  linies.sort((a, b) => a.ts - b.ts);
  const suma = (t: LiniaInforme['tipus']) => arrodonir(linies.filter((l) => l.tipus === t).reduce((s, l) => s + l.import, 0));
  const totalConsum = suma('consum');
  const totalApats = suma('apat');
  const totalAbonaments = suma('abonament');
  return { soci, linies, totalConsum, totalApats, totalAbonaments, total: arrodonir(totalConsum + totalApats + totalAbonaments) };
}

export function informeMes(db: DB, mes: string): InformeSoci[] {
  return db.socis
    .map((s) => informeSoci(db, s, mes))
    .filter((i) => i.linies.length > 0 || i.soci.actiu)
    .sort((a, b) => a.soci.nom.localeCompare(b.soci.nom, 'ca'));
}

export function apatsOberts(db: DB, mes: string) {
  return db.apats.filter((a) => a.estat === 'obert' && mesDe(a.data) === mes);
}

export function estatMes(db: DB, mes: string) {
  return db.tancaments.find((t) => t.mes === mes)?.estat ?? 'obert';
}
