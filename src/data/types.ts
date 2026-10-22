export type Rol = 'soci' | 'junta';

export interface Soci {
  id: string;
  nom: string;
  cognoms: string;
  email: string;
  telefon?: string;
  rol: Rol;
  pinHash: string;
  contrasenyaHash: string;
  refBanc: string; // referència del soci al banc (mandat SEPA gestionat pel banc)
  actiu: boolean;
}

export type Categoria = 'Cervesa' | 'Vi' | 'Refrescos' | 'Aigua' | 'Cafè i licors' | 'Picar';
export const CATEGORIES: Categoria[] = ['Cervesa', 'Vi', 'Refrescos', 'Aigua', 'Cafè i licors', 'Picar'];

export interface Producte {
  id: string;
  nom: string;
  categoria: Categoria;
  preu: number;
  preuConvidat: number | null; // null = mateix preu que el soci
  estoc: number;
  estocMinim: number;
  controlaEstoc: boolean;
  actiu: boolean;
}

export type Origen = 'tauleta' | 'mobil' | 'junta';

export interface Consum {
  id: string;
  sociId: string;
  producteId: string;
  nomProducte: string;
  quantitat: number;
  preuUnitari: number;
  convidat: string | null; // nom del convidat, si és per a un convidat
  ts: number;
  origen: Origen;
  anulat: { ts: number; perId: string } | null;
}

export interface Reposicio {
  id: string;
  producteId: string;
  quantitat: number;
  ts: number;
  perId: string;
  nota?: string;
}

export interface Assistent {
  sociId: string;
  convidats: number;
  parts: number; // per al repartiment proporcional
}

export interface DespesaApat {
  id: string;
  pagadorId: string;
  concepte: string;
  import: number;
}

export type ModeRepartiment = 'iguals' | 'proporcional';

export interface Apat {
  id: string;
  titol: string;
  data: string; // YYYY-MM-DD
  organitzadorId: string;
  mode: ModeRepartiment;
  assistents: Assistent[];
  despeses: DespesaApat[];
  estat: 'obert' | 'tancat';
}

export type Franja = 'Dinar' | 'Sopar' | 'Tot el dia';
export const FRANGES: Franja[] = ['Dinar', 'Sopar', 'Tot el dia'];

export interface Reserva {
  id: string;
  sociId: string;
  data: string; // YYYY-MM-DD
  franja: Franja;
  espai: string;
  persones: number;
  notes: string;
  creada: number;
}

export interface Tancament {
  mes: string; // YYYY-MM
  estat: 'informat' | 'tancat';
  informatTs: number;
  tancatTs: number | null;
}

export interface Correu {
  id: string;
  ts: number;
  per: string; // destinatari real (en desenvolupament es redirigeix)
  destinatariOriginal: string;
  assumpte: string;
  cos: string;
  estat: 'pendent' | 'enviat' | 'error';
  error?: string;
}

export interface Config {
  nomSocietat: string;
  correuSocietat: string;
  modeDesenvolupament: boolean;
  correuProves: string;
  minutsDesfer: number;
  diesReclamacio: number;
  espais: string[];
  urlEnviamentCorreu: string; // URL de l'Apps Script que envia des del Gmail de la societat
  tokenEnviamentCorreu: string;
}

export interface DB {
  versio: number;
  config: Config;
  socis: Soci[];
  productes: Producte[];
  consums: Consum[];
  reposicions: Reposicio[];
  apats: Apat[];
  reserves: Reserva[];
  tancaments: Tancament[];
  correus: Correu[];
}
