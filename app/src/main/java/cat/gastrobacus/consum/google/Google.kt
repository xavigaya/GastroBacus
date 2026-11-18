package cat.gastrobacus.consum.google

import android.content.Context
import android.util.Base64
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class ErrorGoogle(missatge: String, val calIniciarSessio: Boolean = false, val cancelat: Boolean = false) : Exception(missatge)

/**
 * Accés al compte de Google de la tauleta: Gmail (enviar) i Drive (còpies).
 * Fa servir l'API d'autorització de Google Play Services; el token es renova sol.
 */
object Google {
    private val ABASTS = listOf(
        Scope("https://www.googleapis.com/auth/gmail.send"),
        Scope("https://www.googleapis.com/auth/drive.file"),
        Scope("email"),
    )

    /** L'activitat registra aquí com obrir la pantalla de Google quan cal que algú hi doni permís. */
    var llancador: ((IntentSenderRequest) -> Unit)? = null
    private var pendent: CompletableDeferred<AuthorizationResult>? = null

    fun resultatAutoritzacio(context: Context, codiResultat: Int, data: android.content.Intent?) {
        val p = pendent ?: return
        pendent = null
        try {
            p.complete(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data))
        } catch (e: Exception) {
            val codi = (e as? com.google.android.gms.common.api.ApiException)?.statusCode
            val cancelat = codiResultat == android.app.Activity.RESULT_CANCELED && (codi == null || codi == 16 || codi == 12501)
            p.completeExceptionally(ErrorGoogle(explicaError(e, codiResultat), true, cancelat))
        }
    }

    /** Tradueix l'error de Google a un missatge que ajudi a saber què falla a la configuració. */
    internal fun explicaError(e: Exception, codiResultat: Int = android.app.Activity.RESULT_OK): String {
        val codi = (e as? com.google.android.gms.common.api.ApiException)?.statusCode
        val detall = e.message?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
        return when (codi) {
            10 -> "Google no reconeix l'app (error 10). A Google Cloud cal un client OAuth de tipus Android amb el paquet cat.gastrobacus.consum i l'empremta SHA-1 de la guia."
            16, 12501 -> if (codiResultat == android.app.Activity.RESULT_CANCELED) "S'ha tancat la pantalla de Google sense donar permís (error $codi). Si Google ha dit «Accés bloquejat», cal afegir aquest compte com a usuari de prova a la pantalla de consentiment o publicar-la." else "S'ha cancel·lat l'inici de sessió (error $codi)."
            7 -> "No hi ha connexió amb Google (error 7)."
            8 -> "Error intern de Google Play Services (error 8)$detall."
            null -> if (codiResultat == android.app.Activity.RESULT_CANCELED) "S'ha tancat la pantalla de Google sense donar permís. Si Google ha dit «Accés bloquejat», cal afegir aquest compte com a usuari de prova a la pantalla de consentiment o publicar-la." else "No s'ha pogut iniciar la sessió amb Google$detall."
            else -> "No s'ha pogut iniciar la sessió amb Google (error $codi)$detall."
        }
    }

    // ---------- Mètode alternatiu: compte de la tauleta + GoogleAuthUtil ----------
    // Si l'API d'autorització falla (p. ex. error 8 en algunes tauletes), es tria el compte
    // amb el selector d'Android i es demana el token directament. Queda recordat per a la còpia de les 4.

    private const val TIPUS_COMPTE = "com.google"
    private val ABAST_ALTERNATIU = "oauth2:" + ABASTS.joinToString(" ") { it.scopeUri }
    private fun prefs(context: Context) = context.getSharedPreferences("google", Context.MODE_PRIVATE)
    private fun compteAlternatiu(context: Context) = prefs(context).getString("compte", null)
    fun oblida(context: Context) = prefs(context).edit().remove("compte").apply()

    /** L'activitat registra aquí com obrir una pantalla d'Android i esperar-ne el resultat. */
    var llancadorIntent: ((android.content.Intent) -> Unit)? = null
    private var pendentIntent: CompletableDeferred<Pair<Int, android.content.Intent?>>? = null

    fun resultatIntent(codi: Int, data: android.content.Intent?) {
        pendentIntent?.complete(codi to data); pendentIntent = null
    }

    private suspend fun obre(intent: android.content.Intent): Pair<Int, android.content.Intent?> {
        val obrir = llancadorIntent ?: throw ErrorGoogle("No es pot obrir la pantalla de Google ara mateix.", true)
        val d = CompletableDeferred<Pair<Int, android.content.Intent?>>()
        pendentIntent = d
        cat.gastrobacus.consum.Quiosc.desfixa()
        withContext(Dispatchers.Main) { obrir(intent) }
        return d.await()
    }

    private suspend fun triaCompte(): String {
        @Suppress("DEPRECATION")
        val intent = android.accounts.AccountManager.newChooseAccountIntent(null, null, arrayOf(TIPUS_COMPTE), false, null, null, null, null)
        val (codi, data) = obre(intent)
        if (codi != android.app.Activity.RESULT_OK) throw ErrorGoogle("No s'ha triat cap compte de Google.", true)
        return data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME) ?: throw ErrorGoogle("No s'ha triat cap compte de Google.", true)
    }

    private suspend fun tokenAlternatiu(context: Context, compte: String, interactiu: Boolean): String {
        val account = android.accounts.Account(compte, TIPUS_COMPTE)
        repeat(3) {
            try {
                return withContext(Dispatchers.IO) { com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, ABAST_ALTERNATIU) }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                if (!interactiu) throw ErrorGoogle("Cal tornar a iniciar la sessió amb Google a Gestió > Configuració.", true)
                val intent = e.intent ?: throw ErrorGoogle("Google demana permís però no ha obert cap pantalla (${e.message}).", true)
                val (codi, _) = obre(intent)
                if (codi != android.app.Activity.RESULT_OK) throw ErrorGoogle("No s'ha donat permís a l'app.", true)
            } catch (e: com.google.android.gms.auth.GoogleAuthException) {
                throw ErrorGoogle(explicaErrorAlternatiu(e.message ?: ""), true)
            } catch (e: java.io.IOException) {
                throw ErrorGoogle("No hi ha connexió amb Google.")
            }
        }
        throw ErrorGoogle("No s'ha donat permís a l'app.", true)
    }

    internal fun explicaErrorAlternatiu(m: String): String = when {
        m.contains("UNREGISTERED_ON_API_CONSOLE") || m.contains("INVALID_AUDIENCE") ->
            "Google no reconeix l'app ($m). A Google Cloud cal un client OAuth de tipus Android amb el paquet cat.gastrobacus.consum i l'empremta SHA-1 de la guia, al mateix projecte on hi ha la pantalla de consentiment."
        m.contains("ServiceDisabled") -> "El compte de Google té els serveis desactivats ($m)."
        m.contains("BadAuthentication") || m.contains("NeedPermission") -> "Cal tornar a posar la contrasenya del compte a Ajustos > Comptes de la tauleta ($m)."
        else -> "Google ha retornat un error: $m"
    }

    /**
     * Retorna un token d'accés. Si [interactiu] és cert i cal permís, obre la pantalla de Google.
     * En segon pla (còpia de les 4) no pot demanar res: si cal permís, falla amb calIniciarSessio.
     */
    suspend fun token(context: Context, interactiu: Boolean): String {
        compteAlternatiu(context)?.let { return tokenAlternatiu(context, it, interactiu) }
        return try {
            tokenAutoritzacio(context, interactiu)
        } catch (e: ErrorGoogle) {
            if (!interactiu || e.cancelat) throw e
            // Segon intent amb el mètode alternatiu
            val compte = triaCompte()
            val t = try { tokenAlternatiu(context, compte, true) } catch (e2: ErrorGoogle) {
                throw ErrorGoogle("${e.message}\nAmb el mètode alternatiu: ${e2.message}", true)
            }
            prefs(context).edit().putString("compte", compte).apply()
            t
        }
    }

    private suspend fun tokenAutoritzacio(context: Context, interactiu: Boolean): String {
        val peticio = AuthorizationRequest.builder().setRequestedScopes(ABASTS).build()
        val r = try {
            Identity.getAuthorizationClient(context).authorize(peticio).await()
        } catch (e: Exception) {
            throw ErrorGoogle(explicaError(e), true)
        }
        if (!r.hasResolution()) return r.accessToken ?: throw ErrorGoogle("Google no ha retornat cap permís.", true)
        if (!interactiu) throw ErrorGoogle("Cal tornar a iniciar la sessió amb Google a Gestió > Configuració.", true)
        val obrir = llancador ?: throw ErrorGoogle("No es pot obrir la pantalla de Google ara mateix.", true)
        val d = CompletableDeferred<AuthorizationResult>()
        pendent = d
        // Amb la pantalla fixada Android no deixa obrir la finestra de Google: es desfixa i es torna a fixar en tornar
        cat.gastrobacus.consum.Quiosc.desfixa()
        obrir(IntentSenderRequest.Builder(r.pendingIntent!!.intentSender).build())
        val r2 = d.await()
        return r2.accessToken ?: throw ErrorGoogle("No s'ha donat permís a l'app.", true)
    }

    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun crida(req: Request): ByteArray = withContext(Dispatchers.IO) {
        try {
            http.newCall(req).execute().use { resp ->
                val cos = resp.body?.bytes() ?: ByteArray(0)
                if (resp.code == 401) throw ErrorGoogle("La sessió amb Google ha caducat. Torna a iniciar-la.", true)
                if (!resp.isSuccessful) throw ErrorGoogle("Google ha respost amb un error (${resp.code}): ${String(cos).take(200)}")
                cos
            }
        } catch (e: java.io.IOException) {
            throw ErrorGoogle("No hi ha connexió a internet.")
        }
    }

    suspend fun correuDelCompte(token: String): String {
        val r = crida(Request.Builder().url("https://www.googleapis.com/oauth2/v3/userinfo").header("Authorization", "Bearer $token").build())
        return json.parseToJsonElement(String(r)).jsonObject["email"]?.jsonPrimitive?.content ?: ""
    }

    // ---------- Gmail ----------

    data class Adjunt(val nom: String, val tipus: String, val dades: ByteArray)

    private fun capcalera(t: String) = "=?UTF-8?B?" + Base64.encodeToString(t.toByteArray(), Base64.NO_WRAP) + "?="
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.DEFAULT) // amb salts de línia, apte per a MIME

    suspend fun enviaCorreu(token: String, de: String, nomRemitent: String, per: String, assumpte: String, cos: String, adjunt: Adjunt? = null) {
        val limit = "gastrobacus_${System.nanoTime()}"
        val mime = buildString {
            append("From: ${capcalera(nomRemitent)} <$de>\r\n")
            append("To: $per\r\n")
            append("Subject: ${capcalera(assumpte)}\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: multipart/mixed; boundary=\"$limit\"\r\n\r\n")
            append("--$limit\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n")
            append(b64(cos.toByteArray())).append("\r\n")
            if (adjunt != null) {
                append("--$limit\r\nContent-Type: ${adjunt.tipus}; name=\"${adjunt.nom}\"\r\n")
                append("Content-Disposition: attachment; filename=\"${adjunt.nom}\"\r\nContent-Transfer-Encoding: base64\r\n\r\n")
                append(b64(adjunt.dades)).append("\r\n")
            }
            append("--$limit--\r\n")
        }
        val raw = Base64.encodeToString(mime.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val body = buildJsonObject { put("raw", raw) }.toString().toRequestBody("application/json".toMediaType())
        crida(
            Request.Builder().url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .header("Authorization", "Bearer $token").post(body).build(),
        )
    }

    // ---------- Drive ----------

    const val CARPETA = "GastroBacus - còpies de seguretat"

    data class FitxerCopia(val id: String, val nom: String, val creat: String, val mida: Long)

    private suspend fun carpeta(token: String): String {
        val q = "name = '$CARPETA' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=" + java.net.URLEncoder.encode(q, "UTF-8") + "&fields=files(id)"
        val r = json.parseToJsonElement(String(crida(Request.Builder().url(url).header("Authorization", "Bearer $token").build()))).jsonObject
        r["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content?.let { return it }
        val meta = buildJsonObject { put("name", CARPETA); put("mimeType", "application/vnd.google-apps.folder") }
        val creada = crida(
            Request.Builder().url("https://www.googleapis.com/drive/v3/files?fields=id").header("Authorization", "Bearer $token")
                .post(meta.toString().toRequestBody("application/json".toMediaType())).build(),
        )
        return json.parseToJsonElement(String(creada)).jsonObject["id"]!!.jsonPrimitive.content
    }

    suspend fun pujaCopia(token: String, nom: String, contingut: String) {
        val carpeta = carpeta(token)
        val gz = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(contingut.toByteArray()) } }.toByteArray()
        val meta = buildJsonObject {
            put("name", nom)
            put("mimeType", "application/gzip")
            put("parents", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(carpeta)) })
        }
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(gz.toRequestBody("application/gzip".toMediaType()))
            .build()
        crida(
            Request.Builder().url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
                .header("Authorization", "Bearer $token").post(body).build(),
        )
    }

    suspend fun llistaCopies(token: String): List<FitxerCopia> {
        val carpeta = carpeta(token)
        val q = "'$carpeta' in parents and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=" + java.net.URLEncoder.encode(q, "UTF-8") +
            "&orderBy=createdTime desc&pageSize=200&fields=files(id,name,createdTime,size)"
        val r = json.parseToJsonElement(String(crida(Request.Builder().url(url).header("Authorization", "Bearer $token").build()))).jsonObject
        return r["files"]?.jsonArray?.map {
            val o = it as JsonObject
            FitxerCopia(
                o["id"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content,
                o["createdTime"]?.jsonPrimitive?.content ?: "", o["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
            )
        } ?: emptyList()
    }

    suspend fun baixaCopia(token: String, id: String): String {
        val b = crida(Request.Builder().url("https://www.googleapis.com/drive/v3/files/$id?alt=media").header("Authorization", "Bearer $token").build())
        return GZIPInputStream(b.inputStream()).use { String(it.readBytes()) }
    }

    suspend fun eliminaCopia(token: String, id: String) {
        crida(Request.Builder().url("https://www.googleapis.com/drive/v3/files/$id").header("Authorization", "Bearer $token").delete().build())
    }
}
