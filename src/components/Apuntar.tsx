import { useEffect, useMemo, useState } from 'react';
import { useDB, type ItemApunt } from '../data/store';
import { CATEGORIES, type Categoria, type Origen, type Soci } from '../data/types';
import { arrodonir, euros, hora } from '../data/util';
import { COLOR_CATEGORIA, Dialeg, Icona, useEsperaAccio } from './comuns';

interface Props {
  soci: Soci;
  origen: Origen;
  onApuntat?: (total: number) => void;
}

export function Apuntar({ soci, origen, onApuntat }: Props) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [cat, setCat] = useState<Categoria | 'Tots'>('Tots');
  const [cistella, setCistella] = useState<ItemApunt[]>([]);
  const [perA, setPerA] = useState<string | null>(null); // null = el soci
  const [convidats, setConvidats] = useState<string[]>([]);
  const [demanaConvidat, setDemanaConvidat] = useState(false);
  const [nomConvidat, setNomConvidat] = useState('');
  const [enviant, setEnviant] = useState(false);
  const [ara, setAra] = useState(() => Date.now());
  useEffect(() => {
    const t = setInterval(() => setAra(Date.now()), 15000);
    return () => clearInterval(t);
  }, []);

  const productes = db.productes.filter((p) => p.actiu && (cat === 'Tots' || p.categoria === cat));
  const preuDe = (producteId: string, convidat: string | null) => {
    const p = db.productes.find((x) => x.id === producteId)!;
    return convidat && p.preuConvidat != null ? p.preuConvidat : p.preu;
  };
  const total = arrodonir(cistella.reduce((s, i) => s + i.quantitat * preuDe(i.producteId, i.convidat), 0));

  const canvia = (producteId: string, convidat: string | null, delta: number) =>
    setCistella((c) => {
      const i = c.findIndex((x) => x.producteId === producteId && x.convidat === convidat);
      if (i < 0) return delta > 0 ? [...c, { producteId, convidat, quantitat: delta }] : c;
      const n = [...c];
      n[i] = { ...n[i], quantitat: n[i].quantitat + delta };
      return n.filter((x) => x.quantitat > 0);
    });

  const quantitatA = (producteId: string) =>
    cistella.filter((x) => x.producteId === producteId && x.convidat === perA).reduce((s, x) => s + x.quantitat, 0);

  const avui = useMemo(() => {
    const inici = new Date();
    inici.setHours(0, 0, 0, 0);
    return db.consums
      .filter((c) => c.sociId === soci.id && c.ts >= inici.getTime())
      .sort((a, b) => b.ts - a.ts);
  }, [db.consums, soci.id]);

  const confirma = async () => {
    setEnviant(true);
    const r = await espera(() => accions.apuntar(soci.id, cistella, origen), `Apuntat: ${euros(total)}`);
    setEnviant(false);
    if (r) {
      setCistella([]);
      setPerA(null);
      setAra(Date.now());
      onApuntat?.(total);
    }
  };

  const afegeixConvidat = () => {
    const n = nomConvidat.trim();
    if (!n) return;
    if (!convidats.includes(n)) setConvidats([...convidats, n]);
    setPerA(n);
    setNomConvidat('');
    setDemanaConvidat(false);
  };

  const minuts = db.config.minutsDesfer;

  return (
    <div className="apuntar">
      <section aria-label="Productes">
        <div className="categories" role="toolbar" aria-label="Categories">
          <button aria-pressed={cat === 'Tots'} onClick={() => setCat('Tots')}>
            Tot
          </button>
          {CATEGORIES.map((c) => (
            <button key={c} aria-pressed={cat === c} onClick={() => setCat(c)}>
              <i style={{ background: COLOR_CATEGORIA[c] }} />
              {c}
            </button>
          ))}
        </div>
        <div className="graella-productes">
          {productes.map((p) => {
            const q = quantitatA(p.id);
            const preu = perA && p.preuConvidat != null ? p.preuConvidat : p.preu;
            const sense = p.controlaEstoc && p.estoc <= 0;
            return (
              <button
                key={p.id}
                className={'producte' + (sense ? ' esgotat' : '')}
                style={{ ['--color-cat' as string]: COLOR_CATEGORIA[p.categoria] }}
                onClick={() => canvia(p.id, perA, 1)}
                aria-label={`${p.nom}, ${euros(preu)}${q ? `, ${q} a la llista` : ''}`}
              >
                {q > 0 && <span className="quantitat">{q}</span>}
                <span className="nom">{p.nom}</span>
                <span>
                  <span className="preu">{euros(preu)}</span>
                  {sense ? (
                    <span className="poc alerta">Sense estoc</span>
                  ) : p.controlaEstoc && p.estoc <= p.estocMinim ? (
                    <span className="poc">En queden {p.estoc}</span>
                  ) : null}
                </span>
              </button>
            );
          })}
        </div>
      </section>

      <aside className="compte bloc" aria-label="El que apuntes">
        <div className="pila" style={{ gap: '.5rem' }}>
          <h3>Per a qui és?</h3>
          <div className="selector-convidat">
            <button aria-pressed={perA === null} onClick={() => setPerA(null)}>
              Per a mi
            </button>
            {convidats.map((c) => (
              <button key={c} aria-pressed={perA === c} onClick={() => setPerA(c)}>
                {c}
              </button>
            ))}
            <button onClick={() => setDemanaConvidat(true)}>+ Convidat</button>
          </div>
          {perA && <p className="suau">Va al teu compte, a preu de convidat.</p>}
        </div>

        {cistella.length === 0 ? (
          <p className="suau">Toca un producte per afegir-lo.</p>
        ) : (
          <div>
            {cistella.map((i) => {
              const p = db.productes.find((x) => x.id === i.producteId)!;
              return (
                <div className="compte-linia" key={i.producteId + (i.convidat ?? '')}>
                  <button className="mini" onClick={() => canvia(i.producteId, i.convidat, -1)} aria-label={`Treu un ${p.nom}`}>
                    <Icona nom="menys" mida={16} />
                  </button>
                  <span>
                    {i.quantitat} × {p.nom}
                    {i.convidat && <small className="suau"> ({i.convidat})</small>}
                  </span>
                  <button className="mini" onClick={() => canvia(i.producteId, i.convidat, 1)} aria-label={`Afegeix un ${p.nom}`}>
                    <Icona nom="mes" mida={16} />
                  </button>
                  <span className="xifra">{euros(i.quantitat * preuDe(i.producteId, i.convidat))}</span>
                </div>
              );
            })}
          </div>
        )}
        <div className="total-compte">
          <span>Total</span>
          <span className="xifra">{euros(total)}</span>
        </div>
        <button className="btn gran" disabled={!cistella.length || enviant} onClick={confirma}>
          Apuntar al meu compte
        </button>

        {avui.length > 0 && (
          <div className="pila" style={{ gap: '.4rem' }}>
            <h3>Avui</h3>
            <ul className="historial-avui">
              {avui.map((c) => {
                const potDesfer = !c.anulat && ara - c.ts < minuts * 60000;
                return (
                  <li key={c.id} style={c.anulat ? { textDecoration: 'line-through', opacity: 0.6 } : undefined}>
                    <span>
                      <small className="suau">{hora(c.ts)}</small> {c.quantitat > 1 && `${c.quantitat} × `}
                      {c.nomProducte}
                      {c.convidat && <small className="suau"> ({c.convidat})</small>}
                    </span>
                    {potDesfer ? (
                      <button
                        className="btn fantasma petit"
                        onClick={() => espera(() => accions.anular(c.id, soci.id, false), 'Consumició desfeta')}
                      >
                        <Icona nom="desfer" mida={16} /> Desfer
                      </button>
                    ) : (
                      <span className="xifra">{euros(c.quantitat * c.preuUnitari)}</span>
                    )}
                  </li>
                );
              })}
            </ul>
            <small className="suau">Pots desfer una consumició durant {minuts} minuts. Després, parla amb la junta.</small>
          </div>
        )}
      </aside>

      {demanaConvidat && (
        <Dialeg titol="Afegir un convidat" onTanca={() => setDemanaConvidat(false)}>
          <label className="camp">
            <span>Nom del convidat</span>
            <input
              type="text"
              autoFocus
              value={nomConvidat}
              onChange={(e) => setNomConvidat(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && afegeixConvidat()}
              placeholder="Per exemple, Marc"
            />
          </label>
          <p className="suau">El que prengui el convidat s'apuntarà al teu compte.</p>
          <div className="accions">
            <button className="btn fantasma" onClick={() => setDemanaConvidat(false)}>
              Cancel·lar
            </button>
            <button className="btn" onClick={afegeixConvidat} disabled={!nomConvidat.trim()}>
              Afegir
            </button>
          </div>
        </Dialeg>
      )}
    </div>
  );
}
