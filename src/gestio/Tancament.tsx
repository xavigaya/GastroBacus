import { useMemo, useState } from 'react';
import { useDB } from '../data/store';
import { apatsOberts, estatMes, informeMes } from '../data/informes';
import { csvRemesa, excelJunta, pdfJunta } from '../data/exportar';
import { arrodonir, euros, mesActual, mesAnterior, mesDe, nomComplet, nomMes } from '../data/util';
import { Dialeg, Icona, SelectorMes, useEsperaAccio } from '../components/comuns';

export function Tancament() {
  const { db, accions } = useDB();
  const espera = useEsperaAccio();
  const mesos = useMemo(() => {
    const r = [mesActual()];
    for (let i = 0; i < 11; i++) r.push(mesAnterior(r[r.length - 1]));
    return r;
  }, []);
  const [mes, setMes] = useState(() => {
    const pendent = mesos.slice(1).reverse().find((m) => estatMes(db, m) !== 'tancat' && db.consums.some((c) => mesDe(c.ts) === m));
    return pendent ?? mesAnterior(mesActual());
  });
  const [confirmaTancar, setConfirmaTancar] = useState(false);
  const [enviant, setEnviant] = useState(false);

  const estat = estatMes(db, mes);
  const t = db.tancaments.find((x) => x.mes === mes);
  const informes = informeMes(db, mes).filter((i) => i.linies.length > 0);
  const total = arrodonir(informes.reduce((s, i) => s + i.total, 0));
  const oberts = apatsOberts(db, mes);
  const esMesActual = mes === mesActual();
  const limit = t ? t.informatTs + db.config.diesReclamacio * 86400000 : 0;
  const terminiPassat = t ? Date.now() >= limit : false;
  const nm = nomMes(mes);
  const aFavor = informes.filter((i) => i.total < 0);

  const envia = async () => {
    setEnviant(true);
    await espera(async () => {
      const n = await accions.enviarInformes(mes);
      return n;
    }, db.config.urlEnviamentCorreu ? 'Informes enviats' : 'Informes preparats a la bústia de sortida');
    setEnviant(false);
  };

  return (
    <div className="pila gran">
      <div className="fila entre">
        <SelectorMes mes={mes} onCanvi={setMes} mesos={mesos} />
        <span className={'etiqueta ' + (estat === 'tancat' ? 'verd' : estat === 'informat' ? 'ambre' : '')}>
          {estat === 'tancat' ? 'Tancat' : estat === 'informat' ? 'Informes enviats, termini de reclamacions' : 'Obert'}
        </span>
      </div>

      <div className="passos">
        <div className={'pas ' + (estat === 'obert' ? 'actual' : 'fet')}>
          <h3>Revisar</h3>
          <p>
            {informes.length} socis amb consum, total <strong>{euros(total)}</strong>.
          </p>
          {esMesActual && estat === 'obert' && <div className="avis">El mes encara no s'ha acabat.</div>}
          {oberts.length > 0 && (
            <div className="avis">
              Hi ha {oberts.length === 1 ? 'un àpat obert' : `${oberts.length} àpats oberts`} ({oberts.map((a) => a.titol).join(', ')}). No sortiran a l'informe fins que no es passin als comptes.
            </div>
          )}
          <div className="fila">
            <button className="btn fantasma petit" onClick={() => excelJunta(db, mes)} disabled={!informes.length}>
              <Icona nom="descarrega" mida={18} /> Excel
            </button>
            <button className="btn fantasma petit" onClick={() => pdfJunta(db, mes)} disabled={!informes.length}>
              <Icona nom="descarrega" mida={18} /> PDF
            </button>
          </div>
        </div>

        <div className={'pas ' + (estat === 'obert' ? '' : estat === 'informat' ? 'actual fet' : 'fet')}>
          <h3>Enviar informes</h3>
          <p>Cada soci rep el seu detall per correu i el tresorer el resum. S'obren {db.config.diesReclamacio} dies per reclamar.</p>
          {db.config.modeDesenvolupament && <small className="suau">Mode de proves: tots els correus van a {db.config.correuProves}.</small>}
          {t && (
            <p className="suau">
              Enviats el {new Date(t.informatTs).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long' })}. Reclamacions fins al{' '}
              {new Date(limit).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long' })}.
            </p>
          )}
          {estat !== 'tancat' && (
            <button className={'btn ' + (estat === 'informat' ? 'fantasma' : '')} onClick={envia} disabled={!informes.length || enviant}>
              {estat === 'informat' ? 'Tornar a enviar' : 'Enviar informes'}
            </button>
          )}
        </div>

        <div className={'pas ' + (estat === 'tancat' ? 'fet' : estat === 'informat' && terminiPassat ? 'actual' : '')}>
          <h3>Tancar i passar al banc</h3>
          <p>Un cop tancat, el mes ja no es pot modificar i es genera el fitxer de rebuts per al banc.</p>
          {estat === 'tancat' ? (
            <>
              <div className="fila">
                <button className="btn petit" onClick={() => csvRemesa(db, mes)}>
                  <Icona nom="descarrega" mida={18} /> Fitxer per al banc
                </button>
                <button className="btn fantasma petit" onClick={() => espera(() => accions.reobrirMes(mes), `${nm.titol} reobert`)}>
                  Reobrir el mes
                </button>
              </div>
              {aFavor.length > 0 && (
                <div className="avis">
                  {aFavor.map((i) => `${nomComplet(i.soci)} (${euros(-i.total)})`).join(', ')} {aFavor.length > 1 ? 'tenen' : 'té'} saldo a favor per despeses avançades. No surt al fitxer del banc: cal tornar-li els diners a part.
                </div>
              )}
              <small className="suau">De moment és un CSV amb la referència i l'import de cada soci. El format definitiu (remesa SEPA) es farà quan sapiguem què demana el banc.</small>
            </>
          ) : (
            <button className="btn verd" disabled={estat !== 'informat'} onClick={() => setConfirmaTancar(true)}>
              Tancar el mes
            </button>
          )}
        </div>
      </div>

      <section className="pila">
        <h2>Per soci</h2>
        <div className="taula-cont">
          <table>
            <thead>
              <tr>
                <th>Soci</th>
                <th>Ref. banc</th>
                <th className="num">Consum</th>
                <th className="num">Àpats</th>
                <th className="num">Abonaments</th>
                <th className="num">Total</th>
              </tr>
            </thead>
            <tbody>
              {informes.map((i) => (
                <tr key={i.soci.id}>
                  <td>{nomComplet(i.soci)}</td>
                  <td>{i.soci.refBanc}</td>
                  <td className="num">{euros(i.totalConsum)}</td>
                  <td className="num">{i.totalApats ? euros(i.totalApats) : ''}</td>
                  <td className="num">{i.totalAbonaments ? euros(i.totalAbonaments) : ''}</td>
                  <td className="num xifra">{euros(i.total)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={5}>Total</td>
                <td className="num">{euros(total)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      {confirmaTancar && (
        <Dialeg titol={`Tancar ${nm.titol.toLowerCase()}`} onTanca={() => setConfirmaTancar(false)}>
          {!terminiPassat && (
            <div className="avis">
              El termini de reclamacions acaba el {new Date(limit).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long' })}. Pots tancar igualment si ja està tot revisat.
            </div>
          )}
          <p>
            Es carregaran <strong>{euros(arrodonir(informes.filter((i) => i.total > 0).reduce((s, i) => s + i.total, 0)))}</strong> a {informes.filter((i) => i.total > 0).length} socis.
            {aFavor.length > 0 && ` A ${aFavor.length === 1 ? '1 soci' : aFavor.length + ' socis'} se'ls ha de tornar ${euros(-arrodonir(aFavor.reduce((s, i) => s + i.total, 0)))} per despeses que van avançar.`} Després no es podran canviar consumicions ni àpats d'aquest mes, tret que
            el reobris.
          </p>
          <div className="accions">
            <button className="btn fantasma" onClick={() => setConfirmaTancar(false)}>
              Cancel·lar
            </button>
            <button
              className="btn verd"
              onClick={async () => {
                await espera(() => accions.tancarMes(mes), `${nm.titol} tancat`);
                setConfirmaTancar(false);
              }}
            >
              Tancar el mes
            </button>
          </div>
        </Dialeg>
      )}
    </div>
  );
}
