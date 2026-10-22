# GastroBacus

App per portar el consum de la societat gastronòmica. En lloc de deixar monedes, el soci apunta el que pren. A final de mes, cada soci rep un informe i se li carrega l'import al compte.

És una webapp instal·lable (PWA): funciona a la tauleta de la societat i al mòbil de cada soci, tant Android com iPhone, sense passar per cap botiga d'aplicacions.

## Què fa

- **Tauleta de la societat**: el soci tria el seu nom, posa el PIN de 4 xifres i toca el que pren. Es pot apuntar per a convidats, que van al compte del soci (amb preu de convidat si el producte en té). Durant 10 minuts es pot desfer. Si ningú toca la tauleta, la sessió es tanca sola.
- **Mòbil**: cada soci entra amb el seu correu i contrasenya, apunta el que pren i consulta el consum del mes.
- **Reserves** de la cuina o de les taules, amb calendari i control de solapaments.
- **Àpats compartits**: qui ve, quants convidats porta i què ha costat la compra. Es pot repartir a parts iguals o segons el que ha pres cadascú (parts). Qui ha avançat diners se'ls descompta.
- **Gestió (junta)**: productes i preus, socis, estoc amb avís de reposició, correcció de consumicions, resum del mes.
- **Tancament de mes**, en tres passos:
  1. Revisar (Excel i PDF per a la junta).
  2. Enviar l'informe a cada soci i el resum al tresorer. S'obre un termini de reclamacions.
  3. Tancar el mes i generar el fitxer per al banc. De moment és un CSV; el format SEPA definitiu està pendent de saber què demana el banc.

## Estat actual

Primera versió amb **dades fictícies**, desades al navegador de cada aparell. Encara no es comparteixen entre la tauleta i els mòbils: això arribarà a la fase 2 amb Supabase (pla gratuït), amb l'esquema ja preparat a `supabase/schema.sql`.

Dades de prova:

- PIN de tots els socis: `1234`
- Mòbil: `jordi.puig@exemple.cat` (junta) o `anna.bosch@exemple.cat` (soci), contrasenya `gastro`
- Els correus, en mode de proves, van tots a `xavigaya+gastrobacus@gmail.com`

## Desenvolupament

```bash
npm install
npm run dev        # servidor de desenvolupament
npm run build      # genera dist/ per publicar
```

L'enviament de correus des del Gmail de la societat s'explica a `docs/correu-gmail.md`.

## Estructura

```
src/
  data/        model, dades de prova, magatzem, informes i exportacions
  components/  pantalles dels socis (apuntar, consum, reserves, àpats)
  gestio/      pantalles de la junta
backend/       script de Google per enviar correus des del Gmail
supabase/      esquema de base de dades per a la fase 2
docs/          documentació
```

## Pendent

- Fase 2: dades compartides al núvol (Supabase) i comptes reals.
- Format de la remesa bancària (SEPA) quan se sàpiga quin banc i quin format.
- Valorar targetes o clauers NFC/QR per identificar-se a la tauleta.
- Carregar els socis i productes reals.
