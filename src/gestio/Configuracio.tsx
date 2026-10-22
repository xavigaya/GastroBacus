import { useState } from 'react';
import { useDB } from '../data/store';
import type { Config, Correu } from '../data/types';
import { hora } from '../data/util';
import { Dialeg, useEsperaAccio } from '../components/comuns';

export function Correus() {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [obert, setObert] = useState<Correu | null>(null);
  const cfg = db.config;

  return (
    <div className="pila">
      {!cfg.urlEnviamentCorreu ? (
        <div className="avis">
          Encara no s'ha connectat el Gmail de la societat, així que els correus es queden aquí. Pots obrir-los un a un al teu programa de correu, o
          configurar l'enviament automàtic a Configuració.
        </div>
      ) : (
        <div className="avis ok">Els correus s'envien des del Gmail de la societat.</div>
      )}
      {db.correus.length === 0 ? (
        <p className="suau">Encara no s'ha enviat cap correu. Els informes mensuals s'envien des de Tancament de mes.</p>
      ) : (
        <div className="taula-cont">
          <table>
            <thead>
              <tr>
                <th>Quan</th>
                <th>Per a</th>
                <th>Assumpte</th>
                <th>Estat</th>
              </tr>
            </thead>
            <tbody>
              {db.correus.map((c) => (
                <tr key={c.id} onClick={() => setObert(c)} style={{ cursor: 'pointer' }}>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    {new Date(c.ts).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })} {hora(c.ts)}
                  </td>
                  <td>
                    {c.destinatariOriginal}
                    {c.per !== c.destinatariOriginal && <small className="suau"><br />va a {c.per}</small>}
                  </td>
                  <td>
                    <button className="enllac" style={{ textAlign: 'left' }}>{c.assumpte}</button>
                  </td>
                  <td>
                    <span className={'etiqueta ' + (c.estat === 'enviat' ? 'verd' : c.estat === 'error' ? 'vermell' : 'ambre')}>
                      {c.estat === 'enviat' ? 'Enviat' : c.estat === 'error' ? 'Error' : 'Pendent'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {obert && (
        <Dialeg titol={obert.assumpte} onTanca={() => setObert(null)}>
          <p className="suau">
            Per a {obert.per}
            {obert.per !== obert.destinatariOriginal && ` (en lloc de ${obert.destinatariOriginal}, mode de proves)`}
          </p>
          <pre className="correu">{obert.cos}</pre>
          {obert.error && <div className="avis error">{obert.error}</div>}
          <div className="accions">
            <a
              className="btn fantasma"
              href={`mailto:${encodeURIComponent(obert.per)}?subject=${encodeURIComponent(obert.assumpte)}&body=${encodeURIComponent(obert.cos)}`}
            >
              Obrir al correu
            </a>
            {cfg.urlEnviamentCorreu && obert.estat !== 'enviat' && (
              <button className="btn fantasma" onClick={() => espera(() => accions.reintentarCorreu(obert.id), 'Correu enviat').then(() => setObert(null))}>
                Tornar a enviar
              </button>
            )}
            {obert.estat !== 'enviat' && (
              <button className="btn" onClick={() => espera(() => accions.marcarCorreuEnviat(obert.id), 'Marcat com a enviat').then(() => setObert(null))}>
                Marcar com a enviat
              </button>
            )}
          </div>
        </Dialeg>
      )}
    </div>
  );
}

export function Configuracio() {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const [c, setC] = useState<Config>(db.config);
  const [espaisText, setEspaisText] = useState(db.config.espais.join('\n'));
  const [confirmaReset, setConfirmaReset] = useState(false);

  const desa = () =>
    espera(
      () => accions.guardarConfig({ ...c, espais: espaisText.split('\n').map((x) => x.trim()).filter(Boolean) }),
      'Configuració desada',
    );

  return (
    <div className="pila gran" style={{ maxWidth: 760 }}>
      <section className="bloc pila">
        <h2>Societat</h2>
        <div className="graella-form">
          <label className="camp">
            <span>Nom</span>
            <input type="text" value={c.nomSocietat} onChange={(e) => setC({ ...c, nomSocietat: e.target.value })} />
          </label>
          <label className="camp">
            <span>Gmail de la societat</span>
            <input type="email" value={c.correuSocietat} onChange={(e) => setC({ ...c, correuSocietat: e.target.value })} />
            <small>Des d'aquí s'envien els informes, i el tresorer hi rep el resum.</small>
          </label>
        </div>
        <label className="camp">
          <span>Espais que es poden reservar</span>
          <textarea value={espaisText} onChange={(e) => setEspaisText(e.target.value)} />
          <small>Un per línia.</small>
        </label>
      </section>

      <section className="bloc pila">
        <h2>Normes</h2>
        <div className="graella-form">
          <label className="camp">
            <span>Minuts per desfer una consumició</span>
            <input type="number" min={0} max={120} value={c.minutsDesfer} onChange={(e) => setC({ ...c, minutsDesfer: Number(e.target.value) })} />
          </label>
          <label className="camp">
            <span>Dies per reclamar l'informe</span>
            <input type="number" min={0} max={30} value={c.diesReclamacio} onChange={(e) => setC({ ...c, diesReclamacio: Number(e.target.value) })} />
          </label>
        </div>
      </section>

      <section className="bloc pila">
        <h2>Enviament de correus</h2>
        <label className="check">
          <input type="checkbox" checked={c.modeDesenvolupament} onChange={(e) => setC({ ...c, modeDesenvolupament: e.target.checked })} />
          Mode de proves: enviar tots els correus a una sola adreça
        </label>
        {c.modeDesenvolupament && (
          <label className="camp">
            <span>Adreça de proves</span>
            <input type="email" value={c.correuProves} onChange={(e) => setC({ ...c, correuProves: e.target.value })} />
          </label>
        )}
        <label className="camp">
          <span>Adreça de l'script d'enviament (Google Apps Script)</span>
          <input type="text" value={c.urlEnviamentCorreu} onChange={(e) => setC({ ...c, urlEnviamentCorreu: e.target.value })} placeholder="https://script.google.com/macros/s/…/exec" />
          <small>Si està buit, els correus queden a la bústia de sortida. Les instruccions són a docs/correu-gmail.md del projecte.</small>
        </label>
        <label className="camp">
          <span>Clau secreta de l'script</span>
          <input type="password" value={c.tokenEnviamentCorreu} onChange={(e) => setC({ ...c, tokenEnviamentCorreu: e.target.value })} />
        </label>
      </section>

      <div className="fila">
        <button className="btn" onClick={desa}>
          Desar la configuració
        </button>
      </div>

      <section className="bloc pila" style={{ borderColor: 'var(--error)' }}>
        <h2>Dades de prova</h2>
        <p>Esborra-ho tot i torna a carregar els socis, productes i consumicions fictícies.</p>
        <div>
          <button className="btn perill" onClick={() => setConfirmaReset(true)}>
            Restablir les dades de prova
          </button>
        </div>
      </section>
      {confirmaReset && (
        <Dialeg titol="Restablir les dades de prova" onTanca={() => setConfirmaReset(false)}>
          <p>S'esborraran totes les consumicions, socis, àpats i reserves d'aquest dispositiu i es tornaran a crear les dades fictícies.</p>
          <div className="accions">
            <button className="btn fantasma" onClick={() => setConfirmaReset(false)}>
              Cancel·lar
            </button>
            <button
              className="btn perill"
              onClick={async () => {
                await espera(() => accions.restablirDadesProva(), 'Dades de prova restablertes');
                setConfirmaReset(false);
                location.reload();
              }}
            >
              Esborrar i restablir
            </button>
          </div>
        </Dialeg>
      )}
    </div>
  );
}
