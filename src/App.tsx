import { useCallback, useEffect, useState } from 'react';
import { useDB } from './data/store';
import type { Origen, Soci } from './data/types';
import { CONTRASENYA_PROVES, PIN_PROVES } from './data/seed';
import { euros, inicials, nomComplet } from './data/util';
import { Capcalera, Icona } from './components/comuns';
import { Apuntar } from './components/Apuntar';
import { ElMeuConsum } from './components/ElMeuConsum';
import { Reserves } from './components/Reserves';
import { Apats } from './components/Apats';
import { Gestio } from './gestio/Gestio';

type Dispositiu = 'tauleta' | 'mobil';
const CLAU_DISPOSITIU = 'gastrobacus-dispositiu';
const CLAU_SESSIO = 'gastrobacus-sessio';

const llegeix = (k: string) => {
  try {
    return localStorage.getItem(k);
  } catch {
    return null;
  }
};
const escriu = (k: string, v: string | null) => {
  try {
    if (v === null) localStorage.removeItem(k);
    else localStorage.setItem(k, v);
  } catch {
    /* res */
  }
};

export default function App() {
  const [dispositiu, setDispositiu] = useState<Dispositiu | null>(() => llegeix(CLAU_DISPOSITIU) as Dispositiu | null);
  const tria = (d: Dispositiu | null) => {
    escriu(CLAU_DISPOSITIU, d);
    setDispositiu(d);
  };
  if (dispositiu === 'tauleta') return <Tauleta onCanviaMode={() => tria(null)} />;
  if (dispositiu === 'mobil') return <Mobil onCanviaMode={() => tria(null)} />;
  return <Inici onTria={tria} />;
}

function Inici({ onTria }: { onTria: (d: Dispositiu) => void }) {
  return (
    <main className="inici">
      <div className="inici-cont">
        <div className="logo-gran">
          <img src="logo.svg" alt="" />
          <div>
            <h1>GastroBacus</h1>
            <p className="suau">El compte de la societat, sense monedes.</p>
          </div>
        </div>
        <div className="pila">
          <h2>Com faràs servir aquest aparell?</h2>
          <button className="opcio-dispositiu" onClick={() => onTria('tauleta')}>
            <Icona nom="tauleta" />
            <span>
              <h2>Tauleta de la societat</h2>
              <span className="suau">Es queda a la societat. Cada soci tria el seu nom i posa el PIN.</span>
            </span>
          </button>
          <button className="opcio-dispositiu" onClick={() => onTria('mobil')}>
            <Icona nom="mobil" />
            <span>
              <h2>El meu mòbil</h2>
              <span className="suau">Entres amb el teu correu i consultes o apuntes el que prens.</span>
            </span>
          </button>
        </div>
        <div className="avis">
          <strong>Versió de proves amb dades fictícies.</strong> PIN de tots els socis: {PIN_PROVES}. Al mòbil, entra amb el correu d'un soci (per
          exemple jordi.puig@exemple.cat, que és de la junta) i la contrasenya «{CONTRASENYA_PROVES}».
        </div>
      </div>
    </main>
  );
}

/* ---------- Àrea del soci (compartida entre tauleta i mòbil) ---------- */

type Pestanya = 'apuntar' | 'consum' | 'reserves' | 'apats' | 'gestio';

function AreaSoci({ soci, origen, onSurt, onApuntat }: { soci: Soci; origen: Origen; onSurt: () => void; onApuntat?: (t: number) => void }) {
  const [p, setP] = useState<Pestanya>('apuntar');
  const items: [Pestanya, string, string][] = [
    ['apuntar', 'Apuntar', 'copa'],
    ['consum', 'El meu consum', 'llista'],
    ['reserves', 'Reserves', 'calendari'],
    ['apats', 'Àpats', 'olla'],
  ];
  if (soci.rol === 'junta') items.push(['gestio', 'Gestió', 'clau']);
  useEffect(() => window.scrollTo(0, 0), [p]);

  return (
    <div>
      <Capcalera>
        <span style={{ fontWeight: 700 }}>{soci.nom}</span>
        <button className="btn petit" onClick={onSurt}>
          <Icona nom="sortir" mida={18} /> {origen === 'tauleta' ? 'Acabar' : 'Sortir'}
        </button>
      </Capcalera>
      <main className="pagina">
        {p === 'apuntar' && (
          <div className="pila">
            <h1>Què prens, {soci.nom}?</h1>
            <Apuntar soci={soci} origen={origen} onApuntat={onApuntat} />
          </div>
        )}
        {p === 'consum' && <ElMeuConsum soci={soci} />}
        {p === 'reserves' && <Reserves soci={soci} />}
        {p === 'apats' && <Apats soci={soci} />}
        {p === 'gestio' && soci.rol === 'junta' && <Gestio soci={soci} />}
      </main>
      <nav className="nav-inferior" aria-label="Seccions">
        {items.map(([k, t, ic]) => (
          <button key={k} aria-current={p === k ? 'page' : undefined} onClick={() => setP(k)}>
            <Icona nom={ic} />
            {t}
          </button>
        ))}
      </nav>
    </div>
  );
}

/* ---------- Tauleta ---------- */

const INACTIVITAT_MS = 3 * 60000;

function Tauleta({ onCanviaMode }: { onCanviaMode: () => void }) {
  const { db } = useDB();
  const [triat, setTriat] = useState<Soci | null>(null);
  const [dins, setDins] = useState<Soci | null>(null);
  const [fet, setFet] = useState<number | null>(null);

  const surt = useCallback(() => {
    setDins(null);
    setTriat(null);
    setFet(null);
  }, []);

  // Tanca la sessió de la tauleta si ningú la toca
  useEffect(() => {
    if (!dins) return;
    let t = setTimeout(surt, INACTIVITAT_MS);
    const reinicia = () => {
      clearTimeout(t);
      t = setTimeout(surt, INACTIVITAT_MS);
    };
    window.addEventListener('pointerdown', reinicia);
    window.addEventListener('keydown', reinicia);
    return () => {
      clearTimeout(t);
      window.removeEventListener('pointerdown', reinicia);
      window.removeEventListener('keydown', reinicia);
    };
  }, [dins, surt]);

  // Després d'apuntar, torna a la llista de socis al cap d'uns segons
  useEffect(() => {
    if (fet === null) return;
    const t = setTimeout(surt, 6000);
    return () => clearTimeout(t);
  }, [fet, surt]);

  if (dins) {
    const actual = db.socis.find((s) => s.id === dins.id) ?? dins;
    return (
      <>
        <AreaSoci soci={actual} origen="tauleta" onSurt={surt} onApuntat={(t) => setFet(t)} />
        {fet !== null && (
          <div className="fons-dialeg">
            <div className="dialeg" role="alertdialog" aria-label="Apuntat" style={{ textAlign: 'center', justifyItems: 'center', width: 'min(440px, 100%)' }}>
              <div className="avatar" style={{ width: 72, height: 72, background: 'var(--pampol-suau)', color: 'var(--pampol)', fontSize: '2rem' }}>
                ✓
              </div>
              <h2>Apuntat, {actual.nom}</h2>
              <p className="xifra" style={{ fontSize: '2rem' }}>{euros(fet)}</p>
              <p className="suau">Tornem a la llista de socis en uns segons.</p>
              <div className="accions" style={{ justifyContent: 'center' }}>
                <button className="btn fantasma" onClick={() => setFet(null)}>
                  Apuntar més
                </button>
                <button className="btn" onClick={surt}>
                  Fet
                </button>
              </div>
            </div>
          </div>
        )}
      </>
    );
  }
  if (triat) return <Pin soci={triat} onTorna={() => setTriat(null)} onOk={() => setDins(triat)} />;
  return <LlistaSocis onTria={setTriat} onCanviaMode={onCanviaMode} />;
}

function LlistaSocis({ onTria, onCanviaMode }: { onTria: (s: Soci) => void; onCanviaMode: () => void }) {
  const { db } = useDB();
  const [lletra, setLletra] = useState<string | null>(null);
  const actius = db.socis.filter((s) => s.actiu).sort((a, b) => a.nom.localeCompare(b.nom, 'ca'));
  const lletres = [...new Set(actius.map((s) => s.nom[0].toUpperCase()))];
  const llista = lletra ? actius.filter((s) => s.nom[0].toUpperCase() === lletra) : actius;

  return (
    <div className="tauleta">
      <Capcalera />
      <main className="tauleta-cos">
        <h1>Qui ets?</h1>
        <div className="lletres" role="toolbar" aria-label="Filtra per inicial">
          <button aria-pressed={lletra === null} onClick={() => setLletra(null)} style={{ width: 'auto', padding: '0 .8rem' }}>
            Tots
          </button>
          {lletres.map((l) => (
            <button key={l} aria-pressed={lletra === l} onClick={() => setLletra(l)}>
              {l}
            </button>
          ))}
        </div>
        <div className="graella-socis">
          {llista.map((s) => (
            <button key={s.id} className="fitxa-soci" onClick={() => onTria(s)}>
              <span className="avatar">{inicials(s)}</span>
              <span>
                <strong>{s.nom}</strong>
                <small>{s.cognoms}</small>
              </span>
            </button>
          ))}
        </div>
        <p style={{ marginTop: '2.5rem' }}>
          <button className="enllac suau" onClick={onCanviaMode}>
            Canviar el mode d'aquest aparell
          </button>
        </p>
      </main>
    </div>
  );
}

function Pin({ soci, onTorna, onOk }: { soci: Soci; onTorna: () => void; onOk: () => void }) {
  const { accions } = useDB();
  const [pin, setPin] = useState('');
  const [error, setError] = useState(false);
  const [intents, setIntents] = useState(0);

  const prem = useCallback(
    (d: string) => {
      setError(false);
      setPin((p) => (p.length < 4 ? p + d : p));
    },
    [],
  );

  useEffect(() => {
    if (pin.length !== 4) return;
    accions.verificarPin(soci.id, pin).then((ok) => {
      if (ok) onOk();
      else {
        setError(true);
        setIntents((i) => i + 1);
        setPin('');
      }
    });
  }, [pin, soci.id, accions, onOk]);

  useEffect(() => {
    const f = (e: KeyboardEvent) => {
      if (/^\d$/.test(e.key)) prem(e.key);
      else if (e.key === 'Backspace') setPin((p) => p.slice(0, -1));
      else if (e.key === 'Escape') onTorna();
    };
    window.addEventListener('keydown', f);
    return () => window.removeEventListener('keydown', f);
  }, [prem, onTorna]);

  return (
    <div className="tauleta">
      <Capcalera>
        <button className="btn petit" onClick={onTorna}>
          <Icona nom="enrere" mida={18} /> No sóc jo
        </button>
      </Capcalera>
      <main className="tauleta-cos">
        <div className="pin">
          <span className="avatar" style={{ width: 72, height: 72, fontSize: '1.6rem' }}>
            {inicials(soci)}
          </span>
          <h1>{nomComplet(soci)}</h1>
          <p className="suau">{error ? 'PIN incorrecte. Torna-ho a provar.' : 'Escriu el teu PIN'}</p>
          <div className={'pin-punts' + (error ? ' error' : '')} aria-live="polite" aria-label={`${pin.length} de 4 xifres`}>
            {[0, 1, 2, 3].map((i) => (
              <span key={i} className={i < pin.length ? 'ple' : ''} />
            ))}
          </div>
          <div className="teclat">
            {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((d) => (
              <button key={d} onClick={() => prem(d)}>
                {d}
              </button>
            ))}
            <button className="text" onClick={onTorna}>
              Tornar
            </button>
            <button onClick={() => prem('0')}>0</button>
            <button className="text" onClick={() => setPin((p) => p.slice(0, -1))} aria-label="Esborra">
              Esborra
            </button>
          </div>
          {intents >= 3 && <p className="suau">Si no recordes el PIN, demana a la junta que te'l canviï.</p>}
        </div>
      </main>
    </div>
  );
}

/* ---------- Mòbil ---------- */

function Mobil({ onCanviaMode }: { onCanviaMode: () => void }) {
  const { db, accions } = useDB();
  const [sociId, setSociId] = useState<string | null>(() => llegeix(CLAU_SESSIO));
  const [email, setEmail] = useState('');
  const [contrasenya, setContrasenya] = useState('');
  const [error, setError] = useState('');
  const soci = db.socis.find((s) => s.id === sociId && s.actiu);

  const entra = async (e: React.FormEvent) => {
    e.preventDefault();
    const s = await accions.entrarAmbCorreu(email, contrasenya);
    if (!s) return setError('El correu o la contrasenya no són correctes.');
    escriu(CLAU_SESSIO, s.id);
    setSociId(s.id);
    setError('');
  };
  const surt = () => {
    escriu(CLAU_SESSIO, null);
    setSociId(null);
    setContrasenya('');
  };

  if (soci) return <AreaSoci soci={soci} origen="mobil" onSurt={surt} />;

  return (
    <main className="inici">
      <form className="inici-cont" onSubmit={entra}>
        <div className="logo-gran">
          <img src="logo.svg" alt="" />
          <h1>GastroBacus</h1>
        </div>
        <div className="bloc pila">
          <h2>Entra al teu compte</h2>
          <label className="camp">
            <span>Correu</span>
            <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="camp">
            <span>Contrasenya</span>
            <input type="password" autoComplete="current-password" value={contrasenya} onChange={(e) => setContrasenya(e.target.value)} required />
          </label>
          {error && <div className="avis error">{error}</div>}
          <button className="btn gran" type="submit">
            Entrar
          </button>
          <small className="suau">Si no tens contrasenya o l'has oblidada, demana-la a la junta.</small>
        </div>
        <div className="avis">
          Proves: jordi.puig@exemple.cat (junta) o anna.bosch@exemple.cat (soci), contrasenya «{CONTRASENYA_PROVES}».
        </div>
        <button type="button" className="enllac suau" onClick={onCanviaMode}>
          Canviar el mode d'aquest aparell
        </button>
      </form>
    </main>
  );
}
