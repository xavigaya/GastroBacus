# Guia d'instal·lació de GastroBacus

Aquesta guia explica com deixar la tauleta de la societat a punt. Es fa una sola vegada i són tres parts:

1. Preparar el compte de Google (Google Cloud), perquè l'app pugui enviar correus i fer còpies al Drive.
2. Instal·lar l'app a la tauleta.
3. Configurar la tauleta: fixació de pantalla, arrencada automàtica i bateria.

Necessites el Gmail de la societat (el que farà servir la tauleta) i uns 30 minuts.

---

## 1. Google Cloud

L'app no té cap servidor propi. Per enviar els informes i desar les còpies fa servir directament el Gmail i el Drive del compte de la tauleta. Google demana que l'app estigui registrada en un projecte de Google Cloud. És gratuït.

### 1.1 Crear el projecte

1. Entra a <https://console.cloud.google.com> amb el **Gmail de la societat**.
2. Accepta les condicions si és la primera vegada.
3. A dalt, al selector de projectes, tria **Projecte nou**.
   - Nom: `GastroBacus`
   - Prem **Crea** i, quan acabi, assegura't que és el projecte seleccionat a dalt.

### 1.2 Activar les APIs de Gmail i de Drive

1. Menú ☰ > **APIs i serveis** > **Biblioteca**.
2. Cerca **Gmail API**, entra-hi i prem **Activa**.
3. Torna a la biblioteca, cerca **Google Drive API** i prem **Activa**.

### 1.3 Pantalla de consentiment

És la pantalla que veuràs quan la tauleta iniciï la sessió amb Google.

1. Menú ☰ > **APIs i serveis** > **Pantalla de consentiment d'OAuth** (pot aparèixer com a *Google Auth Platform*).
2. Prem **Comença** (o **Configura**):
   - Nom de l'aplicació: `GastroBacus`
   - Correu d'assistència: el Gmail de la societat
   - Públic: **Extern**
   - Informació de contacte: el Gmail de la societat
3. A l'apartat **Accés a les dades** (o *Àmbits*), afegeix aquests tres àmbits:
   - `https://www.googleapis.com/auth/gmail.send`
   - `https://www.googleapis.com/auth/drive.file`
   - `email` (o `.../auth/userinfo.email`)
4. A **Públic**, prem **Publica l'aplicació** perquè passi a l'estat **En producció**.

   > Si es deixa en mode *Prova*, Google fa caducar el permís cada 7 dies i caldria tornar a iniciar la sessió cada setmana.
   > En producció, com que l'app no està verificada per Google, en iniciar la sessió sortirà l'avís **«Google no ha verificat aquesta aplicació»**. És normal: prem **Configuració avançada** > **Ves a GastroBacus**. Només ho veureu el dia que inicieu la sessió.

### 1.4 Registrar l'app Android

1. Menú ☰ > **APIs i serveis** > **Credencials** (o *Clients*).
2. **Crea credencials** > **ID de client d'OAuth**.
3. Tipus d'aplicació: **Android**.
   - Nom: `GastroBacus tauleta`
   - Nom del paquet: `cat.gastrobacus.consum`
   - Empremta del certificat SHA-1:

     ```
     74:48:D0:B9:33:CB:14:96:73:21:F7:4A:74:97:BA:D1:DA:8B:EB:83
     ```

4. Prem **Crea**. No cal descarregar res ni copiar cap clau a l'app.

> L'empremta SHA-1 correspon a la clau amb què està signat l'APK (`gastrobacus.jks`). Si algun dia es canvia la clau de signatura, cal posar aquí la nova empremta.

---

## 2. Instal·lar l'app a la tauleta

Requisits: tauleta **Android 10 o més nova**, amb wifi.

1. A la tauleta, inicia la sessió de Google amb el **Gmail de la societat** (Configuració > Comptes), si encara no ho està.
2. Passa el fitxer `GastroBacus.apk` a la tauleta. Per exemple, envia'l per correu al Gmail de la societat i obre'l des de la tauleta, o copia'l amb un cable USB.
3. Obre el fitxer. Android avisarà que no és de la Play Store: permet la instal·lació des d'aquesta font (Gmail, Fitxers…) i prem **Instal·la**.
4. Obre **GastroBacus**. Surten tres opcions:
   - **Recuperar una còpia del Drive**: si ja s'havia fet servir en una altra tauleta.
   - **Començar de zero**: crea el primer membre de la junta amb el seu PIN.
   - **Provar amb dades fictícies**: per provar-la (PIN de tots els socis: `1234`).
5. Entra amb un membre de la junta > **Gestió** > **Configuració** > **Iniciar la sessió amb Google** i tria el Gmail de la societat. Accepta els permisos (enviar correus i fitxers del Drive creats per l'app).

A partir d'aquí, cada dia a les 4 de la matinada es farà una còpia a la carpeta **«GastroBacus - còpies de seguretat»** del Drive.

### Actualitzar l'app

Quan hi hagi una versió nova, n'hi ha prou d'instal·lar el nou `GastroBacus.apk` per sobre: les dades es conserven. Ha d'estar signat amb la mateixa clau (`gastrobacus.jks`), o Android no deixarà instal·lar-lo.

---

## 3. Configurar la tauleta

### 3.1 Fixació de pantalla

1. Configuració d'Android > **Seguretat** (o *Seguretat i privadesa* > *Més opcions de seguretat*) > **Fixació d'aplicacions** > activa-la.
   - Si demana *Sol·licita el PIN per deixar de fixar*, és millor deixar-ho **desactivat**: la junta ja desfixa des de l'app.
2. Obre GastroBacus. La primera vegada Android preguntarà si vols fixar l'app: prem **D'acord**.

Per sortir de l'app (manteniment, actualitzacions): un membre de la junta entra a **Gestió** > **Configuració** > **Desfixar per fer manteniment**.

> La fixació de pantalla impedeix sortir de l'app per accident. Algú que conegui el truc d'Android (mantenir premuts *Enrere* i *Aplicacions recents*) podria desfixar-la. Si algun dia cal un bloqueig total, es pot passar a mode quiosc complet; requereix restablir la tauleta de fàbrica.

### 3.2 Que s'obri sola en engegar

A **Gestió** > **Configuració** > **Tauleta**:

1. **Donar el permís** obre l'ajust *Mostra sobre altres aplicacions*: activa'l per a GastroBacus. Amb això, quan la tauleta es reinicia, l'app s'obre sola.
2. Opcional però recomanat: **Triar la pantalla d'inici** i escollir **GastroBacus**. Així, el botó d'inici d'Android sempre torna a l'app.

Després de tots dos ajustos, torna a l'app i prem **Tornar a fixar**.

### 3.3 Bateria i còpia de les 4

Perquè la còpia de la matinada es faci:

- Deixa la tauleta **endollada** i amb el **wifi** activat.
- Configuració d'Android > **Aplicacions** > **GastroBacus** > **Bateria** > **Sense restriccions** (o *No optimitzar*).

Si a les 4 no hi ha connexió, la còpia es fa quan torni. A **Gestió** > **Còpies** es veu la data de la darrera còpia i si n'ha fallat alguna.

---

## Si alguna cosa falla

| Què passa | Què fer |
|---|---|
| En iniciar la sessió amb Google surt un error 10 o *DEVELOPER_ERROR* | Revisa el nom del paquet i l'empremta SHA-1 de l'apartat 1.4. |
| Surt *Access blocked* o *accés bloquejat* | La pantalla de consentiment no està publicada o li falta algun àmbit (apartat 1.3). |
| *Cal tornar a iniciar la sessió amb Google* | Gestió > Configuració > Tornar a iniciar la sessió. |
| Els correus queden pendents | Gestió > Correus > Enviar els pendents. Mira el missatge d'error de cada correu. |
| S'ha espatllat la tauleta | A la tauleta nova: instal·la l'APK > Recuperar una còpia del Drive. |
| S'ha perdut el PIN de la junta | Si hi ha un altre membre de la junta, que el canviï des de Gestió > Socis. Si no, cal recuperar una còpia anterior o començar de zero. |

## Fitxers que cal guardar

- `GastroBacus.apk`: l'app.
- `gastrobacus.jks` i `keystore.properties`: la clau de signatura i les seves contrasenyes. **Guardeu-les en un lloc segur i no les pugeu mai al repositori.** Sense aquesta clau no es poden instal·lar actualitzacions a sobre de l'app actual.
