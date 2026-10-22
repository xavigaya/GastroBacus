/**
 * GastroBacus: enviament de correus des del Gmail de la societat.
 *
 * Com instal·lar-ho (vegeu docs/correu-gmail.md):
 *  1. Entra a https://script.google.com amb el Gmail de la societat i crea un projecte nou.
 *  2. Enganxa aquest codi.
 *  3. A Configuració del projecte > Propietats de l'script, afegeix TOKEN amb una clau secreta llarga.
 *  4. Implementa > Nova implementació > Aplicació web. Executa com: Jo. Qui hi té accés: Qualsevol usuari.
 *  5. Copia l'adreça que acaba en /exec i posa-la, amb el TOKEN, a Gestió > Configuració de l'app.
 */

function doPost(e) {
  try {
    var dades = JSON.parse(e.postData.contents);
    var token = PropertiesService.getScriptProperties().getProperty('TOKEN');
    if (!token || dades.token !== token) return resposta({ ok: false, error: 'Clau incorrecta' });
    if (!dades.to || !dades.subject || !dades.body) return resposta({ ok: false, error: 'Falten dades' });

    GmailApp.sendEmail(dades.to, dades.subject, dades.body, { name: 'GastroBacus' });
    return resposta({ ok: true });
  } catch (err) {
    return resposta({ ok: false, error: String(err) });
  }
}

function resposta(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

/** Per provar-ho des de l'editor d'Apps Script: envia un correu de prova a la mateixa compta. */
function prova() {
  GmailApp.sendEmail(Session.getActiveUser().getEmail(), 'GastroBacus: prova', 'Si reps aquest correu, l\'enviament funciona.');
}
