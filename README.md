# GastroBacus

App Android per portar el consum de la societat gastronòmica. En lloc de deixar monedes, el soci apunta el que pren a la tauleta de la societat. A final de mes, cada soci rep l'informe per correu i se li carrega l'import al compte.

L'app és **autoallotjada**: viu només a la tauleta, amb les dades en una base de dades local, sense cap servidor. L'únic servei extern és el compte de Google de la tauleta, que s'utilitza per enviar els correus (Gmail) i per fer una còpia de seguretat diària (Drive).

## Què fa

**Soci (a la tauleta)**
- Tria el seu nom i escriu el PIN de 4 xifres.
- Apunta el que pren, també per a convidats (al seu compte, amb preu de convidat si el producte en té).
- Pot desfer una consumició durant 10 minuts.
- Consulta el seu consum del mes, amb el detall.
- Fa i anul·la reserves de la cuina o de les taules.
- Crea i edita àpats compartits.
- Canvia el seu PIN.
- Si ningú toca la tauleta durant 3 minuts, la sessió es tanca sola.

**Junta**
- Resum del mes, productes més demanats i avisos.
- Productes i preus, socis, estoc amb avís de reposició.
- Correcció de consumicions i apuntar-ne a un soci.
- Tancament de mes en tres passos:
  1. Revisar (Excel i PDF per correu).
  2. Enviar l'informe a cada soci (amb el PDF adjunt) i el resum a tresoreria. S'obre un termini de reclamacions.
  3. Tancar el mes i generar el fitxer per al banc (de moment un CSV; el format SEPA definitiu està pendent).
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

- Format de la remesa bancària (SEPA) quan se sàpiga el banc.
- Valorar targetes o clauers NFC per identificar-se.
- Carregar els socis i productes reals.
