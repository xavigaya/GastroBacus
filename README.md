# GastroBacus

App Android per portar el consum de la societat gastronòmica. En lloc de deixar monedes, el soci apunta el que pren a la tauleta de la societat. A final de mes, cada soci rep l'informe per correu i se li carrega l'import al compte.

L'app és **autoallotjada**: viu només a la tauleta, amb les dades en una base de dades local, sense cap servidor. L'únic servei extern és el compte de Google de la tauleta, que s'utilitza per enviar els correus (Gmail) i per fer una còpia de seguretat diària (Drive).

## Què fa

**Soci (a la tauleta)**
- Tria el seu nom i escriu el PIN de 4 xifres.
- Apunta el que pren. Els productes surten agrupats per tipus i hi ha un filtre de **Favorits**: els que marca la junta i els que més pren cada soci.
- Pot afegir **convidats**: van al seu compte (amb preu de convidat) i surten a la pantalla d'inici fins a les 7 del matí, perquè es puguin apuntar sols sense PIN.
- Pot **pagar al moment per Bizum** en lloc d'anar a la factura mensual. La junta confirma que el Bizum ha arribat; si no, passa a la factura.
- Pot avisar que **falta un producte** o proposar-ne un de nou.
- A **El meu compte** veu el que deu en total i, quan fa la transferència, prem **Pagar** i n'anota la data i el concepte. Queda pendent fins que tresoreria la comprova. Si té un deute de fa més de 45 dies (configurable), el seu nom surt en **vermell** a la pantalla d'inici.
- Pot desfer una consumició durant 10 minuts.
- Consulta el seu consum del mes, amb el detall.
- Fa i anul·la reserves de la cuina o de les taules.
- Crea i edita activitats (àpats): **privades**, **de socis** o **obertes** (també a persones de fora), amb l'espai (**tot el local** o compartit) i el **preu de la cadira** (diferent per a socis i no socis, segons el tipus).
- Canvia el seu PIN.
- Si ningú toca la tauleta durant 3 minuts, la sessió es tanca sola.

**Junta**
- Resum del mes, productes més demanats i avisos.
- Productes i preus, amb **importació des de CSV** de productes i inventari inicial.
- **Estoc**: comptador de consum des de la darrera comanda; en arribar una comanda es pot sumar el que entra o fer un recompte, i el comptador torna a zero. **Avisos** a la tauleta i per correu quan un producte arriba a l'estoc mínim o se n'ha consumit una quantitat determinada.
- **Comandes**: peticions dels socis i comanda al proveïdor per correu, amb la proposta del que cal reposar.
- **Cobraments**: transferències i Bizums per comprovar, i llista de socis amb deute i des de quan.
- **Socis**: altes i baixes amb data i historial, liquidació en donar de baixa, tornar a donar d'alta i **importació des de CSV**.
- Cada reserva (o activitat) envia un avís al **responsable de reserves**.
- **Activitats**: neteja (sense, normal o ampliada) amb preu, carregada a qui organitza; el **sopar de socis** es pot cobrar en **efectiu** (els cuiners entren el tiquet i cada assistent els paga la seva part i la cadira en mà); la **gent de fora** té un soci responsable que en paga la part, la cadira i les begudes.
- **Resums automàtics per correu**, amb destinataris configurables: **estoc** cada setmana, **tresoreria** (consumicions, pagaments i saldos) cada 15 o 30 dies i cada **activitat** l'endemà.
- Correcció de consumicions i apuntar-ne a un soci.
- Tancament de mes en tres passos:
  1. Revisar (Excel i PDF per correu).
  2. Enviar l'informe a cada soci (amb el PDF adjunt) i el resum a tresoreria. S'obre un termini de reclamacions.
  3. Tancar el mes. Si s'activa el rebut bancari, també es genera el fitxer per al banc (de moment un CSV).
- Correus pendents i enviats, còpies de seguretat (fer-ne, veure-les i recuperar-ne), configuració.

**Àpats compartits**: a parts iguals (els convidats compten com una persona més) o segons el que ha pres cadascú (parts). Les despeses que algú ha avançat se li descompten.

**Tauleta**: fixació de pantalla d'Android, s'obre sola en engegar i es pot fer servir com a pantalla d'inici.

## Instal·lació

Vegeu [docs/guia-installacio.md](docs/guia-installacio.md): configuració de Google Cloud, instal·lació de l'APK i ajustos de la tauleta.

## Desenvolupament

Kotlin, Jetpack Compose, Room, WorkManager i l'API d'autorització de Google Play Services. Android 10 (API 29) o superior.

```bash
./gradlew :app:testDebugUnitTest   # proves de lògica i captures de pantalla (a captures/)
./gradlew :app:assembleDebug       # APK de proves
./gradlew :app:assembleRelease     # APK signat (necessita keystore.properties i gastrobacus.jks)
```

`keystore.properties` i `gastrobacus.jks` no es pugen al repositori.

```
app/src/main/java/cat/gastrobacus/consum/
  dades/      model, base de dades, repositori, informes i àpats, dades de prova
  google/     Gmail i Drive
  exportar/   PDF, Excel i fitxer per al banc
  ui/         pantalles del soci
  ui/gestio/  pantalles de la junta
  Serveis.kt  correus, còpies i la còpia diària de les 4
  Quiosc.kt   fixació de pantalla i arrencada automàtica
```

## Dades de prova

En instal·lar-la, l'app ofereix carregar dades fictícies: 24 socis, 30 productes i dos mesos de consumicions. El PIN de tots els socis és `1234`. Jordi, Montse i Pere són de la junta. En mode de proves, tots els correus van a `xavigaya+gastrobacus@gmail.com`.

## Pendent

- Decidir si es manté el rebut bancari o només la transferència (de moment, desactivat).
- Valorar targetes o clauers NFC per identificar-se.
- Carregar els socis i productes reals (plantilles a `docs/plantilles/`).
