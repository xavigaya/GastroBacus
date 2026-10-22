import { useMemo, useState } from 'react';
import { useDB } from '../data/store';
import type { Soci } from '../data/types';
import { estatMes, informeSoci } from '../data/informes';
import { pdfSoci } from '../data/exportar';
import { arrodonir, euros, mesActual, mesAnterior, nomMes } from '../data/util';
import { COLOR_CATEGORIA, Icona, SelectorMes } from './comuns';

export function ElMeuConsum({ soci }: { soci: Soci }) {
  const { db } = useDB();
  const [mes, setMes] = useState(mesActual());
  const mesos = useMemo(() => {
    const r = [mesActual()];
    for (let i = 0; i < 11; i++) r.push(mesAnterior(r[r.length - 1]));
    return r;
  }, []);
  const inf = informeSoci(db, soci, mes);
  const estat = estatMes(db, mes);

  const perCategoria = useMemo(() => {
    const m = new Map<string, number>();
    for (const c of db.consums) {
      if (c.sociId !== soci.id || c.anulat) continue;
      const d = new Date(c.ts);
      if (`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}` !== mes) continue;
      const cat = db.productes.find((p) => p.id === c.producteId)?.categoria ?? 'Picar';
      m.set(cat, arrodonir((m.get(cat) ?? 0) + c.quantitat * c.preuUnitari));
    }
    return [...m.entries()].sort((a, b) => b[1] - a[1]);
  }, [db, soci.id, mes]);
  const maxCat = Math.max(1, ...perCategoria.map((x) => x[1]));

  return (
    <div className="pila gran">
      <div className="fila entre">
        <h1>El meu consum</h1>
        <SelectorMes mes={mes} onCanvi={setMes} mesos={mesos} />
      </div>

      <div className="bloc pila">
        <p className="suau">
          {estat === 'obert' ? `Portes gastat ${nomMes(mes).llarg}` : estat === 'informat' ? 'Pendent de passar al banc' : 'Carregat al teu compte'}
        </p>
        <div className="xifra-gran">{euros(inf.total)}</div>
        <div className="fila" style={{ gap: '.4rem' }}>
          <span className="etiqueta">Consumicions {euros(inf.totalConsum)}</span>
          {inf.totalApats !== 0 && <span className="etiqueta">Àpats {euros(inf.totalApats)}</span>}
          {inf.totalAbonaments !== 0 && <span className="etiqueta verd">Despeses avançades {euros(inf.totalAbonaments)}</span>}
        </div>
        {perCategoria.length > 0 && (
          <div className="barres" aria-label="Consum per categoria">
            {perCategoria.map(([cat, v]) => (
              <div className="barra" key={cat}>
                <span>{cat}</span>
                <span className="pista">
                  <span style={{ width: `${(v / maxCat) * 100}%`, background: COLOR_CATEGORIA[cat as keyof typeof COLOR_CATEGORIA] }} />
                </span>
                <span className="xifra">{euros(v)}</span>
              </div>
            ))}
          </div>
        )}
        {inf.linies.length > 0 && (
          <div>
            <button className="btn secundari petit" onClick={() => pdfSoci(inf, mes)}>
              <Icona nom="descarrega" mida={18} /> Descarregar en PDF
            </button>
          </div>
        )}
      </div>

      <section className="pila">
        <h2>Detall</h2>
        {inf.linies.length === 0 ? (
          <p className="suau">Encara no hi ha res apuntat aquest mes.</p>
        ) : (
          <div className="taula-cont">
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Concepte</th>
                  <th className="num">Import</th>
                </tr>
              </thead>
              <tbody>
                {[...inf.linies].reverse().map((l, i) => (
                  <tr key={i}>
                    <td>{new Date(l.ts).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })}</td>
                    <td>
                      {l.quantitat > 1 && `${l.quantitat} × `}
                      {l.concepte}
                      {l.detall && <small className="suau"> · {l.detall}</small>}
                    </td>
                    <td className="num">{euros(l.import)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
