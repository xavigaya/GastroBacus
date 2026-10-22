import { useState } from 'react';
import { useDB } from '../data/store';
import type { Apat, Soci } from '../data/types';
import { repartiment } from '../data/informes';
import { avuiISO, dataLlarga, euros, nomComplet, uid } from '../data/util';
import { Icona, useEsperaAccio } from './comuns';

export function Apats({ soci }: { soci: Soci }) {
  const { db } = useDB();
  const [editant, setEditant] = useState<Apat | null>(null);

  if (editant) return <EditorApat inicial={editant} soci={soci} onFet={() => setEditant(null)} />;

  const apats = [...db.apats].sort((a, b) => b.data.localeCompare(a.data));
  const nou = (): Apat => ({
    id: uid(),
    titol: '',
    data: avuiISO(),
    organitzadorId: soci.id,
    mode: 'iguals',
    assistents: [{ sociId: soci.id, convidats: 0, parts: 1 }],
    despeses: [],
    estat: 'obert',
  });

  return (
    <div className="pila gran">
      <div className="fila entre">
        <h1>Àpats compartits</h1>
        <button className="btn" onClick={() => setEditant(nou())}>
          <Icona nom="mes" mida={20} /> Nou àpat
        </button>
      </div>
      <p className="suau" style={{ maxWidth: '62ch' }}>
        Apunta qui ve a dinar o sopar i què ha costat la compra. Qui ha avançat diners se'ls descompta, i a la resta se'ls carrega la seva part al
        compte del mes.
      </p>
      {apats.length === 0 && <p className="suau">Encara no hi ha cap àpat. Crea'n un per repartir-ne les despeses.</p>}
      <div className="pila">
        {apats.map((a) => {
          const r = repartiment(a);
          const meu = r.quotes.find((q) => q.sociId === soci.id);
          const org = db.socis.find((s) => s.id === a.organitzadorId);
          return (
            <button key={a.id} className="bloc fila entre" style={{ textAlign: 'left', cursor: 'pointer', width: '100%' }} onClick={() => setEditant(a)}>
              <span className="pila" style={{ gap: '.2rem' }}>
                <strong style={{ fontSize: '1.1rem' }}>{a.titol || 'Àpat sense títol'}</strong>
                <span className="suau">
                  {dataLlarga(a.data)}. {a.assistents.length} socis, organitza {org ? org.nom : '—'}
                </span>
              </span>
              <span className="pila" style={{ gap: '.2rem', justifyItems: 'end' }}>
                <span className={'etiqueta ' + (a.estat === 'obert' ? 'ambre' : 'verd')}>{a.estat === 'obert' ? 'Obert' : 'Passat al compte'}</span>
                <span className="xifra">{euros(r.total)}</span>
                {meu && (
                  <small className={meu.saldo < 0 ? 'saldo-negatiu' : 'saldo-positiu'}>
                    {meu.saldo < 0 ? `Se't torna ${euros(-meu.saldo)}` : `Et toca ${euros(meu.saldo)}`}
                  </small>
                )}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function EditorApat({ inicial, soci, onFet }: { inicial: Apat; soci: Soci; onFet: () => void }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [a, setA] = useState<Apat>(structuredClone(inicial));
  const esJunta = soci.rol === 'junta';
  const potEditar = a.estat === 'obert' && (a.organitzadorId === soci.id || esJunta || a.assistents.some((x) => x.sociId === soci.id));
  const potTancar = a.estat === 'obert' && (a.organitzadorId === soci.id || esJunta);
  const existeix = db.apats.some((x) => x.id === a.id);
  const r = repartiment(a);
  const nom = (id: string) => {
    const s = db.socis.find((x) => x.id === id);
    return s ? nomComplet(s) : '—';
  };
  const socisDisponibles = db.socis.filter((s) => s.actiu && !a.assistents.some((x) => x.sociId === s.id));
  const upd = (f: (x: Apat) => void) =>
    setA((prev) => {
      const n = structuredClone(prev);
      f(n);
      return n;
    });

  const desa = async (tancar = false) => {
    if (!a.titol.trim()) return espera(async () => Promise.reject(new Error('Posa un títol a l’àpat.')));
    const nou = { ...a, estat: tancar ? ('tancat' as const) : a.estat };
    const ok = await espera(() => accions.guardarApat(nou), tancar ? 'Àpat passat als comptes' : 'Àpat desat');
    if (ok !== undefined) onFet();
  };

  return (
    <div className="pila gran">
      <div className="fila">
        <button className="btn fantasma petit" onClick={onFet}>
          <Icona nom="enrere" mida={18} /> Àpats
        </button>
      </div>
      <h1>{existeix ? a.titol || 'Àpat' : 'Nou àpat'}</h1>
      {a.estat === 'tancat' && <div className="avis ok">Aquest àpat ja s'ha passat als comptes del mes.</div>}

      <fieldset disabled={!potEditar} style={{ border: 0, padding: 0, margin: 0 }} className="pila gran">
        <div className="graella-form">
          <label className="camp">
            <span>Títol</span>
            <input type="text" value={a.titol} onChange={(e) => upd((x) => (x.titol = e.target.value))} placeholder="Per exemple, Sopar de caragols" />
          </label>
          <label className="camp">
            <span>Data</span>
            <input type="date" value={a.data} onChange={(e) => upd((x) => (x.data = e.target.value))} />
          </label>
        </div>

        <section className="pila">
          <div className="fila entre">
            <h2>Com es reparteix</h2>
            <div className="interruptor" role="group" aria-label="Mode de repartiment">
              <button aria-pressed={a.mode === 'iguals'} onClick={() => upd((x) => (x.mode = 'iguals'))}>
                A parts iguals
              </button>
              <button aria-pressed={a.mode === 'proporcional'} onClick={() => upd((x) => (x.mode = 'proporcional'))}>
                Segons el que ha pres
              </button>
            </div>
          </div>
          <p className="suau" style={{ maxWidth: '62ch' }}>
            {a.mode === 'iguals'
              ? 'Cada persona paga el mateix. Els convidats compten com una persona més i van al compte del soci que els porta.'
              : 'Dona a cadascú les parts que ha pres: 1 és una ració normal, 0,5 mitja, 2 el doble. Si porta convidats, suma’ls a les seves parts.'}
          </p>
        </section>

        <section className="pila">
          <h2>Qui hi ve</h2>
          <div className="taula-cont">
            <table>
              <thead>
                <tr>
                  <th>Soci</th>
                  <th className="num">{a.mode === 'iguals' ? 'Convidats' : 'Parts'}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {a.assistents.map((x, i) => (
                  <tr key={x.sociId}>
                    <td>{nom(x.sociId)}</td>
                    <td className="num">
                      {a.mode === 'iguals' ? (
                        <input type="number" min={0} max={20} value={x.convidats} style={{ width: 90 }} aria-label={`Convidats de ${nom(x.sociId)}`}
                          onChange={(e) => upd((y) => (y.assistents[i].convidats = Math.max(0, Number(e.target.value))))} />
                      ) : (
                        <input type="number" min={0} step={0.5} value={x.parts} style={{ width: 90 }} aria-label={`Parts de ${nom(x.sociId)}`}
                          onChange={(e) => upd((y) => (y.assistents[i].parts = Math.max(0, Number(e.target.value))))} />
                      )}
                    </td>
                    <td className="num">
                      <button className="mini" aria-label={`Treu ${nom(x.sociId)}`} onClick={() => upd((y) => y.assistents.splice(i, 1))}>
                        <Icona nom="creu" mida={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {socisDisponibles.length > 0 && (
            <select value="" aria-label="Afegir un soci" onChange={(e) => e.target.value && upd((y) => y.assistents.push({ sociId: e.target.value, convidats: 0, parts: 1 }))}>
              <option value="">+ Afegir un soci…</option>
              {socisDisponibles.map((s) => (
                <option key={s.id} value={s.id}>
                  {nomComplet(s)}
                </option>
              ))}
            </select>
          )}
        </section>

        <section className="pila">
          <h2>Despeses de l'àpat</h2>
          <p className="suau">El que algú ha pagat de la seva butxaca per a l'àpat. Se li descomptarà del que li toca.</p>
          {a.despeses.map((d, i) => (
            <div key={d.id} className="graella-form" style={{ gridTemplateColumns: 'minmax(160px, 1.2fr) minmax(160px, 1.5fr) 120px auto', alignItems: 'end' }}>
              <label className="camp">
                <span>Ho ha pagat</span>
                <select value={d.pagadorId} onChange={(e) => upd((y) => (y.despeses[i].pagadorId = e.target.value))}>
                  {db.socis.filter((s) => s.actiu).map((s) => (
                    <option key={s.id} value={s.id}>
                      {nomComplet(s)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="camp">
                <span>Concepte</span>
                <input type="text" value={d.concepte} onChange={(e) => upd((y) => (y.despeses[i].concepte = e.target.value))} placeholder="Carn, verdura…" />
              </label>
              <label className="camp">
                <span>Import (€)</span>
                <input type="number" min={0} step={0.01} value={d.import} onChange={(e) => upd((y) => (y.despeses[i].import = Math.max(0, Number(e.target.value))))} />
              </label>
              <button className="mini" style={{ marginBottom: 7 }} aria-label="Treu la despesa" onClick={() => upd((y) => y.despeses.splice(i, 1))}>
                <Icona nom="creu" mida={16} />
              </button>
            </div>
          ))}
          <div>
            <button className="btn secundari petit" onClick={() => upd((y) => y.despeses.push({ id: uid(), pagadorId: soci.id, concepte: '', import: 0 }))}>
              <Icona nom="mes" mida={18} /> Afegir una despesa
            </button>
          </div>
        </section>
      </fieldset>

      <section className="bloc pila">
        <div className="fila entre">
          <h2>Repartiment</h2>
          <span className="suau">
            Total {euros(r.total)}
            {r.perUnitat > 0 && `, ${euros(r.perUnitat)} per ${a.mode === 'iguals' ? 'persona' : 'part'}`}
          </span>
        </div>
        <div className="repart">
          <div className="repart-fila suau" style={{ fontWeight: 700, fontSize: '.9rem' }}>
            <span>Soci</span>
            <span className="num unit">{a.mode === 'iguals' ? 'Persones' : 'Parts'}</span>
            <span className="num">Li toca</span>
            <span className="num">Resultat</span>
          </div>
          {r.quotes.map((q) => (
            <div className="repart-fila" key={q.sociId}>
              <span>
                {nom(q.sociId)}
                {q.pagat > 0 && <small className="suau"> (ha pagat {euros(q.pagat)})</small>}
              </span>
              <span className="num unit">{q.unitats.toLocaleString('ca-ES')}</span>
              <span className="num xifra">{euros(q.quota)}</span>
              <span className={'num xifra ' + (q.saldo < 0 ? 'saldo-negatiu' : 'saldo-positiu')}>
                {q.saldo < 0 ? `se li torna ${euros(-q.saldo)}` : `paga ${euros(q.saldo)}`}
              </span>
            </div>
          ))}
        </div>
      </section>

      <div className="fila">
        {potEditar && (
          <button className="btn" onClick={() => desa(false)}>
            Desar
          </button>
        )}
        {potTancar && (
          <button className="btn verd" onClick={() => desa(true)} disabled={r.total <= 0 || a.assistents.length === 0}>
            Passar als comptes del mes
          </button>
        )}
        {esJunta && a.estat === 'tancat' && (
          <button className="btn fantasma" onClick={async () => (await espera(() => accions.guardarApat({ ...a, estat: 'obert' }), 'Àpat reobert')) !== undefined && onFet()}>
            Reobrir
          </button>
        )}
        {existeix && a.estat === 'obert' && (a.organitzadorId === soci.id || esJunta) && (
          <button className="btn fantasma" onClick={async () => (await espera(() => accions.eliminarApat(a.id), 'Àpat eliminat')) !== undefined && onFet()}>
            Eliminar
          </button>
        )}
      </div>
    </div>
  );
}
