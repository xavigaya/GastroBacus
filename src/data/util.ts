export const uid = () =>
  (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2) + Date.now().toString(36));

export async function hash(text: string): Promise<string> {
  const data = new TextEncoder().encode('gastrobacus:' + text);
  const buf = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('');
}

const eur = new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' });
export const euros = (n: number) => eur.format(Math.round(n * 100) / 100 + 0);
export const arrodonir = (n: number) => Math.round(n * 100) / 100;

export const mesDe = (ts: number | string) => {
  const d = typeof ts === 'string' ? new Date(ts + 'T12:00:00') : new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
};
export const mesActual = () => mesDe(Date.now());
export const avuiISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

const NOMS_MESOS = ['gener', 'febrer', 'març', 'abril', 'maig', 'juny', 'juliol', 'agost', 'setembre', 'octubre', 'novembre', 'desembre'];
export const nomMes = (mes: string) => {
  const [a, m] = mes.split('-').map(Number);
  const n = NOMS_MESOS[m - 1];
  const prep = /^[aeiou]/.test(n) ? "d'" : 'de ';
  return { curt: n, llarg: `${prep}${n} de ${a}`, titol: `${n[0].toUpperCase()}${n.slice(1)} ${a}` };
};
export const mesAnterior = (mes: string) => {
  const [a, m] = mes.split('-').map(Number);
  return m === 1 ? `${a - 1}-12` : `${a}-${String(m - 1).padStart(2, '0')}`;
};
export const mesSeguent = (mes: string) => {
  const [a, m] = mes.split('-').map(Number);
  return m === 12 ? `${a + 1}-01` : `${a}-${String(m + 1).padStart(2, '0')}`;
};

export const dataCurta = (ts: number) =>
  new Date(ts).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' });
export const hora = (ts: number) =>
  new Date(ts).toLocaleTimeString('ca-ES', { hour: '2-digit', minute: '2-digit' });
export const dataLlarga = (iso: string) =>
  new Date(iso + 'T12:00:00').toLocaleDateString('ca-ES', { weekday: 'long', day: 'numeric', month: 'long' });

export const nomComplet = (s: { nom: string; cognoms: string }) => `${s.nom} ${s.cognoms}`;
export const inicials = (s: { nom: string; cognoms: string }) =>
  (s.nom[0] + (s.cognoms[0] ?? '')).toUpperCase();
