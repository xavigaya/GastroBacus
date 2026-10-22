import type { DB, Producte, Soci, Consum, Apat, Reserva, Categoria } from './types';
import { hash, uid, mesActual, mesAnterior } from './util';

// Dades fictícies per a proves. PIN de tots els socis: 1234. Contrasenya: gastro
export const PIN_PROVES = '1234';
export const CONTRASENYA_PROVES = 'gastro';

const SOCIS: [string, string, 'soci' | 'junta'][] = [
  ['Jordi', 'Puig Farré', 'junta'],
  ['Montse', 'Solé Vidal', 'junta'],
  ['Pere', 'Capdevila Roca', 'junta'],
  ['Anna', 'Bosch Serra', 'soci'],
  ['Ramon', 'Gené Cases', 'soci'],
  ['Núria', 'Florensa Pla', 'soci'],
  ['Josep', 'Marsol Bonet', 'soci'],
  ['Carme', 'Riu Sabaté', 'soci'],
  ['Xavier', 'Torrent Mas', 'soci'],
  ['Teresa', 'Llobet Ibars', 'soci'],
  ['Joan', 'Pujol Arqué', 'soci'],
  ['Mercè', 'Badia Gort', 'soci'],
  ['Antoni', 'Segura Coll', 'soci'],
  ['Rosa', 'Vilanova Pons', 'soci'],
  ['Francesc', 'Escolà Mir', 'soci'],
  ['Laia', 'Roig Tarragó', 'soci'],
  ['Miquel', 'Balcells Font', 'soci'],
  ['Pilar', 'Companys Ros', 'soci'],
  ['Enric', 'Sanuy Prats', 'soci'],
  ['Glòria', 'Fabregat Nadal', 'soci'],
  ['Lluís', 'Oliva Camps', 'soci'],
  ['Marta', 'Jové Agustí', 'soci'],
  ['Ferran', 'Gilabert Sala', 'soci'],
  ['Dolors', 'Sendra Mor', 'soci'],
];

const PRODUCTES: [string, Categoria, number, number | null, number, number][] = [
  ['Cervesa Moritz 33 cl', 'Cervesa', 1.3, 1.6, 96, 24],
  ['Estrella Damm 33 cl', 'Cervesa', 1.3, 1.6, 72, 24],
  ['Cervesa sense alcohol', 'Cervesa', 1.2, 1.5, 30, 12],
  ['Quinto Damm 20 cl', 'Cervesa', 0.9, 1.1, 48, 24],
  ['Cervesa artesana Lleida', 'Cervesa', 2.5, 3.0, 18, 6],
  ['Vi negre Costers del Segre', 'Vi', 6.5, 8.0, 14, 6],
  ['Vi blanc Costers del Segre', 'Vi', 6.0, 7.5, 9, 6],
  ['Vi rosat', 'Vi', 5.5, 7.0, 6, 4],
  ['Copa de vi de la casa', 'Vi', 1.0, 1.5, 0, 0],
  ['Cava brut', 'Vi', 7.0, 9.0, 8, 4],
  ['Vermut de la casa', 'Vi', 1.5, 2.0, 0, 0],
  ['Coca-Cola', 'Refrescos', 1.2, 1.5, 40, 12],
  ['Coca-Cola Zero', 'Refrescos', 1.2, 1.5, 36, 12],
  ['Fanta de taronja', 'Refrescos', 1.2, 1.5, 20, 12],
  ['Fanta de llimona', 'Refrescos', 1.2, 1.5, 10, 12],
  ['Tònica', 'Refrescos', 1.2, 1.5, 24, 12],
  ['Bitter Kas', 'Refrescos', 1.4, 1.7, 12, 6],
  ['Suc de préssec', 'Refrescos', 1.1, 1.4, 15, 6],
  ['Aigua 50 cl', 'Aigua', 0.5, 0.8, 60, 24],
  ['Aigua 1,5 l', 'Aigua', 0.8, 1.0, 24, 12],
  ['Aigua amb gas', 'Aigua', 0.9, 1.2, 18, 6],
  ['Cafè', 'Cafè i licors', 0.6, 1.0, 0, 0],
  ['Tallat', 'Cafè i licors', 0.7, 1.1, 0, 0],
  ['Ratafia', 'Cafè i licors', 1.5, 2.0, 0, 0],
  ['Gin-tònic', 'Cafè i licors', 4.0, 5.0, 0, 0],
  ['Patates fregides', 'Picar', 1.0, 1.3, 30, 10],
  ['Olives', 'Picar', 1.2, 1.5, 14, 6],
  ['Fruits secs', 'Picar', 1.5, 1.8, 12, 6],
  ['Llonganissa (ració)', 'Picar', 3.0, 3.5, 8, 4],
  ['Formatge (ració)', 'Picar', 3.5, 4.0, 4, 4],
];

// Generador pseudoaleatori determinista perquè les dades de prova siguin sempre iguals
function rng(seed: number) {
  return () => {
    seed = (seed * 1664525 + 1013904223) % 4294967296;
    return seed / 4294967296;
  };
}

export async function crearDadesProva(): Promise<DB> {
  const pinHash = await hash(PIN_PROVES);
  const contrasenyaHash = await hash(CONTRASENYA_PROVES);
  const r = rng(42);

  const socis: Soci[] = SOCIS.map(([nom, cognoms, rol], i) => ({
    id: `s${i + 1}`,
    nom,
    cognoms,
    email: `${nom.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')}.${cognoms
      .split(' ')[0]
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')}@exemple.cat`,
    rol,
    pinHash,
    contrasenyaHash,
    refBanc: `GB-${String(i + 1).padStart(4, '0')}`,
    actiu: true,
  }));

  const productes: Producte[] = PRODUCTES.map(([nom, categoria, preu, preuConvidat, estoc, estocMinim], i) => ({
    id: `p${i + 1}`,
    nom,
    categoria,
    preu,
    preuConvidat,
    estoc,
    estocMinim,
    controlaEstoc: estocMinim > 0,
    actiu: true,
  }));

  // Consumicions del mes passat i del mes actual fins avui
  const consums: Consum[] = [];
  const ara = new Date();
  const iniciMesPassat = new Date(ara.getFullYear(), ara.getMonth() - 1, 1).getTime();
  const dies = Math.floor((ara.getTime() - iniciMesPassat) / 86400000);
  const populars = [0, 0, 1, 1, 3, 8, 10, 11, 18, 21, 22, 25, 26];
  for (let d = 0; d < dies; d++) {
    const diaSetmana = new Date(iniciMesPassat + d * 86400000).getDay();
    const visites = diaSetmana === 5 || diaSetmana === 6 ? 12 : diaSetmana === 0 ? 7 : 4;
    for (let v = 0; v < visites; v++) {
      const soci = socis[Math.floor(r() * socis.length)];
      const n = 1 + Math.floor(r() * 3);
      for (let k = 0; k < n; k++) {
        const p = productes[r() < 0.75 ? populars[Math.floor(r() * populars.length)] : Math.floor(r() * productes.length)];
        const convidat = r() < 0.08 ? ['Marc', 'Sílvia', 'el cunyat', 'Albert', 'Elena'][Math.floor(r() * 5)] : null;
        const ts = iniciMesPassat + d * 86400000 + (12 + Math.floor(r() * 10)) * 3600000 + Math.floor(r() * 59) * 60000;
        if (ts > Date.now() - 3600000) continue;
        consums.push({
          id: uid(),
          sociId: soci.id,
          producteId: p.id,
          nomProducte: p.nom,
          quantitat: 1,
          preuUnitari: convidat && p.preuConvidat != null ? p.preuConvidat : p.preu,
          convidat,
          ts,
          origen: r() < 0.7 ? 'tauleta' : 'mobil',
          anulat: null,
        });
      }
    }
  }

  const mes = mesActual();
  const mesPassat = mesAnterior(mes);
  const apats: Apat[] = [
    {
      id: uid(),
      titol: 'Calçotada de la colla',
      data: `${mesPassat}-20`,
      organitzadorId: 's2',
      mode: 'iguals',
      assistents: ['s1', 's2', 's4', 's5', 's7', 's9', 's12', 's15'].map((sociId, i) => ({
        sociId,
        convidats: i === 3 ? 1 : 0,
        parts: 1,
      })),
      despeses: [
        { id: uid(), pagadorId: 's2', concepte: 'Calçots i salsa', import: 64.5 },
        { id: uid(), pagadorId: 's5', concepte: 'Carn a la brasa', import: 92.3 },
        { id: uid(), pagadorId: 's1', concepte: 'Pa i postres', import: 21.2 },
      ],
      estat: 'tancat',
    },
    {
      id: uid(),
      titol: 'Sopar de caragols',
      data: `${mes}-${String(Math.max(1, Math.min(ara.getDate() - 1, 17))).padStart(2, '0')}`,
      organitzadorId: 's3',
      mode: 'proporcional',
      assistents: [
        { sociId: 's3', convidats: 0, parts: 1 },
        { sociId: 's6', convidats: 0, parts: 1 },
        { sociId: 's8', convidats: 2, parts: 3 },
        { sociId: 's10', convidats: 0, parts: 0.5 },
        { sociId: 's13', convidats: 0, parts: 1 },
      ],
      despeses: [
        { id: uid(), pagadorId: 's3', concepte: 'Caragols (3 kg)', import: 54 },
        { id: uid(), pagadorId: 's13', concepte: 'Allioli, pa i amanida', import: 18.4 },
      ],
      estat: 'obert',
    },
  ];

  const dataDe = (offset: number) => {
    const d = new Date(ara.getFullYear(), ara.getMonth(), ara.getDate() + offset);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  const espais = ['Cuina i menjador gran', 'Taula del racó'];
  const reserves: Reserva[] = [
    { id: uid(), sociId: 's4', data: dataDe(2), franja: 'Sopar', espai: espais[0], persones: 12, notes: 'Aniversari', creada: Date.now() - 86400000 * 3 },
    { id: uid(), sociId: 's11', data: dataDe(3), franja: 'Dinar', espai: espais[1], persones: 6, notes: '', creada: Date.now() - 86400000 * 2 },
    { id: uid(), sociId: 's7', data: dataDe(9), franja: 'Tot el dia', espai: espais[0], persones: 20, notes: 'Arrossada popular', creada: Date.now() - 86400000 },
  ];

  return {
    versio: 1,
    config: {
      nomSocietat: 'GastroBacus',
      correuSocietat: 'gastrobacus.societat@gmail.com',
      modeDesenvolupament: true,
      correuProves: 'xavigaya+gastrobacus@gmail.com',
      minutsDesfer: 10,
      diesReclamacio: 5,
      espais,
      urlEnviamentCorreu: '',
      tokenEnviamentCorreu: '',
    },
    socis,
    productes,
    consums,
    reposicions: [],
    apats,
    reserves,
    tancaments: [{ mes: mesAnterior(mesPassat), estat: 'tancat', informatTs: Date.now() - 86400000 * 50, tancatTs: Date.now() - 86400000 * 44 }],
    correus: [],
  };
}
