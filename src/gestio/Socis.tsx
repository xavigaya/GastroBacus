import { useState } from 'react';
import { useDB } from '../data/store';
import type { Soci } from '../data/types';
import { nomComplet, uid } from '../data/util';
import { Dialeg, Icona, useEsperaAccio } from '../components/comuns';

export function GestioSocis() {
  const { db } = useDB();
  const [editant, setEditant] = useState<Soci | null>(null);
  const [cerca, setCerca] = useState('');
  const llista = db.socis
    .filter((s) => (nomComplet(s) + ' ' + s.email).toLowerCase().includes(cerca.toLowerCase()))
    .sort((a, b) => a.nom.localeCompare(b.nom, 'ca'));
  const seguentRef = `GB-${String(db.socis.length + 1).padStart(4, '0')}`;

  return (
    <div className="pila">
      <div className="fila entre">
        <input type="search" placeholder="Cerca un soci" value={cerca} onChange={(e) => setCerca(e.target.value)} style={{ maxWidth: 320 }} aria-label="Cerca un soci" />
        <button
          className="btn"
          onClick={() => setEditant({ id: uid(), nom: '', cognoms: '', email: '', rol: 'soci', pinHash: '', contrasenyaHash: '', refBanc: seguentRef, actiu: true })}
        >
          <Icona nom="mes" mida={20} /> Nou soci
        </button>
      </div>
      <p className="suau">{db.socis.filter((s) => s.actiu).length} socis actius.</p>
      <div className="taula-cont">
        <table>
          <thead>
            <tr>
              <th>Nom</th>
              <th>Correu</th>
              <th>Ref. banc</th>
              <th>Rol</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {llista.map((s) => (
              <tr key={s.id} style={!s.actiu ? { opacity: 0.5 } : undefined}>
                <td>
                  <strong>{nomComplet(s)}</strong> {!s.actiu && <span className="etiqueta">De baixa</span>}
                </td>
                <td>{s.email}</td>
                <td>{s.refBanc}</td>
                <td>{s.rol === 'junta' ? <span className="etiqueta most">Junta</span> : 'Soci'}</td>
                <td className="num">
                  <button className="btn fantasma petit" onClick={() => setEditant(s)}>
                    Editar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editant && <EditorSoci inicial={editant} onTanca={() => setEditant(null)} />}
    </div>
  );
}

function EditorSoci({ inicial, onTanca }: { inicial: Soci; onTanca: () => void }) {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [s, setS] = useState(inicial);
  const [pin, setPin] = useState('');
  const [contrasenya, setContrasenya] = useState('');
  const existeix = db.socis.some((x) => x.id === s.id);
  const pinValid = pin === '' ? existeix : /^\d{4}$/.test(pin);
  const contraValida = contrasenya === '' ? existeix : contrasenya.length >= 6;
  const correuValid = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(s.email);

  const desa = async () => {
    const r = await espera(
      () => accions.guardarSoci({ ...s, nom: s.nom.trim(), cognoms: s.cognoms.trim(), email: s.email.trim() }, pin || undefined, contrasenya || undefined),
      'Soci desat',
    );
    if (r) onTanca();
  };

  return (
    <Dialeg titol={existeix ? 'Editar soci' : 'Nou soci'} onTanca={onTanca}>
      <div className="graella-form">
        <label className="camp">
          <span>Nom</span>
          <input type="text" value={s.nom} onChange={(e) => setS({ ...s, nom: e.target.value })} autoFocus />
        </label>
        <label className="camp">
          <span>Cognoms</span>
          <input type="text" value={s.cognoms} onChange={(e) => setS({ ...s, cognoms: e.target.value })} />
        </label>
      </div>
      <div className="graella-form">
        <label className="camp">
          <span>Correu</span>
          <input type="email" value={s.email} onChange={(e) => setS({ ...s, email: e.target.value })} />
          <small>Hi rebrà l'informe mensual i l'usarà per entrar des del mòbil.</small>
        </label>
        <label className="camp">
          <span>Referència al banc</span>
          <input type="text" value={s.refBanc} onChange={(e) => setS({ ...s, refBanc: e.target.value })} />
          <small>La que fa servir el banc per al rebut del soci.</small>
        </label>
      </div>
      <div className="graella-form">
        <label className="camp">
          <span>PIN per a la tauleta</span>
          <input type="password" inputMode="numeric" maxLength={4} value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} placeholder={existeix ? 'Deixa-ho buit per no canviar-lo' : '4 xifres'} />
        </label>
        <label className="camp">
          <span>Contrasenya per al mòbil</span>
          <input type="password" value={contrasenya} onChange={(e) => setContrasenya(e.target.value)} placeholder={existeix ? 'Deixa-ho buit per no canviar-la' : 'Mínim 6 caràcters'} />
        </label>
      </div>
      <label className="check">
        <input type="checkbox" checked={s.rol === 'junta'} onChange={(e) => setS({ ...s, rol: e.target.checked ? 'junta' : 'soci' })} />
        És de la junta (pot gestionar l'app)
      </label>
      <label className="check">
        <input type="checkbox" checked={s.actiu} onChange={(e) => setS({ ...s, actiu: e.target.checked })} />
        Soci actiu
      </label>
      <div className="accions">
        <button className="btn fantasma" onClick={onTanca}>
          Cancel·lar
        </button>
        <button className="btn" onClick={desa} disabled={!s.nom.trim() || !correuValid || !pinValid || !contraValida}>
          Desar
        </button>
      </div>
    </Dialeg>
  );
}
