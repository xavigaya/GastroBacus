import { useMemo, useState } from 'react';
import { useDB } from '../data/store';
import type { Soci } from '../data/types';
import { estatMes, informeMes } from '../data/informes';
import { arrodonir, euros, mesActual, mesDe, nomComplet, nomMes } from '../data/util';
import { GestioProductes, GestioEstoc } from './Productes';
import { GestioSocis } from './Socis';
import { GestioConsum } from './Consum';
import { Tancament } from './Tancament';
import { Correus, Configuracio } from './Configuracio';

const SECCIONS = ['Resum', 'Consumicions', 'Productes', 'Estoc', 'Socis', 'Tancament de mes', 'Correus', 'Configuració'] as const;
type Seccio = (typeof SECCIONS)[number];

export function Gestio({ soci }: { soci: Soci }) {
  const { db } = useDB();
  const [s, setS] = useState<Seccio>('Resum');
  const perReposar = db.productes.filter((p) => p.actiu && p.controlaEstoc && p.estoc <= p.estocMinim).length;
  const pendents = db.correus.filter((c) => c.estat !== 'enviat').length;

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>Gestió</h1>
      <nav className="pestanyes" aria-label="Seccions de gestió">
        {SECCIONS.map((x) => (
          <button key={x} aria-current={s === x ? 'page' : undefined} onClick={() => setS(x)}>
            {x}
            {x === 'Estoc' && perReposar > 0 && <span className="bombolla">{perReposar}</span>}
            {x === 'Correus' && pendents > 0 && <span className="bombolla">{pendents}</span>}
          </button>
        ))}
      </nav>
      {s === 'Resum' && <Resum onVes={setS} />}
      {s === 'Consumicions' && <GestioConsum soci={soci} />}
      {s === 'Productes' && <GestioProductes />}
      {s === 'Estoc' && <GestioEstoc soci={soci} />}
      {s === 'Socis' && <GestioSocis />}
      {s === 'Tancament de mes' && <Tancament />}
      {s === 'Correus' && <Correus />}
      {s === 'Configuració' && <Configuracio />}
    </div>
  );
}

function Resum({ onVes }: { onVes: (s: Seccio) => void }) {
  const { db } = useDB();
  const mes = mesActual();
  const informes = useMemo(() => informeMes(db, mes), [db, mes]);
  const total = arrodonir(informes.reduce((s, i) => s + i.total, 0));
  const consumsMes = db.consums.filter((c) => !c.anulat && mesDe(c.ts) === mes);
  const unitats = consumsMes.reduce((s, c) => s + c.quantitat, 0);
  const perReposar = db.productes.filter((p) => p.actiu && p.controlaEstoc && p.estoc <= p.estocMinim);
  const top = [...informes].sort((a, b) => b.total - a.total).slice(0, 5);
  const apatsOberts = db.apats.filter((a) => a.estat === 'obert');

  const perProducte = new Map<string, number>();
  consumsMes.forEach((c) => perProducte.set(c.nomProducte, (perProducte.get(c.nomProducte) ?? 0) + c.quantitat));
  const topProd = [...perProducte.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5);
  const maxP = Math.max(1, ...topProd.map((x) => x[1]));

  const pendentsTancar = [...new Set(db.consums.map((c) => mesDe(c.ts)))].filter((m) => m < mes && estatMes(db, m) !== 'tancat').sort();

  return (
    <div className="pila gran">
      <div className="resum">
        <div className="bloc">
          <h3>Total {nomMes(mes).llarg}</h3>
          <div className="xifra">{euros(total)}</div>
          <small className="suau">fins avui</small>
        </div>
        <div className="bloc">
          <h3>Consumicions</h3>
          <div className="xifra">{unitats}</div>
          <small className="suau">{new Set(consumsMes.map((c) => c.sociId)).size} socis diferents</small>
        </div>
        <div className="bloc">
          <h3>Productes per reposar</h3>
          <div className="xifra" style={{ color: perReposar.length ? 'var(--ambre)' : undefined }}>{perReposar.length}</div>
          <button className="enllac" onClick={() => onVes('Estoc')}>Veure l'estoc</button>
        </div>
        <div className="bloc">
          <h3>Àpats oberts</h3>
          <div className="xifra">{apatsOberts.length}</div>
          <small className="suau">pendents de passar als comptes</small>
        </div>
      </div>

      {pendentsTancar.length > 0 && (
        <div className="avis">
          {pendentsTancar.map((m) => nomMes(m).titol).join(', ')} encara no {pendentsTancar.length > 1 ? 'estan tancats' : 'està tancat'}.{' '}
          <button className="enllac" onClick={() => onVes('Tancament de mes')}>
            Anar al tancament
          </button>
        </div>
      )}

      <div className="graella-form" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', alignItems: 'start' }}>
        <section className="bloc pila">
          <h2>Qui porta més aquest mes</h2>
          <ol style={{ margin: 0, paddingLeft: '1.3rem' }}>
            {top.map((i) => (
              <li key={i.soci.id} style={{ padding: '.25rem 0' }}>
                <span className="fila entre">
                  <span>{nomComplet(i.soci)}</span>
                  <span className="xifra">{euros(i.total)}</span>
                </span>
              </li>
            ))}
          </ol>
        </section>
        <section className="bloc pila">
          <h2>El més demanat</h2>
          <div className="barres">
            {topProd.map(([p, n]) => (
              <div className="barra" key={p} style={{ gridTemplateColumns: '1fr 1fr auto' }}>
                <span>{p}</span>
                <span className="pista">
                  <span style={{ width: `${(n / maxP) * 100}%`, background: 'var(--most)' }} />
                </span>
                <span className="xifra">{n}</span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
