import { useState } from 'react';
import { useDB } from '../data/store';
import { FRANGES, type Franja, type Soci } from '../data/types';
import { avuiISO, dataLlarga, mesActual, mesAnterior, mesSeguent, nomComplet, nomMes } from '../data/util';
import { Icona, useEsperaAccio } from './comuns';

const DIES = ['dl', 'dt', 'dc', 'dj', 'dv', 'ds', 'dg'];

export function Reserves({ soci }: { soci: Soci }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const esJunta = soci.rol === 'junta';
  const avui = avuiISO();
  const [mes, setMes] = useState(mesActual());
  const [dia, setDia] = useState(avui);
  const [franja, setFranja] = useState<Franja>('Sopar');
  const [espai, setEspai] = useState(db.config.espais[0] ?? '');
  const [persones, setPersones] = useState(8);
  const [notes, setNotes] = useState('');

  const [a, m] = mes.split('-').map(Number);
  const primer = new Date(a, m - 1, 1);
  const offset = (primer.getDay() + 6) % 7;
  const diesMes = new Date(a, m, 0).getDate();
  const celles: { iso: string; n: number; fora: boolean }[] = [];
  const nCelles = Math.ceil((offset + diesMes) / 7) * 7;
  for (let i = 0; i < nCelles; i++) {
    const d = new Date(a, m - 1, 1 - offset + i);
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    celles.push({ iso, n: d.getDate(), fora: d.getMonth() !== m - 1 });
  }

  const delDia = db.reserves.filter((r) => r.data === dia).sort((x, y) => FRANGES.indexOf(x.franja) - FRANGES.indexOf(y.franja));
  const meves = db.reserves.filter((r) => r.sociId === soci.id && r.data >= avui).sort((x, y) => x.data.localeCompare(y.data));
  const properes = db.reserves.filter((r) => r.data >= avui).sort((x, y) => x.data.localeCompare(y.data));

  const reserva = async () => {
    const ok = await espera(
      () => accions.crearReserva({ sociId: soci.id, data: dia, franja, espai, persones, notes }),
      'Reserva feta',
    );
    if (ok !== undefined) setNotes('');
  };

  const Fila = ({ r }: { r: (typeof db.reserves)[number] }) => {
    const qui = db.socis.find((s) => s.id === r.sociId);
    const pot = r.sociId === soci.id || esJunta;
    return (
      <li className="fila entre" style={{ padding: '.6rem 0', borderBottom: '1px solid var(--vora)' }}>
        <span>
          <strong>{dataLlarga(r.data)}</strong>, {r.franja.toLowerCase()}
          <br />
          <span className="suau">
            {r.espai}, {r.persones} persones{qui && r.sociId !== soci.id ? `, ${nomComplet(qui)}` : ''}
            {r.notes && `. ${r.notes}`}
          </span>
        </span>
        {pot && (
          <button className="btn fantasma petit" onClick={() => espera(() => accions.eliminarReserva(r.id), 'Reserva anul·lada')}>
            Anul·lar
          </button>
        )}
      </li>
    );
  };

  return (
    <div className="pila gran">
      <h1>Reserves</h1>
      <div className="graella-form" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', alignItems: 'start' }}>
        <div className="bloc pila">
          <div className="fila entre">
            <button className="mini" onClick={() => setMes(mesAnterior(mes))} aria-label="Mes anterior">
              <Icona nom="enrere" mida={18} />
            </button>
            <h3>{nomMes(mes).titol}</h3>
            <button className="mini" onClick={() => setMes(mesSeguent(mes))} aria-label="Mes següent" style={{ transform: 'scaleX(-1)' }}>
              <Icona nom="enrere" mida={18} />
            </button>
          </div>
          <div className="calendari">
            {DIES.map((d) => (
              <div className="dsem" key={d}>
                {d}
              </div>
            ))}
            {celles.map((c) => {
              const n = db.reserves.filter((r) => r.data === c.iso).length;
              return (
                <button
                  key={c.iso}
                  className={(c.fora ? 'fora ' : '') + (c.iso === avui ? 'avui' : '')}
                  aria-pressed={c.iso === dia}
                  disabled={c.iso < avui}
                  onClick={() => setDia(c.iso)}
                  aria-label={`${dataLlarga(c.iso)}${n ? `, ${n} reserves` : ''}`}
                >
                  {c.n}
                  {n > 0 && (
                    <span className="pics">
                      {Array.from({ length: Math.min(n, 3) }).map((_, i) => (
                        <i key={i} />
                      ))}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        <div className="bloc pila">
          <h2 style={{ textTransform: 'capitalize' }}>{dataLlarga(dia)}</h2>
          {delDia.length === 0 ? (
            <p className="suau">No hi ha cap reserva aquest dia.</p>
          ) : (
            <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              {delDia.map((r) => (
                <Fila r={r} key={r.id} />
              ))}
            </ul>
          )}
          <h3>Fer una reserva</h3>
          <div className="graella-form" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}>
            <label className="camp">
              <span>Quan</span>
              <select value={franja} onChange={(e) => setFranja(e.target.value as Franja)}>
                {FRANGES.map((f) => (
                  <option key={f}>{f}</option>
                ))}
              </select>
            </label>
            <label className="camp">
              <span>Persones</span>
              <input type="number" min={1} max={80} value={persones} onChange={(e) => setPersones(Number(e.target.value))} />
            </label>
          </div>
          <label className="camp">
            <span>Espai</span>
            <select value={espai} onChange={(e) => setEspai(e.target.value)}>
              {db.config.espais.map((e) => (
                <option key={e}>{e}</option>
              ))}
            </select>
          </label>
          <label className="camp">
            <span>Notes</span>
            <input type="text" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Opcional: motiu, necessitats…" />
          </label>
          <button className="btn" onClick={reserva} disabled={persones < 1}>
            Reservar
          </button>
        </div>
      </div>

      <section className="pila">
        <h2>{esJunta ? 'Properes reserves' : 'Les meves reserves'}</h2>
        {(esJunta ? properes : meves).length === 0 ? (
          <p className="suau">Cap reserva pendent.</p>
        ) : (
          <ul className="bloc" style={{ listStyle: 'none', margin: 0, paddingTop: '.2rem', paddingBottom: '.2rem' }}>
            {(esJunta ? properes : meves).map((r) => (
              <Fila r={r} key={r.id} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
