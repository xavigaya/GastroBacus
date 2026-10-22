import { useState } from 'react';
import { useDB } from '../data/store';
import { CATEGORIES, type Categoria, type Producte, type Soci } from '../data/types';
import { dataCurta, euros, uid } from '../data/util';
import { COLOR_CATEGORIA, Dialeg, Icona, useEsperaAccio } from '../components/comuns';

export function GestioProductes() {
  const { db } = useDB();
  const [editant, setEditant] = useState<Producte | null>(null);
  const [cerca, setCerca] = useState('');
  const llista = db.productes
    .filter((p) => p.nom.toLowerCase().includes(cerca.toLowerCase()))
    .sort((a, b) => CATEGORIES.indexOf(a.categoria) - CATEGORIES.indexOf(b.categoria) || a.nom.localeCompare(b.nom, 'ca'));

  return (
    <div className="pila">
      <div className="fila entre">
        <input type="search" placeholder="Cerca un producte" value={cerca} onChange={(e) => setCerca(e.target.value)} style={{ maxWidth: 320 }} aria-label="Cerca un producte" />
        <button
          className="btn"
          onClick={() => setEditant({ id: uid(), nom: '', categoria: 'Cervesa', preu: 1, preuConvidat: null, estoc: 0, estocMinim: 0, controlaEstoc: true, actiu: true })}
        >
          <Icona nom="mes" mida={20} /> Nou producte
        </button>
      </div>
      <div className="taula-cont">
        <table>
          <thead>
            <tr>
              <th>Producte</th>
              <th>Categoria</th>
              <th className="num">Preu soci</th>
              <th className="num">Preu convidat</th>
              <th className="num">Estoc</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {llista.map((p) => (
              <tr key={p.id} style={!p.actiu ? { opacity: 0.5 } : undefined}>
                <td>
                  <strong>{p.nom}</strong> {!p.actiu && <span className="etiqueta">Amagat</span>}
                </td>
                <td>
                  <span className="fila" style={{ gap: '.4rem' }}>
                    <i style={{ width: 10, height: 10, borderRadius: 9, background: COLOR_CATEGORIA[p.categoria], display: 'inline-block' }} />
                    {p.categoria}
                  </span>
                </td>
                <td className="num">{euros(p.preu)}</td>
                <td className="num">{p.preuConvidat == null ? <span className="suau">igual</span> : euros(p.preuConvidat)}</td>
                <td className="num">{p.controlaEstoc ? p.estoc : <span className="suau">no es compta</span>}</td>
                <td className="num">
                  <button className="btn fantasma petit" onClick={() => setEditant(p)}>
                    Editar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editant && <EditorProducte inicial={editant} onTanca={() => setEditant(null)} />}
    </div>
  );
}

function EditorProducte({ inicial, onTanca }: { inicial: Producte; onTanca: () => void }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [p, setP] = useState(inicial);
  const [preuConv, setPreuConv] = useState(inicial.preuConvidat != null);
  const existeix = db.productes.some((x) => x.id === p.id);
  const desa = async () => {
    const r = await espera(() => accions.guardarProducte({ ...p, nom: p.nom.trim(), preuConvidat: preuConv ? p.preuConvidat ?? p.preu : null }), 'Producte desat');
    if (r) onTanca();
  };
  return (
    <Dialeg titol={existeix ? 'Editar producte' : 'Nou producte'} onTanca={onTanca}>
      <label className="camp">
        <span>Nom</span>
        <input type="text" value={p.nom} onChange={(e) => setP({ ...p, nom: e.target.value })} autoFocus />
      </label>
      <div className="graella-form">
        <label className="camp">
          <span>Categoria</span>
          <select value={p.categoria} onChange={(e) => setP({ ...p, categoria: e.target.value as Categoria })}>
            {CATEGORIES.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
        <label className="camp">
          <span>Preu per al soci (€)</span>
          <input type="number" min={0} step={0.05} value={p.preu} onChange={(e) => setP({ ...p, preu: Number(e.target.value) })} />
        </label>
      </div>
      <label className="check">
        <input type="checkbox" checked={preuConv} onChange={(e) => setPreuConv(e.target.checked)} />
        Els convidats paguen un preu diferent
      </label>
      {preuConv && (
        <label className="camp">
          <span>Preu per a convidats (€)</span>
          <input type="number" min={0} step={0.05} value={p.preuConvidat ?? p.preu} onChange={(e) => setP({ ...p, preuConvidat: Number(e.target.value) })} />
        </label>
      )}
      <label className="check">
        <input type="checkbox" checked={p.controlaEstoc} onChange={(e) => setP({ ...p, controlaEstoc: e.target.checked })} />
        Portar el control d'estoc d'aquest producte
      </label>
      {p.controlaEstoc && (
        <div className="graella-form">
          <label className="camp">
            <span>Unitats a la societat</span>
            <input type="number" min={0} value={p.estoc} onChange={(e) => setP({ ...p, estoc: Number(e.target.value) })} />
          </label>
          <label className="camp">
            <span>Avisa'm quan en quedin</span>
            <input type="number" min={0} value={p.estocMinim} onChange={(e) => setP({ ...p, estocMinim: Number(e.target.value) })} />
          </label>
        </div>
      )}
      <label className="check">
        <input type="checkbox" checked={p.actiu} onChange={(e) => setP({ ...p, actiu: e.target.checked })} />
        Visible per apuntar
      </label>
      <div className="accions">
        <button className="btn fantasma" onClick={onTanca}>
          Cancel·lar
        </button>
        <button className="btn" onClick={desa} disabled={!p.nom.trim() || p.preu < 0}>
          Desar
        </button>
      </div>
    </Dialeg>
  );
}

export function GestioEstoc({ soci }: { soci: Soci }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [nomesBaix, setNomesBaix] = useState(false);
  const [entrades, setEntrades] = useState<Record<string, string>>({});
  const amb = db.productes.filter((p) => p.actiu && p.controlaEstoc);
  const llista = amb
    .filter((p) => !nomesBaix || p.estoc <= p.estocMinim)
    .sort((a, b) => a.estoc - a.estocMinim - (b.estoc - b.estocMinim));
  const reposicions = [...db.reposicions].sort((a, b) => b.ts - a.ts).slice(0, 12);

  const reposa = async (id: string) => {
    const n = Math.round(Number(entrades[id]));
    if (!n) return;
    const p = db.productes.find((x) => x.id === id)!;
    const r = await espera(() => accions.reposar(id, n, soci.id), `${n > 0 ? 'Afegides' : 'Tretes'} ${Math.abs(n)} unitats de ${p.nom}`);
    if (r) setEntrades({ ...entrades, [id]: '' });
  };

  return (
    <div className="pila gran">
      <div className="fila entre">
        <p className="suau" style={{ maxWidth: '60ch' }}>
          Quan arribi una comanda, escriu quantes unitats entren. Cada consumició apuntada en resta una. Per corregir un recompte, posa un número
          negatiu.
        </p>
        <label className="check">
          <input type="checkbox" checked={nomesBaix} onChange={(e) => setNomesBaix(e.target.checked)} />
          Només el que cal reposar
        </label>
      </div>
      <div className="taula-cont">
        <table>
          <thead>
            <tr>
              <th>Producte</th>
              <th className="num">Queden</th>
              <th className="num">Mínim</th>
              <th>Entrada</th>
            </tr>
          </thead>
          <tbody>
            {llista.map((p) => (
              <tr key={p.id}>
                <td>
                  {p.nom}{' '}
                  {p.estoc <= 0 ? (
                    <span className="etiqueta vermell">Esgotat</span>
                  ) : p.estoc <= p.estocMinim ? (
                    <span className="etiqueta ambre">Cal reposar</span>
                  ) : null}
                </td>
                <td className="num xifra">{p.estoc}</td>
                <td className="num">{p.estocMinim}</td>
                <td>
                  <span className="fila" style={{ gap: '.4rem', flexWrap: 'nowrap' }}>
                    <input
                      type="number"
                      style={{ width: 90, minHeight: 40 }}
                      value={entrades[p.id] ?? ''}
                      placeholder="+24"
                      aria-label={`Unitats que entren de ${p.nom}`}
                      onChange={(e) => setEntrades({ ...entrades, [p.id]: e.target.value })}
                      onKeyDown={(e) => e.key === 'Enter' && reposa(p.id)}
                    />
                    <button className="btn petit" onClick={() => reposa(p.id)} disabled={!Number(entrades[p.id])}>
                      Afegir
                    </button>
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {reposicions.length > 0 && (
        <section className="pila">
          <h2>Darreres entrades</h2>
          <ul style={{ margin: 0, paddingLeft: '1.2rem' }}>
            {reposicions.map((r) => {
              const p = db.productes.find((x) => x.id === r.producteId);
              const qui = db.socis.find((x) => x.id === r.perId);
              return (
                <li key={r.id}>
                  {dataCurta(r.ts)}: {r.quantitat > 0 ? '+' : ''}
                  {r.quantitat} {p?.nom} <span className="suau">({qui?.nom})</span>
                </li>
              );
            })}
          </ul>
        </section>
      )}
    </div>
  );
}
