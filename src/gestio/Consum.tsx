import { useMemo, useState } from 'react';
import { useDB } from '../data/store';
import type { Soci } from '../data/types';
import { estatMes } from '../data/informes';
import { euros, hora, mesActual, mesAnterior, mesDe, nomComplet } from '../data/util';
import { Dialeg, Icona, SelectorMes, useEsperaAccio } from '../components/comuns';

export function GestioConsum({ soci }: { soci: Soci }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [mes, setMes] = useState(mesActual());
  const [filtreSoci, setFiltreSoci] = useState('');
  const [afegint, setAfegint] = useState(false);
  const [limit, setLimit] = useState(100);
  const mesos = useMemo(() => {
    const r = [mesActual()];
    for (let i = 0; i < 11; i++) r.push(mesAnterior(r[r.length - 1]));
    return r;
  }, []);
  const tancat = estatMes(db, mes) === 'tancat';
  const llista = db.consums
    .filter((c) => mesDe(c.ts) === mes && (!filtreSoci || c.sociId === filtreSoci))
    .sort((a, b) => b.ts - a.ts);
  const socis = [...db.socis].sort((a, b) => a.nom.localeCompare(b.nom, 'ca'));

  return (
    <div className="pila">
      <div className="fila entre">
        <div className="fila">
          <SelectorMes mes={mes} onCanvi={setMes} mesos={mesos} />
          <select value={filtreSoci} onChange={(e) => setFiltreSoci(e.target.value)} style={{ width: 'auto' }} aria-label="Filtra per soci">
            <option value="">Tots els socis</option>
            {socis.map((s) => (
              <option key={s.id} value={s.id}>
                {nomComplet(s)}
              </option>
            ))}
          </select>
        </div>
        <button className="btn" onClick={() => setAfegint(true)} disabled={tancat}>
          <Icona nom="mes" mida={20} /> Apuntar a un soci
        </button>
      </div>
      {tancat && <div className="avis">Aquest mes està tancat. Per corregir-hi alguna cosa, reobre'l des de Tancament de mes.</div>}
      <p className="suau">
        {llista.filter((c) => !c.anulat).length} consumicions, {euros(llista.filter((c) => !c.anulat).reduce((s, c) => s + c.quantitat * c.preuUnitari, 0))}.
      </p>
      <div className="taula-cont">
        <table>
          <thead>
            <tr>
              <th>Quan</th>
              <th>Soci</th>
              <th>Producte</th>
              <th>Des de</th>
              <th className="num">Import</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {llista.slice(0, limit).map((c) => {
              const s = db.socis.find((x) => x.id === c.sociId);
              return (
                <tr key={c.id} className={c.anulat ? 'anulat' : ''}>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    {new Date(c.ts).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })} {hora(c.ts)}
                  </td>
                  <td>{s ? nomComplet(s) : '—'}</td>
                  <td>
                    {c.quantitat > 1 && `${c.quantitat} × `}
                    {c.nomProducte}
                    {c.convidat && <small className="suau"> (convidat: {c.convidat})</small>}
                  </td>
                  <td>{c.origen === 'tauleta' ? 'Tauleta' : c.origen === 'mobil' ? 'Mòbil' : 'Junta'}</td>
                  <td className="num">{euros(c.quantitat * c.preuUnitari)}</td>
                  <td className="num">
                    {c.anulat ? (
                      <small className="suau">Anul·lada</small>
                    ) : (
                      !tancat && (
                        <button className="btn fantasma petit" onClick={() => espera(() => accions.anular(c.id, soci.id, true), 'Consumició anul·lada')}>
                          Anul·lar
                        </button>
                      )
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {llista.length > limit && (
        <button className="btn fantasma" onClick={() => setLimit(limit + 200)}>
          Mostrar-ne més ({llista.length - limit} més)
        </button>
      )}
      {afegint && <ApuntarJunta onTanca={() => setAfegint(false)} />}
    </div>
  );
}

function ApuntarJunta({ onTanca }: { onTanca: () => void }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const actius = db.socis.filter((s) => s.actiu).sort((a, b) => a.nom.localeCompare(b.nom, 'ca'));
  const productes = db.productes.filter((p) => p.actiu);
  const [sociId, setSociId] = useState(actius[0]?.id ?? '');
  const [producteId, setProducteId] = useState(productes[0]?.id ?? '');
  const [quantitat, setQuantitat] = useState(1);
  const [convidat, setConvidat] = useState('');
  const avui = new Date();
  const [data, setData] = useState(`${avui.getFullYear()}-${String(avui.getMonth() + 1).padStart(2, '0')}-${String(avui.getDate()).padStart(2, '0')}`);

  const desa = async () => {
    const ts = new Date(data + 'T' + new Date().toTimeString().slice(0, 5)).getTime();
    const r = await espera(() => accions.apuntar(sociId, [{ producteId, quantitat, convidat: convidat.trim() || null }], 'junta', ts), 'Consumició apuntada');
    if (r) onTanca();
  };

  return (
    <Dialeg titol="Apuntar a un soci" onTanca={onTanca}>
      <p className="suau">Per a consumicions que algú es va oblidar d'apuntar.</p>
      <label className="camp">
        <span>Soci</span>
        <select value={sociId} onChange={(e) => setSociId(e.target.value)}>
          {actius.map((s) => (
            <option key={s.id} value={s.id}>
              {nomComplet(s)}
            </option>
          ))}
        </select>
      </label>
      <div className="graella-form">
        <label className="camp">
          <span>Producte</span>
          <select value={producteId} onChange={(e) => setProducteId(e.target.value)}>
            {productes.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nom} ({euros(p.preu)})
              </option>
            ))}
          </select>
        </label>
        <label className="camp">
          <span>Quantitat</span>
          <input type="number" min={1} value={quantitat} onChange={(e) => setQuantitat(Math.max(1, Number(e.target.value)))} />
        </label>
      </div>
      <div className="graella-form">
        <label className="camp">
          <span>Dia</span>
          <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
        </label>
        <label className="camp">
          <span>Convidat</span>
          <input type="text" value={convidat} onChange={(e) => setConvidat(e.target.value)} placeholder="Buit si és per al soci" />
        </label>
      </div>
      <div className="accions">
        <button className="btn fantasma" onClick={onTanca}>
          Cancel·lar
        </button>
        <button className="btn" onClick={desa} disabled={!sociId || !producteId}>
          Apuntar
        </button>
      </div>
    </Dialeg>
  );
}
